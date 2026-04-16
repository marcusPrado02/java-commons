package com.marcusprado02.commons.adapters.queue.sqs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.regions.Region;

class SqsConfigurationTest {

  @Test
  void builder_shouldCreateConfigurationWithDefaults() {
    SqsConfiguration config =
        SqsConfiguration.builder()
            .queueUrl("https://sqs.us-east-1.amazonaws.com/123456789/my-queue")
            .region(Region.US_EAST_1)
            .build();

    assertThat(config.queueUrl())
        .isEqualTo("https://sqs.us-east-1.amazonaws.com/123456789/my-queue");
    assertThat(config.region()).isEqualTo(Region.US_EAST_1);
    assertThat(config.endpoint()).isNull();
    assertThat(config.requestTimeout()).isEqualTo(Duration.ofSeconds(10));
    assertThat(config.maxBatchSize()).isEqualTo(10);
    assertThat(config.fifoQueue()).isFalse();
  }

  @Test
  void builder_shouldCreateFullConfiguration() {
    URI endpoint = URI.create("http://localhost:4566");
    SqsConfiguration config =
        SqsConfiguration.builder()
            .queueUrl("https://sqs.eu-west-1.amazonaws.com/123/queue.fifo")
            .region(Region.EU_WEST_1)
            .endpoint(endpoint)
            .requestTimeout(Duration.ofSeconds(30))
            .maxBatchSize(5)
            .fifoQueue(true)
            .build();

    assertThat(config.region()).isEqualTo(Region.EU_WEST_1);
    assertThat(config.endpoint()).isEqualTo(endpoint);
    assertThat(config.requestTimeout()).isEqualTo(Duration.ofSeconds(30));
    assertThat(config.maxBatchSize()).isEqualTo(5);
    assertThat(config.fifoQueue()).isTrue();
  }

  @Test
  void forLocalStack_shouldSetRegionAndEndpoint() {
    SqsConfiguration config =
        SqsConfiguration.forLocalStack("http://localhost:4566/000000000000/my-queue").build();

    assertThat(config.queueUrl()).isEqualTo("http://localhost:4566/000000000000/my-queue");
    assertThat(config.region()).isEqualTo(Region.US_EAST_1);
    assertThat(config.endpoint()).isEqualTo(URI.create("http://localhost:4566"));
  }

  @Test
  void forAws_shouldSetQueueUrlAndRegion() {
    SqsConfiguration config =
        SqsConfiguration.forAws(
                "https://sqs.us-west-2.amazonaws.com/123456789/my-queue", Region.US_WEST_2)
            .build();

    assertThat(config.queueUrl())
        .isEqualTo("https://sqs.us-west-2.amazonaws.com/123456789/my-queue");
    assertThat(config.region()).isEqualTo(Region.US_WEST_2);
    assertThat(config.endpoint()).isNull();
  }

  @Test
  void builder_withMinimumBatchSize_shouldSucceed() {
    SqsConfiguration config =
        SqsConfiguration.builder()
            .queueUrl("https://sqs/queue")
            .region(Region.US_EAST_1)
            .maxBatchSize(1)
            .build();
    assertThat(config.maxBatchSize()).isEqualTo(1);
  }

  @Test
  void constructor_withNullQueueUrl_shouldThrow() {
    assertThatThrownBy(
            () ->
                new SqsConfiguration(
                    null, Region.US_EAST_1, null, Duration.ofSeconds(10), 10, false))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void constructor_withNullRegion_shouldThrow() {
    assertThatThrownBy(
            () ->
                new SqsConfiguration(
                    "https://sqs/queue", null, null, Duration.ofSeconds(10), 10, false))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void constructor_withInvalidMaxBatchSize_shouldThrow() {
    assertThatThrownBy(
            () ->
                new SqsConfiguration(
                    "https://sqs/queue", Region.US_EAST_1, null, Duration.ofSeconds(10), 0, false))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("maxBatchSize must be between 1 and 10");

    assertThatThrownBy(
            () ->
                new SqsConfiguration(
                    "https://sqs/queue", Region.US_EAST_1, null, Duration.ofSeconds(10), 11, false))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("maxBatchSize must be between 1 and 10");
  }
}
