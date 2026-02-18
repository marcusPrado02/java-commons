# Port: Queue

## Visão Geral

`commons-ports-queue` define contratos para processamento assíncrono com filas, abstraindo serviços como AWS SQS e Azure Storage Queue.

**Quando usar:**
- Processamento assíncrono
- Workloads com picos de demanda
- Desacoplamento de serviços
- Retry com backoff
- Dead letter queues
- Batch processing

**Implementações disponíveis:**
- `commons-adapters-queue-sqs` - AWS SQS
- `commons-adapters-queue-azure-storage-queue` - Azure Storage Queue

---

## 📦 Instalação

```xml
<!-- Port (interface) -->
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-ports-queue</artifactId>
    <version>${commons.version}</version>
</dependency>

<!-- Adapter -->
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-adapters-queue-sqs</artifactId>
    <version>${commons.version}</version>
</dependency>
```

---

## 📨 QueuePublisher Interface

### Core Methods

```java
public interface QueuePublisher {
    
    /**
     * Envia mensagem para fila.
     */
    Result<MessageId> send(
        String queueName,
        QueueMessage message
    );
    
    /**
     * Envia com delay.
     */
    Result<MessageId> sendDelayed(
        String queueName,
        QueueMessage message,
        Duration delay
    );
    
    /**
     * Envia batch.
     */
    Result<List<MessageId>> sendBatch(
        String queueName,
        List<QueueMessage> messages
    );
}
```

### QueueMessage Model

```java
public record QueueMessage(
    String body,
    Map<String, String> attributes,
    Optional<String> deduplicationId,  // For exactly-once
    Optional<String> groupId           // For FIFO queues
) {
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        public Builder body(String body);
        public Builder attribute(String key, String value);
        public Builder deduplicationId(String id);
        public Builder groupId(String groupId);
        public QueueMessage build();
    }
    
    public static QueueMessage of(String body) {
        return new QueueMessage(body, Map.of(), Optional.empty(), Optional.empty());
    }
}
```

---

## 🔄 QueueConsumer Interface

### Core Methods

```java
public interface QueueConsumer {
    
    /**
     * Recebe mensagens da fila (polling).
     */
    Result<List<ReceivedMessage>> receive(
        String queueName,
        ReceiveOptions options
    );
    
    /**
     * Deleta mensagem (após processamento bem-sucedido).
     */
    Result<Void> delete(
        String queueName,
        String receiptHandle
    );
    
    /**
     * Estende visibility timeout (para processamento longo).
     */
    Result<Void> changeVisibility(
        String queueName,
        String receiptHandle,
        Duration timeout
    );
}
```

### Received Message

```java
public record ReceivedMessage(
    String messageId,
    String body,
    Map<String, String> attributes,
    String receiptHandle,
    int receiveCount
) {
    public boolean isRedelivered() {
        return receiveCount > 1;
    }
}

public record ReceiveOptions(
    int maxMessages,           // 1-10
    Duration visibilityTimeout, // How long to hide message
    Duration waitTime          // Long polling
) {
    public static ReceiveOptions defaults() {
        return new ReceiveOptions(
            10,
            Duration.ofSeconds(30),
            Duration.ofSeconds(20)
        );
    }
}
```

---

## 📧 Email Queue Example

### Email Sending Service

```java
@Service
public class AsyncEmailService {
    
    private final QueuePublisher queuePublisher;
    private final ObjectMapper objectMapper;
    
    private static final String QUEUE_NAME = "email-queue";
    
    public Result<MessageId> sendEmailAsync(EmailRequest emailRequest) {
        try {
            String body = objectMapper.writeValueAsString(emailRequest);
            
            QueueMessage message = QueueMessage.builder()
                .body(body)
                .attribute("type", "email")
                .attribute("userId", emailRequest.userId().value())
                .deduplicationId(emailRequest.id().value())  // Prevent duplicates
                .build();
            
            return queuePublisher.send(QUEUE_NAME, message)
                .andThen(messageId -> {
                    log.info("Email queued")
                        .field("emailId", emailRequest.id().value())
                        .field("messageId", messageId.value())
                        .log();
                });
            
        } catch (JsonProcessingException e) {
            return Result.error(Error.of(
                "JSON_SERIALIZATION_ERROR",
                "Failed to serialize email request"
            ));
        }
    }
}

public record EmailRequest(
    EmailRequestId id,
    UserId userId,
    EmailAddress to,
    String subject,
    String body
) {}
```

### Email Consumer

