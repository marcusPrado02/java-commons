package com.marcusprado02.commons.adapters.queue.azure;

import static org.assertj.core.api.Assertions.*;

import com.marcusprado02.commons.kernel.errors.ErrorCode;
import com.marcusprado02.commons.kernel.result.Result;
import com.marcusprado02.commons.ports.queue.*;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class AzureStorageQueueAdapterTest {

  private static final DockerImageName AZURITE_IMAGE =
      DockerImageName.parse("mcr.microsoft.com/azure-storage/azurite:latest");

  @Container
  static GenericContainer<?> azurite =
      new GenericContainer<>(AZURITE_IMAGE)
          .withExposedPorts(10001) // Queue service port
          .withCommand("azurite-queue", "--queueHost", "0.0.0.0", "--loose");

  private AzureStorageQueueAdapter<TestMessage> adapter;

  record TestMessage(String message, int value) {}

  @BeforeEach
  void setUp() {
    String queueEndpoint =
        String.format(
            "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;"
                + "AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;"
                + "QueueEndpoint=http://%s:%d/devstoreaccount1;",
            azurite.getHost(), azurite.getMappedPort(10001));

    AzureStorageQueueConfiguration config =
        AzureStorageQueueConfiguration.builder()
            .connectionString(queueEndpoint)
            .queueName("test-queue")
            .build();

    adapter = new AzureStorageQueueAdapter<>(config, TestMessage.class);
  }

  @AfterEach
  void tearDown() {
    if (adapter != null) {
      adapter.purge();
    }
  }

  @Test
  void shouldSendAndReceiveMessage() {
    // Given
    TestMessage testMessage = new TestMessage("Hello Azure", 123);
    QueueMessage<TestMessage> message =
        QueueMessage.<TestMessage>builder().payload(testMessage).build();

    // When
    Result<SendMessageResult> sendResult = adapter.send(message);
    Result<List<ReceivedMessage<TestMessage>>> receiveResult =
        adapter.receive(1, Duration.ofSeconds(30));

    // Then
    assertThat(sendResult.isOk()).isTrue();
    assertThat(sendResult.getOrNull().messageId()).isNotNull();

    assertThat(receiveResult.isOk()).isTrue();
    assertThat(receiveResult.getOrNull()).hasSize(1);

    ReceivedMessage<TestMessage> received = receiveResult.getOrNull().get(0);
    assertThat(received.payload()).isEqualTo(testMessage);
    assertThat(received.messageId()).isNotNull();
    assertThat(received.receiptHandle()).isNotNull();
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
    Result<BatchSendResult> result = adapter.sendBatch(messages);

    // Then
    assertThat(result.isOk()).isTrue();
    BatchSendResult batchResult = result.getOrNull();
    assertThat(batchResult.successCount()).isEqualTo(3);
    assertThat(batchResult.failureCount()).isEqualTo(0);
  }

  @Test
  void shouldDeleteMessage() {
    // Given
    QueueMessage<TestMessage> message =
        QueueMessage.<TestMessage>builder().payload(new TestMessage("To be deleted", 999)).build();

    adapter.send(message);
    Result<List<ReceivedMessage<TestMessage>>> receiveResult =
        adapter.receive(1, Duration.ofSeconds(30));
    String receiptHandle = receiveResult.getOrNull().get(0).receiptHandle();

    // When
    Result<Void> deleteResult = adapter.delete(receiptHandle);

    // Then
    assertThat(deleteResult.isOk()).isTrue();

    // Verify message is deleted
    Result<List<ReceivedMessage<TestMessage>>> checkResult =
        adapter.receive(1, Duration.ofSeconds(1));
    assertThat(checkResult.getOrNull()).isEmpty();
  }

  @Test
  void shouldDeleteBatchMessages() {
    // Given
    adapter.sendBatch(
        List.of(
            QueueMessage.<TestMessage>builder().payload(new TestMessage("Msg 1", 1)).build(),
            QueueMessage.<TestMessage>builder().payload(new TestMessage("Msg 2", 2)).build()));

    Result<List<ReceivedMessage<TestMessage>>> receiveResult =
        adapter.receive(2, Duration.ofSeconds(30));
    List<String> receiptHandles =
        receiveResult.getOrNull().stream().map(ReceivedMessage::receiptHandle).toList();

    // When
    Result<BatchDeleteResult> result = adapter.deleteBatch(receiptHandles);

    // Then
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull().successCount()).isEqualTo(2);
    assertThat(result.getOrNull().failureCount()).isEqualTo(0);
  }

  @Test
  void shouldChangeVisibility() {
    // Given
    QueueMessage<TestMessage> message =
        QueueMessage.<TestMessage>builder()
            .payload(new TestMessage("Visibility test", 456))
            .build();

    adapter.send(message);
    Result<List<ReceivedMessage<TestMessage>>> receiveResult =
        adapter.receive(1, Duration.ofSeconds(5));
    String receiptHandle = receiveResult.getOrNull().get(0).receiptHandle();

    // When - make message immediately available
    Result<Void> visibilityResult = adapter.changeVisibility(receiptHandle, Duration.ZERO);

    // Then
    assertThat(visibilityResult.isOk()).isTrue();

    // Message should be available again immediately
    Result<List<ReceivedMessage<TestMessage>>> reReceiveResult =
        adapter.receive(1, Duration.ofSeconds(1));
    assertThat(reReceiveResult.getOrNull()).isNotEmpty();
  }

  @Test
  void shouldGetQueueAttributes() {
    // Given
    adapter.sendBatch(
        List.of(
            QueueMessage.<TestMessage>builder().payload(new TestMessage("Attr 1", 1)).build(),
            QueueMessage.<TestMessage>builder().payload(new TestMessage("Attr 2", 2)).build()));

    // Wait a bit for messages to be visible
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    // When
    Result<QueueAttributes> result = adapter.getAttributes();

    // Then
    assertThat(result.isOk()).isTrue();
    QueueAttributes attrs = result.getOrNull();
    assertThat(attrs.approximateNumberOfMessages()).isGreaterThanOrEqualTo(0);
    assertThat(attrs.fifoQueue()).isFalse();
  }

  @Test
  void shouldReceiveOne() {
    // Given
    adapter.send(
        QueueMessage.<TestMessage>builder()
            .payload(new TestMessage("Single message", 789))
            .build());

    // When
    Result<Optional<ReceivedMessage<TestMessage>>> result =
        adapter.receiveOne(Duration.ofSeconds(30));

    // Then
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isPresent();
    assertThat(result.getOrNull().get().payload().message()).isEqualTo("Single message");
  }

  @Test
  void shouldReturnEmptyWhenNoMessages() {
    // When
    Result<List<ReceivedMessage<TestMessage>>> result = adapter.receive(5, Duration.ofSeconds(1));

    // Then
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isEmpty();
  }

  @Test
  void shouldFailWithInvalidMaxMessages() {
    // When
    Result<List<ReceivedMessage<TestMessage>>> result = adapter.receive(100, Duration.ofSeconds(1));

    // Then
    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code()).isEqualTo(ErrorCode.of("INVALID_MAX_MESSAGES"));
  }

  @Test
  void shouldPurgeQueue() {
    // Given
    adapter.sendBatch(
        List.of(
            QueueMessage.<TestMessage>builder().payload(new TestMessage("Purge 1", 1)).build(),
            QueueMessage.<TestMessage>builder().payload(new TestMessage("Purge 2", 2)).build()));

    // When
    Result<Void> purgeResult = adapter.purge();

    // Then
    assertThat(purgeResult.isOk()).isTrue();

    // Wait for purge to complete
    try {
      Thread.sleep(500);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    // Verify queue is empty
    Result<List<ReceivedMessage<TestMessage>>> checkResult =
        adapter.receive(10, Duration.ofSeconds(1));
    assertThat(checkResult.getOrNull()).isEmpty();
  }

  @Test
  void shouldSendMessageWithDelay() {
    // Given
    QueueMessage<TestMessage> message =
        QueueMessage.<TestMessage>builder()
            .payload(new TestMessage("Delayed", 555))
            .delay(Duration.ofSeconds(3))
            .build();

    // When
    Result<SendMessageResult> sendResult = adapter.send(message);

    // Then
    assertThat(sendResult.isOk()).isTrue();

    // Message should not be immediately available
    Result<List<ReceivedMessage<TestMessage>>> immediateCheck =
        adapter.receive(1, Duration.ofSeconds(1));
    assertThat(immediateCheck.getOrNull()).isEmpty();

    // Wait for delay
    try {
      Thread.sleep(3500);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    // Message should now be available
    Result<List<ReceivedMessage<TestMessage>>> delayedCheck =
        adapter.receive(1, Duration.ofSeconds(1));
    assertThat(delayedCheck.getOrNull()).hasSize(1);
  }
}
