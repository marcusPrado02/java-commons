package com.marcusprado02.commons.adapters.queue.sqs;

import static org.assertj.core.api.Assertions.assertThat;

import com.marcusprado02.commons.ports.queue.QueueMessage;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.regions.Region;

/**
 * Branch coverage tests using a non-existent endpoint so network calls fail fast without requiring
 * LocalStack, covering validation and early-exit paths.
 */
class SqsBranchTest {

  private static SqsConfiguration stdConfig() {
    return SqsConfiguration.builder()
        .queueUrl("https://sqs.us-east-1.amazonaws.com/123456789/test-queue")
        .region(Region.US_EAST_1)
        .endpoint(URI.create("http://localhost:1"))
        .build();
  }

  private static SqsConfiguration fifoConfig() {
    return SqsConfiguration.builder()
        .queueUrl("https://sqs.us-east-1.amazonaws.com/123456789/test.fifo")
        .region(Region.US_EAST_1)
        .endpoint(URI.create("http://localhost:1"))
        .fifoQueue(true)
        .build();
  }

  private static SqsQueueAdapter<String> adapter(SqsConfiguration cfg) {
    return new SqsQueueAdapter<>(cfg, String.class);
  }

  // ── receive() validation branches ────────────────────────────────────────

  @Test
  void receive_maxMessagesLessThanOne_returnsFail() {
    var result = adapter(stdConfig()).receive(0, Duration.ofSeconds(30));
    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("INVALID_MAX_MESSAGES");
  }

  @Test
  void receive_maxMessagesGreaterThanTen_returnsFail() {
    var result = adapter(stdConfig()).receive(11, Duration.ofSeconds(30));
    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("INVALID_MAX_MESSAGES");
  }

  // ── sendBatch() validation branches ──────────────────────────────────────

  @Test
  void sendBatch_emptyList_returnsOkWithEmptyResult() {
    var result = adapter(stdConfig()).sendBatch(List.of());
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull().successful()).isEmpty();
    assertThat(result.getOrNull().failed()).isEmpty();
  }

  @Test
  void sendBatch_exceedsMaxBatchSize_returnsFail() {
    SqsConfiguration smallBatch =
        SqsConfiguration.builder()
            .queueUrl("https://sqs.us-east-1.amazonaws.com/123/q")
            .region(Region.US_EAST_1)
            .endpoint(URI.create("http://localhost:1"))
            .maxBatchSize(1)
            .build();
    List<QueueMessage<String>> messages =
        List.of(
            QueueMessage.<String>builder().payload("a").build(),
            QueueMessage.<String>builder().payload("b").build());
    var result = adapter(smallBatch).sendBatch(messages);
    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("BATCH_SIZE_EXCEEDED");
  }

  // ── deleteBatch() validation branches ────────────────────────────────────

  @Test
  void deleteBatch_emptyList_returnsOkWithEmptyResult() {
    var result = adapter(stdConfig()).deleteBatch(List.of());
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull().successful()).isEmpty();
    assertThat(result.getOrNull().failed()).isEmpty();
  }

  @Test
  void deleteBatch_exceedsMaxBatchSize_returnsFail() {
    SqsConfiguration smallBatch =
        SqsConfiguration.builder()
            .queueUrl("https://sqs.us-east-1.amazonaws.com/123/q")
            .region(Region.US_EAST_1)
            .endpoint(URI.create("http://localhost:1"))
            .maxBatchSize(1)
            .build();
    var result = adapter(smallBatch).deleteBatch(List.of("r1", "r2"));
    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("BATCH_SIZE_EXCEEDED");
  }

  // ── send() with attributes and delay → exception path ────────────────────

  @Test
  void send_withAttributesAndDelay_failsWithException() {
    QueueMessage<String> message =
        QueueMessage.<String>builder()
            .payload("hello")
            .attribute("key", "value")
            .delay(Duration.ofSeconds(5))
            .build();
    var result = adapter(stdConfig()).send(message);
    assertThat(result.isFail()).isTrue();
  }

  // ── send() with FIFO: messageGroupId + deduplicationId branches ──────────

  @Test
  void send_fifoWithGroupIdAndDedup_failsWithException() {
    QueueMessage<String> message =
        QueueMessage.<String>builder()
            .payload("fifo-msg")
            .messageGroupId("group-1")
            .deduplicationId("dedup-1")
            .build();
    var result = adapter(fifoConfig()).send(message);
    assertThat(result.isFail()).isTrue();
  }

  // ── sendBatch() with FIFO branches ───────────────────────────────────────

  @Test
  void sendBatch_fifoWithGroupIdAndDedup_failsWithException() {
    List<QueueMessage<String>> messages =
        List.of(
            QueueMessage.<String>builder()
                .payload("msg")
                .messageGroupId("g1")
                .deduplicationId("d1")
                .attribute("k", "v")
                .delay(Duration.ofSeconds(1))
                .build());
    var result = adapter(fifoConfig()).sendBatch(messages);
    assertThat(result.isFail()).isTrue();
  }

  // ── SqsConfiguration static factory methods ──────────────────────────────

  @Test
  void forLocalStack_returnsConfigWithLocalEndpoint() {
    SqsConfiguration config = SqsConfiguration.forLocalStack("http://sqs/q").build();
    assertThat(config.endpoint()).isNotNull();
    assertThat(config.region()).isEqualTo(Region.US_EAST_1);
  }

  @Test
  void forAws_returnsConfigWithNoEndpoint() {
    SqsConfiguration config = SqsConfiguration.forAws("https://sqs/q", Region.EU_WEST_1).build();
    assertThat(config.endpoint()).isNull();
    assertThat(config.region()).isEqualTo(Region.EU_WEST_1);
  }

  @Test
  void configuration_invalidMaxBatchSize_throwsIllegalArgument() {
    org.junit.jupiter.api.Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            SqsConfiguration.builder()
                .queueUrl("q")
                .region(Region.US_EAST_1)
                .maxBatchSize(11)
                .build());
  }

  // ── methods that reach SQS and fail (Exception catch paths) ──────────────

  @Test
  void receive_validParams_callsSqsAndFails() {
    var result = adapter(stdConfig()).receive(5, Duration.ofSeconds(30));
    assertThat(result.isFail()).isTrue();
  }

  @Test
  void delete_callsSqsAndFails() {
    var result = adapter(stdConfig()).delete("some-receipt-handle");
    assertThat(result.isFail()).isTrue();
  }

  @Test
  void deleteBatch_validParams_callsAndFails() {
    var result = adapter(stdConfig()).deleteBatch(List.of("handle-1", "handle-2"));
    assertThat(result.isFail()).isTrue();
  }

  @Test
  void changeVisibility_callsAndFails() {
    var result = adapter(stdConfig()).changeVisibility("receipt-handle", Duration.ofSeconds(30));
    assertThat(result.isFail()).isTrue();
  }

  @Test
  void purge_callsAndFails() {
    var result = adapter(stdConfig()).purge();
    assertThat(result.isFail()).isTrue();
  }

  @Test
  void getAttributes_callsAndFails() {
    var result = adapter(stdConfig()).getAttributes();
    assertThat(result.isFail()).isTrue();
  }

  // ── createSqsClient: endpoint == null branch ──────────────────────────────

  @Test
  void constructor_noEndpoint_createsAdapterSuccessfully() {
    SqsConfiguration config =
        SqsConfiguration.forAws("https://sqs.us-east-1.amazonaws.com/123456789/q", Region.US_EAST_1)
            .build();
    assertThat(new SqsQueueAdapter<>(config, String.class)).isNotNull();
  }
}
