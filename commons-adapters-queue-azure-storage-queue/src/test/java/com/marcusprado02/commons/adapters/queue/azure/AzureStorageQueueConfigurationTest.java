package com.marcusprado02.commons.adapters.queue.azure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class AzureStorageQueueConfigurationTest {

  private static final String CONN =
      "DefaultEndpointsProtocol=https;AccountName=test;AccountKey=abc";

  @Test
  void builder_shouldCreateConfigurationWithDefaults() {
    AzureStorageQueueConfiguration config =
        AzureStorageQueueConfiguration.builder()
            .connectionString(CONN)
            .queueName("my-queue")
            .build();

    assertThat(config.connectionString()).isEqualTo(CONN);
    assertThat(config.queueName()).isEqualTo("my-queue");
    assertThat(config.requestTimeout()).isEqualTo(Duration.ofSeconds(30));
    assertThat(config.maxBatchSize()).isEqualTo(32);
  }

  @Test
  void builder_shouldCreateFullConfiguration() {
    AzureStorageQueueConfiguration config =
        AzureStorageQueueConfiguration.builder()
            .connectionString(CONN)
            .queueName("my-queue")
            .requestTimeout(Duration.ofSeconds(10))
            .maxBatchSize(5)
            .build();

    assertThat(config.requestTimeout()).isEqualTo(Duration.ofSeconds(10));
    assertThat(config.maxBatchSize()).isEqualTo(5);
  }

  @Test
  void forAzurite_shouldSetConnectionStringAndQueueName() {
    AzureStorageQueueConfiguration config =
        AzureStorageQueueConfiguration.forAzurite("test-queue").build();

    assertThat(config.queueName()).isEqualTo("test-queue");
    assertThat(config.connectionString()).contains("devstoreaccount1");
    assertThat(config.connectionString()).contains("QueueEndpoint=http://127.0.0.1:10001");
  }

  @Test
  void forAzure_shouldSetConnectionStringAndQueueName() {
    AzureStorageQueueConfiguration config =
        AzureStorageQueueConfiguration.forAzure(CONN, "prod-queue").build();

    assertThat(config.connectionString()).isEqualTo(CONN);
    assertThat(config.queueName()).isEqualTo("prod-queue");
  }

  @Test
  void builder_withMinimumBatchSize_shouldSucceed() {
    AzureStorageQueueConfiguration config =
        AzureStorageQueueConfiguration.builder()
            .connectionString(CONN)
            .queueName("q")
            .maxBatchSize(1)
            .build();
    assertThat(config.maxBatchSize()).isEqualTo(1);
  }

  @Test
  void builder_withMaximumBatchSize_shouldSucceed() {
    AzureStorageQueueConfiguration config =
        AzureStorageQueueConfiguration.builder()
            .connectionString(CONN)
            .queueName("q")
            .maxBatchSize(32)
            .build();
    assertThat(config.maxBatchSize()).isEqualTo(32);
  }

  @Test
  void constructor_withNullConnectionString_shouldThrow() {
    assertThatThrownBy(
            () -> new AzureStorageQueueConfiguration(null, "my-queue", Duration.ofSeconds(30), 32))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void constructor_withNullQueueName_shouldThrow() {
    assertThatThrownBy(
            () -> new AzureStorageQueueConfiguration(CONN, null, Duration.ofSeconds(30), 32))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void constructor_withNullRequestTimeout_shouldThrow() {
    assertThatThrownBy(() -> new AzureStorageQueueConfiguration(CONN, "q", null, 32))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void constructor_withInvalidMaxBatchSize_shouldThrow() {
    assertThatThrownBy(
            () -> new AzureStorageQueueConfiguration(CONN, "q", Duration.ofSeconds(30), 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("maxBatchSize must be between 1 and 32");

    assertThatThrownBy(
            () -> new AzureStorageQueueConfiguration(CONN, "q", Duration.ofSeconds(30), 33))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("maxBatchSize must be between 1 and 32");
  }
}