```java
@Service
public class EmailQueueConsumer {
    
    private final QueueConsumer queueConsumer;
    private final EmailSender emailSender;
    private final ObjectMapper objectMapper;
    
    private static final String QUEUE_NAME = "email-queue";
    
    @Scheduled(fixedDelay = 1000)  // Poll every second
    public void processEmails() {
        ReceiveOptions options = new ReceiveOptions(
            10,                          // Process up to 10 messages
            Duration.ofMinutes(5),       // 5 min visibility timeout
            Duration.ofSeconds(20)       // Long polling (reduces empty receives)
        );
        
        Result<List<ReceivedMessage>> result = queueConsumer.receive(
            QUEUE_NAME,
            options
        );
        
        if (result.isError()) {
            log.error("Failed to receive messages")
                .error(result.getError())
                .log();
            return;
        }
        
        List<ReceivedMessage> messages = result.get();
        
        for (ReceivedMessage message : messages) {
            processMessage(message);
        }
    }
    
    private void processMessage(ReceivedMessage message) {
        try {
            // Parse message
            EmailRequest request = objectMapper.readValue(
                message.body(),
                EmailRequest.class
            );
            
            // Send email
            Result<MessageId> result = emailSender.send(buildEmail(request));
            
            if (result.isOk()) {
                // Success - delete from queue
                queueConsumer.delete(QUEUE_NAME, message.receiptHandle());
                
                log.info("Email processed successfully")
                    .field("emailId", request.id().value())
                    .field("messageId", message.messageId())
                    .log();
                
            } else {
                // Error - will retry (message becomes visible again)
                log.error("Failed to send email")
                    .error(result.getError())
                    .field("emailId", request.id().value())
                    .field("receiveCount", message.receiveCount())
                    .log();
                
                // If too many retries, it will go to DLQ automatically
            }
            
        } catch (Exception e) {
            log.error("Failed to process message")
                .exception(e)
                .field("messageId", message.messageId())
                .log();
        }
    }
}
```

---

## 🖼️ Image Processing Queue

### Image Upload Service

```java
@Service
public class ImageUploadService {
    
    private final FileStorage fileStorage;
    private final QueuePublisher queuePublisher;
    
    private static final String QUEUE_NAME = "image-processing-queue";
    
    public Result<ImageId> uploadImage(MultipartFile file, UserId userId) {
        // Store original image
        ImageId imageId = ImageId.generate();
        String key = "images/" + userId.value() + "/" + imageId.value() + "/original";
        
        return fileStorage.store(key, file.getInputStream(), file.getSize())
            .andThen(location -> {
                // Queue for processing (thumbnails, optimization)
                ImageProcessingTask task = new ImageProcessingTask(
                    imageId,
                    userId,
                    location.key(),
                    file.getContentType()
                );
                
                return queueProcessingTask(task);
            })
            .map(messageId -> imageId);
    }
    
    private Result<MessageId> queueProcessingTask(ImageProcessingTask task) {
        QueueMessage message = QueueMessage.builder()
            .body(serializeToJson(task))
            .attribute("type", "image-processing")
            .attribute("imageId", task.imageId().value())
            .deduplicationId(task.imageId().value())
            .build();
        
        return queuePublisher.send(QUEUE_NAME, message);
    }
}
```

### Image Processing Worker

```java
@Service
public class ImageProcessingWorker {
    
    private final QueueConsumer queueConsumer;
    private final FileStorage fileStorage;
    private final ThumbnailService thumbnailService;
    
    private static final String QUEUE_NAME = "image-processing-queue";
    
    @Scheduled(fixedDelay = 500)
    public void processImages() {
        Result<List<ReceivedMessage>> result = queueConsumer.receive(
            QUEUE_NAME,
            ReceiveOptions.defaults()
        );
        
        result.ifOk(messages -> 
            messages.forEach(this::processImage)
        );
    }
    
    private void processImage(ReceivedMessage message) {
        try {
            ImageProcessingTask task = parseTask(message.body());
            
            // Download original
            Result<byte[]> imageResult = fileStorage.retrieve(task.originalKey());
            
            if (imageResult.isError()) {
                log.error("Failed to download image")
                    .error(imageResult.getError())
                    .log();
                return;
            }
            
            byte[] originalImage = imageResult.get();
            
            // Generate thumbnails
            generateThumbnails(task, originalImage);
            
            // Delete from queue
            queueConsumer.delete(QUEUE_NAME, message.receiptHandle());
            
            log.info("Image processed")
                .field("imageId", task.imageId().value())
                .field("messageId", message.messageId())
                .log();
            
        } catch (Exception e) {
            log.error("Failed to process image")
                .exception(e)
                .field("messageId", message.messageId())
                .log();
        }
    }
    
    private void generateThumbnails(ImageProcessingTask task, byte[] originalImage) {
        List<ThumbnailSize> sizes = List.of(
            new ThumbnailSize(150, 150),
            new ThumbnailSize(300, 300),
            new ThumbnailSize(600, 600)
        );
        
        for (ThumbnailSize size : sizes) {
            byte[] thumbnail = thumbnailService.resize(originalImage, size);
            
            String key = String.format(
                "images/%s/%s/thumbnail_%dx%d",
                task.userId().value(),
                task.imageId().value(),
                size.width(),
                size.height()
            );
            
            fileStorage.store(key, thumbnail);
        }
    }
}
```

