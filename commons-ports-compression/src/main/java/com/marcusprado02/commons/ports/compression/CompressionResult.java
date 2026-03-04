package com.marcusprado02.commons.ports.compression;

import java.time.Duration;
import java.time.Instant;

/**
 * Result of a compression or decompression operation.
 *
 * <p>This record provides detailed information about the compression operation, including size
 * metrics, performance data, and algorithm details.
 *
 * @param algorithm the compression algorithm used
 * @param originalSize original data size in bytes
 * @param compressedSize compressed data size in bytes
 * @param compressionRatio compression ratio (compressedSize / originalSize)
 * @param processingTime time taken for the operation
 * @param timestamp when the operation completed
 * @param checksumVerified whether checksum validation passed (if applicable)
 * @author Marcus Prado
 * @since 1.0.0
 */
public record CompressionResult(
    CompressionAlgorithm algorithm,
    long originalSize,
    long compressedSize,
    double compressionRatio,
    Duration processingTime,
    Instant timestamp,
    boolean checksumVerified) {

  /** Compact constructor with validation and calculations. */
  public CompressionResult {
    if (algorithm == null) {
      throw new IllegalArgumentException("Algorithm cannot be null");
    }
    if (originalSize < 0) {
      throw new IllegalArgumentException("Original size cannot be negative");
    }
    if (compressedSize < 0) {
      throw new IllegalArgumentException("Compressed size cannot be negative");
    }
    if (processingTime == null || processingTime.isNegative()) {
      throw new IllegalArgumentException("Processing time must be non-null and non-negative");
    }
    if (timestamp == null) {
      timestamp = Instant.now();
    }

    // Recalculate compression ratio to ensure consistency
    if (originalSize == 0) {
      compressionRatio = 1.0; // No compression possible for empty data
    } else {
      compressionRatio = (double) compressedSize / originalSize;
    }
  }

  /**
   * Creates a builder for compression result.
   *
   * @param algorithm the compression algorithm
   * @return builder instance
   */
  public static Builder builder(CompressionAlgorithm algorithm) {
    return new Builder(algorithm);
  }

  /**
   * Gets the space saved in bytes (original - compressed).
   *
   * @return space saved in bytes (can be negative if compression increased size)
   */
  public long getSpaceSaved() {
    return originalSize - compressedSize;
  }

  /**
   * Gets the space saved as a percentage.
   *
   * @return space saved percentage (0-100, can be negative)
   */
  public double getSpaceSavedPercentage() {
    if (originalSize == 0) {
      return 0.0;
    }
    return ((double) getSpaceSaved() / originalSize) * 100.0;
  }

  /**
   * Checks if compression actually reduced the size.
   *
   * @return true if compressed size is smaller than original
   */
  public boolean isEffective() {
    return compressedSize < originalSize;
  }

  /**
   * Gets processing throughput in bytes per second.
   *
   * @return throughput in bytes/second
   */
  public double getThroughputBytesPerSecond() {
    if (processingTime.isZero()) {
      return Double.POSITIVE_INFINITY;
    }
    return (double) originalSize / processingTime.toMillis() * 1000.0;
  }

  /**
   * Gets processing throughput in megabytes per second.
   *
   * @return throughput in MB/second
   */
  public double getThroughputMBPerSecond() {
    return getThroughputBytesPerSecond() / (1024.0 * 1024.0);
  }

  /**
   * Formats the result as a human-readable string.
   *
   * @return formatted result string
   */
  public String formatSummary() {
    return String.format(
        "%s: %,d → %,d bytes (%.1f%% saved, %.2f MB/s)",
        algorithm.name(),
        originalSize,
        compressedSize,
        getSpaceSavedPercentage(),
        getThroughputMBPerSecond());
  }

  /** Builder for compression result. */
  public static class Builder {
    private final CompressionAlgorithm algorithm;
    private long originalSize = 0;
    private long compressedSize = 0;
    private Duration processingTime = Duration.ZERO;
    private Instant timestamp = Instant.now();
    private boolean checksumVerified = false;

    private Builder(CompressionAlgorithm algorithm) {
      this.algorithm = algorithm;
    }

    /**
     * Sets original data size.
     *
     * @param size original size in bytes
     * @return this builder
     */
    public Builder originalSize(long size) {
      this.originalSize = size;
      return this;
    }

    /**
     * Sets compressed data size.
     *
     * @param size compressed size in bytes
     * @return this builder
     */
    public Builder compressedSize(long size) {
      this.compressedSize = size;
      return this;
    }

    /**
     * Sets processing time.
     *
     * @param time processing duration
     * @return this builder
     */
    public Builder processingTime(Duration time) {
      this.processingTime = time;
      return this;
    }

    /**
     * Sets timestamp.
     *
     * @param timestamp operation timestamp
     * @return this builder
     */
    public Builder timestamp(Instant timestamp) {
      this.timestamp = timestamp;
      return this;
    }

    /**
     * Sets checksum verification status.
     *
     * @param verified true if checksum was verified
     * @return this builder
     */
    public Builder checksumVerified(boolean verified) {
      this.checksumVerified = verified;
      return this;
    }

    /**
     * Builds the compression result.
     *
     * @return compression result
     */
    public CompressionResult build() {
      return new CompressionResult(
          algorithm,
          originalSize,
          compressedSize,
          0.0, // Will be recalculated in constructor
          processingTime,
          timestamp,
          checksumVerified);
    }
  }
}
