# Commons Adapters - Compression

Multi-algorithm compression adapters supporting GZIP, DEFLATE, Brotli, LZ4, ZSTD, and Snappy compression algorithms.

## Features

- **6 Compression Algorithms**: Support for GZIP, DEFLATE, Brotli, LZ4, ZSTD, and Snappy
- **Streaming Compression**: Memory-efficient processing for large datasets
- **Automatic Algorithm Detection**: Validate compressed data format
- **Performance Metrics**: Detailed compression statistics and throughput measurements
- **Adaptive Selection**: Content-aware algorithm selection
- **Thread Safety**: All implementations are thread-safe
- **Robust Error Handling**: Result pattern with comprehensive error information

## Supported Algorithms

| Algorithm | Library | Speed | Ratio | Best Use Case |
|-----------|---------|-------|-------|---------------|
| GZIP | Java built-in | Good | Good | General purpose, HTTP |
| DEFLATE | Java built-in | Good | Good | ZIP files, HTTP |
| Brotli | Brotli4j 1.16.0 | Slow | Excellent | Web assets, static content |
| LZ4 | LZ4-Java 1.8.0 | Very Fast | Fair | Real-time, logs, streaming |
| ZSTD | Zstd-JNI 1.5.5-11 | Fast | Very Good | Modern general purpose |
| Snappy | Snappy-Java 1.1.10.5 | Very Fast | Fair | Database, network protocols |

## Quick Start

### Basic Usage

```java
// Create compression adapter
CompressionPort compressionPort = CompressionFactory.createCompressionPort();

// Compress data
CompressionOptions options = CompressionOptions.defaultFor(CompressionAlgorithm.GZIP);
Result<byte[]> result = compressionPort.compress(originalData, options);

if (result.isOk()) {
    byte[] compressed = result.getValue();
    System.out.printf("Compressed %d bytes to %d bytes%n",
        originalData.length, compressed.length);
}
```

### Streaming Compression

```java
// Create streaming adapter for large files
StreamingCompressionPort streamingPort = CompressionFactory.createStreamingCompressionPort();

CompressionOptions options = CompressionOptions.fastFor(CompressionAlgorithm.LZ4);
Result<StreamingCompressor> compressorResult = streamingPort.createCompressor(outputStream, options);

if (compressorResult.isOk()) {
    try (StreamingCompressor compressor = compressorResult.getValue()) {
        // Process data in chunks
        byte[] buffer = new byte[64 * 1024]; // 64KB chunks
        int bytesRead;

        while ((bytesRead = inputStream.read(buffer)) != -1) {
            compressor.write(buffer, 0, bytesRead);
        }

        // Finish and get statistics
        Result<CompressionResult> stats = compressor.finish();
        if (stats.isOk()) {
            System.out.println(stats.getValue().formatSummary());
        }
    }
}
```

## Algorithm Selection

### Performance-Based Selection

```java
// Speed-optimized configuration
CompressionConfiguration speedConfig = CompressionFactory.createSpeedOptimizedConfiguration();
CompressionOptions speedOptions = speedConfig.createDefaultOptions(); // Uses LZ4

// Ratio-optimized configuration
CompressionConfiguration ratioConfig = CompressionFactory.createRatioOptimizedConfiguration();
CompressionOptions ratioOptions = ratioConfig.createDefaultOptions(); // Uses Brotli

// Balanced configuration
CompressionConfiguration balancedConfig = CompressionFactory.createBalancedConfiguration();
CompressionOptions balancedOptions = balancedConfig.createDefaultOptions(); // Uses ZSTD
```

### Adaptive Algorithm Selection

```java
// Create adaptive compressor that selects algorithm based on content
CompressionFactory.AdaptiveCompressionPort adaptive =
    new CompressionFactory.AdaptiveCompressionPort(balancedConfig);

// Algorithm selection based on data characteristics
CompressionAlgorithm algorithm = adaptive.selectAlgorithm(dataSize, "application/json");
CompressionOptions options = balancedConfig.createOptions(algorithm);

Result<byte[]> compressed = adaptive.compress(data, options);
```

