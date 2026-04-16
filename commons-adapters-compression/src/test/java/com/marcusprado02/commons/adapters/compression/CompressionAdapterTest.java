package com.marcusprado02.commons.adapters.compression;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marcusprado02.commons.kernel.result.Result;
import com.marcusprado02.commons.ports.compression.CompressionAlgorithm;
import com.marcusprado02.commons.ports.compression.CompressionOptions;
import com.marcusprado02.commons.ports.compression.StreamingCompressionPort.StreamingCompressor;
import com.marcusprado02.commons.ports.compression.StreamingCompressionPort.StreamingDecompressor;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class CompressionAdapterTest {

  private static final byte[] SAMPLE_DATA =
      "Hello World! This is test data for compression testing. "
          .repeat(20)
          .getBytes(StandardCharsets.UTF_8);

  private final MultiAlgorithmCompressionAdapter adapter = new MultiAlgorithmCompressionAdapter();

  // ── helpers ───────────────────────────────────────────────────────────────

  private byte[] compress(CompressionAlgorithm algo) throws Exception {
    var options = CompressionOptions.defaultFor(algo);
    var out = new ByteArrayOutputStream();
    Result<?> r = adapter.compress(new ByteArrayInputStream(SAMPLE_DATA), out, options);
    assertThat(r.isOk()).withFailMessage("compress(%s) failed: %s", algo, r).isTrue();
    return out.toByteArray();
  }

  private byte[] decompress(CompressionAlgorithm algo, byte[] compressed) {
    var options = CompressionOptions.defaultFor(algo);
    var out = new ByteArrayOutputStream();
    Result<?> r = adapter.decompress(new ByteArrayInputStream(compressed), out, options);
    assertThat(r.isOk()).withFailMessage("decompress(%s) failed: %s", algo, r).isTrue();
    return out.toByteArray();
  }

  // ── GZIP round-trip ───────────────────────────────────────────────────────

  @Test
  void gzipShouldCompressAndDecompress() throws Exception {
    byte[] compressed = compress(CompressionAlgorithm.GZIP);
    byte[] decompressed = decompress(CompressionAlgorithm.GZIP, compressed);
    assertThat(decompressed).isEqualTo(SAMPLE_DATA);
  }

  @Test
  void gzipCompressByteArrayShouldWork() {
    var options = CompressionOptions.defaultFor(CompressionAlgorithm.GZIP);
    Result<byte[]> result = adapter.compress(SAMPLE_DATA, options);
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).hasSizeGreaterThan(0);
  }

  @Test
  void gzipDecompressByteArrayShouldWork() {
    var options = CompressionOptions.defaultFor(CompressionAlgorithm.GZIP);
    byte[] compressed = adapter.compress(SAMPLE_DATA, options).getOrNull();
    Result<byte[]> result = adapter.decompress(compressed, options);
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isEqualTo(SAMPLE_DATA);
  }

  // ── DEFLATE round-trip ────────────────────────────────────────────────────

  @Test
  void deflateShouldCompressAndDecompress() throws Exception {
    byte[] compressed = compress(CompressionAlgorithm.DEFLATE);
    byte[] decompressed = decompress(CompressionAlgorithm.DEFLATE, compressed);
    assertThat(decompressed).isEqualTo(SAMPLE_DATA);
  }

  // ── LZ4 round-trip ────────────────────────────────────────────────────────

  @Test
  void lz4ShouldCompressAndDecompress() throws Exception {
    byte[] compressed = compress(CompressionAlgorithm.LZ4);
    byte[] decompressed = decompress(CompressionAlgorithm.LZ4, compressed);
    assertThat(decompressed).isEqualTo(SAMPLE_DATA);
  }

  @Test
  void lz4ByteArrayShouldWork() {
    var options = CompressionOptions.defaultFor(CompressionAlgorithm.LZ4);
    Result<byte[]> compressed = adapter.compress(SAMPLE_DATA, options);
    assertThat(compressed.isOk()).isTrue();
    Result<byte[]> decompressed = adapter.decompress(compressed.getOrNull(), options);
    assertThat(decompressed.isOk()).isTrue();
    assertThat(decompressed.getOrNull()).isEqualTo(SAMPLE_DATA);
  }

  // ── ZSTD round-trip ───────────────────────────────────────────────────────

  @Test
  void zstdShouldCompressAndDecompress() throws Exception {
    byte[] compressed = compress(CompressionAlgorithm.ZSTD);
    byte[] decompressed = decompress(CompressionAlgorithm.ZSTD, compressed);
    assertThat(decompressed).isEqualTo(SAMPLE_DATA);
  }

  // ── Snappy round-trip ─────────────────────────────────────────────────────

  @Test
  void snappyShouldCompressAndDecompress() throws Exception {
    byte[] compressed = compress(CompressionAlgorithm.SNAPPY);
    byte[] decompressed = decompress(CompressionAlgorithm.SNAPPY, compressed);
    assertThat(decompressed).isEqualTo(SAMPLE_DATA);
  }

  // ── null arguments → fail ─────────────────────────────────────────────────

  @Test
  void compressWithNullInputShouldReturnFail() {
    var options = CompressionOptions.defaultFor(CompressionAlgorithm.GZIP);
    Result<?> result = adapter.compress(null, new ByteArrayOutputStream(), options);
    assertThat(result.isFail()).isTrue();
  }

  @Test
  void compressWithNullOutputShouldReturnFail() {
    var options = CompressionOptions.defaultFor(CompressionAlgorithm.GZIP);
    Result<?> result = adapter.compress(new ByteArrayInputStream(SAMPLE_DATA), null, options);
    assertThat(result.isFail()).isTrue();
  }

  // ── getCompressionRatio ───────────────────────────────────────────────────

  @Test
  void getCompressionRatioShouldReturnPositiveValue() {
    var options = CompressionOptions.defaultFor(CompressionAlgorithm.GZIP);
    Result<Double> result = adapter.getCompressionRatio(SAMPLE_DATA, options);
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isGreaterThan(0.0);
  }

  // ── isCompressed ─────────────────────────────────────────────────────────

  @Test
  void isCompressedShouldDetectGzipMagicBytes() {
    var options = CompressionOptions.defaultFor(CompressionAlgorithm.GZIP);
    byte[] compressed = adapter.compress(SAMPLE_DATA, options).getOrNull();

    Result<Boolean> result = adapter.isCompressed(compressed, CompressionAlgorithm.GZIP);
    assertThat(result.isOk()).isTrue();
    // GZIP magic bytes: 0x1f, 0x8b
    assertThat(result.getOrNull()).isTrue();
  }

  @Test
  void isCompressedShouldReturnFalseForPlainData() {
    Result<Boolean> result = adapter.isCompressed(SAMPLE_DATA, CompressionAlgorithm.GZIP);
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isFalse();
  }

  // ── getSupportedAlgorithms ────────────────────────────────────────────────

  @Test
  void getSupportedAlgorithmsShouldReturnAllAlgorithms() {
    CompressionAlgorithm[] supported = adapter.getSupportedAlgorithms();
    assertThat(supported).containsExactlyInAnyOrder(CompressionAlgorithm.values());
  }

  // ── getDefaultCompressionLevel ────────────────────────────────────────────

  @Test
  void getDefaultCompressionLevelForGzipShouldBeSix() {
    assertThat(adapter.getDefaultCompressionLevel(CompressionAlgorithm.GZIP)).isEqualTo(6);
  }

  @Test
  void getDefaultCompressionLevelForLz4ShouldBeOne() {
    assertThat(adapter.getDefaultCompressionLevel(CompressionAlgorithm.LZ4)).isEqualTo(1);
  }

  // ── CompressionFactory ────────────────────────────────────────────────────

  @Test
  void factoryCreateCompressionPortShouldWork() {
    var port = CompressionFactory.createCompressionPort();
    assertThat(port).isNotNull();
    assertThat(port.getSupportedAlgorithms()).isNotEmpty();
  }

  @Test
  void factoryCreateStreamingPortShouldWork() {
    var port = CompressionFactory.createStreamingCompressionPort();
    assertThat(port).isNotNull();
  }

  @Test
  void factorySpeedOptimizedConfigShouldPreferSpeed() {
    var config = CompressionFactory.createSpeedOptimizedConfiguration();
    assertThat(config.isPreferSpeed()).isTrue();
    assertThat(config.getDefaultAlgorithm()).isEqualTo(CompressionAlgorithm.LZ4);
  }

  @Test
  void factoryRatioOptimizedConfigShouldNotPreferSpeed() {
    var config = CompressionFactory.createRatioOptimizedConfiguration();
    assertThat(config.isPreferSpeed()).isFalse();
    assertThat(config.getDefaultAlgorithm()).isEqualTo(CompressionAlgorithm.BROTLI);
  }

  @Test
  void factoryBalancedConfigShouldUseZstd() {
    var config = CompressionFactory.createBalancedConfiguration();
    assertThat(config.getDefaultAlgorithm()).isEqualTo(CompressionAlgorithm.ZSTD);
  }

  @Test
  void factoryWebOptimizedConfigShouldEnableChecksums() {
    var config = CompressionFactory.createWebOptimizedConfiguration();
    assertThat(config.isEnableChecksums()).isTrue();
    assertThat(config.getDefaultAlgorithm()).isEqualTo(CompressionAlgorithm.BROTLI);
  }

  // ── CompressionConfiguration.createOptions ───────────────────────────────

  @Test
  void configCreateOptionsShouldProduceValidOptions() {
    var config = CompressionFactory.createSpeedOptimizedConfiguration();
    var options = config.createOptions(CompressionAlgorithm.GZIP);

    assertThat(options.algorithm()).isEqualTo(CompressionAlgorithm.GZIP);
    assertThat(options.level()).isGreaterThan(0);
  }

  @Test
  void configCreateDefaultOptionsShouldUseDefaultAlgorithm() {
    var config = CompressionFactory.createBalancedConfiguration();
    var options = config.createDefaultOptions();

    assertThat(options.algorithm()).isEqualTo(CompressionAlgorithm.ZSTD);
  }

  @Test
  void configCreateFallbackOptionsShouldUseFallbackAlgorithm() {
    var config = CompressionFactory.createBalancedConfiguration();
    var options = config.createFallbackOptions();

    assertThat(options.algorithm()).isEqualTo(CompressionAlgorithm.GZIP);
  }

  // ── CompressionConfiguration builder validation ───────────────────────────

  @Test
  void builderWithNegativeLevelShouldThrow() {
    assertThatThrownBy(
            () ->
                CompressionFactory.CompressionConfiguration.builder()
                    .maxCompressionLevel(-1)
                    .build())
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void builderWithNonPositiveBufferShouldThrow() {
    assertThatThrownBy(
            () -> CompressionFactory.CompressionConfiguration.builder().bufferSize(0).build())
        .isInstanceOf(IllegalArgumentException.class);
  }

  // ── AdaptiveCompressionPort ───────────────────────────────────────────────

  @Test
  void adaptivePortSelectAlgorithmForSmallDataShouldReturnLz4() {
    var config = CompressionFactory.createBalancedConfiguration();
    var adaptive = new CompressionFactory.AdaptiveCompressionPort(config);

    CompressionAlgorithm algo = adaptive.selectAlgorithm(512, null); // < 1KB
    assertThat(algo).isEqualTo(CompressionAlgorithm.LZ4);
  }

  @Test
  void adaptivePortSelectAlgorithmForLargeDataSpeedModeShouldReturnLz4() {
    var config =
        CompressionFactory.CompressionConfiguration.builder()
            .defaultAlgorithm(CompressionAlgorithm.ZSTD)
            .fallbackAlgorithm(CompressionAlgorithm.GZIP)
            .preferSpeed(true)
            .bufferSize(65536)
            .build();
    var adaptive = new CompressionFactory.AdaptiveCompressionPort(config);

    CompressionAlgorithm algo = adaptive.selectAlgorithm(200L * 1024 * 1024, null); // > 100MB
    assertThat(algo).isEqualTo(CompressionAlgorithm.LZ4);
  }

  @Test
  void adaptivePortSelectAlgorithmForTextContentShouldPreferBrotliInRatioMode() {
    var config = CompressionFactory.createRatioOptimizedConfiguration();
    var adaptive = new CompressionFactory.AdaptiveCompressionPort(config);

    // Medium size, text content — should pick Brotli (ratio mode, text)
    CompressionAlgorithm algo = adaptive.selectAlgorithm(50_000, "text/html");
    assertThat(algo).isEqualTo(CompressionAlgorithm.BROTLI);
  }

  @Test
  void adaptivePortSelectAlgorithmForJsonContentShouldUseTextPath() {
    var config = CompressionFactory.createSpeedOptimizedConfiguration();
    var adaptive = new CompressionFactory.AdaptiveCompressionPort(config);

    // Speed mode, text content (JSON) → GZIP
    CompressionAlgorithm algo = adaptive.selectAlgorithm(50_000, "application/json");
    assertThat(algo).isEqualTo(CompressionAlgorithm.GZIP);
  }

  @Test
  void adaptivePortShouldDelegateCompressToAdapter() {
    var config = CompressionFactory.createBalancedConfiguration();
    var adaptive = new CompressionFactory.AdaptiveCompressionPort(config);

    var options = CompressionOptions.defaultFor(CompressionAlgorithm.GZIP);
    var out = new ByteArrayOutputStream();
    Result<?> result = adaptive.compress(new ByteArrayInputStream(SAMPLE_DATA), out, options);

    assertThat(result.isOk()).isTrue();
    assertThat(out.toByteArray()).hasSizeGreaterThan(0);
  }

  @Test
  void adaptivePortGetSupportedAlgorithmsShouldReturnAll() {
    var adaptive =
        new CompressionFactory.AdaptiveCompressionPort(
            CompressionFactory.createBalancedConfiguration());
    assertThat(adaptive.getSupportedAlgorithms()).isNotEmpty();
  }

  // ── MultiAlgorithmCompressionAdapter: additional coverage ────────────────

  @Test
  void brotliShouldCompressAndDecompress() throws Exception {
    byte[] compressed = compress(CompressionAlgorithm.BROTLI);
    byte[] decompressed = decompress(CompressionAlgorithm.BROTLI, compressed);
    assertThat(decompressed).isEqualTo(SAMPLE_DATA);
  }

  @Test
  void decompressNullByteArrayShouldReturnFail() {
    var options = CompressionOptions.defaultFor(CompressionAlgorithm.GZIP);
    Result<byte[]> result = adapter.decompress((byte[]) null, options);
    assertThat(result.isFail()).isTrue();
  }

  @Test
  void compressNullByteArrayShouldReturnFail() {
    var options = CompressionOptions.defaultFor(CompressionAlgorithm.GZIP);
    Result<byte[]> result = adapter.compress((byte[]) null, options);
    assertThat(result.isFail()).isTrue();
  }

  @Test
  void getCompressionRatioNullDataShouldReturnFail() {
    var options = CompressionOptions.defaultFor(CompressionAlgorithm.GZIP);
    Result<Double> result = adapter.getCompressionRatio(null, options);
    assertThat(result.isFail()).isTrue();
  }

  @Test
  void getCompressionRatioEmptyDataShouldReturnOne() {
    var options = CompressionOptions.defaultFor(CompressionAlgorithm.GZIP);
    Result<Double> result = adapter.getCompressionRatio(new byte[0], options);
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isEqualTo(1.0);
  }

  @Test
  void isCompressedNullDataShouldReturnFail() {
    Result<Boolean> result = adapter.isCompressed(null, CompressionAlgorithm.GZIP);
    assertThat(result.isFail()).isTrue();
  }

  @Test
  void isCompressedNullAlgorithmShouldReturnFail() {
    Result<Boolean> result = adapter.isCompressed(SAMPLE_DATA, null);
    assertThat(result.isFail()).isTrue();
  }

  @Test
  void isCompressedDeflateDataShouldReturnTrue() throws Exception {
    byte[] compressed = compress(CompressionAlgorithm.DEFLATE);
    Result<Boolean> result = adapter.isCompressed(compressed, CompressionAlgorithm.DEFLATE);
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isTrue();
  }

  @Test
  void isCompressedLz4ShouldReturnOk() throws Exception {
    // LZ4BlockOutputStream does not write the LZ4 frame magic number, so detection
    // returns false — but the call itself succeeds (no error).
    byte[] compressed = compress(CompressionAlgorithm.LZ4);
    Result<Boolean> result = adapter.isCompressed(compressed, CompressionAlgorithm.LZ4);
    assertThat(result.isOk()).isTrue();
  }

  @Test
  void isCompressedZstdDataShouldReturnTrue() throws Exception {
    byte[] compressed = compress(CompressionAlgorithm.ZSTD);
    Result<Boolean> result = adapter.isCompressed(compressed, CompressionAlgorithm.ZSTD);
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isTrue();
  }

  @Test
  void isCompressedSnappyDataShouldReturnTrue() throws Exception {
    byte[] compressed = compress(CompressionAlgorithm.SNAPPY);
    Result<Boolean> result = adapter.isCompressed(compressed, CompressionAlgorithm.SNAPPY);
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isTrue();
  }

  @Test
  void getDefaultCompressionLevelForAllAlgorithmsShouldReturnValidLevels() {
    assertThat(adapter.getDefaultCompressionLevel(CompressionAlgorithm.DEFLATE)).isEqualTo(6);
    assertThat(adapter.getDefaultCompressionLevel(CompressionAlgorithm.BROTLI)).isEqualTo(6);
    assertThat(adapter.getDefaultCompressionLevel(CompressionAlgorithm.SNAPPY)).isEqualTo(1);
    assertThat(adapter.getDefaultCompressionLevel(CompressionAlgorithm.ZSTD)).isEqualTo(3);
  }

  @Test
  void compressWithNullOptionsShouldReturnFail() {
    Result<?> result =
        adapter.compress(new ByteArrayInputStream(SAMPLE_DATA), new ByteArrayOutputStream(), null);
    assertThat(result.isFail()).isTrue();
  }

  // ── StreamingCompressionAdapter ───────────────────────────────────────────

  private final StreamingCompressionAdapter streaming = new StreamingCompressionAdapter();

  @Test
  void streamingCreateCompressorNullOutputShouldReturnFail() {
    var options = CompressionOptions.defaultFor(CompressionAlgorithm.GZIP);
    Result<StreamingCompressor> result = streaming.createCompressor(null, options);
    assertThat(result.isFail()).isTrue();
  }

  @Test
  void streamingCreateCompressorNullOptionsShouldReturnFail() {
    Result<StreamingCompressor> result =
        streaming.createCompressor(new ByteArrayOutputStream(), null);
    assertThat(result.isFail()).isTrue();
  }

  @Test
  void streamingCreateDecompressorNullInputShouldReturnFail() {
    var options = CompressionOptions.defaultFor(CompressionAlgorithm.GZIP);
    Result<StreamingDecompressor> result = streaming.createDecompressor(null, options);
    assertThat(result.isFail()).isTrue();
  }

  @Test
  void streamingCreateDecompressorNullOptionsShouldReturnFail() {
    Result<StreamingDecompressor> result =
        streaming.createDecompressor(new ByteArrayInputStream(SAMPLE_DATA), null);
    assertThat(result.isFail()).isTrue();
  }

  @Test
  void streamingGzipCompressorShouldWriteFlushFinishAndClose() throws Exception {
    var out = new ByteArrayOutputStream();
    var options = CompressionOptions.defaultFor(CompressionAlgorithm.GZIP);
    Result<StreamingCompressor> compResult = streaming.createCompressor(out, options);
    assertThat(compResult.isOk()).isTrue();

    try (StreamingCompressor compressor = compResult.getOrNull()) {
      assertThat(compressor.write(SAMPLE_DATA).isOk()).isTrue();
      assertThat(compressor.flush().isOk()).isTrue();
      assertThat(compressor.finish().isOk()).isTrue();
      assertThat(compressor.getBytesWritten()).isEqualTo(SAMPLE_DATA.length);
      assertThat(compressor.getCompressedBytes()).isGreaterThanOrEqualTo(0);
      assertThat(compressor.getCurrentStats()).isNotNull();
    }

    assertThat(out.toByteArray()).hasSizeGreaterThan(0);
  }

  @Test
  void streamingGzipDecompressorShouldReadSkipHasMoreAndClose() throws Exception {
    // First compress with GZIP
    var compressedOut = new ByteArrayOutputStream();
    var compOptions = CompressionOptions.defaultFor(CompressionAlgorithm.GZIP);
    Result<StreamingCompressor> compResult = streaming.createCompressor(compressedOut, compOptions);
    try (StreamingCompressor compressor = compResult.getOrNull()) {
      compressor.write(SAMPLE_DATA);
      compressor.finish();
    }

    // Now decompress with streaming decompressor
    var in = new ByteArrayInputStream(compressedOut.toByteArray());
    Result<StreamingDecompressor> decompResult = streaming.createDecompressor(in, compOptions);
    assertThat(decompResult.isOk()).isTrue();

    try (StreamingDecompressor decompressor = decompResult.getOrNull()) {
      var buffer = new byte[512];
      Result<Integer> readResult = decompressor.read(buffer);
      assertThat(readResult.isOk()).isTrue();
      assertThat(decompressor.skip(0).isOk()).isTrue();
      assertThat(decompressor.hasMore().isOk()).isTrue();
      assertThat(decompressor.getDecompressedBytes()).isGreaterThanOrEqualTo(0);
      assertThat(decompressor.getCompressedBytesRead()).isGreaterThanOrEqualTo(0);
      assertThat(decompressor.getCurrentStats()).isNotNull();
    }
  }

  @Test
  void streamingLz4CompressorShouldWork() throws Exception {
    var out = new ByteArrayOutputStream();
    var options = CompressionOptions.defaultFor(CompressionAlgorithm.LZ4);
    Result<StreamingCompressor> compResult = streaming.createCompressor(out, options);
    assertThat(compResult.isOk()).isTrue();

    try (StreamingCompressor compressor = compResult.getOrNull()) {
      assertThat(compressor.write(SAMPLE_DATA).isOk()).isTrue();
      assertThat(compressor.finish().isOk()).isTrue();
    }
    assertThat(out.toByteArray()).hasSizeGreaterThan(0);
  }

  @Test
  void streamingDeflateCompressorShouldWork() throws Exception {
    var out = new ByteArrayOutputStream();
    var options = CompressionOptions.defaultFor(CompressionAlgorithm.DEFLATE);
    Result<StreamingCompressor> compResult = streaming.createCompressor(out, options);
    assertThat(compResult.isOk()).isTrue();

    try (StreamingCompressor compressor = compResult.getOrNull()) {
      assertThat(compressor.write(SAMPLE_DATA).isOk()).isTrue();
      assertThat(compressor.finish().isOk()).isTrue();
    }
    assertThat(out.toByteArray()).hasSizeGreaterThan(0);
  }

  @Test
  void streamingZstdCompressorShouldWork() throws Exception {
    var out = new ByteArrayOutputStream();
    var options = CompressionOptions.defaultFor(CompressionAlgorithm.ZSTD);
    Result<StreamingCompressor> compResult = streaming.createCompressor(out, options);
    assertThat(compResult.isOk()).isTrue();

    try (StreamingCompressor compressor = compResult.getOrNull()) {
      assertThat(compressor.write(SAMPLE_DATA).isOk()).isTrue();
      assertThat(compressor.finish().isOk()).isTrue();
    }
    assertThat(out.toByteArray()).hasSizeGreaterThan(0);
  }

  @Test
  void streamingSnappyCompressorShouldWork() throws Exception {
    var out = new ByteArrayOutputStream();
    var options = CompressionOptions.defaultFor(CompressionAlgorithm.SNAPPY);
    Result<StreamingCompressor> compResult = streaming.createCompressor(out, options);
    assertThat(compResult.isOk()).isTrue();

    try (StreamingCompressor compressor = compResult.getOrNull()) {
      assertThat(compressor.write(SAMPLE_DATA).isOk()).isTrue();
      assertThat(compressor.finish().isOk()).isTrue();
    }
    assertThat(out.toByteArray()).hasSizeGreaterThan(0);
  }

  @Test
  void streamingBrotliCompressorShouldWork() throws Exception {
    var out = new ByteArrayOutputStream();
    var options = CompressionOptions.defaultFor(CompressionAlgorithm.BROTLI);
    Result<StreamingCompressor> compResult = streaming.createCompressor(out, options);
    assertThat(compResult.isOk()).isTrue();

    try (StreamingCompressor compressor = compResult.getOrNull()) {
      assertThat(compressor.write(SAMPLE_DATA).isOk()).isTrue();
      assertThat(compressor.finish().isOk()).isTrue();
    }
    assertThat(out.toByteArray()).hasSizeGreaterThan(0);
  }

  @Test
  void streamingFinishTwiceShouldReturnOkBothTimes() throws Exception {
    var out = new ByteArrayOutputStream();
    var options = CompressionOptions.defaultFor(CompressionAlgorithm.GZIP);
    Result<StreamingCompressor> compResult = streaming.createCompressor(out, options);
    StreamingCompressor compressor = compResult.getOrNull();
    compressor.write(SAMPLE_DATA);
    compressor.finish(); // first finish
    Result<com.marcusprado02.commons.ports.compression.CompressionResult> second =
        compressor.finish(); // second finish — already-finished path
    assertThat(second.isOk()).isTrue();
    compressor.close();
  }

  @Test
  void streamingWriteNullDataShouldReturnFail() throws Exception {
    var out = new ByteArrayOutputStream();
    var options = CompressionOptions.defaultFor(CompressionAlgorithm.GZIP);
    StreamingCompressor compressor = streaming.createCompressor(out, options).getOrNull();
    Result<Void> result = compressor.write(null);
    assertThat(result.isFail()).isTrue();
    compressor.close();
  }

  @Test
  void streamingReadNullBufferShouldReturnFail() throws Exception {
    var in = new ByteArrayInputStream(compress(CompressionAlgorithm.GZIP));
    var options = CompressionOptions.defaultFor(CompressionAlgorithm.GZIP);
    StreamingDecompressor decompressor = streaming.createDecompressor(in, options).getOrNull();
    Result<Integer> result = decompressor.read(null);
    assertThat(result.isFail()).isTrue();
    decompressor.close();
  }

  // ── AdaptiveCompressionPort: additional coverage ─────────────────────────

  @Test
  void adaptivePortDecompressShouldWork() throws Exception {
    var config = CompressionFactory.createBalancedConfiguration();
    var adaptive = new CompressionFactory.AdaptiveCompressionPort(config);

    byte[] compressed = compress(CompressionAlgorithm.GZIP);
    var options = CompressionOptions.defaultFor(CompressionAlgorithm.GZIP);
    var out = new ByteArrayOutputStream();
    Result<?> result = adaptive.decompress(new ByteArrayInputStream(compressed), out, options);
    assertThat(result.isOk()).isTrue();
  }

  @Test
  void adaptivePortGetDefaultCompressionLevelShouldReturnPositive() {
    var adaptive =
        new CompressionFactory.AdaptiveCompressionPort(
            CompressionFactory.createBalancedConfiguration());
    assertThat(adaptive.getDefaultCompressionLevel(CompressionAlgorithm.GZIP)).isGreaterThan(0);
  }
}
