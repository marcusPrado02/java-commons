package com.marcusprado02.commons.adapters.queue.sqs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.marcusprado02.commons.ports.queue.QueueMessage;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SqsException;

class SqsMockExceptionTest {

  private static SqsClientBuilder stubBuilder(SqsClient client) {
    SqsClientBuilder builder = mock(SqsClientBuilder.class);
    when(builder.region(any())).thenReturn(builder);
    when(builder.overrideConfiguration(any(ClientOverrideConfiguration.class))).thenReturn(builder);
    when(builder.build()).thenReturn(client);
    return builder;
  }

  private static SqsQueueAdapter<String> adapter(SqsClient client) {
    SqsConfiguration config =
        SqsConfiguration.builder()
            .queueUrl("https://sqs.us-east-1.amazonaws.com/123/q")
            .region(Region.US_EAST_1)
            .build();
    return new SqsQueueAdapter<>(config, String.class);
  }

  private static SqsException sqsEx() {
    return mock(SqsException.class);
  }

  // ── send() SqsException branch ────────────────────────────────────────────

  @Test
  void send_sqsException_returnsSqsSendError() {
    SqsClient sqsClient = mock(SqsClient.class);
    SqsClientBuilder mockBuilder = stubBuilder(sqsClient);
    try (var staticMock = mockStatic(SqsClient.class)) {
      staticMock.when(SqsClient::builder).thenReturn(mockBuilder);
      SqsQueueAdapter<String> sut = adapter(sqsClient);

      when(sqsClient.sendMessage(any(SendMessageRequest.class))).thenThrow(sqsEx());

      var result = sut.send(QueueMessage.<String>builder().payload("msg").build());
      assertThat(result.isFail()).isTrue();
      assertThat(result.problemOrNull().code().value()).isEqualTo("SQS_SEND_ERROR");
    }
  }

  // ── sendBatch() SqsException branch ──────────────────────────────────────

  @Test
  void sendBatch_sqsException_returnsSqsBatchSendError() {
    SqsClient sqsClient = mock(SqsClient.class);
    SqsClientBuilder mockBuilder = stubBuilder(sqsClient);
    try (var staticMock = mockStatic(SqsClient.class)) {
      staticMock.when(SqsClient::builder).thenReturn(mockBuilder);
      SqsQueueAdapter<String> sut = adapter(sqsClient);

      when(sqsClient.sendMessageBatch(any(SendMessageBatchRequest.class))).thenThrow(sqsEx());

      var result = sut.sendBatch(List.of(QueueMessage.<String>builder().payload("m").build()));
      assertThat(result.isFail()).isTrue();
      assertThat(result.problemOrNull().code().value()).isEqualTo("SQS_BATCH_SEND_ERROR");
    }
  }

  // ── receive() SqsException branch ────────────────────────────────────────

  @Test
  void receive_sqsException_returnsSqsReceiveError() {
    SqsClient sqsClient = mock(SqsClient.class);
    SqsClientBuilder mockBuilder = stubBuilder(sqsClient);
    try (var staticMock = mockStatic(SqsClient.class)) {
      staticMock.when(SqsClient::builder).thenReturn(mockBuilder);
      SqsQueueAdapter<String> sut = adapter(sqsClient);

      when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenThrow(sqsEx());

      var result = sut.receive(5, Duration.ofSeconds(30));
      assertThat(result.isFail()).isTrue();
      assertThat(result.problemOrNull().code().value()).isEqualTo("SQS_RECEIVE_ERROR");
    }
  }

  // ── receive(): mapToReceivedMessage IOException (invalid JSON body) ────────

  @Test
  void receive_invalidJsonBody_filteredOut() {
    SqsClient sqsClient = mock(SqsClient.class);
    SqsClientBuilder mockBuilder = stubBuilder(sqsClient);
    try (var staticMock = mockStatic(SqsClient.class)) {
      staticMock.when(SqsClient::builder).thenReturn(mockBuilder);
      SqsQueueAdapter<String> sut = adapter(sqsClient);

      Message badMsg =
          Message.builder().messageId("bad-id").receiptHandle("rh").body("not-valid-json").build();
      when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
          .thenReturn(ReceiveMessageResponse.builder().messages(badMsg).build());

      var result = sut.receive(1, Duration.ofSeconds(30));
      assertThat(result.isOk()).isTrue();
      assertThat(result.getOrNull()).isEmpty();
    }
  }