### Content-Aware Selection

```java
public CompressionAlgorithm selectByContent(byte[] data, String mimeType) {
    // Text content benefits from better compression ratios
    if (HttpCompression.isCompressible(mimeType)) {
        if (data.length > 1024 * 1024) { // Large text files
            return CompressionAlgorithm.BROTLI; // Best ratio
        } else {
            return CompressionAlgorithm.GZIP;   // Good balance
        }
    }

    // Binary data or real-time scenarios prefer speed
    if (data.length > 100 * 1024 * 1024) { // Very large files
        return CompressionAlgorithm.LZ4;       // Fastest
    } else {
        return CompressionAlgorithm.ZSTD;      // Good balance
    }
}
```

## Configuration Options

### Algorithm-Specific Tuning

```java
// GZIP with maximum compression
CompressionOptions gzipMax = CompressionOptions.builder(CompressionAlgorithm.GZIP)
    .level(9)           // Maximum compression
    .bufferSize(128 * 1024)  // 128KB buffer
    .headerEnabled(true)     // Include headers
    .checksumEnabled(true)   // Enable CRC32 checksum
    .build();

// LZ4 for streaming with large buffers
CompressionOptions lz4Stream = CompressionOptions.builder(CompressionAlgorithm.LZ4)
    .level(1)                // Fastest LZ4
    .bufferSize(1024 * 1024) // 1MB buffer for throughput
    .build();

// Brotli for web content
CompressionOptions brotliWeb = CompressionOptions.builder(CompressionAlgorithm.BROTLI)
    .level(4)           // Good balance for web
    .bufferSize(32 * 1024)   // 32KB for HTTP streams
    .build();
```

### Compression Level Guide

```java
// Fast compression (prioritize speed)
CompressionOptions fast = CompressionOptions.fastFor(algorithm);

// Best compression (prioritize ratio)
CompressionOptions best = CompressionOptions.bestFor(algorithm);

// Default balanced compression
CompressionOptions balanced = CompressionOptions.defaultFor(algorithm);

// Custom levels by algorithm
int level = switch (algorithm) {
    case GZIP, DEFLATE -> 6;  // 1-9, default 6
    case BROTLI -> 4;         // 0-11, default 6
    case LZ4 -> 1;            // 1-12, default 1
    case SNAPPY -> 1;         // Fixed at 1
    case ZSTD -> 3;           // 1-22, default 3
};
```

## Streaming Operations

### Large File Compression

```java
public void compressLargeFile(Path inputPath, Path outputPath, CompressionAlgorithm algorithm) {
    CompressionOptions options = CompressionOptions.builder(algorithm)
        .bufferSize(1024 * 1024)  // 1MB buffer for large files
        .level(algorithm == CompressionAlgorithm.LZ4 ? 1 : 6)
        .build();

    StreamingCompressionPort streamingPort = CompressionFactory.createStreamingCompressionPort();

    try (InputStream input = Files.newInputStream(inputPath);
         OutputStream output = Files.newOutputStream(outputPath)) {

        Result<StreamingCompressor> result = streamingPort.createCompressor(output, options);

        if (result.isOk()) {
            try (StreamingCompressor compressor = result.getValue()) {
                byte[] buffer = new byte[options.bufferSize()];
                int bytesRead;

                while ((bytesRead = input.read(buffer)) != -1) {
                    Result<Void> writeResult = compressor.write(buffer, 0, bytesRead);
                    if (!writeResult.isOk()) {
                        throw new IOException("Compression failed: " + writeResult.getProblem().getDetail());
                    }
                }

                Result<CompressionResult> finishResult = compressor.finish();
                if (finishResult.isOk()) {
                    CompressionResult stats = finishResult.getValue();
                    System.out.printf("Compression completed: %s%n", stats.formatSummary());
                    System.out.printf("Throughput: %.2f MB/s%n", stats.getThroughputMBPerSecond());
                }
            }
        }
    } catch (IOException e) {
        System.err.println("File compression failed: " + e.getMessage());
    }
}
```

