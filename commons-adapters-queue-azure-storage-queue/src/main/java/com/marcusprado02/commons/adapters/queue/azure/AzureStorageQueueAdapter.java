package com.marcusprado02.commons.adapters.queue.azure;

import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.queue.QueueClient;
import com.azure.storage.queue.QueueClientBuilder;
import com.azure.storage.queue.models.QueueMessageItem;
import com.azure.storage.queue.models.QueueProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.marcusprado02.commons.kernel.errors.ErrorCategory;
import com.marcusprado02.commons.kernel.errors.ErrorCode;
import com.marcusprado02.commons.kernel.errors.Problem;
import com.marcusprado02.commons.kernel.errors.Severity;
import com.marcusprado02.commons.kernel.result.Result;
import com.marcusprado02.commons.ports.queue.BatchDeleteResult;
import com.marcusprado02.commons.ports.queue.BatchSendResult;
import com.marcusprado02.commons.ports.queue.QueueAttributes;
import com.marcusprado02.commons.ports.queue.QueueMessage;
import com.marcusprado02.commons.ports.queue.QueuePort;
import com.marcusprado02.commons.ports.queue.ReceivedMessage;
import com.marcusprado02.commons.ports.queue.SendMessageResult;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Azure Storage Queue adapter for QueuePort.
 *
 * @param <T> the type of message payload
 */
public final class AzureStorageQueueAdapter<T> implements QueuePort<T> {

  private static final Logger logger = LoggerFactory.getLogger(AzureStorageQueueAdapter.class);
  private static final int MAX_MESSAGES_PER_RECEIVE = 32;
  private static final Duration MAX_VISIBILITY_TIMEOUT = Duration.ofHours(7 * 24); // 7 days

  private final AzureStorageQueueConfiguration configuration;
  private final QueueClient queueClient;
  private final ObjectMapper objectMapper;
  private final Class<T> payloadType;

  public AzureStorageQueueAdapter(
      AzureStorageQueueConfiguration configuration, Class<T> payloadType) {
    this.configuration =
        Objects.requireNonNull(configuration, "configuration cannot be null");
    this.payloadType = Objects.requireNonNull(payloadType, "payloadType cannot be null");
    this.queueClient = createQueueClient(configuration);
    this.objectMapper = createObjectMapper();
  }

  private QueueClient createQueueClient(AzureStorageQueueConfiguration config) {
    QueueClient client =
        new QueueClientBuilder()
            .connectionString(config.connectionString())
            .queueName(config.queueName())
            .buildClient();

    // Create queue if it doesn't exist
    try {
      client.create();
      logger.info("Created queue: {}", config.queueName());
    } catch (Exception e) {
      // Queue likely already exists
      logger.debug("Queue already exists: {}", config.queueName());
    }

    return client;
  }

