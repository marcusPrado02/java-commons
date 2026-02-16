package com.marcusprado02.commons.ports.compression;

import java.util.Objects;

/**
 * Configuration options for compression operations.
 *
 * <p>This record provides a type-safe way to configure compression parameters
 * such as algorithm, compression level, buffer size, and other algorithm-specific settings.
 *
 * @param algorithm the compression algorithm to use
 * @param level compression level (algorithm-specific, typically 1-9)
 * @param bufferSize buffer size for streaming operations (bytes)
 * @param headerEnabled whether to include headers (for applicable algorithms)
 * @param checksumEnabled whether to include checksum validation
 * @param windowSize compression window size (for algorithms that support it)
 *
 * @author Marcus Prado
 * @since 1.0.0
 */
public record CompressionOptions(
        CompressionAlgorithm algorithm,
        int level,
        int bufferSize,
        boolean headerEnabled,
        boolean checksumEnabled,
        int windowSize
) {

    /**
     * Default buffer size for streaming operations (64KB).
     */
    public static final int DEFAULT_BUFFER_SIZE = 64 * 1024;

    /**
     * Default compression window size.
     */
    public static final int DEFAULT_WINDOW_SIZE = 15;

    /**
     * Compact constructor with validation.
     */
    public CompressionOptions {
        Objects.requireNonNull(algorithm, "Algorithm cannot be null");

        if (bufferSize <= 0) {
            throw new IllegalArgumentException("Buffer size must be positive");
        }

        if (windowSize < 1 || windowSize > 15) {
            throw new IllegalArgumentException("Window size must be between 1 and 15");
        }

        // Validate compression level based on algorithm
        int minLevel = getMinCompressionLevel(algorithm);
        int maxLevel = getMaxCompressionLevel(algorithm);

        if (level < minLevel || level > maxLevel) {
            throw new IllegalArgumentException(
                String.format("Compression level %d is not valid for %s (valid range: %d-%d)",
                    level, algorithm, minLevel, maxLevel)
            );
        }
    }

    /**
     * Creates a builder for compression options.
     *
     * @param algorithm the compression algorithm
     * @return builder instance
     */
    public static Builder builder(CompressionAlgorithm algorithm) {
        return new Builder(algorithm);
    }

    /**
     * Creates default compression options for the specified algorithm.
     *
     * @param algorithm the compression algorithm
     * @return default options
     */
    public static CompressionOptions defaultFor(CompressionAlgorithm algorithm) {
        return builder(algorithm).build();
    }

    /**
     * Creates fast compression options (prioritizing speed over ratio).
     *
     * @param algorithm the compression algorithm
     * @return fast compression options
     */
    public static CompressionOptions fastFor(CompressionAlgorithm algorithm) {
        return builder(algorithm)
                .level(getMinCompressionLevel(algorithm))
                .build();
    }

    /**
     * Creates best compression options (prioritizing ratio over speed).
     *
     * @param algorithm the compression algorithm
     * @return best compression options
     */
    public static CompressionOptions bestFor(CompressionAlgorithm algorithm) {
        return builder(algorithm)
                .level(getMaxCompressionLevel(algorithm))
                .build();
    }

    /**
     * Gets minimum compression level for the algorithm.
     */
    private static int getMinCompressionLevel(CompressionAlgorithm algorithm) {
        return switch (algorithm) {
            case GZIP, DEFLATE -> 1;
            case BROTLI -> 0;
            case LZ4, SNAPPY -> 1;
            case ZSTD -> 1;
        };
    }

    /**
     * Gets maximum compression level for the algorithm.
     */
    private static int getMaxCompressionLevel(CompressionAlgorithm algorithm) {
        return switch (algorithm) {
            case GZIP, DEFLATE -> 9;
            case BROTLI -> 11;
            case LZ4 -> 12;
            case SNAPPY -> 1; // Snappy has only one level
            case ZSTD -> 22;
        };
    }

    /**
     * Gets default compression level for the algorithm.
     */
    private static int getDefaultCompressionLevel(CompressionAlgorithm algorithm) {
        return switch (algorithm) {
            case GZIP, DEFLATE -> 6;
            case BROTLI -> 6;
            case LZ4 -> 1;
            case SNAPPY -> 1;
            case ZSTD -> 3;
        };
    }

    /**
     * Builder for compression options.
     */
    public static class Builder {
        private final CompressionAlgorithm algorithm;
        private int level;
        private int bufferSize = DEFAULT_BUFFER_SIZE;
        private boolean headerEnabled = true;
        private boolean checksumEnabled = true;
        private int windowSize = DEFAULT_WINDOW_SIZE;

        private Builder(CompressionAlgorithm algorithm) {
            this.algorithm = Objects.requireNonNull(algorithm, "Algorithm cannot be null");
            this.level = getDefaultCompressionLevel(algorithm);
        }

        /**
         * Sets compression level.
         *
         * @param level compression level
         * @return this builder
         */
        public Builder level(int level) {
            this.level = level;
            return this;
        }

        /**
         * Sets buffer size for streaming operations.
         *
         * @param bufferSize buffer size in bytes
         * @return this builder
         */
        public Builder bufferSize(int bufferSize) {
            this.bufferSize = bufferSize;
            return this;
        }

        /**
         * Sets whether to include headers.
         *
         * @param enabled true to include headers
         * @return this builder
         */
        public Builder headerEnabled(boolean enabled) {
            this.headerEnabled = enabled;
            return this;
        }

        /**
         * Sets whether to include checksum validation.
         *
         * @param enabled true to include checksum
         * @return this builder
         */
        public Builder checksumEnabled(boolean enabled) {
            this.checksumEnabled = enabled;
            return this;
        }

        /**
         * Sets compression window size.
         *
         * @param windowSize window size (1-15)
         * @return this builder
         */
        public Builder windowSize(int windowSize) {
            this.windowSize = windowSize;
            return this;
        }

        /**
         * Builds the compression options.
         *
         * @return compression options
         */
        public CompressionOptions build() {
            return new CompressionOptions(
                algorithm,
                level,
                bufferSize,
                headerEnabled,
                checksumEnabled,
                windowSize
            );
        }
    }
}