### Progressive Decompression

```java
public void decompressProgressively(InputStream compressedInput, OutputStream output,
                                  CompressionOptions options) {
    StreamingCompressionPort streamingPort = CompressionFactory.createStreamingCompressionPort();

    Result<StreamingDecompressor> result = streamingPort.createDecompressor(compressedInput, options);

    if (result.isOk()) {
        try (StreamingDecompressor decompressor = result.getValue()) {
            byte[] buffer = new byte[64 * 1024];

            while (true) {
                Result<Integer> readResult = decompressor.read(buffer);
                if (!readResult.isOk()) {
                    System.err.println("Read error: " + readResult.getProblem().getDetail());
                    break;
                }

                int bytesRead = readResult.getValue();
                if (bytesRead == -1) break; // End of stream

                output.write(buffer, 0, bytesRead);

                // Optional: check if more data is available
                Result<Boolean> hasMore = decompressor.hasMore();
                if (hasMore.isOk() && !hasMore.getValue()) {
                    break;
                }
            }

            CompressionResult stats = decompressor.getCurrentStats();
            System.out.printf("Decompression stats: %s%n", stats.formatSummary());

        } catch (IOException e) {
            System.err.println("Decompression failed: " + e.getMessage());
        }
    }
}
```

## Performance Optimization

### Buffer Size Tuning

```java
// Buffer size recommendations by use case
public int getOptimalBufferSize(CompressionAlgorithm algorithm, long dataSize) {
    return switch (algorithm) {
        case LZ4, SNAPPY -> {
            // Fast algorithms benefit from larger buffers
            if (dataSize > 100 * 1024 * 1024) yield 2 * 1024 * 1024; // 2MB
            else if (dataSize > 10 * 1024 * 1024) yield 1024 * 1024;  // 1MB
            else yield 256 * 1024; // 256KB
        }
        case BROTLI -> {
            // Slower algorithms use smaller buffers to reduce memory pressure
            if (dataSize > 50 * 1024 * 1024) yield 256 * 1024; // 256KB
            else yield 64 * 1024; // 64KB
        }
        case GZIP, DEFLATE, ZSTD -> {
            // Balanced algorithms use moderate buffers
            if (dataSize > 50 * 1024 * 1024) yield 512 * 1024; // 512KB
            else yield 128 * 1024; // 128KB
        }
    };
}
```

### Compression Level Selection

```java
public int selectCompressionLevel(CompressionAlgorithm algorithm, boolean prioritizeSpeed, long dataSize) {
    if (prioritizeSpeed) {
        return switch (algorithm) {
            case GZIP, DEFLATE -> 1;
            case BROTLI -> 0;
            case LZ4, SNAPPY -> 1;
            case ZSTD -> 1;
        };
    }

    // For large data, use moderate levels to balance time vs ratio
    if (dataSize > 100 * 1024 * 1024) {
        return switch (algorithm) {
            case GZIP, DEFLATE -> 4;
            case BROTLI -> 4;
            case LZ4 -> 3;
            case SNAPPY -> 1;
            case ZSTD -> 3;
        };
    }

    // For smaller data, can afford higher compression
    return switch (algorithm) {
        case GZIP, DEFLATE -> 6;
        case BROTLI -> 6;
        case LZ4 -> 6;
        case SNAPPY -> 1;
        case ZSTD -> 6;
    };
}
```

## Validation and Testing

### Compression Effectiveness

```java
public void validateCompression(byte[] originalData, CompressionAlgorithm algorithm) {
    CompressionPort port = CompressionFactory.createCompressionPort();
    CompressionOptions options = CompressionOptions.defaultFor(algorithm);

    // Test compression ratio
    Result<Double> ratioResult = port.getCompressionRatio(originalData, options);
    if (ratioResult.isOk()) {
        double ratio = ratioResult.getValue();
        System.out.printf("%s compression ratio: %.3f%n", algorithm, ratio);

        if (ratio > 0.95) {
            System.out.println("Warning: Low compression effectiveness (< 5% reduction)");
        }
    }

    // Test round-trip integrity
    Result<byte[]> compressed = port.compress(originalData, options);
    if (compressed.isOk()) {
        Result<byte[]> decompressed = port.decompress(compressed.getValue(), options);
        if (decompressed.isOk()) {
            boolean identical = Arrays.equals(originalData, decompressed.getValue());
            System.out.printf("%s round-trip integrity: %s%n", algorithm,
                identical ? "PASS" : "FAIL");
        }
    }
}
```

