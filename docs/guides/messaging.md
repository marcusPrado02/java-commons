# Guia: Messaging Patterns

## Visão Geral

Guia completo de padrões de mensageria para arquiteturas event-driven com Kafka, RabbitMQ e Azure Service Bus.

---

## 🎯 Quando Usar Mensageria

### Use Cases

✅ **Comunicação Assíncrona** - Desacoplar serviços  
✅ **Event-Driven Architecture** - Reagir a eventos de domínio  
✅ **Load Leveling** - Absorver picos de carga  
✅ **Fan-out** - Um evento, múltiplos consumers  
✅ **Saga Pattern** - Transações distribuídas  

❌ **Request-Response Síncrono** - Use REST/gRPC  
❌ **Queries** - Use APIs diretas  
❌ **Dados em tempo real** - Considere WebSockets  

---

## 📨 Message Types

### 1. Events

Fatos que aconteceram no passado.

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

// Publicação
publisher.publish(Message.builder()
    .topic("orders.events")
    .key(order.id().value())
    .payload(new OrderCreated(...))
    .build());
```

### 2. Commands

Ações a serem executadas.

```java
public record ProcessPayment(
    String commandId,
    String orderId,
    BigDecimal amount,
    String paymentMethod
) {
    public String commandType() {
        return "ProcessPayment";
    }
}

// Publicação
publisher.publish(Message.builder()
    .topic("payment.commands")
    .key(command.orderId())
    .payload(command)
    .build());
```

### 3. Queries

Requisições de dados (menos comum).

```java
public record GetOrderDetails(
    String queryId,
    String orderId,
    String replyTo
) {}
```

---

## 🔄 Message Patterns

### 1. Fire-and-Forget

Publica e esquece.

```java
@Service
public class NotificationService {
    
    private final MessagePublisher publisher;
    
    public void sendWelcomeEmail(User user) {
        publisher.publish(Message.builder()
            .topic("notifications.email")
            .key(user.id().value())
            .payload(new SendEmailCommand(
                user.email(),
                "Welcome!",
                "Welcome to our platform"
            ))
            .build());
        
        // Não espera resposta
    }
}
```

### 2. Request-Reply

```java
@Service
public class OrderQueryService {
    
    private final MessagePublisher publisher;
    private final Map<String, CompletableFuture<OrderDetails>> pendingQueries = 
        new ConcurrentHashMap<>();
    
    public CompletableFuture<OrderDetails> getOrderDetails(OrderId orderId) {
        String queryId = UUID.randomUUID().toString();
        String replyTopic = "order-queries-replies." + queryId;
        
        CompletableFuture<OrderDetails> future = new CompletableFuture<>();
        pendingQueries.put(queryId, future);
        
        // Publica query
        publisher.publish(Message.builder()
            .topic("order.queries")
            .key(orderId.value())
            .payload(new GetOrderDetails(queryId, orderId.value(), replyTopic))
            .build());
        
        return future.orTimeout(5, TimeUnit.SECONDS);
    }
    
    @KafkaListener(topics = "order-queries-replies.#{T(java.util.UUID).randomUUID()}")
    public void handleReply(String replyJson) {
        OrderDetails details = deserialize(replyJson);
        
        CompletableFuture<OrderDetails> future = pendingQueries.remove(details.queryId());
        if (future != null) {
            future.complete(details);
        }
    }
}
```

### 3. Pub/Sub (Fan-out)

Um evento, múltiplos consumers.

```java
// Publisher
publisher.publish(Message.builder()
    .topic("orders.created")
    .key(order.id().value())
    .payload(new OrderCreated(...))
    .build());

// Consumer 1: Inventory
@KafkaListener(topics = "orders.created", groupId = "inventory-service")
public void handleOrderCreated(OrderCreated event) {
    inventoryService.reserveStock(event);
}

// Consumer 2: Email
@KafkaListener(topics = "orders.created", groupId = "email-service")
public void handleOrderCreated(OrderCreated event) {
    emailService.sendConfirmation(event);
}

// Consumer 3: Analytics
@KafkaListener(topics = "orders.created", groupId = "analytics-service")
public void handleOrderCreated(OrderCreated event) {
    analyticsService.trackOrder(event);
}
```

### 4. Competing Consumers

Múltiplas instâncias do mesmo consumer (load balancing).

```java
// Instância 1
@KafkaListener(topics = "order.processing", groupId = "order-processor")
public void process(OrderCommand command) {
    // Processa
}

// Instância 2 (mesma aplicação, outro pod)
@KafkaListener(topics = "order.processing", groupId = "order-processor")
public void process(OrderCommand command) {
    // Processa
}

// Kafka distribui mensagens entre as instâncias
```

---

## 🔐 Message Guarantees

### At-Most-Once

Mensagem pode ser perdida, nunca duplicada.

```java
@KafkaListener(topics = "logs", groupId = "logger")
public void handleLog(LogMessage log) {
    logger.info(log.message());
    // Não faz commit - pode perder mensagens
}
```

### At-Least-Once

Mensagem nunca é perdida, pode ser duplicada.

```java
@KafkaListener(topics = "orders.created")
public void handleOrderCreated(OrderCreated event) {
    try {
        processOrder(event);
        // Commit automático após sucesso
    } catch (Exception e) {
        // Requeue - pode processar duplicado
        throw e;
    }
}
```

### Exactly-Once

Mensagem processada exatamente uma vez (requer idempotência).

```java
@KafkaListener(topics = "payments.process")
@Transactional
public void processPayment(ProcessPaymentCommand command) {
    // 1. Verifica idempotência
    if (processedCommands.exists(command.commandId())) {
        return;  // Já processado
    }
    
    // 2. Processa
    paymentService.process(command);
    
    // 3. Marca como processado (mesma transação)
    processedCommands.save(command.commandId());
    
    // Commit transacional: Kafka offset + DB transaction
}
```

---

## 📊 Kafka Patterns

### 1. Partitioning Strategy

```java
// Por aggregate ID (garante ordem)
publisher.publish(Message.builder()
    .topic("orders.events")
    .key(order.id().value())  // ✅ Mesma key = mesma partição
    .payload(event)
    .build());

