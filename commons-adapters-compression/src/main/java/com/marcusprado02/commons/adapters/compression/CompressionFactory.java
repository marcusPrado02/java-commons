package com.marcusprado02.commons.adapters.compression;

import com.marcusprado02.commons.ports.compression.CompressionAlgorithm;
import com.marcusprado02.commons.ports.compression.CompressionOptions;
import com.marcusprado02.commons.ports.compression.CompressionPort;
import com.marcusprado02.commons.ports.compression.StreamingCompressionPort;
import java.util.Objects;

/**
 * Factory class for creating compression adapters with various configurations.
 *
 * <p>This factory provides convenient methods for creating compression adapters with different
 * performance profiles and algorithm preferences.
 *
 * @author Marcus Prado
 * @since 1.0.0
 */
public final class CompressionFactory {

  private CompressionFactory() {
    // Utility class
  }

  /**
   * Creates a multi-algorithm compression adapter with default configuration.
   *
   * @return compression port supporting all available algorithms
   */
  public static CompressionPort createCompressionPort() {
    return new MultiAlgorithmCompressionAdapter();
  }

  /**
   * Creates a streaming compression adapter with default configuration.
   *
   * @return streaming compression port for large data processing
   */
  public static StreamingCompressionPort createStreamingCompressionPort() {
    return new StreamingCompressionAdapter();
  }

  /**
   * Creates a compression configuration optimized for speed. Uses fast algorithms with minimal
   * compression levels.
   *
   * @return configuration for speed-optimized compression
   */
  public static CompressionConfiguration createSpeedOptimizedConfiguration() {
    return CompressionConfiguration.builder()
        .defaultAlgorithm(CompressionAlgorithm.LZ4)
        .fallbackAlgorithm(CompressionAlgorithm.SNAPPY)
        .preferSpeed(true)
        .maxCompressionLevel(3)
        .bufferSize(256 * 1024) // 256KB for high throughput
        .build();
  }

  /**
   * Creates a compression configuration optimized for compression ratio. Uses algorithms that
   * provide best compression ratios with higher levels.
   *
   * @return configuration for ratio-optimized compression
   */
  public static CompressionConfiguration createRatioOptimizedConfiguration() {
    return CompressionConfiguration.builder()
        .defaultAlgorithm(CompressionAlgorithm.BROTLI)
        .fallbackAlgorithm(CompressionAlgorithm.ZSTD)
        .preferSpeed(false)
        .maxCompressionLevel(9)
        .bufferSize(64 * 1024) // 64KB for better ratio
        .build();
  }

  /**
   * Creates a compression configuration balanced between speed and ratio. Uses moderate compression
   * levels with versatile algorithms.
   *
   * @return configuration for balanced compression
   */
  public static CompressionConfiguration createBalancedConfiguration() {
    return CompressionConfiguration.builder()
        .defaultAlgorithm(CompressionAlgorithm.ZSTD)
        .fallbackAlgorithm(CompressionAlgorithm.GZIP)
        .preferSpeed(false)
        .maxCompressionLevel(6)
        .bufferSize(128 * 1024) // 128KB balanced buffer
        .build();
  }

  /**
   * Creates a compression configuration for web/HTTP usage. Uses algorithms commonly supported by
   * web browsers and proxies.
   *
   * @return configuration for web-optimized compression
   */
  public static CompressionConfiguration createWebOptimizedConfiguration() {
    return CompressionConfiguration.builder()
        .defaultAlgorithm(CompressionAlgorithm.BROTLI)
        .fallbackAlgorithm(CompressionAlgorithm.GZIP)
        .preferSpeed(false)
        .maxCompressionLevel(6)
        .bufferSize(32 * 1024) // 32KB for HTTP streams
        .enableChecksums(true)
        .build();
  }

  /** Configuration class for compression adapters. */
  public static class CompressionConfiguration {
    private final CompressionAlgorithm defaultAlgorithm;
    private final CompressionAlgorithm fallbackAlgorithm;
    private final boolean preferSpeed;
    private final int maxCompressionLevel;
    private final int bufferSize;
    private final boolean enableChecksums;
    private final boolean enableHeaders;

