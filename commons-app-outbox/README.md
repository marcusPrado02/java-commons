# commons-app-outbox

Pattern de Transactional Outbox para garantir consistência eventual entre operações de banco de dados e publicação de eventos em sistemas distribuídos.

## 📋  Visão Geral

O padrão **Transactional Outbox** resolve o problema clássico de dual-writes em sistemas distribuídos: garantir que mudanças no banco de dados e publicação de eventos ocorram atomicamente.

### Problema Resolvido

Sem o pattern Outbox, você pode ter:
- ✗ Evento publicado mas transação de DB falhou (rollback)
- ✗ Transação commitada mas evento não publicado (falha de rede)
- ✗ Dados inconsistentes entre sistemas

Com Outbox:
- ✓ At-least-once delivery garantido
- ✓ Atomicidade entre DB e eventos
- ✓ Resiliência a falhas de rede
- ✓ Ordenação de eventos preservada por agregado

## 🎯 Casos de Uso

- **Event-Driven Architecture**: Publicação de domain events de forma confiável
- **Saga Orchestration**: Coordenação de transações distribuídas
- **CDC (Change Data Capture)**: Captura de mudanças para data pipelines
- **Notification Systems**: Envio garantido de notificações
- **Audit Logging**: Registro imutável de eventos de negócio

## 📦 Componentes

### Model

```java
public record OutboxMessage(
    OutboxMessageId id,
    String aggregateType,      // Ex: "Order", "Payment"
    String aggregateId,        // Ex: "order-123"
    String eventType,          // Ex: "OrderCreated"
    String topic,              // Ex: "orders.created"
    OutboxPayload payload,     // Serialized event
    Map<String, String> headers,
    Instant occurredAt,
    OutboxStatus status,
    int attempts
) {}
```

### OutboxStatus State Machine

```
PENDING ──────┐               Nova mensagem aguardando processamento
   ↓          │
PROCESSING    │               Sendo processada por um worker
   ↓          │
PUBLISHED     │               Publicada com sucesso no message broker
              │
FAILED ───────┘ (retry)       Falha temporária, pode ser retentada
   ↓
DEAD                          Máximo de tentativas excedidas (DLQ)
```

### OutboxRepositoryPort

Interface para persistência do Outbox:

```java
void append(OutboxMessage message);                    // Adicionar nova mensagem
List<OutboxMessage> fetchBatch(OutboxStatus, limit);   // Buscar mensagens para processar
boolean markProcessing(id, processingAt);              // Marcar como processando (thread-safe)
void markPublished(id, publishedAt);                   // Marcar como publicada
void markFailed(id, reason, attempts);                 // Marcar como falhada
void markRetryable(id, reason, attempts);              // Retentar mensagem falhada
void markDead(id, reason, attempts);                   // Mover para DLQ
Optional<OutboxMessage> findById(id);                  // Buscar por ID
long countByStatus(status);                            // Contar por status
int deletePublishedOlderThan(instant);                 // Cleanup de mensagens antigas
```

## 🚀 Como Usar

### 1. Adicionar ao Domain Service

```java
@Transactional
public Order createOrder(CreateOrderCommand cmd) {
    // 1. Persistir agregado
    Order order = Order.create(cmd);
    orderRepository.save(order);
    
    // 2. Criar outbox message na MESMA transação
    OutboxMessage message = new OutboxMessage(
        new OutboxMessageId(UUID.randomUUID().toString()),
        "Order",
        order.getId(),
        "OrderCreated",
        "orders.created",
        new OutboxPayload("application/json", serialize(order.getEvent())),
        Map.of("trace-id", MDC.get("trace-id")),
        Instant.now(),
        OutboxStatus.PENDING,
        0
    );
    
    outboxRepository.append(message);  // Mesmo TX!
    
    return order;
}
```

### 2. Configurar Outbox Processor

