# Commons Adapters Queue SQS

AWS SQS adapter implementation for `QueuePort` interface.

## Features

✅ **Type-safe operations** - Generic `QueuePort<T>` with JSON serialization
✅ **Send operations** - Single and batch message sending
✅ **Receive operations** - Configurable max messages and visibility timeout
✅ **Delete operations** - Single and batch message deletion
✅ **FIFO queue support** - Message groups and deduplication
✅ **Message attributes** - Custom metadata for messages
✅ **Delayed delivery** - Schedule messages for future processing
✅ **Visibility timeout** - Dynamic message visibility control
✅ **Queue management** - Purge and attribute retrieval
✅ **Result pattern** - Type-safe error handling
✅ **LocalStack support** - Local testing without AWS

## Installation

```xml
<dependency>
  <groupId>com.marcusprado02</groupId>
  <artifactId>commons-adapters-queue-sqs</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## Quick Start

### Basic Usage

```java
// Configuration
SqsConfiguration config = SqsConfiguration.forAws(
    "https://sqs.us-east-1.amazonaws.com/123456789012/my-queue",
    Region.US_EAST_1
).build();

// Create adapter
SqsQueueAdapter<MyMessage> queue = new SqsQueueAdapter<>(config, MyMessage.class);

// Send message
QueueMessage<MyMessage> message = QueueMessage.<MyMessage>builder()
    .payload(new MyMessage("Hello", 123))
    .build();

Result<SendMessageResult> sendResult = queue.send(message);

// Receive messages
Result<List<ReceivedMessage<MyMessage>>> receiveResult =
    queue.receive(10, Duration.ofSeconds(30));

if (receiveResult.isOk()) {
    for (ReceivedMessage<MyMessage> received : receiveResult.getValue()) {
        // Process message
        System.out.println(received.payload());

        // Delete after processing
        queue.delete(received.receiptHandle());
    }
}
```

## Configuration

### AWS Production

```java
SqsConfiguration config = SqsConfiguration.forAws(
    "https://sqs.us-east-1.amazonaws.com/123456789012/my-queue",
    Region.US_EAST_1
)
.requestTimeout(Duration.ofSeconds(15))
.maxBatchSize(10)
.build();
```

### LocalStack Development

```java
SqsConfiguration config = SqsConfiguration.forLocalStack(
    "http://localhost:4566/000000000000/my-queue"
)
.build();
```

### FIFO Queue

```java
SqsConfiguration config = SqsConfiguration.forAws(
    "https://sqs.us-east-1.amazonaws.com/123456789012/my-queue.fifo",
    Region.US_EAST_1
)
.fifoQueue(true)
.build();
```

## Operations

### Send Message

```java
QueueMessage<Order> message = QueueMessage.<Order>builder()
    .payload(new Order("ORD-123", 99.99))
    .attribute("customerId", "CUST-456")
    .attribute("priority", "high")
    .delay(Duration.ofSeconds(10))
    .build();

Result<SendMessageResult> result = queue.send(message);

if (result.isOk()) {
    System.out.println("Message ID: " + result.getValue().messageId());
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
    BatchSendResult batchResult = result.getValue();
    System.out.println("Successful: " + batchResult.successCount());
    System.out.println("Failed: " + batchResult.failureCount());
}
```

### FIFO Queue Messages

```java
QueueMessage<Event> message = QueueMessage.<Event>builder()
    .payload(new Event("USER_CREATED", userId))
    .messageGroupId("user-events")  // Group messages for ordering
    .deduplicationId(UUID.randomUUID().toString())  // Prevent duplicates
    .build();

queue.send(message);
```

### Receive Messages

```java
// Receive up to 10 messages with 30 second visibility timeout
Result<List<ReceivedMessage<Order>>> result =
    queue.receive(10, Duration.ofSeconds(30));

