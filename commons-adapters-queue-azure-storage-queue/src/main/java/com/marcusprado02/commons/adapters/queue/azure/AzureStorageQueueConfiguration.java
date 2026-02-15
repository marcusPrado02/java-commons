package com.marcusprado02.commons.adapters.queue.azure;

import java.time.Duration;
import java.util.Objects;

/**
 * Configuration for Azure Storage Queue adapter.
 *
 * @param connectionString Azure Storage connection string
 * @param queueName Queue name
 * @param requestTimeout Request timeout duration
 * @param maxBatchSize Maximum number of messages in batch operations
 */
public record AzureStorageQueueConfiguration(
    String connectionString, String queueName, Duration requestTimeout, int maxBatchSize) {

  public AzureStorageQueueConfiguration {
    Objects.requireNonNull(connectionString, "connectionString cannot be null");
    Objects.requireNonNull(queueName, "queueName cannot be null");
    Objects.requireNonNull(requestTimeout, "requestTimeout cannot be null");

    if (maxBatchSize < 1 || maxBatchSize > 32) {
      throw new IllegalArgumentException("maxBatchSize must be between 1 and 32");
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates configuration for Azurite local development.
   *
   * @param queueName queue name
   * @return configuration builder
   */
  public static Builder forAzurite(String queueName) {
    return builder()
        .connectionString(
            "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;"
                + "AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;"
                + "QueueEndpoint=http://127.0.0.1:10001/devstoreaccount1;")
        .queueName(queueName);
  }

  /**
   * Creates configuration for Azure Cloud.
   *
   * @param connectionString Azure Storage connection string
   * @param queueName queue name
   * @return configuration builder
   */
  public static Builder forAzure(String connectionString, String queueName) {
    return builder().connectionString(connectionString).queueName(queueName);
  }

  public static class Builder {
    private String connectionString;
    private String queueName;
    private Duration requestTimeout = Duration.ofSeconds(30);
    private int maxBatchSize = 32;

    public Builder connectionString(String connectionString) {
      this.connectionString = connectionString;
      return this;
    }

    public Builder queueName(String queueName) {
      this.queueName = queueName;
      return this;
    }

    public Builder requestTimeout(Duration requestTimeout) {
      this.requestTimeout = requestTimeout;
      return this;
    }

    public Builder maxBatchSize(int maxBatchSize) {
      this.maxBatchSize = maxBatchSize;
      return this;
    }

    public AzureStorageQueueConfiguration build() {
      return new AzureStorageQueueConfiguration(
          connectionString, queueName, requestTimeout, maxBatchSize);
    }
  }
}