    private CompressionConfiguration(Builder builder) {
      this.defaultAlgorithm = builder.defaultAlgorithm;
      this.fallbackAlgorithm = builder.fallbackAlgorithm;
      this.preferSpeed = builder.preferSpeed;
      this.maxCompressionLevel = builder.maxCompressionLevel;
      this.bufferSize = builder.bufferSize;
      this.enableChecksums = builder.enableChecksums;
      this.enableHeaders = builder.enableHeaders;
    }

    public CompressionAlgorithm getDefaultAlgorithm() {
      return defaultAlgorithm;
    }

    public CompressionAlgorithm getFallbackAlgorithm() {
      return fallbackAlgorithm;
    }

    public boolean isPreferSpeed() {
      return preferSpeed;
    }

    public int getMaxCompressionLevel() {
      return maxCompressionLevel;
    }

    public int getBufferSize() {
      return bufferSize;
    }

    public boolean isEnableChecksums() {
      return enableChecksums;
    }

    public boolean isEnableHeaders() {
      return enableHeaders;
    }

    /**
     * Creates compression options for the specified algorithm using this configuration.
     *
     * @param algorithm compression algorithm
     * @return compression options
     */
    public CompressionOptions createOptions(CompressionAlgorithm algorithm) {
      Objects.requireNonNull(algorithm, "Algorithm cannot be null");

      int level;
      if (preferSpeed) {
        level =
            switch (algorithm) {
              case GZIP, DEFLATE -> 1;
              case BROTLI -> 0;
              case LZ4, SNAPPY -> 1;
              case ZSTD -> 1;
            };
      } else {
        level =
            Math.min(
                maxCompressionLevel,
                switch (algorithm) {
                  case GZIP, DEFLATE -> 9;
                  case BROTLI -> 11;
                  case LZ4 -> 12;
                  case SNAPPY -> 1;
                  case ZSTD -> 22;
                });
      }

      return CompressionOptions.builder(algorithm)
          .level(level)
          .bufferSize(bufferSize)
          .headerEnabled(enableHeaders)
          .checksumEnabled(enableChecksums)
          .build();
    }

    /**
     * Creates compression options using the default algorithm.
     *
     * @return compression options with default algorithm
     */
    public CompressionOptions createDefaultOptions() {
      return createOptions(defaultAlgorithm);
    }

    /**
     * Creates compression options using the fallback algorithm.
     *
     * @return compression options with fallback algorithm
     */
    public CompressionOptions createFallbackOptions() {
      return createOptions(fallbackAlgorithm);
    }

    public static Builder builder() {
      return new Builder();
    }

    public static class Builder {
      private CompressionAlgorithm defaultAlgorithm = CompressionAlgorithm.GZIP;
      private CompressionAlgorithm fallbackAlgorithm = CompressionAlgorithm.DEFLATE;
      private boolean preferSpeed = false;
      private int maxCompressionLevel = 6;
      private int bufferSize = 64 * 1024; // 64KB
      private boolean enableChecksums = true;
      private boolean enableHeaders = true;

      public Builder defaultAlgorithm(CompressionAlgorithm algorithm) {
        this.defaultAlgorithm =
            Objects.requireNonNull(algorithm, "Default algorithm cannot be null");
        return this;
      }

      public Builder fallbackAlgorithm(CompressionAlgorithm algorithm) {
        this.fallbackAlgorithm =
            Objects.requireNonNull(algorithm, "Fallback algorithm cannot be null");
        return this;
      }

      public Builder preferSpeed(boolean preferSpeed) {
        this.preferSpeed = preferSpeed;
        return this;
      }

      public Builder maxCompressionLevel(int level) {
        if (level < 0) {
          throw new IllegalArgumentException("Compression level cannot be negative");
        }
        this.maxCompressionLevel = level;
        return this;
      }