---

## ⚡ Batch Processing

### Batch Publisher

```java
@Service
public class OrderNotificationBatchService {
    
    private final QueuePublisher queuePublisher;
    private static final String QUEUE_NAME = "order-notifications";
    
    public Result<List<MessageId>> notifyOrderShipped(List<Order> orders) {
        List<QueueMessage> messages = orders.stream()
            .map(order -> QueueMessage.builder()
                .body(serializeOrder(order))
                .attribute("type", "order-shipped")
                .attribute("orderId", order.id().value())
                .deduplicationId(order.id().value())
                .build()
            )
            .toList();
        
        // Send batch (up to 10 messages at once)
        List<List<QueueMessage>> batches = Lists.partition(messages, 10);
        List<MessageId> allMessageIds = new ArrayList<>();
        
        for (List<QueueMessage> batch : batches) {
            Result<List<MessageId>> result = queuePublisher.sendBatch(QUEUE_NAME, batch);
            
            if (result.isError()) {
                log.error("Failed to send batch")
                    .error(result.getError())
                    .field("batchSize", batch.size())
                    .log();
                return result;
            }
            
            allMessageIds.addAll(result.get());
        }
        
        return Result.ok(allMessageIds);
    }
}
```

---

## ⏰ Delayed Messages

### Scheduled Task Service

```java
@Service
public class ScheduledTaskService {
    
    private final QueuePublisher queuePublisher;
    private static final String QUEUE_NAME = "scheduled-tasks";
    
    public Result<MessageId> scheduleTask(Task task, Duration delay) {
        QueueMessage message = QueueMessage.builder()
            .body(serializeTask(task))
            .attribute("type", "scheduled-task")
            .attribute("taskId", task.id().value())
            .deduplicationId(task.id().value())
            .build();
        
        return queuePublisher.sendDelayed(QUEUE_NAME, message, delay)
            .andThen(messageId -> {
                log.info("Task scheduled")
                    .field("taskId", task.id().value())
                    .field("delay", delay.toSeconds() + "s")
                    .field("messageId", messageId.value())
                    .log();
            });
    }
}

// Example usage
public class OrderService {
    
    private final ScheduledTaskService scheduledTaskService;
    
    public Result<Order> createOrder(OrderRequest request) {
        Order order = Order.create(request);
        
        // Schedule auto-cancellation after 15 minutes if not paid
        Task cancelTask = Task.autoCancelOrder(order.id());
        scheduledTaskService.scheduleTask(
            cancelTask,
            Duration.ofMinutes(15)
        );
        
        return Result.ok(order);
    }
}
```

---

## 🔁 Long Running Tasks

### Visibility Timeout Extension

```java
@Service
public class ReportGenerationWorker {
    
    private final QueueConsumer queueConsumer;
    private final ReportGenerator reportGenerator;
    
    private static final String QUEUE_NAME = "report-generation";
    
    @Scheduled(fixedDelay = 1000)
    public void processReports() {
        Result<List<ReceivedMessage>> result = queueConsumer.receive(
            QUEUE_NAME,
            new ReceiveOptions(
                1,                      // Process one at a time
                Duration.ofMinutes(5),  // Initial timeout
                Duration.ofSeconds(20)
            )
        );
        
        result.ifOk(messages -> 
            messages.forEach(this::processReport)
        );
    }
    
    private void processReport(ReceivedMessage message) {
        try {
            ReportRequest request = parseRequest(message.body());
            
            // Start processing
            CompletableFuture<byte[]> reportFuture = CompletableFuture
                .supplyAsync(() -> reportGenerator.generate(request));
            
            // Extend visibility every 4 minutes (report takes 10+ minutes)
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            
            scheduler.scheduleAtFixedRate(
                () -> extendVisibility(message.receiptHandle()),
                4, 4, TimeUnit.MINUTES
            );
            
            // Wait for completion
            byte[] report = reportFuture.get(30, TimeUnit.MINUTES);
            
            // Stop scheduler
            scheduler.shutdown();
            
            // Store report
            storeReport(request.reportId(), report);
            
            // Delete from queue
            queueConsumer.delete(QUEUE_NAME, message.receiptHandle());
            
            log.info("Report generated")
                .field("reportId", request.reportId().value())
                .log();
            
        } catch (Exception e) {
            log.error("Failed to generate report")
                .exception(e)
                .log();
        }
    }
    
    private void extendVisibility(String receiptHandle) {
        queueConsumer.changeVisibility(
            QUEUE_NAME,
            receiptHandle,
            Duration.ofMinutes(5)
        );
    }
}
```