if (result.isOk()) {
    for (ReceivedMessage<Order> message : result.getValue()) {
        System.out.println("Message ID: " + message.messageId());
        System.out.println("Payload: " + message.payload());
        System.out.println("Receive count: " + message.receiveCount());
        System.out.println("Sent: " + message.sentTimestamp());

        // Access custom attributes
        String customerId = message.attributes().get("customerId");
    }
}
```

### Receive One Message

```java
Result<Optional<ReceivedMessage<Order>>> result =
    queue.receiveOne(Duration.ofSeconds(30));

if (result.isOk() && result.getValue().isPresent()) {
    ReceivedMessage<Order> message = result.getValue().get();
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
    System.out.println("Deleted: " + result.getValue().successCount());
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
    QueueAttributes attrs = result.getValue();
    System.out.println("Messages: " + attrs.approximateNumberOfMessages());
    System.out.println("In flight: " + attrs.approximateNumberOfMessagesNotVisible());
    System.out.println("Delayed: " + attrs.approximateNumberOfMessagesDelayed());
    System.out.println("Total: " + attrs.totalApproximateMessages());
    System.out.println("FIFO: " + attrs.fifoQueue());
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
    SendMessageResult sendResult = result.getValue();
    System.out.println("Success: " + sendResult.messageId());
} else {
    Problem error = result.getError();
    System.err.println("Error: " + error.code() + " - " + error.message());
}
```

### Error Codes

- `SERIALIZATION_ERROR` - Failed to serialize/deserialize payload
- `SQS_SEND_ERROR` - Failed to send message to SQS
- `SQS_RECEIVE_ERROR` - Failed to receive messages
- `SQS_DELETE_ERROR` - Failed to delete message
- `SQS_BATCH_SEND_ERROR` - Failed to send batch
- `SQS_BATCH_DELETE_ERROR` - Failed to delete batch
- `SQS_VISIBILITY_ERROR` - Failed to change visibility
- `SQS_PURGE_ERROR` - Failed to purge queue
- `SQS_ATTRIBUTES_ERROR` - Failed to get queue attributes
- `INVALID_MAX_MESSAGES` - Max messages out of range (1-10)
- `BATCH_SIZE_EXCEEDED` - Batch size exceeds configured maximum
- `UNKNOWN_ERROR` - Unexpected error

## Use Cases

### Task Queue

```java
// Producer
public class TaskProducer {
    private final QueuePort<Task> taskQueue;

    public void scheduleTask(Task task) {
        QueueMessage<Task> message = QueueMessage.<Task>builder()
            .payload(task)
            .attribute("priority", task.priority())
            .attribute("createdBy", task.userId())
            .build();

        taskQueue.send(message);
    }
}

// Consumer
public class TaskConsumer {
    private final QueuePort<Task> taskQueue;

