/**
 * Commons Ports - Compression API.
 *
 * <p>This package provides port interfaces for compression operations, supporting multiple
 * compression algorithms including GZIP, Brotli, LZ4, ZSTD, and Snappy.
 *
 * <h2>Core Interfaces</h2>
 *
 * <ul>
 *   <li>{@link com.marcusprado02.commons.ports.compression.CompressionPort} - Main compression
 *       interface
 *   <li>{@link com.marcusprado02.commons.ports.compression.StreamingCompressionPort} - Streaming
 *       compression for large data
 * </ul>
 *
 * <h2>Configuration</h2>
 *
 * <ul>
 *   <li>{@link com.marcusprado02.commons.ports.compression.CompressionOptions} - Configuration
 *       options
 *   <li>{@link com.marcusprado02.commons.ports.compression.CompressionAlgorithm} - Supported
 *       algorithms
 * </ul>
 *
 * <h2>Results and Metrics</h2>
 *
 * <ul>
 *   <li>{@link com.marcusprado02.commons.ports.compression.CompressionResult} - Operation results
 *       with metrics
 * </ul>
 *
 * <h2>HTTP Integration</h2>
 *
 * <ul>
 *   <li>{@link com.marcusprado02.commons.ports.compression.HttpCompression} - HTTP compression
 *       utilities
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Basic Compression</h3>
 *
 * <pre>{@code
 * CompressionOptions options = CompressionOptions.defaultFor(CompressionAlgorithm.GZIP);
 * Result<byte[]> result = compressionPort.compress(data, options);
 *
 * if (result.isOk()) {
 *     byte[] compressed = result.getValue();
 *     System.out.println("Compressed " + data.length + " to " + compressed.length + " bytes");
 * }
 * }</pre>
 *
 * <h3>Streaming Compression</h3>
 *
 * <pre>{@code
 * CompressionOptions options = CompressionOptions.fastFor(CompressionAlgorithm.LZ4);
 * Result<StreamingCompressor> result = streamingPort.createCompressor(output, options);
 *
 * if (result.isOk()) {
 *     try (StreamingCompressor compressor = result.getValue()) {
 *         compressor.write(chunk1);
 *         compressor.write(chunk2);
 *         CompressionResult stats = compressor.finish().getValue();
 *         System.out.println(stats.formatSummary());
 *     }
 * }
 * }</pre>
 *
 * <h3>HTTP Compression</h3>
 *
 * <pre>{@code
 * String acceptEncoding = request.getHeader("Accept-Encoding");
 * CompressionAlgorithm algorithm = HttpCompression.selectBestAlgorithm(
 *     acceptEncoding,
 *     compressionPort.getSupportedAlgorithms()
 * );
 *
 * if (algorithm != null && HttpCompression.shouldCompress(contentLength, mimeType, 1024)) {
 *     CompressionOptions options = HttpCompression.createHttpOptions(algorithm, false);
 *     Result<byte[]> compressed = compressionPort.compress(content, options);
 *
 *     response.setHeader("Content-Encoding", algorithm.getAlgorithmName());
 *     response.setHeader("Vary", "Accept-Encoding");
 * }
 * }</pre>
 *
 * <h2>Algorithm Characteristics</h2>
 *
 * <table border="1">
 *   <tr>
 *     <th>Algorithm</th>
 *     <th>Speed</th>
 *     <th>Compression Ratio</th>
 *     <th>Best Use Case</th>
 *   </tr>
 *   <tr>
 *     <td>GZIP</td>
 *     <td>Good</td>
 *     <td>Good</td>
 *     <td>General purpose, widely supported</td>
 *   </tr>
 *   <tr>
 *     <td>Brotli</td>
 *     <td>Slow</td>
 *     <td>Excellent</td>
 *     <td>Web content, static assets</td>
 *   </tr>
 *   <tr>
 *     <td>LZ4</td>
 *     <td>Very Fast</td>
 *     <td>Fair</td>
 *     <td>Real-time compression, logs</td>
 *   </tr>
 *   <tr>
 *     <td>ZSTD</td>
 *     <td>Fast</td>
 *     <td>Very Good</td>
 *     <td>Modern general purpose</td>
 *   </tr>
 *   <tr>
 *     <td>Snappy</td>
 *     <td>Very Fast</td>
 *     <td>Fair</td>
 *     <td>Database storage, network protocols</td>
 *   </tr>
 * </table>
 *
 * @author Marcus Prado
 * @version 1.0.0
 * @since 1.0.0
 */
package com.marcusprado02.commons.ports.compression;
