package com.marcusprado02.commons.adapters.queue.azure;

import static org.assertj.core.api.Assertions.assertThat;

import com.marcusprado02.commons.ports.queue.QueueMessage;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Branch-coverage tests that exercise validation paths inside AzureStorageQueueAdapter without
 * needing a real Azure Storage Queue service.
 *
 * <p>Construction uses the Azurite connection string; the create() call in the constructor will
 * fail silently (caught internally), so the adapter is usable for validation tests.
 */
class AzureStorageQueueBranchTest {

  private static AzureStorageQueueConfiguration stdConfig() {
    return AzureStorageQueueConfiguration.forAzurite("test-queue").build();
  }

  private static <T> AzureStorageQueueAdapter<T> adapter(Class<T> cls) {
    return new AzureStorageQueueAdapter<>(stdConfig(), cls);
  }

  // ── receive(): maxMessages validation ────────────────────────────────────

  @Test
  void receive_maxMessagesZero_returnsFail() {
    var result = adapter(String.class).receive(0, Duration.ofSeconds(30));
    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("INVALID_MAX_MESSAGES");
  }

  @Test
  void receive_maxMessages33_returnsFail() {
    var result = adapter(String.class).receive(33, Duration.ofSeconds(30));
    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("INVALID_MAX_MESSAGES");
  }

  @Test
  void receive_visibilityTimeoutOver7Days_returnsFail() {
    var result = adapter(String.class).receive(1, Duration.ofDays(8));
    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("INVALID_VISIBILITY_TIMEOUT");
  }

  // ── sendBatch(): batch validation ────────────────────────────────────────

  @Test
  void sendBatch_emptyList_returnsOk() {
    var result = adapter(String.class).sendBatch(List.of());
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull().successful()).isEmpty();
  }

  @Test
  void sendBatch_exceedsMaxBatchSize_returnsFail() {
    AzureStorageQueueConfiguration smallBatch =
        AzureStorageQueueConfiguration.forAzurite("q").maxBatchSize(1).build();
    var adapter = new AzureStorageQueueAdapter<>(smallBatch, String.class);
    List<QueueMessage<String>> messages =
        List.of(
            QueueMessage.<String>builder().payload("a").build(),
            QueueMessage.<String>builder().payload("b").build());
    var result = adapter.sendBatch(messages);
    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("BATCH_SIZE_EXCEEDED");
  }

  // ── deleteBatch(): batch validation ──────────────────────────────────────

  @Test
  void deleteBatch_emptyList_returnsOk() {
    var result = adapter(String.class).deleteBatch(List.of());
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull().successful()).isEmpty();
  }

  @Test
  void deleteBatch_exceedsMaxBatchSize_returnsFail() {
    AzureStorageQueueConfiguration smallBatch =
        AzureStorageQueueConfiguration.forAzurite("q").maxBatchSize(1).build();
    var adapter = new AzureStorageQueueAdapter<>(smallBatch, String.class);
    var result = adapter.deleteBatch(List.of("r1", "r2"));
    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("BATCH_SIZE_EXCEEDED");
  }

  // ── send(): message too large ─────────────────────────────────────────────

  @Test
  void send_messageLargerThan64KB_returnsFail() {
    // Build a string > 64KB
    String largePayload = "x".repeat(65 * 1024);
    QueueMessage<String> message = QueueMessage.<String>builder().payload(largePayload).build();
    var result = adapter(String.class).send(message);
    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("MESSAGE_TOO_LARGE");
  }

  // ── changeVisibility(): timeout validation ────────────────────────────────

  @Test
  void changeVisibility_visibilityTimeoutOver7Days_returnsFail() {
    var result = adapter(String.class).changeVisibility("msgId popReceipt", Duration.ofDays(8));
    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("INVALID_VISIBILITY_TIMEOUT");
  }

  // ── AzureStorageQueueConfiguration: factory branches ─────────────────────

  @Test
  void forAzure_returnsConfigWithGivenConnectionString() {
    AzureStorageQueueConfiguration config =
        AzureStorageQueueConfiguration.forAzure("custom-conn-str", "my-queue").build();
    assertThat(config.connectionString()).isEqualTo("custom-conn-str");
    assertThat(config.queueName()).isEqualTo("my-queue");
  }
}
