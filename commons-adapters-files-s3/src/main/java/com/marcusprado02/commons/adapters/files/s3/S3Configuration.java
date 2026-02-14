package com.marcusprado02.commons.adapters.files.s3;

import java.net.URI;
import java.util.Objects;

/**
 * Configuration for S3FileStoreAdapter.
 */
public record S3Configuration(
    String region,
    URI endpoint,
    boolean pathStyleAccessEnabled,
    long multipartThresholdBytes,
    int multipartChunkSizeBytes
) {

  public S3Configuration {
    Objects.requireNonNull(region, "region must not be null");
    if (multipartThresholdBytes <= 0) {
      throw new IllegalArgumentException("multipartThresholdBytes must be positive");
    }
    if (multipartChunkSizeBytes <= 0) {
      throw new IllegalArgumentException("multipartChunkSizeBytes must be positive");
    }
  }

  /**
   * Default configuration for AWS S3.
   */
  public static S3Configuration defaults(String region) {
    return new S3Configuration(
        region,
        null,
        false,
        5 * 1024 * 1024, // 5 MB
        5 * 1024 * 1024  // 5 MB
    );
  }

  /**
   * Configuration for LocalStack (testing).
   */
  public static S3Configuration localStack(String endpoint) {
    return new S3Configuration(
        "us-east-1",
        URI.create(endpoint),
        true,
        5 * 1024 * 1024,
        5 * 1024 * 1024
    );
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String region = "us-east-1";
    private URI endpoint;
    private boolean pathStyleAccessEnabled = false;
    private long multipartThresholdBytes = 5 * 1024 * 1024; // 5 MB
    private int multipartChunkSizeBytes = 5 * 1024 * 1024; // 5 MB

    public Builder region(String region) {
      this.region = region;
      return this;
    }

    public Builder endpoint(URI endpoint) {
      this.endpoint = endpoint;
      return this;
    }

    public Builder endpoint(String endpoint) {
      this.endpoint = URI.create(endpoint);
      return this;
    }

    public Builder pathStyleAccessEnabled(boolean pathStyleAccessEnabled) {
      this.pathStyleAccessEnabled = pathStyleAccessEnabled;
      return this;
    }

    public Builder multipartThresholdBytes(long multipartThresholdBytes) {
      this.multipartThresholdBytes = multipartThresholdBytes;
      return this;
    }

    public Builder multipartChunkSizeBytes(int multipartChunkSizeBytes) {
      this.multipartChunkSizeBytes = multipartChunkSizeBytes;
      return this;
    }

    public S3Configuration build() {
      return new S3Configuration(
          region,
          endpoint,
          pathStyleAccessEnabled,
          multipartThresholdBytes,
          multipartChunkSizeBytes
      );
    }
  }
}
