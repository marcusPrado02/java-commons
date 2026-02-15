package com.marcusprado02.commons.adapters.queue.sqs;

import static org.assertj.core.api.Assertions.*;

import com.marcusprado02.commons.kernel.errors.ErrorCode;
import com.marcusprado02.commons.kernel.result.Result;
import com.marcusprado02.commons.ports.queue.*;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.CreateQueueResponse;

@Testcontainers
class SqsQueueAdapterTest {

  private static final DockerImageName LOCALSTACK_IMAGE =
      DockerImageName.parse("localstack/localstack:3.0");

  @Container
  static LocalStackContainer localStack =
      new LocalStackContainer(LOCALSTACK_IMAGE).withServices(Service.SQS);

  private SqsQueueAdapter<TestMessage> adapter;
  private String queueUrl;

  @BeforeEach
  void setUp() {
    // Create queue
    try (SqsClient sqsClient =
        SqsClient.builder()
            .region(Region.of(localStack.getRegion()))
            .endpointOverride(localStack.getEndpointOverride(Service.SQS))
            .build()) {

      CreateQueueRequest createRequest =
          CreateQueueRequest.builder().queueName("test-queue").build();

      CreateQueueResponse createResponse = sqsClient.createQueue(createRequest);
      queueUrl = createResponse.queueUrl();
    }

    // Create adapter
    SqsConfiguration configuration =
        SqsConfiguration.builder()
            .queueUrl(queueUrl)
            .region(Region.of(localStack.getRegion()))
            .endpoint(localStack.getEndpointOverride(Service.SQS))
            .build();

    adapter = new SqsQueueAdapter<>(configuration, TestMessage.class);
  }

  @AfterEach
  void tearDown() {
    if (adapter != null) {
      adapter.close();
    }
  }

  @Test
  void shouldSendAndReceiveMessage() {
    // Given
    TestMessage payload = new TestMessage("Test", 123);
    QueueMessage<TestMessage> message =
        QueueMessage.<TestMessage>builder().payload(payload).build();

    // When - Send
    Result<SendMessageResult> sendResult = adapter.send(message);

    // Then - Send successful
    assertThat(sendResult.isOk()).isTrue();
    assertThat(sendResult.getOrNull().messageId()).isNotNull();

    // When - Receive
    Result<List<ReceivedMessage<TestMessage>>> receiveResult =
        adapter.receive(1, Duration.ofSeconds(30));

    // Then - Receive successful
    assertThat(receiveResult.isOk()).isTrue();
    assertThat(receiveResult.getOrNull()).hasSize(1);

    ReceivedMessage<TestMessage> received = receiveResult.getOrNull().get(0);
    assertThat(received.payload().content()).isEqualTo("Test");
    assertThat(received.payload().number()).isEqualTo(123);
    assertThat(received.receiptHandle()).isNotNull();
  }

  @Test
  void shouldSendMessageWithAttributes() {
    // Given
    TestMessage payload = new TestMessage("With attributes", 456);
    QueueMessage<TestMessage> message =
        QueueMessage.<TestMessage>builder()
            .payload(payload)
            .attribute("key1", "value1")
            .attribute("key2", "value2")
            .build();

    // When
    Result<SendMessageResult> sendResult = adapter.send(message);
    Result<List<ReceivedMessage<TestMessage>>> receiveResult =
        adapter.receive(1, Duration.ofSeconds(30));

    // Then
    assertThat(sendResult.isOk()).isTrue();
    assertThat(receiveResult.isOk()).isTrue();

    ReceivedMessage<TestMessage> received = receiveResult.getOrNull().get(0);
    assertThat(received.attributes()).containsEntry("key1", "value1");
    assertThat(received.attributes()).containsEntry("key2", "value2");
  }

  @Test
  void shouldSendBatchMessages() {
    // Given
    List<QueueMessage<TestMessage>> messages =
        List.of(
            QueueMessage.<TestMessage>builder().payload(new TestMessage("Message 1", 1)).build(),
            QueueMessage.<TestMessage>builder().payload(new TestMessage("Message 2", 2)).build(),
            QueueMessage.<TestMessage>builder().payload(new TestMessage("Message 3", 3)).build());

    // When
    Result<BatchSendResult> sendResult = adapter.sendBatch(messages);

    // Then
    assertThat(sendResult.isOk()).isTrue();
    BatchSendResult result = sendResult.getOrNull();
    assertThat(result.successCount()).isEqualTo(3);
    assertThat(result.failureCount()).isEqualTo(0);
  }

