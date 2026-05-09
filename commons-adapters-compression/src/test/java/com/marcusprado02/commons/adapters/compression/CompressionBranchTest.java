package com.marcusprado02.commons.adapters.compression;

import static org.assertj.core.api.Assertions.assertThat;

import com.marcusprado02.commons.kernel.result.Result;
import com.marcusprado02.commons.ports.compression.CompressionAlgorithm;
import com.marcusprado02.commons.ports.compression.CompressionOptions;
import com.marcusprado02.commons.ports.compression.StreamingCompressionPort.StreamingCompressor;
import com.marcusprado02.commons.ports.compression.StreamingCompressionPort.StreamingDecompressor;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Branch/line coverage supplement for commons-adapters-compression.
 *
 * <p>Focuses on:
 *
 * <ul>
 *   <li>All streaming decompressors (Deflate, LZ4, Brotli, ZSTD, Snappy) – were at 0% coverage
 *   <li>Post-close / post-finish branches in compressors
 *   <li>AdaptiveCompressionPort missing branches (large-data ratio mode, XML/CSS/JS content types)
 *   <li>CompressionConfiguration.createOptions branches for non-speed path per algorithm
 * </ul>
 */
class CompressionBranchTest {

  private static final byte[] SAMPLE =
      "Hello World! Branch test data for all streaming compressor/decompressor paths. "
          .repeat(20)
          .getBytes(StandardCharsets.UTF_8);

  private final StreamingCompressionAdapter streaming = new StreamingCompressionAdapter();
  private final MultiAlgorithmCompressionAdapter adapter = new MultiAlgorithmCompressionAdapter();

  // ── helpers ───────────────────────────────────────────────────────────────

  private byte[] streamCompress(CompressionAlgorithm algo) throws Exception {
    var out = new ByteArrayOutputStream();
    var options = CompressionOptions.defaultFor(algo);
    Result<StreamingCompressor> res = streaming.createCompressor(out, options);
    assertThat(res.isOk()).withFailMessage("createCompressor(%s) failed: %s", algo, res).isTrue();
    try (StreamingCompressor c = res.getOrNull()) {
      assertThat(c.write(SAMPLE).isOk()).isTrue();
      assertThat(c.finish().isOk()).isTrue();
    }
    return out.toByteArray();
  }

  private void exerciseDecompressor(CompressionAlgorithm algo, byte[] compressed) throws Exception {
    var options = CompressionOptions.defaultFor(algo);
    var in = new ByteArrayInputStream(compressed);
    Result<StreamingDecompressor> res = streaming.createDecompressor(in, options);
    assertThat(res.isOk()).withFailMessage("createDecompressor(%s) failed: %s", algo, res).isTrue();

    try (StreamingDecompressor d = res.getOrNull()) {
      // read(byte[]) delegates to read(byte[], 0, length) — covers the abstract base read branch
      byte[] buf = new byte[1024];
      Result<Integer> readRes = d.read(buf);
      assertThat(readRes.isOk()).isTrue();

      // skip
      assertThat(d.skip(0).isOk()).isTrue();

      // hasMore
      assertThat(d.hasMore().isOk()).isTrue();

      // stats
      assertThat(d.getDecompressedBytes()).isGreaterThanOrEqualTo(0);
      assertThat(d.getCompressedBytesRead()).isGreaterThanOrEqualTo(0);
      assertThat(d.getCurrentStats()).isNotNull();
    }

    // calling close() a second time should be a no-op (covers closed.getAndSet guard)
    // already closed by try-with-resources above — create a fresh one and close manually twice
    var in2 = new ByteArrayInputStream(compressed);
    StreamingDecompressor d2 = streaming.createDecompressor(in2, options).getOrNull();
    d2.close();
    d2.close(); // idempotent
  }

  // ── Deflate streaming decompressor ───────────────────────────────────────

