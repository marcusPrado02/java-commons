# Kafka Adapter Guide

## Overview

This guide covers the **Kafka adapter** (`commons-adapters-messaging-kafka`) for event-driven microservices with Apache Kafka.

**Key Features:**
- Producer with acknowledgments
- Consumer with offset management
- Partitioning strategies
- Consumer groups
- Exactly-once semantics
- Dead letter topics
- Monitoring & metrics

---

## 📦 Installation

```xml
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-adapters-messaging-kafka</artifactId>
    <version>${commons.version}</version>
</dependency>
```

---

## ⚙️ Configuration

### Kafka Properties

```yaml
# application.yml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    
    # Producer Configuration
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
      acks: all  # Wait for all replicas
      retries: 3
      compression-type: lz4
      batch-size: 16384  # 16KB
      linger-ms: 10      # Wait up to 10ms to batch
      buffer-memory: 33554432  # 32MB
      
      properties:
        max.in.flight.requests.per.connection: 5
        enable.idempotence: true  # Exactly-once producer
    
    # Consumer Configuration
    consumer:
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      group-id: order-service
      auto-offset-reset: earliest  # earliest | latest
      enable-auto-commit: false    # Manual commit for reliability
      max-poll-records: 500
      
      properties:
        isolation.level: read_committed  # For transactional producers
        session.timeout.ms: 30000
        heartbeat.interval.ms: 10000
        max.poll.interval.ms: 300000  # 5 minutes
```

### Spring Boot Auto-Configuration

```java
@Configuration
@EnableKafka
public class KafkaConfig {
    
    @Bean
    public KafkaMessagePublisher kafkaPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        return new KafkaMessagePublisherAdapter(kafkaTemplate);
    }
    
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
        ConsumerFactory<String, String> consumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(3);  // 3 consumer threads
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        
        // Error handling
        factory.setCommonErrorHandler(
            new DefaultErrorHandler(
                new FixedBackOff(1000L, 3)  // 3 retries with 1s delay
            )
        );
        
        return factory;
    }
}
```

---

## 📤 Producer Examples

### Basic Publishing

```java
@Service
public class OrderEventPublisher {
    
    private final MessagePublisher messagePublisher;
    private final Serializer jsonSerializer;
    
    public Result<Void> publishOrderCreated(OrderCreatedEvent event) {
        return jsonSerializer.serializeToString(event)
            .andThen(json -> {
                Message message = Message.builder()
                    .topic("order-events")
                    .key(event.orderId().value())  // Partition by orderId
                    .body(json)
                    .header("eventType", "OrderCreated")
                    .header("eventVersion", "1.0")
                    .header("timestamp", event.occurredAt().toString())
                    .build();
                
                return messagePublisher.publish(message);
            })
            .andThen(messageId -> {
                log.info("Event published")
                    .field("eventId", event.eventId().value())
                    .field("messageId", messageId.value())
                    .field("topic", "order-events")
                    .log();
            })
            .mapToVoid();
    }
}
```

### Transactional Publishing

```java
@Service
public class TransactionalOrderService {
    
    private final OrderRepository orderRepository;
    private final MessagePublisher messagePublisher;
    
    @Transactional
    public Result<Order> createOrder(CreateOrderRequest request) {
        // Save order
        Result<Order> orderResult = Order.create(request)
            .andThen(order -> orderRepository.save(order));
        
        if (orderResult.isError()) {
            return orderResult;
        }
        
        Order order = orderResult.get();
        
        // Publish event (within same transaction via Outbox)
        OrderCreatedEvent event = order.events().stream()
            .filter(e -> e instanceof OrderCreatedEvent)
            .map(e -> (OrderCreatedEvent) e)
            .findFirst()
            .orElseThrow();
        
        return publishOrderCreated(event)
            .map(v -> order);
    }
}
```

### Batch Publishing

```java
@Service
public class BatchEventPublisher {
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    
    public Result<Void> publishBatch(List<DomainEvent> events) {
        List<ProducerRecord<String, String>> records = events.stream()
            .map(event -> new ProducerRecord<>(
                "domain-events",
                event.aggregateId().value(),  // Key
                serializeEvent(event)          // Value
            ))
            .toList();
        
        // Send all records
        List<CompletableFuture<SendResult<String, String>>> futures = records.stream()
            .map(record -> kafkaTemplate.send(record))
            .toList();
        
        // Wait for all
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();
            return Result.ok();
        } catch (Exception e) {
            return Result.error(Error.of("BATCH_PUBLISH_ERROR", e.getMessage()));
        }
    }
}
```

---

## 📥 Consumer Examples

