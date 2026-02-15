# Commons Adapters Queue Azure Storage Queue

Azure Storage Queue adapter implementation for `QueuePort` interface.

## Features

✅ **Type-safe operations** - Generic `QueuePort<T>` with JSON serialization
✅ **Send operations** - Single and batch message sending
✅ **Receive operations** - Configurable max messages and visibility timeout
✅ **Delete operations** - Single and batch message deletion
✅ **Message delay** - Schedule messages for future processing
✅ **Visibility timeout** - Dynamic message visibility control (up to 7 days)
✅ **Queue management** - Purge and attribute retrieval
✅ **Result pattern** - Type-safe error handling
✅ **Azurite support** - Local testing without Azure
✅ **Poison message handling** - Track dequeue count

## Installation

```xml
<dependency>
  <groupId>com.marcusprado02</groupId>
  <artifactId>commons-adapters-queue-azure-storage-queue</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## Quick Start

### Basic Usage

```java
// Configuration
AzureStorageQueueConfiguration config =
    AzureStorageQueueConfiguration.forAzure(
        "DefaultEndpointsProtocol=https;AccountName=myaccount;...",
        "my-queue"
    ).build();

// Create adapter
AzureStorageQueueAdapter<MyMessage> queue =
    new AzureStorageQueueAdapter<>(config, MyMessage.class);

// Send message
QueueMessage<MyMessage> message = QueueMessage.<MyMessage>builder()
    .payload(new MyMessage("Hello", 123))
    .build();

Result<SendMessageResult> sendResult = queue.send(message);

// Receive messages
Result<List<ReceivedMessage<MyMessage>>> receiveResult =
    queue.receive(10, Duration.ofSeconds(30));

if (receiveResult.isOk()) {
    for (ReceivedMessage<MyMessage> received : receiveResult.getOrNull()) {
        // Process message
        System.out.println(received.payload());

        // Delete after processing
        queue.delete(received.receiptHandle());
    }
}
```

## Configuration

### Azure Cloud Production

```java
AzureStorageQueueConfiguration config =
    AzureStorageQueueConfiguration.forAzure(
        "DefaultEndpointsProtocol=https;AccountName=myaccount;"
            + "AccountKey=key;EndpointSuffix=core.windows.net",
        "production-queue"
    )
    .requestTimeout(Duration.ofSeconds(15))
    .maxBatchSize(32)
    .build();
```

### Azurite Local Development

```java
AzureStorageQueueConfiguration config =
    AzureStorageQueueConfiguration.forAzurite("dev-queue").build();
```

### Custom Configuration

```java
AzureStorageQueueConfiguration config =
    AzureStorageQueueConfiguration.builder()
        .connectionString("...")
        .queueName("custom-queue")
        .requestTimeout(Duration.ofSeconds(20))
        .maxBatchSize(16)
        .build();
```

## Operations

### Send Message

```java
QueueMessage<Order> message = QueueMessage.<Order>builder()
    .payload(new Order("ORD-123", 99.99))
    .delay(Duration.ofSeconds(10))  // Delayed delivery
    .build();

Result<SendMessageResult> result = queue.send(message);

if (result.isOk()) {
    System.out.println("Message ID: " + result.getOrNull().messageId());
}
```

### Send Batch

```java
List<QueueMessage<Order>> messages = List.of(
    QueueMessage.<Order>builder()
        .payload(new Order("ORD-1", 10.0))
        .build(),
    QueueMessage.<Order>builder()
        .payload(new Order("ORD-2", 20.0))
        .build()
);

Result<BatchSendResult> result = queue.sendBatch(messages);

if (result.isOk()) {
    BatchSendResult batchResult = result.getOrNull();
    System.out.println("Successful: " + batchResult.successCount());
    System.out.println("Failed: " + batchResult.failureCount());
}
```

### Receive Messages

```java
// Receive up to 32 messages with 30 second visibility timeout
Result<List<ReceivedMessage<Order>>> result =
    queue.receive(32, Duration.ofSeconds(30));