```java
@Component
public class OutboxProcessor {
    
    @Scheduled(fixedDelay = 1000)  // Polling a cada 1s
    @Transactional
    public void processOutbox() {
        List<OutboxMessage> messages = outboxRepository
            .fetchBatch(OutboxStatus.PENDING, 100);
        
        for (OutboxMessage msg : messages) {
            // Marcar como PROCESSING (thread-safe!)
            boolean acquired = outboxRepository
                .markProcessing(msg.id(), Instant.now());
            
            if (!acquired) continue;  // Outra instância já pegou
            
            try {
                // Publicar no broker
                messagingPort.publish(msg.topic(), msg.payload());
                
                // Marcar como publicada
                outboxRepository.markPublished(msg.id(), Instant.now());
                
            } catch (Exception e) {
                int newAttempts = msg.attempts() + 1;
                
                if (newAttempts >= MAX_RETRIES) {
                    // Mover para DLQ
                    outboxRepository.markDead(msg.id(), e.getMessage(), newAttempts);
                } else {
                    // Agendar retry
                    outboxRepository.markFailed(msg.id(), e.getMessage(), newAttempts);
                }
            }
        }
    }
}
```

### 3. Configurar Retry Strategy

```java
@Component
public class OutboxRetryScheduler {
    
    @Scheduled(fixedDelay = 60000)  // A cada 1 minuto
    @Transactional
    public void retryFailed() {
        List<OutboxMessage> failedMessages = outboxRepository
            .fetchBatch(OutboxStatus.FAILED, 50);
        
        Instant now = Instant.now();
        
        for (OutboxMessage msg : failedMessages) {
            // Exponential backoff: 1min, 2min, 4min, 8min...
            long backoffSeconds = (long) Math.pow(2, msg.attempts()) * 60;
            Instant nextRetry = msg.occurredAt().plusSeconds(backoffSeconds);
            
            if (now.isAfter(nextRetry)) {
                // Marcar como PENDING para retry
                outboxRepository.markRetryable(
                    msg.id(), 
                    "Retry after backoff", 
                    msg.attempts()
                );
            }
        }
    }
}
```

### 4. Cleanup de Mensagens Antigas

```java
@Component
public class OutboxCleanup {
    
    @Scheduled(cron = "0 0 2 * * *")  // Todo dia às 2h
    @Transactional
    public void cleanupOldMessages() {
        Instant cutoff = Instant.now().minus(7, ChronoUnit.DAYS);
        
        int deleted = outboxRepository.deletePublishedOlderThan(cutoff);
        
        log.info("Deleted {} old outbox messages", deleted);
    }
}
```

## 🔧 Implementações Disponíveis

### JPA Adapter (PostgreSQL, MySQL, etc)

```xml
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-adapters-persistence-jpa</artifactId>
</dependency>
```

```java
@Bean
public OutboxRepositoryPort outboxRepository(EntityManager em) {
    return new JpaOutboxRepositoryAdapter(em);
}
```

Schema SQL criado automaticamente com Hibernate:

```sql
CREATE TABLE commons_outbox (
    id VARCHAR(64) PRIMARY KEY,
    aggregate_type VARCHAR(120) NOT NULL,
    aggregate_id VARCHAR(120) NOT NULL,
    event_type VARCHAR(180) NOT NULL,
    topic VARCHAR(180) NOT NULL,
    content_type VARCHAR(120) NOT NULL,
    payload BYTEA NOT NULL,
    headers_json TEXT,
    occurred_at TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL,
    attempts INTEGER NOT NULL,
    processing_at TIMESTAMP,
    published_at TIMESTAMP,
    last_error VARCHAR(500)
);

CREATE INDEX idx_outbox_status ON commons_outbox(status);
CREATE INDEX idx_outbox_occurred_at ON commons_outbox(occurred_at);
```

## 🎭 Concurrency & Thread Safety

### Pessimistic Locking

O método `markProcessing` usa **SELECT FOR UPDATE** para garantir que apenas um worker processe cada mensagem:

