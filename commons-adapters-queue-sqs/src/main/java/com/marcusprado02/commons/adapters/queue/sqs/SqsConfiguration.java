package com.marcusprado02.commons.adapters.queue.sqs;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;
import software.amazon.awssdk.regions.Region;

/** Configuration for AWS SQS queue adapter. */
public record SqsConfiguration(
    String queueUrl,
    Region region,
    URI endpoint,
    Duration requestTimeout,
    int maxBatchSize,
    boolean fifoQueue) {

  public SqsConfiguration {
    Objects.requireNonNull(queueUrl, "queueUrl cannot be null");
    Objects.requireNonNull(region, "region cannot be null");
    if (maxBatchSize < 1 || maxBatchSize > 10) {
      throw new IllegalArgumentException("maxBatchSize must be between 1 and 10");
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  /** Creates configuration for LocalStack testing. */
  public static Builder forLocalStack(String queueUrl) {
    return builder()
        .queueUrl(queueUrl)
        .region(Region.US_EAST_1)
        .endpoint(URI.create("http://localhost:4566"));
  }

  /** Creates configuration for AWS production. */
  public static Builder forAws(String queueUrl, Region region) {
    return builder().queueUrl(queueUrl).region(region);
  }

  public static final class Builder {
    private String queueUrl;
    private Region region = Region.US_EAST_1;
    private URI endpoint;
    private Duration requestTimeout = Duration.ofSeconds(10);
    private int maxBatchSize = 10;
    private boolean fifoQueue = false;

    private Builder() {}

    public Builder queueUrl(String queueUrl) {
      this.queueUrl = queueUrl;
      return this;
    }

    public Builder region(Region region) {
      this.region = region;
      return this;
    }

    public Builder endpoint(URI endpoint) {
      this.endpoint = endpoint;
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

    public Builder fifoQueue(boolean fifoQueue) {
      this.fifoQueue = fifoQueue;
      return this;
    }

    public SqsConfiguration build() {
      return new SqsConfiguration(
          queueUrl, region, endpoint, requestTimeout, maxBatchSize, fifoQueue);
    }
  }
}