### Basic Consumer

```java
@Service
public class OrderEventConsumer {
    
    private final OrderService orderService;
    private final Serializer jsonSerializer;
    
    @KafkaListener(
        topics = "order-events",
        groupId = "order-processor",
        concurrency = "3"  // 3 parallel consumers
    )
    public void consumeOrderEvents(
        ConsumerRecord<String, String> record,
        Acknowledgment acknowledgment
    ) {
        try {
            // Deserialize event
            Result<OrderCreatedEvent> eventResult = jsonSerializer
                .deserializeFromString(record.value(), OrderCreatedEvent.class);
            
            if (eventResult.isError()) {
                log.error("Failed to deserialize event")
                    .error(eventResult.getError())
                    .field("offset", record.offset())
                    .log();
                
                acknowledgment.acknowledge();  // Skip invalid message
                return;
            }
            
            OrderCreatedEvent event = eventResult.get();
            
            // Process event (idempotent)
            Result<Void> processResult = orderService.processOrderCreated(event);
            
            if (processResult.isError()) {
                log.error("Failed to process event")
                    .error(processResult.getError())
                    .field("eventId", event.eventId().value())
                    .log();
                
                // Don't acknowledge - will retry
                throw new RuntimeException("Processing failed");
            }
            
            // Success - commit offset
            acknowledgment.acknowledge();
            
            log.info("Event processed")
                .field("eventId", event.eventId().value())
                .field("partition", record.partition())
                .field("offset", record.offset())
                .log();
            
        } catch (Exception e) {
            log.error("Consumer error")
                .exception(e)
                .field("topic", record.topic())
                .field("partition", record.partition())
                .field("offset", record.offset())
                .log();
            
            throw e;  // Trigger retry
        }
    }
}
```

### Consumer with Idempotency

```java
@Service
public class IdempotentOrderConsumer {
    
    private final OrderService orderService;
    private final IdempotencyService idempotencyService;
    
    @KafkaListener(topics = "order-events")
    public void consume(
        ConsumerRecord<String, String> record,
        Acknowledgment acknowledgment
    ) {
        // Extract idempotency key from headers
        String eventId = new String(
            record.headers().lastHeader("eventId").value()
        );
        
        IdempotencyKey key = IdempotencyKey.of(
            "order-event-consumer",
            eventId
        );
        
        // Check if already processed
        if (idempotencyService.isProcessed(key)) {
            log.info("Event already processed (idempotent)")
                .field("eventId", eventId)
                .field("offset", record.offset())
                .log();
            
            acknowledgment.acknowledge();
            return;
        }
        
        try {
            // Process event
            OrderCreatedEvent event = deserialize(record.value());
            orderService.processOrderCreated(event);
            
            // Mark as processed
            idempotencyService.markProcessed(key);
            
            // Commit offset
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            log.error("Processing failed").exception(e).log();
            throw e;
        }
    }
}
```

---

## 🎯 Partitioning Strategies

### By Entity ID

```java
@Service
public class PartitionedPublisher {
    
    private final MessagePublisher messagePublisher;
    
    public Result<Void> publishOrderEvent(OrderEvent event) {
        // All events for same order go to same partition
        Message message = Message.builder()
            .topic("order-events")
            .key(event.orderId().value())  // Partition key
            .body(serialize(event))
            .build();
        
        return messagePublisher.publish(message).mapToVoid();
    }
}
```

### Custom Partitioner

```java
public class TenantPartitioner implements Partitioner {
    
    @Override
    public int partition(
        String topic,
        Object key,
        byte[] keyBytes,
        Object value,
        byte[] valueBytes,
        Cluster cluster
    ) {
        // Extract tenant ID from key
        String tenantId = extractTenantId(key.toString());
        
        // Get available partitions
        int numPartitions = cluster.partitionCountForTopic(topic);
        
        // Hash tenant ID to partition
        return Math.abs(tenantId.hashCode()) % numPartitions;
    }
}

// Configuration
@Configuration
public class KafkaConfig {
    
    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.PARTITIONER_CLASS_CONFIG, TenantPartitioner.class);
        // ... other configs
        return new DefaultKafkaProducerFactory<>(config);
    }
}
```

---

## 🔄 Consumer Groups

### Multiple Consumers (Scaling)

```yaml
# Service Instance 1
spring.kafka.consumer.group-id: order-processor

# Service Instance 2 (same group)
spring.kafka.consumer.group-id: order-processor

# Service Instance 3 (same group)
spring.kafka.consumer.group-id: order-processor

# Result: Each partition consumed by only one instance
# If topic has 6 partitions:
# - Instance 1: partitions 0, 1
# - Instance 2: partitions 2, 3
# - Instance 3: partitions 4, 5
```

