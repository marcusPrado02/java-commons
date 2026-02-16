# Commons Ports - Compression

Port interfaces for compression operations supporting multiple algorithms including GZIP, Brotli, LZ4, ZSTD, and Snappy.

## Features

- **Multiple Algorithms**: Support for 6 compression algorithms with different performance characteristics
- **Streaming Support**: Process large datasets without loading everything into memory
- **HTTP Integration**: Built-in utilities for web API compression
- **Performance Metrics**: Detailed statistics including compression ratios and throughput
- **Type Safety**: Result pattern for robust error handling
- **Thread Safe**: All operations are thread-safe and suitable for concurrent use

## Supported Algorithms

| Algorithm | Speed | Ratio | Best Use Case |
|-----------|-------|-------|---------------|
| GZIP | Good | Good | General purpose, widely supported |
| DEFLATE | Good | Good | ZIP files, HTTP compression |
| Brotli | Slow | Excellent | Web content, static assets |
| LZ4 | Very Fast | Fair | Real-time compression, logs |
| ZSTD | Fast | Very Good | Modern general purpose |
| Snappy | Very Fast | Fair | Database storage, network protocols |

## Basic Usage

### Simple Compression

```java
// Create compression options
CompressionOptions options = CompressionOptions.defaultFor(CompressionAlgorithm.GZIP);

// Compress data
Result<byte[]> result = compressionPort.compress(data, options);

if (result.isOk()) {
    byte[] compressed = result.getValue();
    System.out.printf("Original: %d bytes, Compressed: %d bytes%n",
        data.length, compressed.length);
}
```

### Stream Compression

```java
// Create streaming compressor
CompressionOptions options = CompressionOptions.fastFor(CompressionAlgorithm.LZ4);
Result<StreamingCompressor> result = streamingPort.createCompressor(outputStream, options);

if (result.isOk()) {
    try (StreamingCompressor compressor = result.getValue()) {
        // Compress data in chunks
        compressor.write(chunk1);
        compressor.write(chunk2);
        compressor.write(chunk3);

        // Finish and get statistics
        Result<CompressionResult> statsResult = compressor.finish();
        if (statsResult.isOk()) {
            CompressionResult stats = statsResult.getValue();
            System.out.println(stats.formatSummary());
        }
    }
}
```

### Decompression

```java
// Decompress data
CompressionOptions options = CompressionOptions.defaultFor(CompressionAlgorithm.GZIP);
Result<byte[]> result = compressionPort.decompress(compressedData, options);

if (result.isOk()) {
    byte[] decompressed = result.getValue();
    System.out.println("Decompressed " + decompressed.length + " bytes");
}
```

## Configuration Options

### Compression Levels

```java
// Fast compression (prioritize speed)
CompressionOptions fast = CompressionOptions.fastFor(CompressionAlgorithm.GZIP);

// Best compression (prioritize ratio)
CompressionOptions best = CompressionOptions.bestFor(CompressionAlgorithm.GZIP);

// Custom configuration
CompressionOptions custom = CompressionOptions.builder(CompressionAlgorithm.GZIP)
    .level(6)                    // Compression level
    .bufferSize(64 * 1024)      // 64KB buffer
    .headerEnabled(true)         // Include headers
    .checksumEnabled(true)       // Enable checksum validation
    .windowSize(15)              // Window size
    .build();
```

### Streaming Configuration

```java
// Large buffer for high-throughput scenarios
CompressionOptions throughput = CompressionOptions.builder(CompressionAlgorithm.LZ4)
    .bufferSize(1024 * 1024)    // 1MB buffer
    .level(1)                    // Fastest compression
    .build();

// Balanced configuration
CompressionOptions balanced = CompressionOptions.builder(CompressionAlgorithm.ZSTD)
    .bufferSize(256 * 1024)     // 256KB buffer
    .level(3)                   // Default ZSTD level
    .build();
```

## HTTP Integration

### Content Negotiation

```java
// Parse client preferences from Accept-Encoding header
String acceptEncoding = request.getHeader("Accept-Encoding");
List<CompressionAlgorithm> clientPrefs = HttpCompression.parseAcceptEncoding(acceptEncoding);

// Select best algorithm based on server capabilities
CompressionAlgorithm[] supported = compressionPort.getSupportedAlgorithms();
CompressionAlgorithm selected = HttpCompression.selectBestAlgorithm(acceptEncoding, supported);
```

### Response Compression

```java
// Check if content should be compressed
String mimeType = "application/json";
long contentLength = responseData.length;
boolean shouldCompress = HttpCompression.shouldCompress(contentLength, mimeType, 1024);

if (shouldCompress && selected != null) {
    // Create HTTP-optimized compression options
    CompressionOptions options = HttpCompression.createHttpOptions(selected, false);

    // Compress response
    Result<byte[]> compressed = compressionPort.compress(responseData, options);

    if (compressed.isOk()) {
        // Set response headers
        response.setHeader("Content-Encoding", selected.getAlgorithmName());
        response.setHeader("Vary", "Accept-Encoding");
        response.setHeader("Content-Length", String.valueOf(compressed.getValue().length));

        // Write compressed data
        response.getOutputStream().write(compressed.getValue());
    }
}
```