```java
@Override
public boolean markProcessing(OutboxMessageId id, Instant processingAt) {
    try {
        OutboxMessageEntity e = em.createQuery(
            "select o from OutboxMessageEntity o " +
            "where o.id = :id and o.status = :status",
            OutboxMessageEntity.class)
            .setParameter("id", id.value())
            .setParameter("status", OutboxStatus.PENDING)
            .setLockMode(LockModeType.PESSIMISTIC_WRITE)  // 🔒 Lock!
            .getSingleResult();
        
        e.setStatus(OutboxStatus.PROCESSING);
        e.setProcessingAt(processingAt);
        e.setAttempts(e.getAttempts() + 1);
        em.merge(e);
        return true;
    } catch (NoResultException e) {
        return false;  // Já processada por outro worker
    }
}
```

### Scale-Out Seguro

Múltiplas instâncias podem rodar simultaneamente:

```
Instance 1: markProcessing(msg-1) ✓ acquired lock
Instance 2: markProcessing(msg-1) ✗ lock wait → NoResultException → skip
Instance 3: markProcessing(msg-2) ✓ acquired lock
```

## 📊 Monitoramento

### Métricas Recomendadas

```java
@Component
public class OutboxMetrics {
    
    @Scheduled(fixedDelay = 30000)
    public void collectMetrics() {
        meterRegistry.gauge("outbox.pending", 
            outboxRepository.countByStatus(OutboxStatus.PENDING));
        
        meterRegistry.gauge("outbox.processing", 
            outboxRepository.countByStatus(OutboxStatus.PROCESSING));
        
        meterRegistry.gauge("outbox.failed", 
            outboxRepository.countByStatus(OutboxStatus.FAILED));
        
        meterRegistry.gauge("outbox.dead", 
            outboxRepository.countByStatus(OutboxStatus.DEAD));
    }
}
```

### Alertas

- **High PENDING count**: Workers não estão processando rápido o suficiente
- **High FAILED count**: Problemas com message broker ou serialização
- **DEAD messages**: Investigar causa raiz (DLQ)
- **PROCESSING stuck**: Workers podem ter crasheado (timeout?)

## ⚙️ Configuração Avançada

### Reprocessamento de Mensagens Travadas

Detectar mensagens em PROCESSING há muito tempo:

```java
@Scheduled(fixedDelay = 300000)  // A cada 5 minutos
@Transactional
public void detectStuckMessages() {
    Instant threshold = Instant.now().minus(10, ChronoUnit.MINUTES);
    
    em.createQuery(
        "update OutboxMessageEntity o " +
        "set o.status = :pending " +
        "where o.status = :processing " +
        "and o.processingAt < :threshold")
        .setParameter("pending", OutboxStatus.PENDING)
        .setParameter("processing", OutboxStatus.PROCESSING)
        .setParameter("threshold", threshold)
        .executeUpdate();
}
```

## 🧪 Testes

11 testes de integração com Testcontainers PostgreSQL (35.23s):

```bash
./mvnw test -pl commons-adapters-persistence-jpa -Dtest=JpaOutboxRepositoryAdapterTest
```

Testes incluem:
- ✓ Append de mensagens
- ✓ Fetch batch por status
- ✓ Mark processing (thread-safe)
- ✓ Concorrência (múltiplas threads)
- ✓ Transições de estado completas
- ✓ Count by status
- ✓ Delete old messages

## 📚 Referências

- [Transactional Outbox Pattern (Microservices.io)](https://microservices.io/patterns/data/transactional-outbox.html)
- [Outbox Pattern in Spring Boot](https://www.baeldung.com/spring-transactional-outbox-pattern)
- [Debezium Outbox Connector](https://debezium.io/documentation/reference/transformations/outbox-event-router.html)

## 🔜 Próximos Passos

- [ ] CDC com Debezium para processamento mais eficiente
- [ ] Particionamento por aggregate_id para melhor performance
- [ ] Suporte para MongoDB
- [ ] Dead Letter Queue separada
- [ ] Dashboard de monitoramento
