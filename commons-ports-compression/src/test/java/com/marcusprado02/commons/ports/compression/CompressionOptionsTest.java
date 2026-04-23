package com.marcusprado02.commons.ports.compression;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CompressionOptionsTest {

  @Test
  void defaultFor_gzip_creates_valid_options() {
    CompressionOptions opts = CompressionOptions.defaultFor(CompressionAlgorithm.GZIP);
    assertEquals(CompressionAlgorithm.GZIP, opts.algorithm());
    assertEquals(6, opts.level());
    assertEquals(CompressionOptions.DEFAULT_BUFFER_SIZE, opts.bufferSize());
    assertTrue(opts.headerEnabled());
    assertTrue(opts.checksumEnabled());
    assertEquals(CompressionOptions.DEFAULT_WINDOW_SIZE, opts.windowSize());
  }

  @Test
  void fastFor_gzip_uses_min_level() {
    CompressionOptions opts = CompressionOptions.fastFor(CompressionAlgorithm.GZIP);
    assertEquals(1, opts.level());
  }

  @Test
  void bestFor_gzip_uses_max_level() {
    CompressionOptions opts = CompressionOptions.bestFor(CompressionAlgorithm.GZIP);
    assertEquals(9, opts.level());
  }

  @Test
  void defaultFor_brotli_creates_valid_options() {
    CompressionOptions opts = CompressionOptions.defaultFor(CompressionAlgorithm.BROTLI);
    assertEquals(6, opts.level());
  }

  @Test
  void defaultFor_snappy_level_is_1() {
    CompressionOptions opts = CompressionOptions.defaultFor(CompressionAlgorithm.SNAPPY);
    assertEquals(1, opts.level());
  }

  @Test
  void defaultFor_zstd_level_is_3() {
    CompressionOptions opts = CompressionOptions.defaultFor(CompressionAlgorithm.ZSTD);
    assertEquals(3, opts.level());
  }

  @Test
  void defaultFor_lz4_level_is_1() {
    CompressionOptions opts = CompressionOptions.defaultFor(CompressionAlgorithm.LZ4);
    assertEquals(1, opts.level());
  }

  @Test
  void builder_custom_options() {
    CompressionOptions opts = CompressionOptions.builder(CompressionAlgorithm.GZIP)
        .level(3)
        .bufferSize(1024)
        .headerEnabled(false)
        .checksumEnabled(false)
        .windowSize(10)
        .build();

    assertEquals(3, opts.level());
    assertEquals(1024, opts.bufferSize());
    assertFalse(opts.headerEnabled());
    assertFalse(opts.checksumEnabled());
    assertEquals(10, opts.windowSize());
  }

  @Test
  void rejects_null_algorithm() {
    assertThrows(NullPointerException.class,
        () -> CompressionOptions.defaultFor(null));
  }

  @Test
  void rejects_invalid_buffer_size() {
    assertThrows(IllegalArgumentException.class,
        () -> CompressionOptions.builder(CompressionAlgorithm.GZIP).bufferSize(0).build());
  }

  @Test
  void rejects_invalid_window_size_too_low() {
    assertThrows(IllegalArgumentException.class,
        () -> CompressionOptions.builder(CompressionAlgorithm.GZIP).windowSize(0).build());
  }

  @Test
  void rejects_invalid_window_size_too_high() {
    assertThrows(IllegalArgumentException.class,
        () -> CompressionOptions.builder(CompressionAlgorithm.GZIP).windowSize(16).build());
  }

  @Test
  void rejects_level_out_of_range_for_gzip() {
    assertThrows(IllegalArgumentException.class,
        () -> CompressionOptions.builder(CompressionAlgorithm.GZIP).level(10).build());
  }

  @Test
  void rejects_level_below_minimum() {
    assertThrows(IllegalArgumentException.class,
        () -> CompressionOptions.builder(CompressionAlgorithm.GZIP).level(0).build());
  }
}