  // ── delete() SqsException branch ─────────────────────────────────────────

  @Test
  void delete_sqsException_returnsSqsDeleteError() {
    SqsClient sqsClient = mock(SqsClient.class);
    SqsClientBuilder mockBuilder = stubBuilder(sqsClient);
    try (var staticMock = mockStatic(SqsClient.class)) {
      staticMock.when(SqsClient::builder).thenReturn(mockBuilder);
      SqsQueueAdapter<String> sut = adapter(sqsClient);

      when(sqsClient.deleteMessage(any(DeleteMessageRequest.class))).thenThrow(sqsEx());

      var result = sut.delete("receipt-handle");
      assertThat(result.isFail()).isTrue();
      assertThat(result.problemOrNull().code().value()).isEqualTo("SQS_DELETE_ERROR");
    }
  }

  // ── deleteBatch() SqsException branch ────────────────────────────────────

  @Test
  void deleteBatch_sqsException_returnsSqsBatchDeleteError() {
    SqsClient sqsClient = mock(SqsClient.class);
    SqsClientBuilder mockBuilder = stubBuilder(sqsClient);
    try (var staticMock = mockStatic(SqsClient.class)) {
      staticMock.when(SqsClient::builder).thenReturn(mockBuilder);
      SqsQueueAdapter<String> sut = adapter(sqsClient);

      when(sqsClient.deleteMessageBatch(any(DeleteMessageBatchRequest.class))).thenThrow(sqsEx());

      var result = sut.deleteBatch(List.of("handle-1"));
      assertThat(result.isFail()).isTrue();
      assertThat(result.problemOrNull().code().value()).isEqualTo("SQS_BATCH_DELETE_ERROR");
    }
  }

  // ── changeVisibility() SqsException branch ───────────────────────────────

  @Test
  void changeVisibility_sqsException_returnsSqsVisibilityError() {
    SqsClient sqsClient = mock(SqsClient.class);
    SqsClientBuilder mockBuilder = stubBuilder(sqsClient);
    try (var staticMock = mockStatic(SqsClient.class)) {
      staticMock.when(SqsClient::builder).thenReturn(mockBuilder);
      SqsQueueAdapter<String> sut = adapter(sqsClient);

      when(sqsClient.changeMessageVisibility(any(ChangeMessageVisibilityRequest.class)))
          .thenThrow(sqsEx());

      var result = sut.changeVisibility("receipt-handle", Duration.ofSeconds(30));
      assertThat(result.isFail()).isTrue();
      assertThat(result.problemOrNull().code().value()).isEqualTo("SQS_VISIBILITY_ERROR");
    }
  }

  // ── purge() SqsException branch ──────────────────────────────────────────

  @Test
  void purge_sqsException_returnsSqsPurgeError() {
    SqsClient sqsClient = mock(SqsClient.class);
    SqsClientBuilder mockBuilder = stubBuilder(sqsClient);
    try (var staticMock = mockStatic(SqsClient.class)) {
      staticMock.when(SqsClient::builder).thenReturn(mockBuilder);
      SqsQueueAdapter<String> sut = adapter(sqsClient);

      when(sqsClient.purgeQueue(any(PurgeQueueRequest.class))).thenThrow(sqsEx());

      var result = sut.purge();
      assertThat(result.isFail()).isTrue();
      assertThat(result.problemOrNull().code().value()).isEqualTo("SQS_PURGE_ERROR");
    }
  }

  // ── getAttributes() SqsException branch ──────────────────────────────────

  @Test
  void getAttributes_sqsException_returnsSqsAttributesError() {
    SqsClient sqsClient = mock(SqsClient.class);
    SqsClientBuilder mockBuilder = stubBuilder(sqsClient);
    try (var staticMock = mockStatic(SqsClient.class)) {
      staticMock.when(SqsClient::builder).thenReturn(mockBuilder);
      SqsQueueAdapter<String> sut = adapter(sqsClient);

      when(sqsClient.getQueueAttributes(any(GetQueueAttributesRequest.class))).thenThrow(sqsEx());

      var result = sut.getAttributes();
      assertThat(result.isFail()).isTrue();
      assertThat(result.problemOrNull().code().value()).isEqualTo("SQS_ATTRIBUTES_ERROR");
    }
  }
}