---

## 🧪 Testing

### Mock Queue

```java
public class MockQueue implements QueuePublisher, QueueConsumer {
    
    private final Map<String, Queue<MockMessage>> queues = new ConcurrentHashMap<>();
    
    @Override
    public Result<MessageId> send(String queueName, QueueMessage message) {
        MessageId messageId = MessageId.generate();
        
        Queue<MockMessage> queue = queues.computeIfAbsent(
            queueName,
            k -> new ConcurrentLinkedQueue<>()
        );
        
        queue.add(new MockMessage(
            messageId,
            message,
            UUID.randomUUID().toString(),  // receiptHandle
            0  // receiveCount
        ));
        
        return Result.ok(messageId);
    }
    
    @Override
    public Result<List<ReceivedMessage>> receive(
        String queueName,
        ReceiveOptions options
    ) {
        Queue<MockMessage> queue = queues.get(queueName);
        
        if (queue == null || queue.isEmpty()) {
            return Result.ok(List.of());
        }
        
        List<ReceivedMessage> messages = new ArrayList<>();
        
        for (int i = 0; i < options.maxMessages(); i++) {
            MockMessage mock = queue.poll();
            if (mock == null) break;
            
            messages.add(new ReceivedMessage(
                mock.messageId().value(),
                mock.message().body(),
                mock.message().attributes(),
                mock.receiptHandle(),
                mock.receiveCount() + 1
            ));
        }
        
        return Result.ok(messages);
    }
    
    @Override
    public Result<Void> delete(String queueName, String receiptHandle) {
        // In mock, message is already removed by poll()
        return Result.ok();
    }
    
    private record MockMessage(
        MessageId messageId,
        QueueMessage message,
        String receiptHandle,
        int receiveCount
    ) {}
}
```

### Test Example

```java
class AsyncEmailServiceTest {
    
    private MockQueue mockQueue;
    private AsyncEmailService emailService;
    
    @BeforeEach
    void setUp() {
        mockQueue = new MockQueue();
        emailService = new AsyncEmailService(mockQueue, new ObjectMapper());
    }
    
    @Test
    void shouldQueueEmail() {
        // Given
        EmailRequest request = new EmailRequest(
            EmailRequestId.generate(),
            UserId.from("user-123"),
            EmailAddress.of("john@example.com"),
            "Test Subject",
            "Test Body"
        );
        
        // When
        Result<MessageId> result = emailService.sendEmailAsync(request);
        
        // Then
        assertThat(result.isOk()).isTrue();
        
        // Verify message in queue
        Result<List<ReceivedMessage>> messages = mockQueue.receive(
            "email-queue",
            ReceiveOptions.defaults()
        );
        
        assertThat(messages.isOk()).isTrue();
        assertThat(messages.get()).hasSize(1);
        
        ReceivedMessage message = messages.get().get(0);
        assertThat(message.body()).contains("john@example.com");
    }
}
```

---

## Best Practices

### ✅ DO

```java
// ✅ Use long polling para reduzir custos
new ReceiveOptions(10, timeout, Duration.ofSeconds(20));

// ✅ Delete mensagem após processar
queueConsumer.delete(queueName, receiptHandle);

// ✅ Use deduplicationId
.deduplicationId(order.id().value())

// ✅ Batch send para performance
queuePublisher.sendBatch(queueName, messages);

// ✅ Extend visibility para tasks longas
queueConsumer.changeVisibility(queueName, receiptHandle, Duration.ofMinutes(5));
```

### ❌ DON'T

```java
// ❌ NÃO use short polling
new ReceiveOptions(10, timeout, Duration.ZERO);  // ❌ Expensive!

// ❌ NÃO esqueça de deletar
// Message will be reprocessed!

// ❌ NÃO processe mesmo messageId 2x
if (processedIds.contains(message.messageId())) return;

// ❌ NÃO envie um por um
for (message : messages) {
    queue.send(queueName, message);  // ❌ Use batch!
}

// ❌ NÃO ignore visibility timeout
// Task takes 10 min, timeout is 30s = will retry!
```

---

## Ver Também

- [SQS Adapter](../../../commons-adapters-queue-sqs/) - AWS implementation
- [Messaging](./messaging.md) - Event-driven patterns
- [Outbox Pattern](../app-outbox.md) - Reliable message publishing