### Multiple Consumer Groups (Broadcasting)

```java
// Order Processor Group
@KafkaListener(
    topics = "order-events",
    groupId = "order-processor"
)
public void processOrders(ConsumerRecord<String, String> record) {
    // Process order logic
}

// Analytics Consumer Group (different group = gets all events)
@KafkaListener(
    topics = "order-events",
    groupId = "analytics-processor"
)
public void trackAnalytics(ConsumerRecord<String, String> record) {
    // Analytics logic
}

// Email Notification Group (different group = gets all events)
@KafkaListener(
    topics = "order-events",
    groupId = "email-notification"
)
public void sendEmails(ConsumerRecord<String, String> record) {
    // Email sending logic
}
```

---

## ⚡ Performance Tuning

### Producer Optimization

```yaml
spring.kafka.producer:
  # Batching for throughput
  batch-size: 32768      # 32KB batches
  linger-ms: 20          # Wait 20ms for batch
  
  # Compression
  compression-type: lz4  # lz4 fastest, gzip best ratio
  
  # Memory
  buffer-memory: 67108864  # 64MB buffer
  
  # Throughput vs Latency
  max.in.flight.requests.per.connection: 5  # Pipeline multiple requests
  
  # Reliability
  acks: all              # -1 = all replicas (slowest, safest)
  retries: 3
  enable.idempotence: true
```

### Consumer Optimization

```yaml
spring.kafka.consumer:
  # Throughput
  max-poll-records: 1000        # Fetch up to 1000 records
  fetch-min-bytes: 1024         # Wait for 1KB before returning
  fetch-max-wait-ms: 500        # Max wait time
  
  # Concurrency
  concurrency: 10               # 10 consumer threads
  
  # Session management
  session.timeout.ms: 30000     # 30s session timeout
  heartbeat.interval.ms: 10000  # Heartbeat every 10s
  max.poll.interval.ms: 300000  # 5 min processing time
```

### Parallelism Example

```java
@Configuration
public class KafkaConsumerConfig {
    
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
        ConsumerFactory<String, String> consumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        
        factory.setConsumerFactory(consumerFactory);
        
        // Match partition count for max parallelism
        // If topic has 12 partitions, use 12 concurrent consumers
        factory.setConcurrency(12);
        
        return factory;
    }
}
```

---

## 💀 Dead Letter Topics

### DLT Configuration

```java
@Configuration
public class KafkaErrorHandlingConfig {
    
    @Bean
    public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(
        KafkaTemplate<String, String> kafkaTemplate
    ) {
        return new DeadLetterPublishingRecoverer(
            kafkaTemplate,
            (record, ex) -> {
                // DLT naming: <topic>.DLT
                return new TopicPartition(
                    record.topic() + ".DLT",
                    record.partition()
                );
            }
        );
    }
    
    @Bean
    public DefaultErrorHandler errorHandler(
        DeadLetterPublishingRecoverer recoverer
    ) {
        // Retry 3 times with exponential backoff, then send to DLT
        DefaultErrorHandler handler = new DefaultErrorHandler(
            recoverer,
            new ExponentialBackOffWithMaxRetries(3)
        );
        
        // Don't retry on these exceptions
        handler.addNotRetryableExceptions(
            InvalidMessageException.class,
            ValidationException.class
        );
        
        return handler;
    }
}
```

### DLT Consumer

```java
@Service
public class DeadLetterTopicConsumer {
    
    @KafkaListener(
        topics = "order-events.DLT",
        groupId = "dlt-processor"
    )
    public void processDLT(
        ConsumerRecord<String, String> record,
        @Header(KafkaHeaders.EXCEPTION_MESSAGE) String exceptionMessage,
        @Header(KafkaHeaders.EXCEPTION_STACKTRACE) String stacktrace
    ) {
        log.error("Message in DLT")
            .field("originalTopic", record.topic().replace(".DLT", ""))
            .field("partition", record.partition())
            .field("offset", record.offset())
            .field("key", record.key())
            .field("exception", exceptionMessage)
            .log();
        
        // Handle DLT message:
        // 1. Alert ops team
        // 2. Store in database for manual review
        // 3. Attempt re-processing with fixes
        
        storeFailed Message(record, exceptionMessage);
        alertOpsTeam(record);
    }
}
```

---

## 📊 Monitoring & Metrics

### Producer Metrics