if (result.isOk()) {
    for (ReceivedMessage<Order> message : result.getOrNull()) {
        System.out.println("Message ID: " + message.messageId());
        System.out.println("Payload: " + message.payload());
        System.out.println("Dequeue count: " + message.receiveCount());
        System.out.println("Sent: " + message.sentTimestamp());
    }
}
```

### Receive One Message

```java
Result<Optional<ReceivedMessage<Order>>> result =
    queue.receiveOne(Duration.ofSeconds(30));

if (result.isOk() && result.getOrNull().isPresent()) {
    ReceivedMessage<Order> message = result.getOrNull().get();
    // Process message
}
```

### Delete Message

```java
Result<Void> deleteResult = queue.delete(receiptHandle);

if (deleteResult.isOk()) {
    System.out.println("Message deleted successfully");
}
```

### Delete Batch

```java
List<String> receiptHandles = messages.stream()
    .map(ReceivedMessage::receiptHandle)
    .toList();

Result<BatchDeleteResult> result = queue.deleteBatch(receiptHandles);

if (result.isOk()) {
    System.out.println("Deleted: " + result.getOrNull().successCount());
}
```

### Change Visibility Timeout

```java
// Extend visibility timeout to process longer
Result<Void> result = queue.changeVisibility(
    receiptHandle,
    Duration.ofMinutes(5)
);

// Make message immediately available again
Result<Void> result = queue.changeVisibility(
    receiptHandle,
    Duration.ZERO
);
```

### Queue Attributes

```java
Result<QueueAttributes> result = queue.getAttributes();

if (result.isOk()) {
    QueueAttributes attrs = result.getOrNull();
    System.out.println("Messages: " + attrs.approximateNumberOfMessages());
}
```

### Purge Queue

```java
Result<Void> result = queue.purge();
// Use with caution - deletes all messages!
```

## Error Handling

All operations return `Result<T>` for type-safe error handling:

```java
Result<SendMessageResult> result = queue.send(message);

if (result.isOk()) {
    SendMessageResult sendResult = result.getOrNull();
    System.out.println("Success: " + sendResult.messageId());
} else {
    Problem error = result.problemOrNull();
    System.err.println("Error: " + error.code() + " - " + error.message());
}
```

### Error Codes

- `SERIALIZATION_ERROR` - Failed to serialize/deserialize payload
- `MESSAGE_TOO_LARGE` - Message exceeds 64KB limit
- `AZURE_QUEUE_SEND_ERROR` - Failed to send message
- `AZURE_QUEUE_RECEIVE_ERROR` - Failed to receive messages
- `AZURE_QUEUE_DELETE_ERROR` - Failed to delete message
- `AZURE_QUEUE_VISIBILITY_ERROR` - Failed to change visibility
- `AZURE_QUEUE_PURGE_ERROR` - Failed to purge queue
- `AZURE_QUEUE_ATTRIBUTES_ERROR` - Failed to get queue attributes
- `INVALID_MAX_MESSAGES` - Max messages out of range (1-32)
- `INVALID_VISIBILITY_TIMEOUT` - Visibility timeout exceeds 7 days
- `INVALID_RECEIPT_HANDLE` - Invalid receipt handle format
- `BATCH_SIZE_EXCEEDED` - Batch size exceeds configured maximum

## Use Cases

### Task Queue

```java
public class TaskProducer {
    private final QueuePort<Task> taskQueue;

    public void scheduleTask(Task task) {
        QueueMessage<Task> message = QueueMessage.<Task>builder()
            .payload(task)
            .build();

        taskQueue.send(message);
    }
}

public class TaskConsumer {
    private final QueuePort<Task> taskQueue;