  @Test
  void shouldDeleteMessage() throws InterruptedException {
    // Given
    QueueMessage<TestMessage> message =
        QueueMessage.<TestMessage>builder().payload(new TestMessage("To delete", 999)).build();

    adapter.send(message);
    Thread.sleep(100); // Wait for message propagation

    Result<List<ReceivedMessage<TestMessage>>> receiveResult =
        adapter.receive(1, Duration.ofSeconds(30));
    ReceivedMessage<TestMessage> received = receiveResult.getOrNull().get(0);

    // When
    Result<Void> deleteResult = adapter.delete(received.receiptHandle());

    // Then
    assertThat(deleteResult.isOk()).isTrue();

    // Verify message is deleted
    Thread.sleep(100);
    Result<List<ReceivedMessage<TestMessage>>> afterDelete =
        adapter.receive(1, Duration.ofSeconds(1));
    assertThat(afterDelete.getOrNull()).isEmpty();
  }

  @Test
  void shouldDeleteBatchMessages() throws InterruptedException {
    // Given
    adapter.sendBatch(
        List.of(
            QueueMessage.<TestMessage>builder()
                .payload(new TestMessage("Batch delete 1", 1))
                .build(),
            QueueMessage.<TestMessage>builder()
                .payload(new TestMessage("Batch delete 2", 2))
                .build()));

    Thread.sleep(100);

    Result<List<ReceivedMessage<TestMessage>>> receiveResult =
        adapter.receive(10, Duration.ofSeconds(30));
    List<String> receiptHandles =
        receiveResult.getOrNull().stream().map(ReceivedMessage::receiptHandle).toList();

    // When
    Result<BatchDeleteResult> deleteResult = adapter.deleteBatch(receiptHandles);

    // Then
    assertThat(deleteResult.isOk()).isTrue();
    assertThat(deleteResult.getOrNull().successCount()).isEqualTo(2);
  }

  @Test
  void shouldChangeVisibility() throws InterruptedException {
    // Given
    adapter.send(
        QueueMessage.<TestMessage>builder()
            .payload(new TestMessage("Visibility test", 111))
            .build());

    Thread.sleep(100);

    Result<List<ReceivedMessage<TestMessage>>> receiveResult =
        adapter.receive(1, Duration.ofSeconds(5));
    ReceivedMessage<TestMessage> received = receiveResult.getOrNull().get(0);

    // When - Change visibility to 0 (make immediately available)
    Result<Void> changeResult = adapter.changeVisibility(received.receiptHandle(), Duration.ZERO);

    // Then
    assertThat(changeResult.isOk()).isTrue();
  }

  @Test
  void shouldGetQueueAttributes() {
    // When
    Result<QueueAttributes> attributesResult = adapter.getAttributes();

    // Then
    assertThat(attributesResult.isOk()).isTrue();
    QueueAttributes attributes = attributesResult.getOrNull();
    assertThat(attributes.createdTimestamp()).isNotNull();
    assertThat(attributes.fifoQueue()).isFalse();
  }

  @Test
  void shouldReceiveOne() throws InterruptedException {
    // Given
    adapter.send(
        QueueMessage.<TestMessage>builder()
            .payload(new TestMessage("Single receive", 777))
            .build());

    Thread.sleep(100);

    // When
    Result<Optional<ReceivedMessage<TestMessage>>> receiveResult =
        adapter.receiveOne(Duration.ofSeconds(30));

    // Then
    assertThat(receiveResult.isOk()).isTrue();
    assertThat(receiveResult.getOrNull()).isPresent();
    assertThat(receiveResult.getOrNull().get().payload().number()).isEqualTo(777);
  }

  @Test
  void shouldReturnEmptyWhenNoMessages() {
    // When
    Result<Optional<ReceivedMessage<TestMessage>>> receiveResult =
        adapter.receiveOne(Duration.ofSeconds(1));

    // Then
    assertThat(receiveResult.isOk()).isTrue();
    assertThat(receiveResult.getOrNull()).isEmpty();
  }

  @Test
  void shouldFailWithInvalidMaxMessages() {
    // When
    Result<List<ReceivedMessage<TestMessage>>> result = adapter.receive(11, Duration.ofSeconds(30));

    // Then
    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code()).isEqualTo(ErrorCode.of("INVALID_MAX_MESSAGES"));
  }

  @Test
  void shouldPurgeQueue() {
    // When
    Result<Void> purgeResult = adapter.purge();

    // Then
    assertThat(purgeResult.isOk()).isTrue();
  }

  /** Test payload class */
  public record TestMessage(String content, Integer number) {}
}