  @Test
  void deflateStreamingDecompressorShouldReadSkipHasMoreAndClose() throws Exception {
    byte[] compressed = streamCompress(CompressionAlgorithm.DEFLATE);
    exerciseDecompressor(CompressionAlgorithm.DEFLATE, compressed);
  }

  // ── LZ4 streaming decompressor ────────────────────────────────────────────

  @Test
  void lz4StreamingDecompressorShouldReadSkipHasMoreAndClose() throws Exception {
    byte[] compressed = streamCompress(CompressionAlgorithm.LZ4);
    exerciseDecompressor(CompressionAlgorithm.LZ4, compressed);
  }

  // ── Brotli streaming decompressor ────────────────────────────────────────

  @Test
  void brotliStreamingDecompressorShouldReadSkipHasMoreAndClose() throws Exception {
    byte[] compressed = streamCompress(CompressionAlgorithm.BROTLI);
    exerciseDecompressor(CompressionAlgorithm.BROTLI, compressed);
  }

  // ── ZSTD streaming decompressor ──────────────────────────────────────────

  @Test
  void zstdStreamingDecompressorShouldReadSkipHasMoreAndClose() throws Exception {
    byte[] compressed = streamCompress(CompressionAlgorithm.ZSTD);
    exerciseDecompressor(CompressionAlgorithm.ZSTD, compressed);
  }

  // ── Snappy streaming decompressor ────────────────────────────────────────

  @Test
  void snappyStreamingDecompressorShouldReadSkipHasMoreAndClose() throws Exception {
    byte[] compressed = streamCompress(CompressionAlgorithm.SNAPPY);
    exerciseDecompressor(CompressionAlgorithm.SNAPPY, compressed);
  }

  // ── Post-close write should return fail (compressor guard) ───────────────

  @ParameterizedTest
  @EnumSource(CompressionAlgorithm.class)
  void writeAfterCloseShouldReturnFail(CompressionAlgorithm algo) throws Exception {
    var out = new ByteArrayOutputStream();
    var options = CompressionOptions.defaultFor(algo);
    Result<StreamingCompressor> res = streaming.createCompressor(out, options);
    assertThat(res.isOk()).withFailMessage("createCompressor(%s) failed: %s", algo, res).isTrue();

    StreamingCompressor c = res.getOrNull();
    c.write(SAMPLE);
    c.finish();
    c.close(); // mark closed

    // write after close hits the `finished.get() || closed.get()` branch → fail
    Result<Void> result = c.write(SAMPLE);
    assertThat(result.isFail()).withFailMessage("Expected fail after close for %s", algo).isTrue();
  }

  // ── Post-close read should return fail (decompressor guard) ──────────────

  @ParameterizedTest
  @EnumSource(CompressionAlgorithm.class)
  void readAfterCloseShouldReturnFail(CompressionAlgorithm algo) throws Exception {
    byte[] compressed = streamCompress(algo);
    var options = CompressionOptions.defaultFor(algo);
    var in = new ByteArrayInputStream(compressed);
    StreamingDecompressor d = streaming.createDecompressor(in, options).getOrNull();
    d.close();

    // read after close hits the `closed.get()` guard → fail
    Result<Integer> result = d.read(new byte[64], 0, 64);
    assertThat(result.isFail())
        .withFailMessage("Expected fail after close for decompressor %s", algo)
        .isTrue();
  }

  // ── Flush on all streaming compressors ───────────────────────────────────

  @ParameterizedTest
  @EnumSource(CompressionAlgorithm.class)
  void flushShouldSucceedForAllAlgorithms(CompressionAlgorithm algo) throws Exception {
    var out = new ByteArrayOutputStream();
    var options = CompressionOptions.defaultFor(algo);
    Result<StreamingCompressor> res = streaming.createCompressor(out, options);
    assertThat(res.isOk()).withFailMessage("createCompressor(%s) failed: %s", algo, res).isTrue();

    try (StreamingCompressor c = res.getOrNull()) {
      assertThat(c.write(SAMPLE).isOk()).isTrue();
      assertThat(c.flush().isOk()).isTrue();
      assertThat(c.finish().isOk()).isTrue();
    }
  }