      public Builder bufferSize(int size) {
        if (size <= 0) {
          throw new IllegalArgumentException("Buffer size must be positive");
        }
        this.bufferSize = size;
        return this;
      }

      public Builder enableChecksums(boolean enable) {
        this.enableChecksums = enable;
        return this;
      }

      public Builder enableHeaders(boolean enable) {
        this.enableHeaders = enable;
        return this;
      }

      public CompressionConfiguration build() {
        return new CompressionConfiguration(this);
      }
    }
  }

  /** Adaptive compression port that selects algorithms based on content characteristics. */
  public static class AdaptiveCompressionPort implements CompressionPort {
    private final CompressionPort delegate;
    private final CompressionConfiguration configuration;

    public AdaptiveCompressionPort(CompressionConfiguration configuration) {
      this.delegate = new MultiAlgorithmCompressionAdapter();
      this.configuration = Objects.requireNonNull(configuration, "Configuration cannot be null");
    }

    /**
     * Selects the best algorithm for the given data characteristics.
     *
     * @param dataSize size of data to compress
     * @param contentType MIME type of content (optional)
     * @return recommended compression algorithm
     */
    public CompressionAlgorithm selectAlgorithm(long dataSize, String contentType) {
      // For small data, prefer speed
      if (dataSize < 1024) { // Less than 1KB
        return CompressionAlgorithm.LZ4;
      }

      // For very large data, prefer streaming-friendly algorithms
      if (dataSize > 100 * 1024 * 1024) { // More than 100MB
        return configuration.isPreferSpeed() ? CompressionAlgorithm.LZ4 : CompressionAlgorithm.ZSTD;
      }

      // For text content, prefer better compression ratio
      if (isTextContent(contentType)) {
        return configuration.isPreferSpeed()
            ? CompressionAlgorithm.GZIP
            : CompressionAlgorithm.BROTLI;
      }

      // Default to configuration preference
      return configuration.getDefaultAlgorithm();
    }

    private boolean isTextContent(String contentType) {
      if (contentType == null) {
        return false;
      }
      String normalized = contentType.toLowerCase();
      return normalized.startsWith("text/")
          || normalized.contains("json")
          || normalized.contains("xml")
          || normalized.contains("javascript")
          || normalized.contains("css");
    }

    // Delegate all CompressionPort methods to the underlying implementation
    @Override
    public com.marcusprado02.commons.kernel.result.Result<
            com.marcusprado02.commons.ports.compression.CompressionResult>
        compress(
            java.io.InputStream input, java.io.OutputStream output, CompressionOptions options) {
      return delegate.compress(input, output, options);
    }

    @Override
    public com.marcusprado02.commons.kernel.result.Result<
            com.marcusprado02.commons.ports.compression.CompressionResult>
        decompress(
            java.io.InputStream input, java.io.OutputStream output, CompressionOptions options) {
      return delegate.decompress(input, output, options);
    }

    @Override
    public com.marcusprado02.commons.kernel.result.Result<byte[]> compress(
        byte[] data, CompressionOptions options) {
      return delegate.compress(data, options);
    }

    @Override
    public com.marcusprado02.commons.kernel.result.Result<byte[]> decompress(
        byte[] compressedData, CompressionOptions options) {
      return delegate.decompress(compressedData, options);
    }

    @Override
    public com.marcusprado02.commons.kernel.result.Result<Double> getCompressionRatio(
        byte[] data, CompressionOptions options) {
      return delegate.getCompressionRatio(data, options);
    }

    @Override
    public com.marcusprado02.commons.kernel.result.Result<Boolean> isCompressed(
        byte[] data, CompressionAlgorithm algorithm) {
      return delegate.isCompressed(data, algorithm);
    }

    @Override
    public CompressionAlgorithm[] getSupportedAlgorithms() {
      return delegate.getSupportedAlgorithms();
    }

    @Override
    public int getDefaultCompressionLevel(CompressionAlgorithm algorithm) {
      return delegate.getDefaultCompressionLevel(algorithm);
    }
  }
}
