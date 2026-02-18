# API Reference: commons-app-outbox

## Visão Geral

O módulo `commons-app-outbox` implementa o **Transactional Outbox Pattern** para garantir entrega confiável de mensagens em sistemas distribuídos.

**Problema resolvido:** Como garantir que uma transação de banco de dados e publicação de mensagem sejam atômicas?

**Solução:** Armazene a mensagem na mesma transação do banco, e processe-a depois de forma assíncrona.

**Dependência Maven:**
```xml
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-app-outbox</artifactId>
</dependency>
```

---

## Core Interfaces

### OutboxPublisher

Publica mensagens no outbox (dentro de uma transação).

```java
public interface OutboxPublisher {
    
    /**
     * Publica mensagem no outbox dentro da transação atual
     */
    <T> void publish(
        String topic,           // Tópico destino (ex: "orders.created")
        String aggregateId,     // ID do aggregate (para ordenação)
        T payload               // Payload da mensagem
    );
    
    /**
     * Publica mensagem com headers customizados
     */
    <T> void publish(
        String topic,
        String aggregateId,
        T payload,
        Map<String, String> headers
    );
}
```

### OutboxProcessor

Processa mensagens pendentes do outbox.

```java
public interface OutboxProcessor {
    
    /**
     * Processa próximo batch de mensagens pendentes
     * @return número de mensagens processadas
     */
    int process();
    
    /**
     * Processa até N mensagens pendentes
     */
    int process(int batchSize);
}
```

### OutboxMessage

Representa uma mensagem no outbox.

```java
public class OutboxMessage {
    private UUID id;
    private String topic;
    private String aggregateId;
    private String payload;
    private Map<String, String> headers;
    private OutboxStatus status;        // PENDING, PROCESSING, SENT, FAILED
    private int retryCount;
    private Instant createdAt;
    private Instant processedAt;
    private String errorMessage;
}
```

---

## Exemplo Completo: Order Service

### 1. Domain Event

```java
public record OrderCreated(
    String eventId,
    String orderId,
    String customerId,
    BigDecimal totalAmount,
    Instant occurredAt
) implements DomainEvent {
    
    @Override
    public String eventType() {
        return "OrderCreated";
    }
}
```

### 2. Application Service

```java
@Service
public class OrderApplicationService {
    
    private final OrderRepository orderRepository;
    private final OutboxPublisher outboxPublisher;
    
    @Transactional  // ⚠️ CRÍTICO: Transação engloba tudo!
    public Result<OrderId> createOrder(CreateOrderCommand command) {
        // 1. Cria o aggregate
        Order order = Order.create(
            OrderId.generate(),
            command.tenantId(),
            command.customerId(),
            AuditStamp.now()
        );
        
        // 2. Adiciona itens
        for (OrderItemRequest item : command.items()) {
            Result<Void> result = order.addItem(item.productId(), item.quantity());
            if (result.isFail()) {
                return Result.fail(result.problemOrNull());
            }
        }
        
        // 3. Submit
        Result<Void> submitResult = order.submit();
        if (submitResult.isFail()) {
            return Result.fail(submitResult.problemOrNull());
        }
        
        // 4. Persiste (dentro da transação)
        Result<Void> saveResult = orderRepository.save(order);
        if (saveResult.isFail()) {
            return Result.fail(saveResult.problemOrNull());
        }
        
        // 5. Publica no outbox (mesma transação!)
        List<DomainEvent> events = order.pullDomainEvents();
        for (DomainEvent event : events) {
            outboxPublisher.publish(
                "orders.events",              // topic
                order.id().value(),           // aggregateId (garante ordem)
                event                         // payload
            );
        }
        
        return Result.ok(order.id());
        
        // ✅ Se tudo OK: commit da transação persiste order + outbox messages
        // ❌ Se erro: rollback descarta tudo
    }
}
```

### 3. Outbox Processor (Scheduled)

```java
@Component
public class OutboxScheduler {
    
    private final OutboxProcessor outboxProcessor;
    private final MetricsFacade metrics;
    
    @Scheduled(fixedDelay = 1000)  // Roda a cada 1 segundo
    public void processOutbox() {
        try {
            int processed = outboxProcessor.process(100);  // Batch de 100
            
            if (processed > 0) {
                metrics.recordGauge("outbox.processed", processed);
            }
            
        } catch (Exception e) {
            log.error("Error processing outbox", e);
            metrics.incrementCounter("outbox.processing.error");
        }
    }
}
```

### 4. Message Publisher Implementation

```java
@Component
public class KafkaOutboxMessagePublisher implements MessagePublisher {
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    @Override
    public void publish(Message message) {
        try {
            String payload = objectMapper.writeValueAsString(message.payload());
            
            ProducerRecord<String, String> record = new ProducerRecord<>(
                message.topic(),
                message.key(),
                payload
            );
            
            // Adiciona headers
            message.headers().forEach((k, v) -> 
                record.headers().add(k, v.getBytes(StandardCharsets.UTF_8))
            );
            
            // Envia para Kafka
            kafkaTemplate.send(record).get(5, TimeUnit.SECONDS);
            
        } catch (Exception e) {
            throw new MessagePublishException("Failed to publish to Kafka", e);
        }
    }
}
```

