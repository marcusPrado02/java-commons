# Azure Service Bus Messaging Adapter

This module provides Azure Service Bus implementation of the `commons-ports-messaging` messaging abstractions, enabling cloud-native messaging with enterprise-grade features.

## Features

- **Multiple Authentication Methods**
  - Connection String (for development/testing)
  - Managed Identity (production recommended)
- **Session Support**: Use partition keys as session IDs for ordered message processing
- **Dead Letter Queue**: Automatic DLQ support via abandon/complete semantics
- **Managed Concurrency**: ServiceBusProcessorClient handles threading automatically
- **Custom Headers**: Full support for application properties
- **Automatic Recovery**: Built-in reconnection and retry logic

## Dependencies

```xml
<dependency>
  <groupId>com.marcusprado02.commons</groupId>
  <artifactId>commons-adapters-messaging-azure-servicebus</artifactId>
</dependency>
```

## Quick Start

### Connection String (Development)

```java
// Publisher
AzureServiceBusPublisherAdapter publisher = AzureServiceBusPublisherAdapter.builder()
    .connectionString("Endpoint=sb://namespace.servicebus.windows.net/;SharedAccessKeyName=...;SharedAccessKey=...")
    .build();

// Consumer
AzureServiceBusConsumerAdapter consumer = AzureServiceBusConsumerAdapter.builder()
    .connectionString("Endpoint=sb://namespace.servicebus.windows.net/;SharedAccessKeyName=...;SharedAccessKey=...")
    .build();
```

### Managed Identity (Production)

```java
// Publisher
AzureServiceBusPublisherAdapter publisher = AzureServiceBusPublisherAdapter.builder()
    .fullyQualifiedNamespace("namespace.servicebus.windows.net")
    .build();

// Consumer
AzureServiceBusConsumerAdapter consumer = AzureServiceBusConsumerAdapter.builder()
    .fullyQualifiedNamespace("namespace.servicebus.windows.net")
    .build();
```

## Publishing Messages

```java
JacksonMessageSerializer<OrderCreated> serializer = new JacksonMessageSerializer<>();

MessageEnvelope<OrderCreated> envelope = MessageEnvelope.<OrderCreated>builder()
    .payload(new OrderCreated("order-123", BigDecimal.valueOf(99.99)))
    .topic(TopicName.of("orders"))
    .partitionKey("customer-456")  // Optional: enables sessions
    .headers(MessageHeaders.builder()
        .correlationId("correlation-123")
        .build())
    .build();

publisher.publish(envelope, serializer);
```

## Consuming Messages

```java
JacksonMessageSerializer<OrderCreated> serializer = new JacksonMessageSerializer<>();

consumer.subscribe(
    TopicName.of("orders"),
    ConsumerGroup.of("order-processor"),
    OrderCreated.class,
    serializer,
    envelope -> {
        OrderCreated order = envelope.payload();
        System.out.println("Processing order: " + order.orderId());
        // Message is auto-completed on success, abandoned on exception
    }
);

consumer.start();

// When shutting down
consumer.stop();
consumer.close();
```

## Sessions

Azure Service Bus sessions enable ordered processing of related messages:

```java
MessageEnvelope<OrderEvent> envelope = MessageEnvelope.<OrderEvent>builder()
    .payload(orderEvent)
    .topic(TopicName.of("order-events"))
    .partitionKey("order-123") // Session ID
    .build();
```

All messages with the same partition key are processed sequentially in the same session.

## Dead Letter Queue

Messages are automatically moved to the DLQ when:
- Processing throws an exception (after configured retry attempts)
- Message TTL expires
- Max delivery count exceeded

```java
consumer.subscribe(
    TopicName.of("orders"),
    ConsumerGroup.of("processor"),
    Order.class,
    serializer,
    envelope -> {
        if (shouldRejectMessage(envelope.payload())) {
            throw new RuntimeException("Rejected"); // Moves to DLQ after retries
        }
        // Success: message completed automatically
    }
);
```

## Comparison with Other Adapters