    public void processNextBatch() {
        Result<List<ReceivedMessage<Task>>> result =
            taskQueue.receive(32, Duration.ofMinutes(5));

        if (result.isOk()) {
            for (ReceivedMessage<Task> message : result.getOrNull()) {
                try {
                    executeTask(message.payload());
                    taskQueue.delete(message.receiptHandle());
                } catch (Exception e) {
                    // Let message become visible again for retry
                    logger.error("Task failed, will retry", e);
                }
            }
        }
    }
}
```

### Poison Message Handling

```java
public class PoisonMessageHandler {
    private final QueuePort<Order> orderQueue;
    private static final int MAX_DEQUEUE_COUNT = 5;

    public void processOrders() {
        Result<List<ReceivedMessage<Order>>> result =
            orderQueue.receive(10, Duration.ofMinutes(2));

        if (result.isOk()) {
            for (ReceivedMessage<Order> message : result.getOrNull()) {
                // Check if message is poison
                if (message.receiveCount() >= MAX_DEQUEUE_COUNT) {
                    // Move to dead letter queue or log
                    handlePoisonMessage(message);
                    orderQueue.delete(message.receiptHandle());
                    continue;
                }

                try {
                    processOrder(message.payload());
                    orderQueue.delete(message.receiptHandle());
                } catch (Exception e) {
                    logger.error("Processing failed, attempt " +
                        message.receiveCount(), e);
                }
            }
        }
    }
}
```

### Delayed Processing

```java
public class EmailScheduler {
    private final QueuePort<EmailTask> emailQueue;

    public void scheduleWelcomeEmail(String userId, Duration delay) {
        EmailTask task = new EmailTask("WELCOME", userId);

        QueueMessage<EmailTask> message = QueueMessage.<EmailTask>builder()
            .payload(task)
            .delay(delay)  // Send after delay (max 7 days)
            .build();

        emailQueue.send(message);
    }
}
```

## Testing

### Azurite Integration

```java
@Testcontainers
class MyQueueTest {

    @Container
    static GenericContainer<?> azurite =
        new GenericContainer<>(DockerImageName.parse(
            "mcr.microsoft.com/azure-storage/azurite:latest"))
            .withExposedPorts(10001)
            .withCommand("azurite-queue", "--queueHost", "0.0.0.0", "--loose");

    @Test
    void shouldProcessMessages() {
        String connectionString = String.format(
            "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;"
                + "AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;"
                + "QueueEndpoint=http://%s:%d/devstoreaccount1;",
            azurite.getHost(), azurite.getMappedPort(10001));

        AzureStorageQueueConfiguration config =
            AzureStorageQueueConfiguration.builder()
                .connectionString(connectionString)
                .queueName("test-queue")
                .build();

        AzureStorageQueueAdapter<MyMessage> queue =
            new AzureStorageQueueAdapter<>(config, MyMessage.class);

        // Test operations
        queue.send(QueueMessage.<MyMessage>builder()
            .payload(new MyMessage("test"))
            .build());
    }
}
```

## Best Practices

### ✅ DO

- **Use batch operations** for multiple messages (better performance)
- **Delete messages after processing** to prevent reprocessing
- **Set appropriate visibility timeout** based on processing time
- **Monitor dequeue count** for poison message detection
- **Handle serialization errors** gracefully
- **Close adapter** when done (though not strictly required)
- **Use Result pattern** for error handling
- **Respect 64KB message limit** (use Blob Storage for larger payloads)

### ❌ DON'T

- **Don't exceed batch size limits** (max 32 messages per batch)
- **Don't set visibility timeout too short** (causes duplicate processing)
- **Don't hardcode connection strings** (use configuration)
- **Don't ignore dequeue count** (implement poison message handling)
- **Don't purge queues in production** (data loss!)
- **Don't log message payloads** (may contain sensitive data)
- **Don't exceed 7-day visibility timeout** (Azure limit)

## Azure Storage Queue Limitations

### Message Size
- Maximum: 64 KB per message
- Use Azure Blob Storage for larger payloads (store reference in message)

### Visibility Timeout
- Minimum: 0 seconds (immediate visibility)
- Maximum: 7 days
- Default: 30 seconds

### Time-to-Live
- Maximum: 7 days
- Infinite TTL not supported

### Batch Operations
- Maximum: 32 messages per receive
- No atomic batch send (simulated with individual sends)

### Message Attributes
- Not natively supported
- Can encode in message body if needed

### Ordering
- Not guaranteed (Azure Storage Queue is not FIFO)
- Use Azure Service Bus for ordering guarantees

## Performance Considerations

### Throughput
- Scalable target: 2,000 messages per second per queue
- Use multiple queues for higher throughput

### Visibility Timeout
- Set based on expected processing time
- Default: 30 seconds
- Maximum: 7 days

### Polling
- Configurable via `maxMessages` and `visibilityTimeout`
- Use long polling for efficiency (30 seconds recommended)
- Short polling creates more API calls

### Message Size
- Keep messages small (<64KB)
- Use Blob Storage for large payloads:

```java
// Store large payload in Blob Storage
String blobUrl = blobClient.upload(largePayload);