---

## Configuration

### application.yml

```yaml
commons:
  outbox:
    # Processamento
    batch-size: 100
    processing-interval: 1000  # 1 segundo
    
    # Retry
    max-retries: 3
    retry-delay: 5000  # 5 segundos
    
    # Cleanup
    cleanup-enabled: true
    cleanup-after-days: 7  # Remove mensagens após 7 dias
    
    # Monitoring
    metrics-enabled: true
```

### Spring Configuration

```java
@Configuration
@EnableScheduling
public class OutboxConfiguration {
    
    @Bean
    public OutboxPublisher outboxPublisher(
        OutboxRepository outboxRepository,
        ObjectMapper objectMapper
    ) {
        return new DefaultOutboxPublisher(outboxRepository, objectMapper);
    }
    
    @Bean
    public OutboxProcessor outboxProcessor(
        OutboxRepository outboxRepository,
        MessagePublisher messagePublisher,
        OutboxProperties properties
    ) {
        return new DefaultOutboxProcessor(
            outboxRepository,
            messagePublisher,
            properties
        );
    }
}
```

---

## Database Schema

### PostgreSQL

```sql
CREATE TABLE outbox_messages (
    id UUID PRIMARY KEY,
    topic VARCHAR(255) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    payload JSONB NOT NULL,
    headers JSONB,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    processed_at TIMESTAMP,
    error_message TEXT,
    
    INDEX idx_outbox_status_created (status, created_at),
    INDEX idx_outbox_aggregate (aggregate_id)
);
```

---

## Advanced Patterns

### 1. Partitioned Processing

Processa por partição para paralelismo.

```java
@Component
public class PartitionedOutboxProcessor {
    
    private final OutboxRepository outboxRepository;
    private final List<MessagePublisher> publishers;
    
    @Scheduled(fixedDelay = 1000)
    public void process() {
        int partitionCount = publishers.size();
        
        // Cada thread processa uma partição
        IntStream.range(0, partitionCount)
            .parallel()
            .forEach(partition -> {
                List<OutboxMessage> messages = outboxRepository
                    .findPendingByPartition(partition, partitionCount, 100);
                
                MessagePublisher publisher = publishers.get(partition);
                
                messages.forEach(msg -> {
                    try {
                        publisher.publish(toMessage(msg));
                        outboxRepository.markAsSent(msg.id());
                    } catch (Exception e) {
                        outboxRepository.markAsFailed(msg.id(), e.getMessage());
                    }
                });
            });
    }
}
```

### 2. Dead Letter Queue

Mensagens que falharam múltiplas vezes vão para DLQ.

```java
@Component
public class OutboxProcessorWithDLQ {
    
    private final OutboxRepository outboxRepository;
    private final MessagePublisher mainPublisher;
    private final MessagePublisher dlqPublisher;
    private final int maxRetries = 3;
    
    public void process(OutboxMessage message) {
        try {
            mainPublisher.publish(toMessage(message));
            outboxRepository.markAsSent(message.id());
            
        } catch (Exception e) {
            int newRetryCount = message.retryCount() + 1;
            
            if (newRetryCount >= maxRetries) {
                // Envia para DLQ
                dlqPublisher.publish(Message.builder()
                    .topic("outbox.dlq")
                    .key(message.id().toString())
                    .payload(message)
                    .header("original-topic", message.topic())
                    .header("error", e.getMessage())
                    .build()
                );
                
                outboxRepository.markAsFailed(message.id(), e.getMessage());
                
            } else {
                // Incrementa retry count
                outboxRepository.incrementRetry(message.id());
            }
        }
    }
}
```

### 3. Message Deduplication

Garante idempotência no consumidor.

```java
@Component
public class IdempotentOutboxPublisher implements OutboxPublisher {
    
    private final OutboxRepository outboxRepository;
    
    @Override
    public <T> void publish(String topic, String aggregateId, T payload) {
        // Gera ID determinístico
        String messageId = generateMessageId(topic, aggregateId, payload);
        
        // Verifica se já existe
        if (outboxRepository.existsById(UUID.fromString(messageId))) {
            log.debug("Message already published: {}", messageId);
            return;  // Skip duplicata
        }
        
        // Publica
        OutboxMessage message = OutboxMessage.builder()
            .id(UUID.fromString(messageId))
            .topic(topic)
            .aggregateId(aggregateId)
            .payload(serialize(payload))
            .status(OutboxStatus.PENDING)
            .createdAt(Instant.now())
            .build();
        
        outboxRepository.save(message);
    }
    
    private String generateMessageId(String topic, String aggregateId, Object payload) {
        String content = topic + ":" + aggregateId + ":" + serialize(payload);
        return UUID.nameUUIDFromBytes(content.getBytes()).toString();
    }
}
```