| Feature | Azure Service Bus | Kafka | RabbitMQ |
|---------|------------------|-------|----------|
| Cloud-Native | ✅ Azure-first | ❌ Self-hosted | ❌ Self-hosted |
| Managed Identity | ✅ Yes | ❌ No | ❌ No |
| Sessions | ✅ Built-in | ⚠️ Manual (partition key) | ⚠️ Manual (exclusive queues) |
| DLQ | ✅ Automatic | ⚠️ Manual | ✅ Built-in |
| Throughput | ⚠️ Medium (Premium: high) | ✅ Very high | ⚠️ Medium |
| Latency | ⚠️ ~10-50ms | ⚠️ ~5-20ms | ⚠️ ~5-15ms |
| Transactions | ❌ No | ✅ Yes | ⚠️ Limited |
| Ordering | ✅ Sessions | ✅ Partitions | ⚠️ Manual |
| Cost Model | 💰 Per operation | 💰💰 Infrastructure | 💰💰 Infrastructure |

## When to Use Azure Service Bus

### Best For:
- **Azure-native applications** requiring seamless integration
- **Enterprise messaging** with guaranteed delivery and DLQ
- **Ordered processing** via sessions
- **Managed Identity** authentication
- **Variable workloads** (auto-scaling)

### Consider Alternatives When:
- **Very high throughput** needed (>100K msg/s) → use Kafka
- **Pure pub/sub** patterns → use Event Hubs
- **Multi-cloud** or on-premises → use RabbitMQ or Kafka

## Advanced Configuration

### Custom Message Properties

```java
MessageHeaders headers = MessageHeaders.builder()
    .correlationId("correlation-123")
    .causationId("causation-456")
    .header("priority", "high")
    .header("source-system", "order-api")
    .build();

MessageEnvelope<Order> envelope = MessageEnvelope.<Order>builder()
    .payload(order)
    .headers(headers)
    .build();
```

### TTL and Expiration

Messages have a default TTL of 24 hours. This is configured in the adapter:

```java
// In AzureServiceBusPublisherAdapter.publish()
sbMessage.setTimeToLive(Duration.ofHours(24));
```

To customize TTL, modify the adapter or set it via Azure Portal/CLI.

## Integration with Outbox Pattern

```java
// 1. Save entity + outbox message in transaction
entityRepository.save(order);
outboxRepository.save(outboxMessage);

// 2. Outbox processor reads and publishes
outboxProcessor.process(outboxMessages -> {
    for (OutboxMessage msg : outboxMessages) {
        MessageEnvelope<OrderCreated> envelope = mapToEnvelope(msg);
        publisher.publish(envelope, serializer);
        outboxRepository.markAsPublished(msg.id());
    }
});
```

## Testing

The adapter includes unit tests using Mockito (Azure Service Bus has no local emulator).

For integration tests in real Azure environment:

```java
@Test
void shouldPublishAndConsumeMessage() {
    String connectionString = System.getenv("AZURE_SERVICE_BUS_CONNECTION_STRING");

    AzureServiceBusPublisherAdapter publisher =
        AzureServiceBusPublisherAdapter.builder()
            .connectionString(connectionString)
            .build();

    // Test with real Azure Service Bus namespace
}
```

## Production Considerations

1. **Use Managed Identity**: Avoid connection strings in production
2. **Enable DLQ Monitoring**: Set up alerts for DLQ message counts
3. **Configure Auto-Scaling**: Use Premium tier for auto-scaling
4. **Set Appropriate TTL**: Balance message lifetime vs. storage costs
5. **Monitor Metrics**: Track message count, latency, and errors
6. **Use Sessions Wisely**: Sessions add overhead; use only when ordering is required

## Resources

- [Azure Service Bus Documentation](https://learn.microsoft.com/azure/service-bus-messaging/)
- [Sessions and Message Ordering](https://learn.microsoft.com/azure/service-bus-messaging/message-sessions)
- [Dead Letter Queues](https://learn.microsoft.com/azure/service-bus-messaging/service-bus-dead-letter-queues)
- [Azure SDK for Java](https://github.com/Azure/azure-sdk-for-java/tree/main/sdk/servicebus)