    public void processNextBatch() {
        Result<List<ReceivedMessage<Task>>> result =
            taskQueue.receive(10, Duration.ofMinutes(5));

        if (result.isOk()) {
            for (ReceivedMessage<Task> message : result.getValue()) {
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

### Event-Driven Architecture

```java
public class OrderEventPublisher {
    private final QueuePort<OrderEvent> eventQueue;

    public void publishOrderCreated(Order order) {
        OrderEvent event = new OrderEvent(
            "ORDER_CREATED",
            order.id(),
            Instant.now()
        );

        QueueMessage<OrderEvent> message = QueueMessage.<OrderEvent>builder()
            .payload(event)
            .messageGroupId("order-" + order.id())  // FIFO: maintain order
            .deduplicationId(event.id())  // FIFO: prevent duplicates
            .attribute("customerId", order.customerId())
            .build();

        eventQueue.send(message);
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
            .delay(delay)  // Send after delay
            .build();

        emailQueue.send(message);
    }
}
```

## Testing

### LocalStack Integration

```java
@Testcontainers
class MyQueueTest {

    @Container
    static LocalStackContainer localStack =
        new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.0"))
            .withServices(LocalStackContainer.Service.SQS);

    @Test
    void shouldProcessMessages() {
        // Create queue
        try (SqsClient sqsClient = SqsClient.builder()
                .region(Region.of(localStack.getRegion()))
                .endpointOverride(localStack.getEndpointOverride(Service.SQS))
                .build()) {

            CreateQueueResponse response = sqsClient.createQueue(
                CreateQueueRequest.builder()
                    .queueName("test-queue")
                    .build()
            );

            // Configure adapter
            SqsConfiguration config = SqsConfiguration.builder()
                .queueUrl(response.queueUrl())
                .region(Region.of(localStack.getRegion()))
                .endpoint(localStack.getEndpointOverride(Service.SQS))
                .build();

            SqsQueueAdapter<MyMessage> queue =
                new SqsQueueAdapter<>(config, MyMessage.class);

            // Test operations
            queue.send(QueueMessage.<MyMessage>builder()
                .payload(new MyMessage("test"))
                .build());
        }
    }
}
```

## Best Practices

### ✅ DO

- **Use batch operations** for multiple messages (lower cost, better performance)
- **Delete messages after processing** to prevent reprocessing
- **Set appropriate visibility timeout** based on processing time
- **Use message attributes** for metadata instead of embedding in payload
- **Use FIFO queues** when order matters
- **Handle serialization errors** gracefully
- **Close adapter** when done to release resources
- **Use Result pattern** for error handling

### ❌ DON'T

- **Don't exceed batch size limits** (max 10 messages per batch)
- **Don't set visibility timeout too short** (causes duplicate processing)
- **Don't hardcode queue URLs** (use configuration)
- **Don't ignore receive count** (implement dead letter queue logic)
- **Don't purge queues in production** (data loss!)
- **Don't log message payloads** (may contain sensitive data)
- **Don't mix FIFO and standard** queue configurations

## Performance Considerations

### Throughput

- **Standard queues**: Nearly unlimited throughput
- **FIFO queues**: 300 TPS (3000 with batching)
- **Batch operations**: 10x cost reduction vs individual

### Visibility Timeout

- Set based on expected processing time
- Default: 30 seconds
- Maximum: 12 hours
- Extend dynamically if needed

### Long Polling

```java
// Long polling (recommended) - reduces empty receives
queue.receive(10, Duration.ofSeconds(20));

// Short polling (not recommended) - more API calls
queue.receive(10, Duration.ofSeconds(0));
```

### Message Size

- Maximum: 256 KB
- Use S3 for larger payloads (store reference in message)

## AWS IAM Permissions

Required IAM permissions:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "sqs:SendMessage",
        "sqs:SendMessageBatch",
        "sqs:ReceiveMessage",
        "sqs:DeleteMessage",
        "sqs:DeleteMessageBatch",
        "sqs:ChangeMessageVisibility",
        "sqs:GetQueueAttributes",
        "sqs:PurgeQueue"
      ],
      "Resource": "arn:aws:sqs:*:*:my-queue-name"
    }
  ]
}
```

## Troubleshooting

### Messages not received

- Check visibility timeout isn't too long
- Verify queue URL is correct
- Check IAM permissions
- Ensure messages aren't delayed

### Duplicate messages

- Use FIFO queue with deduplication ID
- Implement idempotent message processing
- Check visibility timeout isn't too short

### Serialization errors

- Verify payload class has proper Jackson annotations
- Check JavaTimeModule is registered
- Ensure payload is JSON-serializable

### LocalStack connection refused

```bash
# Start LocalStack
docker run -d -p 4566:4566 localstack/localstack:3.0

# Create queue
aws --endpoint-url=http://localhost:4566 sqs create-queue --queue-name my-queue
```

## Dependencies

- AWS SDK for Java 2.x (SQS)
- Jackson for JSON serialization
- SLF4J for logging
- Commons Ports Queue (interface)
- Commons Kernel Result (error handling)

---

**See also:**

- [commons-ports-queue](../commons-ports-queue) - Queue port interface
- [AWS SQS Documentation](https://docs.aws.amazon.com/sqs/)
- [Result Pattern](../commons-kernel-result)