---

## Monitoring

### Metrics

```java
@Component
public class OutboxMetrics {
    
    private final OutboxRepository outboxRepository;
    private final MetricsFacade metrics;
    
    @Scheduled(fixedRate = 10000)  // A cada 10 segundos
    public void reportMetrics() {
        // Mensagens pendentes
        long pending = outboxRepository.countByStatus(OutboxStatus.PENDING);
        metrics.recordGauge("outbox.pending", pending);
        
        // Mensagens falhadas
        long failed = outboxRepository.countByStatus(OutboxStatus.FAILED);
        metrics.recordGauge("outbox.failed", failed);
        
        // Lag (mensagem mais antiga pendente)
        outboxRepository.findOldestPending().ifPresent(message -> {
            Duration lag = Duration.between(message.createdAt(), Instant.now());
            metrics.recordTimer("outbox.lag", lag);
        });
    }
}
```

### Health Check

```java
@Component
public class OutboxHealthCheck implements HealthIndicator {
    
    private final OutboxRepository outboxRepository;
    
    @Override
    public Health health() {
        long pending = outboxRepository.countByStatus(OutboxStatus.PENDING);
        long failed = outboxRepository.countByStatus(OutboxStatus.FAILED);
        
        Health.Builder builder = Health.up();
        
        if (pending > 10000) {
            builder = Health.down()
                .withDetail("reason", "Too many pending messages");
        }
        
        if (failed > 1000) {
            builder = builder.status("DEGRADED")
                .withDetail("reason", "High failure rate");
        }
        
        return builder
            .withDetail("pending", pending)
            .withDetail("failed", failed)
            .build();
    }
}
```

---

## Testing

```java
@SpringBootTest
@Transactional
class OutboxIntegrationTest {
    
    @Autowired
    private OrderApplicationService orderService;
    
    @Autowired
    private OutboxRepository outboxRepository;
    
    @Autowired
    private OutboxProcessor outboxProcessor;
    
    @MockBean
    private MessagePublisher messagePublisher;
    
    @Test
    void shouldStoreMessageInOutboxWhenOrderCreated() {
        // Given
        CreateOrderCommand command = new CreateOrderCommand(...);
        
        // When
        Result<OrderId> result = orderService.createOrder(command);
        
        // Then
        assertThat(result.isOk()).isTrue();
        
        List<OutboxMessage> messages = outboxRepository.findPending(10);
        assertThat(messages).hasSize(1);
        
        OutboxMessage message = messages.get(0);
        assertThat(message.topic()).isEqualTo("orders.events");
        assertThat(message.status()).isEqualTo(OutboxStatus.PENDING);
    }
    
    @Test
    void shouldProcessOutboxMessages() {
        // Given - mensagem no outbox
        OutboxMessage message = createPendingMessage();
        outboxRepository.save(message);
        
        // When
        int processed = outboxProcessor.process();
        
        // Then
        assertThat(processed).isEqualTo(1);
        verify(messagePublisher).publish(any());
        
        OutboxMessage updated = outboxRepository.findById(message.id()).orElseThrow();
        assertThat(updated.status()).isEqualTo(OutboxStatus.SENT);
    }
}
```

---

## Best Practices

### ✅ DO

```java
// ✅ Sempre use transação que engloba persistência + outbox
@Transactional
public void createOrder() {
    orderRepository.save(order);      // 1. Persiste
    outboxPublisher.publish(event);   // 2. Outbox (mesma transação)
}

// ✅ Use aggregateId para garantir ordenação
outboxPublisher.publish(
    "orders.events",
    order.id().value(),  // ✅ Garante ordem por order
    event
);

// ✅ Implemente idempotência no consumidor
@KafkaListener(topics = "orders.events")
public void handle(OrderCreated event) {
    if (processedEvents.contains(event.eventId())) {
        return;  // Já processado
    }
    // Processa...
}

// ✅ Monitore lag do outbox
metrics.recordGauge("outbox.lag", lagInSeconds);
```

### ❌ DON'T

```java
// ❌ NÃO publique direto no Kafka dentro da transação
@Transactional
public void createOrder() {
    orderRepository.save(order);
    kafkaTemplate.send(...);  // ❌ Se rollback, mensagem já foi!
}

// ❌ NÃO ignore retry limits
while (true) {
    try {
        publish(message);
        break;
    } catch (Exception e) {
        // ❌ Vai travar forever!
    }
}

// ❌ NÃO deixe outbox crescer infinitamente
// ✅ Configure cleanup de mensagens antigas
```

---

## Ver Também

- [Domain Events Guide](../guides/domain-events.md)
- [Messaging Patterns](../guides/messaging.md)
- [Observability](../guides/observability.md)
- [commons-ports-messaging](../api-reference/ports/messaging.md)
