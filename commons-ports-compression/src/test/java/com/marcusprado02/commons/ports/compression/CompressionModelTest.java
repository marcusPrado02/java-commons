package com.marcusprado02.commons.ports.compression;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class CompressionModelTest {

  // --- CompressionAlgorithm ---

  @Test
  void algorithm_fromName_returns_gzip() {
    assertEquals(CompressionAlgorithm.GZIP, CompressionAlgorithm.fromName("gzip"));
  }

  @Test
  void algorithm_fromName_case_insensitive() {
    assertEquals(CompressionAlgorithm.BROTLI, CompressionAlgorithm.fromName("BR"));
  }

  @Test
  void algorithm_fromName_null_returns_null() {
    assertNull(CompressionAlgorithm.fromName(null));
  }

  @Test
  void algorithm_fromName_empty_returns_null() {
    assertNull(CompressionAlgorithm.fromName(""));
  }

  @Test
  void algorithm_fromName_unknown_returns_null() {
    assertNull(CompressionAlgorithm.fromName("unknown-algo"));
  }

  @Test
  void algorithm_fromMimeType_returns_gzip() {
    assertEquals(CompressionAlgorithm.GZIP, CompressionAlgorithm.fromMimeType("application/gzip"));
  }

  @Test
  void algorithm_fromMimeType_null_returns_null() {
    assertNull(CompressionAlgorithm.fromMimeType(null));
  }

  @Test
  void algorithm_fromMimeType_empty_returns_null() {
    assertNull(CompressionAlgorithm.fromMimeType(""));
  }

  @Test
  void algorithm_fromMimeType_unknown_returns_null() {
    assertNull(CompressionAlgorithm.fromMimeType("application/unknown"));
  }

  @Test
  void algorithm_fromFileExtension_with_dot() {
    assertEquals(CompressionAlgorithm.GZIP, CompressionAlgorithm.fromFileExtension(".gz"));
  }

  @Test
  void algorithm_fromFileExtension_without_dot() {
    assertEquals(CompressionAlgorithm.ZSTD, CompressionAlgorithm.fromFileExtension("zst"));
  }

  @Test
  void algorithm_fromFileExtension_null_returns_null() {
    assertNull(CompressionAlgorithm.fromFileExtension(null));
  }

  @Test
  void algorithm_fromFileExtension_empty_returns_null() {
    assertNull(CompressionAlgorithm.fromFileExtension(""));
  }

  @Test
  void algorithm_fromFileExtension_unknown_returns_null() {
    assertNull(CompressionAlgorithm.fromFileExtension("xyz"));
  }

  @Test
  void algorithm_getters_work() {
    assertEquals("gzip", CompressionAlgorithm.GZIP.getAlgorithmName());
    assertEquals("application/gzip", CompressionAlgorithm.GZIP.getMimeType());
    assertNotNull(CompressionAlgorithm.GZIP.getFileExtensions());
  }

  // --- CompressionResult ---

  @Test
  void compressionResult_valid_calculates_ratio() {
    CompressionResult r =
        CompressionResult.builder(CompressionAlgorithm.GZIP)
            .originalSize(1000L)
            .compressedSize(400L)
            .processingTime(Duration.ofMillis(10))
            .checksumVerified(true)
            .build();
    assertEquals(CompressionAlgorithm.GZIP, r.algorithm());
    assertEquals(1000L, r.originalSize());
    assertEquals(400L, r.compressedSize());
    assertEquals(0.4, r.compressionRatio(), 0.001);
    assertTrue(r.checksumVerified());
  }

  @Test
  void compressionResult_original_size_zero_ratio_is_one() {
    CompressionResult r =
        CompressionResult.builder(CompressionAlgorithm.LZ4)
            .originalSize(0L)
            .compressedSize(0L)
            .processingTime(Duration.ofMillis(1))
            .build();
    assertEquals(1.0, r.compressionRatio(), 0.001);
  }

  @Test
  void compressionResult_null_timestamp_defaults_to_now() {
    Instant before = Instant.now();
    CompressionResult r =
        new CompressionResult(
            CompressionAlgorithm.SNAPPY, 100L, 80L, 0.0, Duration.ofMillis(5), null, false);
    assertTrue(!r.timestamp().isBefore(before));
  }

  @Test
  void compressionResult_null_algorithm_throws() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new CompressionResult(
                null, 100L, 80L, 0.0, Duration.ofMillis(5), Instant.now(), false));
  }

  @Test
  void compressionResult_negative_original_size_throws() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new CompressionResult(
                CompressionAlgorithm.GZIP,
                -1L,
                80L,
                0.0,
                Duration.ofMillis(5),
                Instant.now(),
                false));
  }

  @Test
  void compressionResult_negative_compressed_size_throws() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new CompressionResult(
                CompressionAlgorithm.GZIP,
                100L,
                -1L,
                0.0,
                Duration.ofMillis(5),
                Instant.now(),
                false));
  }

  @Test
  void compressionResult_null_processing_time_throws() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new CompressionResult(
                CompressionAlgorithm.GZIP, 100L, 80L, 0.0, null, Instant.now(), false));
  }

  @Test
  void compressionResult_negative_processing_time_throws() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new CompressionResult(
                CompressionAlgorithm.GZIP,
                100L,
                80L,
                0.0,
                Duration.ofMillis(-1),
                Instant.now(),
                false));
  }

  @Test
  void compressionResult_getSpaceSaved() {
    CompressionResult r =
        CompressionResult.builder(CompressionAlgorithm.DEFLATE)
            .originalSize(1000L)
            .compressedSize(600L)
            .processingTime(Duration.ofMillis(5))
            .build();
    assertEquals(400L, r.getSpaceSaved());
  }

  @Test
  void compressionResult_getSpaceSavedPercentage_zero_original() {
    CompressionResult r =
        CompressionResult.builder(CompressionAlgorithm.DEFLATE)
            .originalSize(0L)
            .compressedSize(0L)
            .processingTime(Duration.ofMillis(1))
            .build();
    assertEquals(0.0, r.getSpaceSavedPercentage(), 0.001);
  }

  @Test
  void compressionResult_getSpaceSavedPercentage_normal() {
    CompressionResult r =
        CompressionResult.builder(CompressionAlgorithm.GZIP)
            .originalSize(1000L)
            .compressedSize(400L)
            .processingTime(Duration.ofMillis(5))
            .build();
    assertEquals(60.0, r.getSpaceSavedPercentage(), 0.001);
  }

  @Test
  void compressionResult_isEffective_true() {
    CompressionResult r =
        CompressionResult.builder(CompressionAlgorithm.GZIP)
            .originalSize(1000L)
            .compressedSize(400L)
            .processingTime(Duration.ofMillis(5))
            .build();
    assertTrue(r.isEffective());
  }

  @Test
  void compressionResult_isEffective_false_when_larger() {
    CompressionResult r =
        CompressionResult.builder(CompressionAlgorithm.GZIP)
            .originalSize(100L)
            .compressedSize(150L)
            .processingTime(Duration.ofMillis(5))
            .build();
    assertFalse(r.isEffective());
  }

  @Test
  void compressionResult_getThroughputBytesPerSecond_zero_time_is_infinity() {
    CompressionResult r =
        CompressionResult.builder(CompressionAlgorithm.LZ4)
            .originalSize(1000L)
            .compressedSize(800L)
            .processingTime(Duration.ZERO)
            .build();
    assertEquals(Double.POSITIVE_INFINITY, r.getThroughputBytesPerSecond());
  }

  @Test
  void compressionResult_getThroughputMegabytesPerSecond() {
    CompressionResult r =
        CompressionResult.builder(CompressionAlgorithm.LZ4)
            .originalSize(1048576L)
            .compressedSize(800000L)
            .processingTime(Duration.ofMillis(1000))
            .build();
    assertEquals(1.0, r.getThroughputMegabytesPerSecond(), 0.01);
  }

  @Test
  void compressionResult_formatSummary_not_null() {
    CompressionResult r =
        CompressionResult.builder(CompressionAlgorithm.ZSTD)
            .originalSize(1000L)
            .compressedSize(400L)
            .processingTime(Duration.ofMillis(5))
            .build();
    assertNotNull(r.formatSummary());
    assertTrue(r.formatSummary().contains("ZSTD"));
  }

  // --- HttpCompression ---

  @Test
  void http_parseAcceptEncoding_null_returns_empty() {
    assertTrue(HttpCompression.parseAcceptEncoding(null).isEmpty());
  }

  @Test
  void http_parseAcceptEncoding_empty_string_returns_empty() {
    assertTrue(HttpCompression.parseAcceptEncoding("").isEmpty());
  }

  @Test
  void http_parseAcceptEncoding_gzip_returns_gzip() {
    List<CompressionAlgorithm> result = HttpCompression.parseAcceptEncoding("gzip");
    assertEquals(1, result.size());
    assertEquals(CompressionAlgorithm.GZIP, result.get(0));
  }

  @Test
  void http_parseAcceptEncoding_multiple_with_quality() {
    List<CompressionAlgorithm> result = HttpCompression.parseAcceptEncoding("gzip;q=0.8, br;q=1.0");
    assertFalse(result.isEmpty());
    assertEquals(CompressionAlgorithm.BROTLI, result.get(0));
  }

  @Test
  void http_parseAcceptEncoding_identity_is_skipped() {
    List<CompressionAlgorithm> result = HttpCompression.parseAcceptEncoding("identity");
    assertTrue(result.isEmpty());
  }

  @Test
  void http_parseAcceptEncoding_wildcard_is_skipped() {
    List<CompressionAlgorithm> result = HttpCompression.parseAcceptEncoding("*");
    assertTrue(result.isEmpty());
  }

  @Test
  void http_parseAcceptEncoding_unknown_encoding_excluded() {
    List<CompressionAlgorithm> result = HttpCompression.parseAcceptEncoding("unknown-enc");
    assertTrue(result.isEmpty());
  }

  @Test
  void http_parseAcceptEncoding_invalid_quality_uses_default() {
    List<CompressionAlgorithm> result = HttpCompression.parseAcceptEncoding("gzip;q=notanumber");
    assertFalse(result.isEmpty());
    assertEquals(CompressionAlgorithm.GZIP, result.get(0));
  }

  @Test
  void http_parseAcceptEncoding_quality_out_of_range_uses_default() {
    List<CompressionAlgorithm> result = HttpCompression.parseAcceptEncoding("gzip;q=1.5");
    assertFalse(result.isEmpty());
  }

  @Test
  void http_selectBestAlgorithm_null_supported_returns_null() {
    assertNull(HttpCompression.selectBestAlgorithm("gzip", null));
  }

  @Test
  void http_selectBestAlgorithm_empty_supported_returns_null() {
    assertNull(HttpCompression.selectBestAlgorithm("gzip", new CompressionAlgorithm[0]));
  }

  @Test
  void http_selectBestAlgorithm_match_found() {
    CompressionAlgorithm result =
        HttpCompression.selectBestAlgorithm(
            "gzip",
            new CompressionAlgorithm[] {CompressionAlgorithm.GZIP, CompressionAlgorithm.BROTLI});
    assertEquals(CompressionAlgorithm.GZIP, result);
  }

  @Test
  void http_selectBestAlgorithm_no_match_returns_null() {
    assertNull(
        HttpCompression.selectBestAlgorithm(
            "gzip", new CompressionAlgorithm[] {CompressionAlgorithm.BROTLI}));
  }

  @Test
  void http_isCompressible_null_returns_false() {
    assertFalse(HttpCompression.isCompressible(null));
  }

  @Test
  void http_isCompressible_empty_returns_false() {
    assertFalse(HttpCompression.isCompressible(""));
  }

  @Test
  void http_isCompressible_text_html_true() {
    assertTrue(HttpCompression.isCompressible("text/html"));
  }

  @Test
  void http_isCompressible_application_json_true() {
    assertTrue(HttpCompression.isCompressible("application/json"));
  }

  @Test
  void http_isCompressible_application_xml_true() {
    assertTrue(HttpCompression.isCompressible("application/xml"));
  }

  @Test
  void http_isCompressible_application_javascript_true() {
    assertTrue(HttpCompression.isCompressible("application/javascript"));
  }

  @Test
  void http_isCompressible_application_json_with_charset() {
    assertTrue(HttpCompression.isCompressible("application/json; charset=utf-8"));
  }

  @Test
  void http_isCompressible_image_png_false() {
    assertFalse(HttpCompression.isCompressible("image/png"));
  }

  @Test
  void http_shouldCompress_below_min_size_false() {
    assertFalse(HttpCompression.shouldCompress(100L, "text/html", 1024));
  }

  @Test
  void http_shouldCompress_above_min_size_and_compressible_true() {
    assertTrue(HttpCompression.shouldCompress(2000L, "text/html", 1024));
  }

  @Test
  void http_shouldCompress_above_min_size_not_compressible_false() {
    assertFalse(HttpCompression.shouldCompress(2000L, "image/png", 1024));
  }

  @Test
  void http_createContentEncodingHeader_returns_algorithm_name() {
    assertEquals("gzip", HttpCompression.createContentEncodingHeader(CompressionAlgorithm.GZIP));
  }

  @Test
  void http_createVaryHeader_returns_accept_encoding() {
    assertEquals("Accept-Encoding", HttpCompression.createVaryHeader());
  }

  @Test
  void http_createHttpOptions_fast_true() {
    CompressionOptions opts = HttpCompression.createHttpOptions(CompressionAlgorithm.GZIP, true);
    assertNotNull(opts);
  }

  @Test
  void http_createHttpOptions_gzip_slow() {
    CompressionOptions opts = HttpCompression.createHttpOptions(CompressionAlgorithm.GZIP, false);
    assertNotNull(opts);
  }

  @Test
  void http_createHttpOptions_deflate_slow() {
    assertNotNull(HttpCompression.createHttpOptions(CompressionAlgorithm.DEFLATE, false));
  }

  @Test
  void http_createHttpOptions_brotli_slow() {
    assertNotNull(HttpCompression.createHttpOptions(CompressionAlgorithm.BROTLI, false));
  }

  @Test
  void http_createHttpOptions_lz4_slow() {
    assertNotNull(HttpCompression.createHttpOptions(CompressionAlgorithm.LZ4, false));
  }

  @Test
  void http_createHttpOptions_snappy_slow() {
    assertNotNull(HttpCompression.createHttpOptions(CompressionAlgorithm.SNAPPY, false));
  }

  @Test
  void http_createHttpOptions_zstd_slow() {
    assertNotNull(HttpCompression.createHttpOptions(CompressionAlgorithm.ZSTD, false));
  }
}