  private ObjectMapper createObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    return mapper;
  }

  @Override
  public Result<SendMessageResult> send(QueueMessage<T> message) {
    try {
      String jsonPayload = serializePayload(message.payload());

      // Azure Storage Queue has max message size of 64KB
      if (jsonPayload.getBytes().length > 64 * 1024) {
        return Result.fail(
            Problem.of(
                ErrorCode.of("MESSAGE_TOO_LARGE"),
                ErrorCategory.BUSINESS,
                Severity.WARNING,
                "Message exceeds 64KB limit"));
      }

      Duration timeToLive = Duration.ofDays(7); // Max TTL for Azure Storage Queue
      Duration visibilityTimeout = message.delay().orElse(Duration.ZERO);

      com.azure.storage.queue.models.SendMessageResult azureResult =
          queueClient
              .sendMessageWithResponse(jsonPayload, visibilityTimeout, timeToLive, null, null)
              .getValue();

      logger.debug("Message sent successfully: {}", azureResult.getMessageId());

      return Result.ok(new SendMessageResult(azureResult.getMessageId(), null)); // No sequence number

    } catch (JsonProcessingException e) {
      logger.error("Failed to serialize message payload", e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("SERIALIZATION_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to serialize message: " + e.getMessage()));
    } catch (Exception e) {
      logger.error("Failed to send message to Azure Storage Queue", e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("AZURE_QUEUE_SEND_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to send message: " + e.getMessage()));
    }
  }

  @Override
  public Result<BatchSendResult> sendBatch(List<QueueMessage<T>> messages) {
    if (messages.isEmpty()) {
      return Result.ok(new BatchSendResult(List.of(), List.of()));
    }

    if (messages.size() > configuration.maxBatchSize()) {
      return Result.fail(
          Problem.of(
              ErrorCode.of("BATCH_SIZE_EXCEEDED"),
              ErrorCategory.BUSINESS,
              Severity.WARNING,
              "Batch size exceeds maximum: " + configuration.maxBatchSize()));
    }

    List<SendMessageResult> successful = new ArrayList<>();
    List<BatchSendResult.BatchFailure> failed = new ArrayList<>();

    for (int i = 0; i < messages.size(); i++) {
      QueueMessage<T> message = messages.get(i);
      Result<SendMessageResult> result = send(message);

      if (result.isOk()) {
        successful.add(result.getOrNull());
      } else {
        failed.add(
            new BatchSendResult.BatchFailure(
                String.valueOf(i),
                result.problemOrNull().code().value(),
                result.problemOrNull().message()));
      }
    }

    logger.info(
        "Batch send completed: {} successful, {} failed", successful.size(), failed.size());

    return Result.ok(new BatchSendResult(successful, failed));
  }

  @Override
  public Result<List<ReceivedMessage<T>>> receive(int maxMessages, Duration visibilityTimeout) {
    if (maxMessages < 1 || maxMessages > MAX_MESSAGES_PER_RECEIVE) {
      return Result.fail(
          Problem.of(
              ErrorCode.of("INVALID_MAX_MESSAGES"),
              ErrorCategory.BUSINESS,
              Severity.WARNING,
              "maxMessages must be between 1 and 32"));
    }

    if (visibilityTimeout.compareTo(MAX_VISIBILITY_TIMEOUT) > 0) {
      return Result.fail(
          Problem.of(
              ErrorCode.of("INVALID_VISIBILITY_TIMEOUT"),
              ErrorCategory.BUSINESS,
              Severity.WARNING,
              "visibilityTimeout cannot exceed 7 days"));
    }

    try {
      PagedIterable<QueueMessageItem> azureMessages =
          queueClient.receiveMessages(maxMessages, visibilityTimeout, null, null);

      List<ReceivedMessage<T>> messages =
          azureMessages.stream()
              .map(this::mapToReceivedMessage)
              .filter(Optional::isPresent)
              .map(Optional::get)
              .collect(Collectors.toList());

      logger.debug("Received {} messages", messages.size());

      return Result.ok(messages);

    } catch (Exception e) {
      logger.error("Failed to receive messages from Azure Storage Queue", e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("AZURE_QUEUE_RECEIVE_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to receive messages: " + e.getMessage()));
    }
  }

  @Override
  public Result<Void> delete(String receiptHandle) {
    try {
      // Receipt handle format: messageId::popReceipt
      String[] parts = receiptHandle.split("::");
      if (parts.length != 2) {
        return Result.fail(
            Problem.of(
                ErrorCode.of("INVALID_RECEIPT_HANDLE"),
                ErrorCategory.BUSINESS,
                Severity.WARNING,
                "Invalid receipt handle format"));
      }

      queueClient.deleteMessage(parts[0], parts[1]);
      logger.debug("Message deleted: {}", parts[0]);

      return Result.ok(null);

    } catch (Exception e) {
      logger.error("Failed to delete message", e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("AZURE_QUEUE_DELETE_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to delete message: " + e.getMessage()));
    }
  }

  @Override
  public Result<BatchDeleteResult> deleteBatch(List<String> receiptHandles) {
    if (receiptHandles.isEmpty()) {
      return Result.ok(new BatchDeleteResult(List.of(), List.of()));
    }

    if (receiptHandles.size() > configuration.maxBatchSize()) {
      return Result.fail(
          Problem.of(
              ErrorCode.of("BATCH_SIZE_EXCEEDED"),
              ErrorCategory.BUSINESS,
              Severity.WARNING,
              "Batch size exceeds maximum: " + configuration.maxBatchSize()));
    }

    List<String> successful = new ArrayList<>();
    List<BatchDeleteResult.BatchFailure> failed = new ArrayList<>();

    for (String receiptHandle : receiptHandles) {
      Result<Void> result = delete(receiptHandle);

      if (result.isOk()) {
        successful.add(receiptHandle);
      } else {
        failed.add(
            new BatchDeleteResult.BatchFailure(
                receiptHandle,
                result.problemOrNull().code().value(),
                result.problemOrNull().message()));
      }
    }

    logger.info(
        "Batch delete completed: {} successful, {} failed", successful.size(), failed.size());

    return Result.ok(new BatchDeleteResult(successful, failed));
  }

  @Override
  public Result<Void> changeVisibility(String receiptHandle, Duration visibilityTimeout) {
    try {
      String[] parts = receiptHandle.split("::");
      if (parts.length != 2) {
        return Result.fail(
            Problem.of(
                ErrorCode.of("INVALID_RECEIPT_HANDLE"),
                ErrorCategory.BUSINESS,
                Severity.WARNING,
                "Invalid receipt handle format"));
      }

      if (visibilityTimeout.compareTo(MAX_VISIBILITY_TIMEOUT) > 0) {
        return Result.fail(
            Problem.of(
                ErrorCode.of("INVALID_VISIBILITY_TIMEOUT"),
                ErrorCategory.BUSINESS,
                Severity.WARNING,
                "visibilityTimeout cannot exceed 7 days"));
      }

      queueClient.updateMessage(parts[0], parts[1], null, visibilityTimeout);
      logger.debug(
          "Message visibility updated: {} to {}s", parts[0], visibilityTimeout.toSeconds());

      return Result.ok(null);

    } catch (Exception e) {
      logger.error("Failed to change message visibility", e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("AZURE_QUEUE_VISIBILITY_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to change visibility: " + e.getMessage()));
    }
  }

  @Override
  public Result<Void> purge() {
    try {
      queueClient.clearMessages();
      logger.warn("Queue purged: {}", configuration.queueName());

      return Result.ok(null);

    } catch (Exception e) {
      logger.error("Failed to purge queue", e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("AZURE_QUEUE_PURGE_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to purge queue: " + e.getMessage()));
    }
  }

  @Override
  public Result<QueueAttributes> getAttributes() {
    try {
      QueueProperties properties = queueClient.getProperties();

      int approximateMessageCount = properties.getApproximateMessagesCount();

      logger.debug("Queue attributes retrieved, message count: {}", approximateMessageCount);

      return Result.ok(
          new QueueAttributes(
              approximateMessageCount,
              0, // Azure Storage Queue doesn't expose in-flight count
              0, // Azure Storage Queue doesn't expose delayed count
              Instant.now(), // Azure doesn't expose creation time
              Instant.now(), // Azure doesn't expose last modified time
              false)); // Not a FIFO queue

    } catch (Exception e) {
      logger.error("Failed to get queue attributes", e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("AZURE_QUEUE_ATTRIBUTES_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to get attributes: " + e.getMessage()));
    }
  }

  private String serializePayload(T payload) throws JsonProcessingException {
    return objectMapper.writeValueAsString(payload);
  }

  private T deserializePayload(String json) throws IOException {
    return objectMapper.readValue(json, payloadType);
  }

  private Optional<ReceivedMessage<T>> mapToReceivedMessage(QueueMessageItem message) {
    try {
      T payload = deserializePayload(message.getBody().toString());

      // Azure Storage Queue doesn't support custom message attributes natively
      // We could encode them in the message body if needed in the future

      // Receipt handle format: messageId::popReceipt
      String receiptHandle = message.getMessageId() + "::" + message.getPopReceipt();

      ReceivedMessage.Builder<T> builder =
          ReceivedMessage.<T>builder()
              .messageId(message.getMessageId())
              .receiptHandle(receiptHandle)
              .payload(payload)
              .attributes(Map.of());

      // Parse system attributes
      if (message.getInsertionTime() != null) {
        builder.sentTimestamp(message.getInsertionTime().toInstant());
      }

      builder.receiveCount((int) message.getDequeueCount());

      return Optional.of(builder.build());

    } catch (IOException e) {
      logger.error("Failed to deserialize message: {}", message.getMessageId(), e);
      return Optional.empty();
    }
  }
}