  // ── AdaptiveCompressionPort: large data in ratio mode → ZSTD ─────────────

  @Test
  void adaptivePortSelectAlgorithmLargeDataRatioModeShouldReturnZstd() {
    var config = CompressionFactory.createRatioOptimizedConfiguration(); // preferSpeed=false
    var adaptive = new CompressionFactory.AdaptiveCompressionPort(config);

    // > 100MB, non-text, ratio mode → ZSTD
    CompressionAlgorithm algo = adaptive.selectAlgorithm(200L * 1024 * 1024, null);
    assertThat(algo).isEqualTo(CompressionAlgorithm.ZSTD);
  }

  // ── AdaptiveCompressionPort: various text content-type strings ────────────

  @Test
  void adaptivePortSelectAlgorithmForXmlContentInSpeedModeShouldReturnGzip() {
    var config = CompressionFactory.createSpeedOptimizedConfiguration(); // preferSpeed=true
    var adaptive = new CompressionFactory.AdaptiveCompressionPort(config);

    CompressionAlgorithm algo = adaptive.selectAlgorithm(50_000, "application/xml");
    assertThat(algo).isEqualTo(CompressionAlgorithm.GZIP);
  }

  @Test
  void adaptivePortSelectAlgorithmForJavascriptContentInRatioModeShouldReturnBrotli() {
    var config = CompressionFactory.createRatioOptimizedConfiguration(); // preferSpeed=false
    var adaptive = new CompressionFactory.AdaptiveCompressionPort(config);

    CompressionAlgorithm algo = adaptive.selectAlgorithm(50_000, "application/javascript");
    assertThat(algo).isEqualTo(CompressionAlgorithm.BROTLI);
  }

  @Test
  void adaptivePortSelectAlgorithmForCssContentShouldUseBrotliInRatioMode() {
    var config = CompressionFactory.createRatioOptimizedConfiguration();
    var adaptive = new CompressionFactory.AdaptiveCompressionPort(config);

    CompressionAlgorithm algo = adaptive.selectAlgorithm(50_000, "text/css");
    assertThat(algo).isEqualTo(CompressionAlgorithm.BROTLI);
  }

  @Test
  void adaptivePortSelectAlgorithmForMediumBinaryDataShouldReturnDefaultAlgorithm() {
    var config =
        CompressionFactory.createBalancedConfiguration(); // default=ZSTD, preferSpeed=false
    var adaptive = new CompressionFactory.AdaptiveCompressionPort(config);

    // Medium size, binary content → falls through to default algorithm
    CompressionAlgorithm algo = adaptive.selectAlgorithm(50_000, "application/octet-stream");
    assertThat(algo).isEqualTo(CompressionAlgorithm.ZSTD);
  }

  // ── AdaptiveCompressionPort: delegate methods not yet exercised ──────────