// Por tenant ID (isolamento)
publisher.publish(Message.builder()
    .topic("users.events")
    .key(user.tenantId().value())  // ✅ Mensagens do mesmo tenant na mesma partição
    .payload(event)
    .build());
```

### 2. Compaction

```java
// Topics com compaction (último estado)
@Configuration
public class KafkaTopicConfig {
    
    @Bean
    public NewTopic userStateTopic() {
        return TopicBuilder.name("user.state")
            .partitions(10)
            .replicas(3)
            .config(TopicConfig.CLEANUP_POLICY_CONFIG, "compact")  // ✅ Compaction
            .build();
    }
}
```

### 3. Dead Letter Queue

```java
@KafkaListener(topics = "orders.process")
public void processOrder(OrderCommand command) {
    try {
        orderService.process(command);
    } catch (Exception e) {
        log.error("Failed to process order, sending to DLQ", e);
        
        publisher.publish(Message.builder()
            .topic("orders.process.dlq")  // Dead Letter Queue
            .key(command.orderId())
            .payload(command)
            .header("error", e.getMessage())
            .header("original-topic", "orders.process")
            .build());
    }
}
```

---

## 🐰 RabbitMQ Patterns

### 1. Work Queue

```java
// Publisher
channel.basicPublish(
    "",                   // default exchange
    "work.queue",         // queue name
    MessageProperties.PERSISTENT_TEXT_PLAIN,
    message.getBytes()
);

// Consumer (competing)
@RabbitListener(queues = "work.queue")
public void processWork(String message) {
    // Processa
}
```

### 2. Topic Exchange

```java
// Publisher
publisher.publish(Message.builder()
    .exchange("events")
    .routingKey("order.created.premium")  // Routing key
    .payload(event)
    .build());

// Consumer 1: Todas orders
@RabbitListener(bindings = @QueueBinding(
    value = @Queue("all-orders"),
    exchange = @Exchange(value = "events", type = "topic"),
    key = "order.#"  // Wildcard
))
public void handleAllOrders(OrderEvent event) {}

// Consumer 2: Apenas premium
@RabbitListener(bindings = @QueueBinding(
    value = @Queue("premium-orders"),
    exchange = @Exchange(value = "events", type = "topic"),
    key = "order.*.premium"
))
public void handlePremiumOrders(OrderEvent event) {}
```

### 3. Priority Queue

```java
@RabbitListener(queues = "tasks", containerFactory = "priorityFactory")
public void processTask(Task task) {
    // Tarefas com maior prioridade primeiro
}

// Publicação
channel.basicPublish(
    "",
    "tasks",
    new AMQP.BasicProperties.Builder()
        .priority(9)  // 0-9, maior = mais prioritário
        .build(),
    message.getBytes()
);
```

---

## ⚡ Performance Patterns

### 1. Batching

```java
@Service
public class BatchedMessagePublisher {
    
    private final List<Message> batch = new ArrayList<>();
    private final int batchSize = 100;
    
    public synchronized void publish(Message message) {
        batch.add(message);
        
        if (batch.size() >= batchSize) {
            flush();
        }
    }
    
    private void flush() {
        if (batch.isEmpty()) return;
        
        kafkaTemplate.send(batch);
        batch.clear();
    }
    
    @Scheduled(fixedDelay = 1000)
    public void flushPeriodically() {
        flush();
    }
}
```

### 2. Parallel Processing

```java
@KafkaListener(
    topics = "orders.process",
    concurrency = "10"  // 10 consumer threads
)
public void processOrder(OrderCommand command) {
    orderService.process(command);
}
```

### 3. Buffering

```java
@Component
public class BufferedConsumer {
    
    private final BlockingQueue<Message> buffer = new LinkedBlockingQueue<>(10000);
    
    @KafkaListener(topics = "events")
    public void receive(Message message) {
        buffer.offer(message);  // Non-blocking
    }
    
    @Scheduled(fixedDelay = 100)
    public void processBuffer() {
        List<Message> messages = new ArrayList<>();
        buffer.drainTo(messages, 100);  // Processa até 100 por vez
        
        messages.parallelStream().forEach(this::process);
    }
}
```

---

## Best Practices

### ✅ DO

```java
// ✅ Use idempotency keys
message.header("idempotency-key", UUID.randomUUID().toString());

// ✅ Propague correlation ID
message.header("correlation-id", RequestContext.getCorrelationId());

// ✅ Versione schemas
message.header("schema-version", "v2");

// ✅ Implemente retry com backoff
@RetryableTopic(
    attempts = "3",
    backoff = @Backoff(delay = 1000, multiplier = 2.0)
)

// ✅ Monitore lag
metrics.recordGauge("kafka.consumer.lag", consumerLag);
```

### ❌ DON'T

```java
// ❌ NÃO bloqueie consumer threads
@KafkaListener
public void handle(Message msg) {
    Thread.sleep(10000);  // ❌
}

// ❌ NÃO perca correlation ID
// ❌ NÃO ignore dead letter queues
// ❌ NÃO use mensageria para queries síncronas
```

---

## Ver Também

- [Domain Events](domain-events.md)
- [Transactional Outbox](../api-reference/app-outbox.md)
- [Kafka Adapter](../api-reference/adapters/messaging-kafka.md)
- [Observability](observability.md)
