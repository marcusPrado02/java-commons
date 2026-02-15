package com.marcusprado02.commons.adapters.queue.sqs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.marcusprado02.commons.kernel.errors.ErrorCategory;
import com.marcusprado02.commons.kernel.errors.ErrorCode;
import com.marcusprado02.commons.kernel.errors.Problem;
import com.marcusprado02.commons.kernel.errors.Severity;
import com.marcusprado02.commons.kernel.result.Result;
import com.marcusprado02.commons.ports.queue.*;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;
import software.amazon.awssdk.services.sqs.model.*;

/**
 * AWS SQS adapter for QueuePort.
 *
 * @param <T> the type of message payload
 */
public final class SqsQueueAdapter<T> implements QueuePort<T> {

  private static final Logger logger = LoggerFactory.getLogger(SqsQueueAdapter.class);

  private final SqsConfiguration configuration;
  private final SqsClient sqsClient;
  private final ObjectMapper objectMapper;
  private final Class<T> payloadType;

  public SqsQueueAdapter(SqsConfiguration configuration, Class<T> payloadType) {
    this.configuration = Objects.requireNonNull(configuration, "configuration cannot be null");
    this.payloadType = Objects.requireNonNull(payloadType, "payloadType cannot be null");
    this.sqsClient = createSqsClient(configuration);
    this.objectMapper = createObjectMapper();
  }

  private SqsClient createSqsClient(SqsConfiguration config) {
    SqsClientBuilder builder = SqsClient.builder().region(config.region());

    if (config.endpoint() != null) {
      builder.endpointOverride(config.endpoint());
    }

    builder.overrideConfiguration(
        ClientOverrideConfiguration.builder().apiCallTimeout(config.requestTimeout()).build());

    return builder.build();
  }