### Format Detection

```java
public void detectCompressionFormat(byte[] data) {
    CompressionPort port = CompressionFactory.createCompressionPort();

    for (CompressionAlgorithm algorithm : port.getSupportedAlgorithms()) {
        Result<Boolean> isCompressed = port.isCompressed(data, algorithm);
        if (isCompressed.isOk() && isCompressed.getValue()) {
            System.out.printf("Data appears to be compressed with: %s%n", algorithm);
            return;
        }
    }

    System.out.println("Data does not appear to be compressed with any supported algorithm");
}
```

## Error Handling

### Comprehensive Error Handling

```java
public void handleCompressionErrors(byte[] data, CompressionOptions options) {
    CompressionPort port = CompressionFactory.createCompressionPort();
    Result<byte[]> result = port.compress(data, options);

    if (result.isOk()) {
        byte[] compressed = result.getValue();
        System.out.printf("Compression successful: %d -> %d bytes%n",
            data.length, compressed.length);
    } else {
        Problem problem = result.getProblem();
        System.err.printf("Compression failed: %s%n", problem.getDetail());

        // Handle specific error categories
        switch (problem.getCategory()) {
            case INVALID_INPUT -> {
                System.err.println("Invalid input parameters - check data and options");
            }
            case INFRASTRUCTURE -> {
                System.err.println("I/O or system error - check available memory/disk space");
                if (problem.getCause() != null) {
                    problem.getCause().printStackTrace();
                }
            }
            case BUSINESS_RULE -> {
                System.err.println("Business validation failed");
            }
        }

        // Log error details for debugging
        System.err.printf("Error code: %s, Severity: %s%n",
            problem.getType(), problem.getSeverity());
    }
}
```

## Dependencies

```xml
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-adapters-compression</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## Algorithm Availability

The adapter automatically detects available compression libraries:

- **GZIP/DEFLATE**: Always available (Java built-in)
- **Brotli**: Available if Brotli4j native libraries are loaded
- **LZ4**: Available if LZ4-Java is on classpath
- **ZSTD**: Available if Zstd-JNI native libraries are loaded
- **Snappy**: Available if Snappy-Java native libraries are loaded

Use `getSupportedAlgorithms()` to check runtime availability:

```java
CompressionPort port = CompressionFactory.createCompressionPort();
CompressionAlgorithm[] supported = port.getSupportedAlgorithms();
System.out.println("Supported algorithms: " + Arrays.toString(supported));
```

## Thread Safety

- **CompressionPort**: Thread-safe, can be shared across threads
- **StreamingCompressionPort**: Thread-safe factory methods
- **StreamingCompressor/Decompressor**: NOT thread-safe, use one instance per thread
- **CompressionOptions/Result**: Immutable, thread-safe

## Performance Benchmarks

Approximate performance characteristics on modern hardware:

| Algorithm | Compression Speed | Decompression Speed | Typical Ratio |
|-----------|-------------------|---------------------|---------------|
| LZ4 | 400-600 MB/s | 1200-2000 MB/s | 50-60% |
| Snappy | 250-400 MB/s | 800-1200 MB/s | 50-65% |
| GZIP(6) | 80-120 MB/s | 300-500 MB/s | 30-40% |
| ZSTD(3) | 100-200 MB/s | 400-800 MB/s | 25-35% |
| Brotli(6) | 20-40 MB/s | 200-400 MB/s | 20-30% |

*Benchmarks vary significantly based on data characteristics, CPU, and memory configuration.*