// Send reference in queue message
QueueMessage<Reference> message = QueueMessage.<Reference>builder()
    .payload(new Reference(blobUrl))
    .build();
```

## Azure IAM & Access Control

### Connection String (Development)

```java
// Use connection string for simplicity in dev/test
String connectionString =
    "DefaultEndpointsProtocol=https;AccountName=myaccount;"
    + "AccountKey=key;EndpointSuffix=core.windows.net";
```

### Managed Identity (Production)

```java
// Use DefaultAzureCredential for production
QueueClient client = new QueueClientBuilder()
    .endpoint("https://myaccount.queue.core.windows.net/myqueue")
    .credential(new DefaultAzureCredentialBuilder().build())
    .buildClient();
```

### Required Permissions

- `Storage Queue Data Contributor` - Send, receive, delete messages
- `Storage Queue Data Reader` - Read messages only
- `Storage Queue Data Message Processor` - Process messages (receive + delete)

## Troubleshooting

### Messages not received

- Check visibility timeout isn't too long
- Verify connection string is correct
- Ensure queue exists (auto-created by adapter)
- Check message hasn't expired (TTL)

### Duplicate messages

- Implement idempotent message processing
- Check visibility timeout isn't too short
- Monitor dequeue count

### Serialization errors

- Verify payload class has proper Jackson annotations
- Check JavaTimeModule is registered
- Ensure payload is JSON-serializable

### Azurite connection refused

```bash
# Start Azurite
docker run -d -p 10001:10001 mcr.microsoft.com/azure-storage/azurite:latest \
    azurite-queue --queueHost 0.0.0.0 --loose
```

### Poison messages

- Monitor dequeue count (`message.receiveCount()`)
- Implement dead letter queue logic
- Set maximum retry limit (recommend 5)

```java
if (message.receiveCount() >= 5) {
    // Move to dead letter queue or alert
    handlePoisonMessage(message);
    queue.delete(message.receiptHandle());
}
```

## Dependencies

- Azure Storage Queue SDK 12.x
- Jackson for JSON serialization
- SLF4J for logging
- Commons Ports Queue (interface)
- Commons Kernel Result (error handling)

## Comparison with AWS SQS

| Feature | Azure Storage Queue | AWS SQS |
|---------|---------------------|---------|
| Max message size | 64 KB | 256 KB |
| Max visibility timeout | 7 days | 12 hours |
| Max batch receive | 32 | 10 |
| FIFO support | No | Yes |
| Message attributes | No (manual) | Yes |
| Ordering | No | FIFO queues |
| Deduplication | No | FIFO queues |
| Max TTL | 7 days | 14 days |
| Pricing | Very low | Low |

---

**See also:**
- [commons-ports-queue](../commons-ports-queue) - Queue port interface
- [commons-adapters-queue-sqs](../commons-adapters-queue-sqs) - AWS SQS adapter
- [Azure Storage Queue Documentation](https://docs.microsoft.com/azure/storage/queues/)
- [Result Pattern](../commons-kernel-result)