  private ObjectMapper createObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    return mapper;
  }

  @Override
  public Result<SendMessageResult> send(QueueMessage<T> message) {
    try {
      String messageBody = serializePayload(message.payload());

      SendMessageRequest.Builder requestBuilder =
          SendMessageRequest.builder().queueUrl(configuration.queueUrl()).messageBody(messageBody);

      // Add message attributes
      if (!message.attributes().isEmpty()) {
        Map<String, MessageAttributeValue> attributes =
            message.attributes().entrySet().stream()
                .collect(
                    Collectors.toMap(
                        Map.Entry::getKey,
                        e ->
                            MessageAttributeValue.builder()
                                .dataType("String")
                                .stringValue(e.getValue())
                                .build()));
        requestBuilder.messageAttributes(attributes);
      }

      // Add delay
      message.delay().ifPresent(delay -> requestBuilder.delaySeconds((int) delay.toSeconds()));

      // FIFO queue specific
      if (configuration.fifoQueue()) {
        message.messageGroupId().ifPresent(groupId -> requestBuilder.messageGroupId(groupId));
        message
            .deduplicationId()
            .ifPresent(dedupId -> requestBuilder.messageDeduplicationId(dedupId));
      }

      SendMessageResponse response = sqsClient.sendMessage(requestBuilder.build());

      logger.debug("Message sent to SQS: messageId={}", response.messageId());

      return Result.ok(SendMessageResult.of(response.messageId(), response.sequenceNumber()));

    } catch (JsonProcessingException e) {
      logger.error("Failed to serialize message payload", e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("SERIALIZATION_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to serialize message: " + e.getMessage()));
    } catch (SqsException e) {
      logger.error("Failed to send message to SQS", e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("SQS_SEND_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to send message: " + e.getMessage()));
    } catch (Exception e) {
      logger.error("Unexpected error sending message", e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("UNKNOWN_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Unexpected error: " + e.getMessage()));
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

    try {
      List<SendMessageBatchRequestEntry> entries = new ArrayList<>();

      for (int i = 0; i < messages.size(); i++) {
        QueueMessage<T> message = messages.get(i);
        String messageBody = serializePayload(message.payload());

        SendMessageBatchRequestEntry.Builder entryBuilder =
            SendMessageBatchRequestEntry.builder().id(String.valueOf(i)).messageBody(messageBody);

        if (!message.attributes().isEmpty()) {
          Map<String, MessageAttributeValue> attributes =
              message.attributes().entrySet().stream()
                  .collect(
                      Collectors.toMap(
                          Map.Entry::getKey,
                          e ->
                              MessageAttributeValue.builder()
                                  .dataType("String")
                                  .stringValue(e.getValue())
                                  .build()));
          entryBuilder.messageAttributes(attributes);
        }

        message.delay().ifPresent(delay -> entryBuilder.delaySeconds((int) delay.toSeconds()));

        if (configuration.fifoQueue()) {
          message.messageGroupId().ifPresent(entryBuilder::messageGroupId);
          message.deduplicationId().ifPresent(entryBuilder::messageDeduplicationId);
        }

        entries.add(entryBuilder.build());
      }

      SendMessageBatchRequest request =
          SendMessageBatchRequest.builder()
              .queueUrl(configuration.queueUrl())
              .entries(entries)
              .build();

      SendMessageBatchResponse response = sqsClient.sendMessageBatch(request);

      List<SendMessageResult> successful =
          response.successful().stream()
              .map(entry -> SendMessageResult.of(entry.messageId(), entry.sequenceNumber()))
              .collect(Collectors.toList());

      List<BatchSendResult.BatchFailure> failed =
          response.failed().stream()
              .map(
                  entry ->
                      new BatchSendResult.BatchFailure(entry.id(), entry.code(), entry.message()))
              .collect(Collectors.toList());

      logger.debug(
          "Batch send completed: {} successful, {} failed", successful.size(), failed.size());

      return Result.ok(new BatchSendResult(successful, failed));

    } catch (JsonProcessingException e) {
      logger.error("Failed to serialize batch messages", e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("SERIALIZATION_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to serialize messages: " + e.getMessage()));
    } catch (SqsException e) {
      logger.error("Failed to send batch messages to SQS", e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("SQS_BATCH_SEND_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to send batch: " + e.getMessage()));
    } catch (Exception e) {
      logger.error("Unexpected error sending batch messages", e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("UNKNOWN_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Unexpected error: " + e.getMessage()));
    }
  }

  @Override
  public Result<List<ReceivedMessage<T>>> receive(int maxMessages, Duration visibilityTimeout) {
    if (maxMessages < 1 || maxMessages > 10) {
      return Result.fail(
          Problem.of(
              ErrorCode.of("INVALID_MAX_MESSAGES"),
              ErrorCategory.BUSINESS,
              Severity.WARNING,
              "maxMessages must be between 1 and 10"));
    }

    try {
      ReceiveMessageRequest request =
          ReceiveMessageRequest.builder()
              .queueUrl(configuration.queueUrl())
              .maxNumberOfMessages(maxMessages)
              .visibilityTimeout((int) visibilityTimeout.toSeconds())
              .messageAttributeNames("All")
              .attributeNames(QueueAttributeName.ALL)
              .build();

      ReceiveMessageResponse response = sqsClient.receiveMessage(request);

      List<ReceivedMessage<T>> messages =
          response.messages().stream()
              .map(this::mapToReceivedMessage)
              .filter(Optional::isPresent)
              .map(Optional::get)
              .collect(Collectors.toList());

      logger.debug("Received {} messages from SQS", messages.size());

      return Result.ok(messages);

    } catch (SqsException e) {
      logger.error("Failed to receive messages from SQS", e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("SQS_RECEIVE_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to receive messages: " + e.getMessage()));
    } catch (Exception e) {
      logger.error("Unexpected error receiving messages", e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("UNKNOWN_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Unexpected error: " + e.getMessage()));
    }
  }

  @Override
  public Result<Void> delete(String receiptHandle) {
    try {
      DeleteMessageRequest request =
          DeleteMessageRequest.builder()
              .queueUrl(configuration.queueUrl())
              .receiptHandle(receiptHandle)
              .build();

      sqsClient.deleteMessage(request);

      logger.debug("Message deleted from SQS");

      return Result.ok(null);

    } catch (SqsException e) {
      logger.error("Failed to delete message from SQS", e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("SQS_DELETE_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to delete message: " + e.getMessage()));
    } catch (Exception e) {
      logger.error("Unexpected error deleting message", e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("UNKNOWN_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Unexpected error: " + e.getMessage()));
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

    try {
      List<DeleteMessageBatchRequestEntry> entries =
          receiptHandles.stream()
              .map(
                  handle ->
                      DeleteMessageBatchRequestEntry.builder()
                          .id(UUID.randomUUID().toString())
                          .receiptHandle(handle)
                          .build())
              .collect(Collectors.toList());

      DeleteMessageBatchRequest request =
          DeleteMessageBatchRequest.builder()
              .queueUrl(configuration.queueUrl())
              .entries(entries)
              .build();

      DeleteMessageBatchResponse response = sqsClient.deleteMessageBatch(request);

      List<String> successful =
          response.successful().stream()
              .map(DeleteMessageBatchResultEntry::id)
              .collect(Collectors.toList());

      List<BatchDeleteResult.BatchFailure> failed =
          response.failed().stream()
              .map(
                  entry ->
                      new BatchDeleteResult.BatchFailure(entry.id(), entry.code(), entry.message()))
              .collect(Collectors.toList());

      logger.debug(
          "Batch delete completed: {} successful, {} failed", successful.size(), failed.size());

      return Result.ok(new BatchDeleteResult(successful, failed));

    } catch (SqsException e) {
      logger.error("Failed to delete batch messages from SQS", e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("SQS_BATCH_DELETE_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to delete batch: " + e.getMessage()));
    } catch (Exception e) {
      logger.error("Unexpected error deleting batch messages", e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("UNKNOWN_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Unexpected error: " + e.getMessage()));
    }
  }

  @Override
  public Result<Void> changeVisibility(String receiptHandle, Duration visibilityTimeout) {
    try {
      ChangeMessageVisibilityRequest request =
          ChangeMessageVisibilityRequest.builder()
              .queueUrl(configuration.queueUrl())
              .receiptHandle(receiptHandle)
              .visibilityTimeout((int) visibilityTimeout.toSeconds())
              .build();

      sqsClient.changeMessageVisibility(request);

      logger.debug("Message visibility timeout changed");

      return Result.ok(null);

    } catch (SqsException e) {
      logger.error("Failed to change message visibility", e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("SQS_VISIBILITY_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to change visibility: " + e.getMessage()));
    } catch (Exception e) {
      logger.error("Unexpected error changing visibility", e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("UNKNOWN_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Unexpected error: " + e.getMessage()));
    }
  }

  @Override
  public Result<Void> purge() {
    try {
      PurgeQueueRequest request =
          PurgeQueueRequest.builder().queueUrl(configuration.queueUrl()).build();

      sqsClient.purgeQueue(request);

      logger.info("Queue purged");

      return Result.ok(null);

    } catch (SqsException e) {
      logger.error("Failed to purge queue", e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("SQS_PURGE_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to purge queue: " + e.getMessage()));
    } catch (Exception e) {
      logger.error("Unexpected error purging queue", e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("UNKNOWN_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Unexpected error: " + e.getMessage()));
    }
  }

  @Override
  public Result<QueueAttributes> getAttributes() {
    try {
      GetQueueAttributesRequest request =
          GetQueueAttributesRequest.builder()
              .queueUrl(configuration.queueUrl())
              .attributeNames(
                  QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES,
                  QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE,
                  QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_DELAYED,
                  QueueAttributeName.CREATED_TIMESTAMP,
                  QueueAttributeName.LAST_MODIFIED_TIMESTAMP,
                  QueueAttributeName.FIFO_QUEUE)
              .build();

      GetQueueAttributesResponse response = sqsClient.getQueueAttributes(request);

      Map<String, String> attrs = response.attributesAsStrings();

      QueueAttributes attributes =
          new QueueAttributes(
              Integer.parseInt(
                  attrs.getOrDefault(
                      QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES.toString(), "0")),
              Integer.parseInt(
                  attrs.getOrDefault(
                      QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE.toString(),
                      "0")),
              Integer.parseInt(
                  attrs.getOrDefault(
                      QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_DELAYED.toString(), "0")),
              Instant.ofEpochSecond(
                  Long.parseLong(
                      attrs.getOrDefault(QueueAttributeName.CREATED_TIMESTAMP.toString(), "0"))),
              Instant.ofEpochSecond(
                  Long.parseLong(
                      attrs.getOrDefault(
                          QueueAttributeName.LAST_MODIFIED_TIMESTAMP.toString(), "0"))),
              Boolean.parseBoolean(
                  attrs.getOrDefault(QueueAttributeName.FIFO_QUEUE.toString(), "false")));

      return Result.ok(attributes);

    } catch (SqsException e) {
      logger.error("Failed to get queue attributes", e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("SQS_ATTRIBUTES_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to get attributes: " + e.getMessage()));
    } catch (Exception e) {
      logger.error("Unexpected error getting queue attributes", e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("UNKNOWN_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Unexpected error: " + e.getMessage()));
    }
  }

  private String serializePayload(T payload) throws JsonProcessingException {
    return objectMapper.writeValueAsString(payload);
  }

  private T deserializePayload(String json) throws IOException {
    return objectMapper.readValue(json, payloadType);
  }

  private Optional<ReceivedMessage<T>> mapToReceivedMessage(Message message) {
    try {
      T payload = deserializePayload(message.body());

      Map<String, String> attributes =
          message.messageAttributes().entrySet().stream()
              .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().stringValue()));

      ReceivedMessage.Builder<T> builder =
          ReceivedMessage.<T>builder()
              .messageId(message.messageId())
              .receiptHandle(message.receiptHandle())
              .payload(payload)
              .attributes(attributes);

      // Parse system attributes
      Map<String, String> systemAttrs = message.attributesAsStrings();

      if (systemAttrs.containsKey(MessageSystemAttributeName.SENT_TIMESTAMP.toString())) {
        builder.sentTimestamp(
            Instant.ofEpochMilli(
                Long.parseLong(
                    systemAttrs.get(MessageSystemAttributeName.SENT_TIMESTAMP.toString()))));
      }

      if (systemAttrs.containsKey(
          MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT.toString())) {
        builder.receiveCount(
            Integer.parseInt(
                systemAttrs.get(MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT.toString())));
      }

      if (systemAttrs.containsKey(MessageSystemAttributeName.MESSAGE_GROUP_ID.toString())) {
        builder.messageGroupId(
            systemAttrs.get(MessageSystemAttributeName.MESSAGE_GROUP_ID.toString()));
      }

      return Optional.of(builder.build());

    } catch (IOException e) {
      logger.error("Failed to deserialize message payload", e);
      return Optional.empty();
    }
  }

  /** Closes the SQS client. */
  public void close() {
    if (sqsClient != null) {
      sqsClient.close();
    }
  }
}