### Middleware Example

```java
@Component
public class CompressionMiddleware {

    private final CompressionPort compressionPort;

    public CompressionMiddleware(CompressionPort compressionPort) {
        this.compressionPort = compressionPort;
    }

    public void processResponse(HttpServletRequest request, HttpServletResponse response, byte[] content) {
        String acceptEncoding = request.getHeader("Accept-Encoding");
        String mimeType = response.getContentType();

        CompressionAlgorithm algorithm = HttpCompression.selectBestAlgorithm(
            acceptEncoding,
            compressionPort.getSupportedAlgorithms()
        );

        if (algorithm != null && HttpCompression.shouldCompress(content.length, mimeType, 1024)) {
            CompressionOptions options = HttpCompression.createHttpOptions(algorithm, false);
            Result<byte[]> result = compressionPort.compress(content, options);

            if (result.isOk()) {
                response.setHeader("Content-Encoding", algorithm.getAlgorithmName());
                response.setHeader("Vary", "Accept-Encoding");
                // ... write compressed content
            }
        }
    }
}
```

## Performance Considerations

### Algorithm Selection by Use Case

```java
// Real-time data (logs, metrics)
CompressionOptions realTime = CompressionOptions.fastFor(CompressionAlgorithm.LZ4);

// Web assets (CSS, JS, HTML)
CompressionOptions webAssets = CompressionOptions.builder(CompressionAlgorithm.BROTLI)
    .level(6)  // Good balance for static content
    .build();

// Database backups (storage-optimized)
CompressionOptions backups = CompressionOptions.bestFor(CompressionAlgorithm.ZSTD);

// Network protocols (speed-optimized)
CompressionOptions network = CompressionOptions.fastFor(CompressionAlgorithm.SNAPPY);
```

### Memory Management

```java
// For large files, use streaming compression
Result<StreamingCompressor> result = streamingPort.createCompressor(output, options);
if (result.isOk()) {
    try (StreamingCompressor compressor = result.getValue()) {
        byte[] buffer = new byte[64 * 1024]; // 64KB chunks
        int bytesRead;

        while ((bytesRead = input.read(buffer)) != -1) {
            compressor.write(buffer, 0, bytesRead);
        }

        CompressionResult stats = compressor.finish().getValue();
        System.out.printf("Processed %d MB at %.2f MB/s%n",
            stats.originalSize() / (1024 * 1024),
            stats.getThroughputMBPerSecond());
    }
}
```

## Error Handling

```java
// Comprehensive error handling
Result<byte[]> result = compressionPort.compress(data, options);

if (result.isOk()) {
    byte[] compressed = result.getValue();
    // Process compressed data
} else {
    Problem problem = result.getProblem();
    System.err.printf("Compression failed: %s (code: %s)%n",
        problem.getDetail(), problem.getType());

    // Handle specific error types
    switch (problem.getType()) {
        case "compression/invalid-options" -> {
            // Handle invalid configuration
        }
        case "compression/io-error" -> {
            // Handle I/O issues
        }
        case "compression/unsupported-algorithm" -> {
            // Handle unsupported algorithm
        }
    }
}
```

## Validation and Testing

```java
// Validate compression effectiveness
Result<Double> ratioResult = compressionPort.getCompressionRatio(data, options);
if (ratioResult.isOk()) {
    double ratio = ratioResult.getValue();
    if (ratio > 0.9) {  // Less than 10% compression
        System.out.println("Warning: Low compression ratio, consider different algorithm");
    }
}

// Verify data integrity
Result<byte[]> compressed = compressionPort.compress(originalData, options);
Result<byte[]> decompressed = compressionPort.decompress(compressed.getValue(), options);

if (compressed.isOk() && decompressed.isOk()) {
    boolean identical = Arrays.equals(originalData, decompressed.getValue());
    System.out.println("Data integrity: " + (identical ? "OK" : "FAILED"));
}
```

## Integration Examples

### Spring Boot Configuration

```java
@Configuration
@EnableConfigurationProperties(CompressionProperties.class)
public class CompressionConfiguration {

    @Bean
    public CompressionPort compressionPort() {
        // Return compression adapter implementation
        return new MultiAlgorithmCompressionAdapter();
    }

    @Bean
    public StreamingCompressionPort streamingCompressionPort(CompressionPort compressionPort) {
        return new StreamingCompressionAdapter(compressionPort);
    }
}

@ConfigurationProperties(prefix = "compression")
public class CompressionProperties {
    private CompressionAlgorithm defaultAlgorithm = CompressionAlgorithm.GZIP;
    private int defaultLevel = 6;
    private int bufferSize = 64 * 1024;

    // Getters and setters
}
```

## Dependencies

```xml
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-ports-compression</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## Implementation

This module provides only the port interfaces. For concrete implementations, use:

- `commons-adapters-compression` - Multi-algorithm compression adapter

## Thread Safety

All interfaces in this module are designed to be thread-safe:

- **CompressionOptions**: Immutable record, safe to share
- **CompressionResult**: Immutable record, safe to share
- **CompressionPort**: Implementations should be thread-safe
- **StreamingCompressionPort**: Individual compressor/decompressor instances are not thread-safe, but the factory methods are