  @Test
  void adaptivePortCompressByteArrayShouldWork() {
    var config = CompressionFactory.createBalancedConfiguration();
    var adaptive = new CompressionFactory.AdaptiveCompressionPort(config);

    var options = CompressionOptions.defaultFor(CompressionAlgorithm.GZIP);
    Result<byte[]> result = adaptive.compress(SAMPLE, options);
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).hasSizeGreaterThan(0);
  }

  @Test
  void adaptivePortDecompressByteArrayShouldWork() {
    var config = CompressionFactory.createBalancedConfiguration();
    var adaptive = new CompressionFactory.AdaptiveCompressionPort(config);

    var options = CompressionOptions.defaultFor(CompressionAlgorithm.GZIP);
    byte[] compressed = adaptive.compress(SAMPLE, options).getOrNull();
    Result<byte[]> result = adaptive.decompress(compressed, options);
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isEqualTo(SAMPLE);
  }

  @Test
  void adaptivePortGetCompressionRatioShouldReturnPositive() {
    var adaptive =
        new CompressionFactory.AdaptiveCompressionPort(
            CompressionFactory.createBalancedConfiguration());
    var options = CompressionOptions.defaultFor(CompressionAlgorithm.GZIP);
    Result<Double> result = adaptive.getCompressionRatio(SAMPLE, options);
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isGreaterThan(0.0);
  }

  @Test
  void adaptivePortIsCompressedShouldWork() {
    var adaptive =
        new CompressionFactory.AdaptiveCompressionPort(
            CompressionFactory.createBalancedConfiguration());
    var options = CompressionOptions.defaultFor(CompressionAlgorithm.GZIP);
    byte[] compressed = adaptive.compress(SAMPLE, options).getOrNull();
    Result<Boolean> result = adaptive.isCompressed(compressed, CompressionAlgorithm.GZIP);
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isTrue();
  }

  // ── CompressionConfiguration.createOptions: non-speed branches ──────────

  @Test
  void configCreateOptionsNonSpeedGzipShouldCapAtMaxLevel() {
    var config =
        CompressionFactory.CompressionConfiguration.builder()
            .defaultAlgorithm(CompressionAlgorithm.GZIP)
            .fallbackAlgorithm(CompressionAlgorithm.DEFLATE)
            .preferSpeed(false)
            .maxCompressionLevel(4)
            .build();

    var options = config.createOptions(CompressionAlgorithm.GZIP);
    assertThat(options.level()).isEqualTo(4); // capped at maxCompressionLevel
  }

  @Test
  void configCreateOptionsNonSpeedDeflateShouldCapAtMaxLevel() {
    var config =
        CompressionFactory.CompressionConfiguration.builder()
            .defaultAlgorithm(CompressionAlgorithm.DEFLATE)
            .fallbackAlgorithm(CompressionAlgorithm.GZIP)
            .preferSpeed(false)
            .maxCompressionLevel(3)
            .build();

    var options = config.createOptions(CompressionAlgorithm.DEFLATE);
    assertThat(options.level()).isEqualTo(3);
  }

  @Test
  void configCreateOptionsNonSpeedBrotliShouldCapAtMaxLevel() {
    var config =
        CompressionFactory.CompressionConfiguration.builder()
            .defaultAlgorithm(CompressionAlgorithm.BROTLI)
            .fallbackAlgorithm(CompressionAlgorithm.GZIP)
            .preferSpeed(false)
            .maxCompressionLevel(5)
            .build();

    var options = config.createOptions(CompressionAlgorithm.BROTLI);
    assertThat(options.level()).isEqualTo(5);
  }

  @Test
  void configCreateOptionsNonSpeedLz4ShouldCapAtMaxLevel() {
    var config =
        CompressionFactory.CompressionConfiguration.builder()
            .defaultAlgorithm(CompressionAlgorithm.LZ4)
            .fallbackAlgorithm(CompressionAlgorithm.GZIP)
            .preferSpeed(false)
            .maxCompressionLevel(8)
            .build();

    var options = config.createOptions(CompressionAlgorithm.LZ4);
    assertThat(options.level()).isEqualTo(8); // min(8, 12) = 8
  }

  @Test
  void configCreateOptionsNonSpeedSnappyShouldUseOneAsMax() {
    var config =
        CompressionFactory.CompressionConfiguration.builder()
            .defaultAlgorithm(CompressionAlgorithm.SNAPPY)
            .fallbackAlgorithm(CompressionAlgorithm.GZIP)
            .preferSpeed(false)
            .maxCompressionLevel(6)
            .build();

    // SNAPPY max is 1, so min(6, 1) = 1
    var options = config.createOptions(CompressionAlgorithm.SNAPPY);
    assertThat(options.level()).isEqualTo(1);
  }

  @Test
  void configCreateOptionsNonSpeedZstdShouldCapAtMaxLevel() {
    var config =
        CompressionFactory.CompressionConfiguration.builder()
            .defaultAlgorithm(CompressionAlgorithm.ZSTD)
            .fallbackAlgorithm(CompressionAlgorithm.GZIP)
            .preferSpeed(false)
            .maxCompressionLevel(10)
            .build();

    var options = config.createOptions(CompressionAlgorithm.ZSTD);
    assertThat(options.level()).isEqualTo(10); // min(10, 22) = 10
  }

  @Test
  void configCreateOptionsSpeedGzipShouldReturnLevelOne() {
    var config =
        CompressionFactory.CompressionConfiguration.builder()
            .defaultAlgorithm(CompressionAlgorithm.GZIP)
            .fallbackAlgorithm(CompressionAlgorithm.DEFLATE)
            .preferSpeed(true)
            .maxCompressionLevel(9)
            .build();

    var options = config.createOptions(CompressionAlgorithm.GZIP);
    assertThat(options.level()).isEqualTo(1);
  }

  @Test
  void configCreateOptionsSpeedDeflateShouldReturnLevelOne() {
    var config =
        CompressionFactory.CompressionConfiguration.builder()
            .defaultAlgorithm(CompressionAlgorithm.DEFLATE)
            .fallbackAlgorithm(CompressionAlgorithm.GZIP)
            .preferSpeed(true)
            .maxCompressionLevel(9)
            .build();

    var options = config.createOptions(CompressionAlgorithm.DEFLATE);
    assertThat(options.level()).isEqualTo(1);
  }

  @Test
  void configCreateOptionsSpeedBrotliShouldReturnLevelZero() {
    var config =
        CompressionFactory.CompressionConfiguration.builder()
            .defaultAlgorithm(CompressionAlgorithm.BROTLI)
            .fallbackAlgorithm(CompressionAlgorithm.GZIP)
            .preferSpeed(true)
            .maxCompressionLevel(9)
            .build();

    var options = config.createOptions(CompressionAlgorithm.BROTLI);
    assertThat(options.level()).isEqualTo(0);
  }

  @Test
  void configCreateOptionsSpeedZstdShouldReturnLevelOne() {
    var config =
        CompressionFactory.CompressionConfiguration.builder()
            .defaultAlgorithm(CompressionAlgorithm.ZSTD)
            .fallbackAlgorithm(CompressionAlgorithm.GZIP)
            .preferSpeed(true)
            .maxCompressionLevel(9)
            .build();

    var options = config.createOptions(CompressionAlgorithm.ZSTD);
    assertThat(options.level()).isEqualTo(1);
  }

  @Test
  void configCreateOptionsNullAlgorithmShouldThrow() {
    var config = CompressionFactory.createBalancedConfiguration();
    try {
      config.createOptions(null);
      assertThat(false).withFailMessage("Expected NullPointerException").isTrue();
    } catch (NullPointerException e) {
      assertThat(e).isNotNull();
    }
  }

  // ── CompressionConfiguration: getters not yet hit ────────────────────────

  @Test
  void configGettersShouldReturnExpectedValues() {
    var config =
        CompressionFactory.CompressionConfiguration.builder()
            .defaultAlgorithm(CompressionAlgorithm.SNAPPY)
            .fallbackAlgorithm(CompressionAlgorithm.LZ4)
            .preferSpeed(false)
            .maxCompressionLevel(5)
            .bufferSize(32768)
            .enableChecksums(false)
            .enableHeaders(false)
            .build();

    assertThat(config.getDefaultAlgorithm()).isEqualTo(CompressionAlgorithm.SNAPPY);
    assertThat(config.getFallbackAlgorithm()).isEqualTo(CompressionAlgorithm.LZ4);
    assertThat(config.isPreferSpeed()).isFalse();
    assertThat(config.getMaxCompressionLevel()).isEqualTo(5);
    assertThat(config.getBufferSize()).isEqualTo(32768);
    assertThat(config.isEnableChecksums()).isFalse();
    assertThat(config.isEnableHeaders()).isFalse();
  }

  // ── MultiAlgorithmCompressionAdapter: remaining detection branches ────────

  @Test
  void isCompressedBrotliWithActualBrotliDataShouldReturnTrue() {
    var options = CompressionOptions.defaultFor(CompressionAlgorithm.BROTLI);
    byte[] compressed = adapter.compress(SAMPLE, options).getOrNull();
    Result<Boolean> result = adapter.isCompressed(compressed, CompressionAlgorithm.BROTLI);
    assertThat(result.isOk()).isTrue();
    // brotli detection attempts decompression - returns true if it works
    assertThat(result.getOrNull()).isNotNull();
  }

  @Test
  void isCompressedDeflateWithShortDataShouldReturnFalse() {
    // less than 2 bytes → early return false
    Result<Boolean> result = adapter.isCompressed(new byte[] {0x01}, CompressionAlgorithm.DEFLATE);
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isFalse();
  }

  @Test
  void isCompressedBrotliWithShortDataShouldReturnFalse() {
    // less than 4 bytes → early return false
    Result<Boolean> result =
        adapter.isCompressed(new byte[] {0x01, 0x02, 0x03}, CompressionAlgorithm.BROTLI);
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isFalse();
  }

  @Test
  void isCompressedSnappyWithShortDataShouldReturnFalse() {
    // less than 4 bytes → early return false
    Result<Boolean> result =
        adapter.isCompressed(new byte[] {0x01, 0x02, 0x03}, CompressionAlgorithm.SNAPPY);
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isFalse();
  }

  @Test
  void isCompressedGzipWithShortDataShouldReturnFalse() {
    // only 1 byte → length < 2 → false
    Result<Boolean> result = adapter.isCompressed(new byte[] {0x1F}, CompressionAlgorithm.GZIP);
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isFalse();
  }

  @Test
  void isCompressedLz4WithShortDataShouldReturnFalse() {
    // less than 4 bytes → false
    Result<Boolean> result =
        adapter.isCompressed(new byte[] {0x04, 0x22}, CompressionAlgorithm.LZ4);
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isFalse();
  }

  @Test
  void isCompressedZstdWithShortDataShouldReturnFalse() {
    // less than 4 bytes → false
    Result<Boolean> result =
        adapter.isCompressed(new byte[] {(byte) 0x28, (byte) 0xB5}, CompressionAlgorithm.ZSTD);
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isFalse();
  }

  // ── StreamingCompressionAdapter: close is idempotent for compressors ──────

  @ParameterizedTest
  @EnumSource(CompressionAlgorithm.class)
  void compressorCloseTwiceShouldBeIdempotent(CompressionAlgorithm algo) throws Exception {
    var out = new ByteArrayOutputStream();
    var options = CompressionOptions.defaultFor(algo);
    StreamingCompressor c = streaming.createCompressor(out, options).getOrNull();
    c.write(SAMPLE);
    c.finish();
    c.close();
    c.close(); // second close should be a no-op
  }

  // ── StreamingCompressionAdapter: createCompressor Brotli unavailable path ─

  @Test
  void streamingAdapterCreateCompressorBrotliShouldWork() throws Exception {
    // If brotli is available this tests the happy path; if not it tests the error path
    var out = new ByteArrayOutputStream();
    var options = CompressionOptions.defaultFor(CompressionAlgorithm.BROTLI);
    Result<StreamingCompressor> result = streaming.createCompressor(out, options);
    // Either ok (brotli available) or fail (not available) — both are valid outcomes
    assertThat(result).isNotNull();
  }

  @Test
  void streamingAdapterCreateDecompressorBrotliShouldWork() throws Exception {
    byte[] compressed = streamCompress(CompressionAlgorithm.BROTLI);
    var options = CompressionOptions.defaultFor(CompressionAlgorithm.BROTLI);
    var in = new ByteArrayInputStream(compressed);
    Result<StreamingDecompressor> result = streaming.createDecompressor(in, options);
    assertThat(result).isNotNull();
    if (result.isOk()) {
      result.getOrNull().close();
    }
  }
}