```java
@Service
public class KafkaMetricsService {
    
    private final MeterRegistry meterRegistry;
    private final KafkaTemplate<String, String> kafkaTemplate;
    
    @Scheduled(fixedRate = 60000)  // Every minute
    public void recordProducerMetrics() {
        Map<MetricName, ? extends Metric> metrics = kafkaTemplate
            .getProducerFactory()
            .createProducer()
            .metrics();
        
        // Record send rate
        Metric sendRate = metrics.get(new MetricName(
            "record-send-rate",
            "producer-metrics",
            "",
            Map.of()
        ));
        
        if (sendRate != null) {
            meterRegistry.gauge(
                "kafka.producer.send.rate",
                (double) sendRate.metricValue()
            );
        }
        
        // Record error rate
        Metric errorRate = metrics.get(new MetricName(
            "record-error-rate",
            "producer-metrics",
            "",
            Map.of()
        ));
        
        if (errorRate != null) {
            meterRegistry.gauge(
                "kafka.producer.error.rate",
                (double) errorRate.metricValue()
            );
        }
    }
}
```

### Consumer Lag Monitoring

```java
@Service
public class ConsumerLagMonitor {
    
    private final MeterRegistry meterRegistry;
    private final AdminClient adminClient;
    
    @Scheduled(fixedRate = 30000)  // Every 30s
    public void monitorConsumerLag() {
        try {
            // Get consumer groups
            ListConsumerGroupsResult groups = adminClient.listConsumerGroups();
            
            for (ConsumerGroupListing group : groups.all().get()) {
                String groupId = group.groupId();
                
                // Get offsets
                Map<TopicPartition, OffsetAndMetadata> offsets = adminClient
                    .listConsumerGroupOffsets(groupId)
                    .partitionsToOffsetAndMetadata()
                    .get();
                
                for (Map.Entry<TopicPartition, OffsetAndMetadata> entry : offsets.entrySet()) {
                    TopicPartition partition = entry.getKey();
                    long consumerOffset = entry.getValue().offset();
                    
                    // Get log end offset
                    long logEndOffset = getLogEndOffset(partition);
                    
                    // Calculate lag
                    long lag = logEndOffset - consumerOffset;
                    
                    // Record metric
                    meterRegistry.gauge(
                        "kafka.consumer.lag",
                        Tags.of(
                            "group", groupId,
                            "topic", partition.topic(),
                            "partition", String.valueOf(partition.partition())
                        ),
                        lag
                    );
                    
                    // Alert if lag is high
                    if (lag > 10000) {
                        log.warn("High consumer lag detected")
                            .field("group", groupId)
                            .field("topic", partition.topic())
                            .field("partition", partition.partition())
                            .field("lag", lag)
                            .log();
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("Failed to monitor consumer lag")
                .exception(e)
                .log();
        }
    }
}
```

---

## 🧪 Testing

### Embedded Kafka Test

```java
@SpringBootTest
@EmbeddedKafka(
    partitions = 3,
    topics = {"order-events"}
)
class OrderEventPublisherTest {
    
    @Autowired
    private OrderEventPublisher publisher;
    
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    
    @Test
    void shouldPublishOrderCreatedEvent() {
        // Given
        Order order = Order.create(customer, items);
        OrderCreatedEvent event = new OrderCreatedEvent(order.id(), order.total());
        
        // When
        Result<Void> result = publisher.publishOrderCreated(event);
        
        // Then
        assertThat(result.isOk()).isTrue();
        
        // Verify published to Kafka
        // (Use ConsumerRecord to verify)
    }
}
```

---

## Best Practices

### ✅ DO

```java
// ✅ Use idempotency keys
IdempotencyKey key = IdempotencyKey.of("consumer", eventId);

// ✅ Partition by entity ID
message.key(order.id().value());

// ✅ Manual offset commit
acknowledgment.acknowledge();

// ✅ Enable producer idempotence
enable.idempotence: true

// ✅ Monitor consumer lag
if (lag > threshold) alert();
```

### ❌ DON'T

```java
// ❌ NÃO use auto-commit em production
enable-auto-commit: true  // ❌ Can lose messages!

// ❌ NÃO ignore partition key
message.key(null);  // ❌ Random partitioning!

// ❌ NÃO processe sem idempotência
process(event);  // ❌ Será reprocessado!

// ❌ NÃO bloqueie consumer thread
Thread.sleep(60000);  // ❌ Session timeout!

// ❌ NÃO ignore DLT
// Messages will be lost!
```

---

## Ver Também

- [Messaging Port](../api-reference/ports/messaging.md) - Port interface
- [Domain Events](./domain-events.md) - Event patterns
- [Outbox Pattern](../api-reference/app-outbox.md) - Transactional publishing
