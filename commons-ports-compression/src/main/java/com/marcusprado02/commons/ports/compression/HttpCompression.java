package com.marcusprado02.commons.ports.compression;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * HTTP compression utilities for web API integration.
 *
 * <p>This class provides utilities for working with HTTP compression headers, content negotiation,
 * and compression algorithm selection based on client preferences.
 *
 * @author Marcus Prado
 * @since 1.0.0
 */
public final class HttpCompression {

  /** HTTP Accept-Encoding header name. */
  public static final String ACCEPT_ENCODING_HEADER = "Accept-Encoding";

  /** HTTP Content-Encoding header name. */
  public static final String CONTENT_ENCODING_HEADER = "Content-Encoding";

  /** HTTP Vary header name. */
  public static final String VARY_HEADER = "Vary";

  private HttpCompression() {
    // Utility class
  }

  /**
   * Parses Accept-Encoding header and returns supported algorithms in preference order.
   *
   * @param acceptEncodingHeader Accept-Encoding header value
   * @return list of algorithms in preference order (highest quality first)
   */
  public static List<CompressionAlgorithm> parseAcceptEncoding(String acceptEncodingHeader) {
    if (acceptEncodingHeader == null || acceptEncodingHeader.trim().isEmpty()) {
      return Collections.emptyList();
    }

    List<EncodingPreference> preferences = new ArrayList<>();
    String[] encodings = acceptEncodingHeader.split(",");

    for (String encoding : encodings) {
      EncodingPreference preference = parseEncodingPreference(encoding.trim());
      if (preference != null) {
        preferences.add(preference);
      }
    }

    // Sort by quality value (highest first)
    preferences.sort((a, b) -> Double.compare(b.quality(), a.quality()));

    return preferences.stream()
        .map(EncodingPreference::algorithm)
        .filter(Objects::nonNull)
        .toList();
  }

  /**
   * Selects the best compression algorithm based on client preferences and server capabilities.
   *
   * @param acceptEncodingHeader Accept-Encoding header value
   * @param supportedAlgorithms algorithms supported by the server
   * @return best algorithm or null if no match found
   */
  public static CompressionAlgorithm selectBestAlgorithm(
      String acceptEncodingHeader, CompressionAlgorithm[] supportedAlgorithms) {

    if (supportedAlgorithms == null || supportedAlgorithms.length == 0) {
      return null;
    }

    List<CompressionAlgorithm> clientPreferences = parseAcceptEncoding(acceptEncodingHeader);
    List<CompressionAlgorithm> serverCapabilities = Arrays.asList(supportedAlgorithms);

    // Find first client preference that server supports
    for (CompressionAlgorithm clientPref : clientPreferences) {
      if (serverCapabilities.contains(clientPref)) {
        return clientPref;
      }
    }

    return null;
  }

  /**
   * Creates Content-Encoding header value for the given algorithm.
   *
   * @param algorithm compression algorithm
   * @return Content-Encoding header value
   */
  public static String createContentEncodingHeader(CompressionAlgorithm algorithm) {
    return algorithm.getAlgorithmName();
  }

  /**
   * Creates Vary header value for compression-aware responses.
   *
   * @return Vary header value
   */
  public static String createVaryHeader() {
    return ACCEPT_ENCODING_HEADER;
  }

  /**
   * Checks if the given MIME type is compressible. Text-based content types generally benefit from
   * compression.
   *
   * @param mimeType MIME type to check
   * @return true if the content type should be compressed
   */
  public static boolean isCompressible(String mimeType) {
    if (mimeType == null || mimeType.trim().isEmpty()) {
      return false;
    }

    String normalized = mimeType.toLowerCase().trim();

    // Remove charset and other parameters
    int semicolon = normalized.indexOf(';');
    if (semicolon > 0) {
      normalized = normalized.substring(0, semicolon).trim();
    }

    // Text-based content types
    if (normalized.startsWith("text/")) {
      return true;
    }

    // Common compressible application types
    return normalized.equals("application/json")
        || normalized.equals("application/xml")
        || normalized.equals("application/javascript")
        || normalized.equals("application/css")
        || normalized.equals("application/x-javascript")
        || normalized.equals("application/xhtml+xml")
        || normalized.equals("application/rss+xml")
        || normalized.equals("application/atom+xml")
        || normalized.startsWith("application/json")
        || // includes application/json; charset=utf-8
        normalized.startsWith("application/xml"); // includes application/xml; charset=utf-8
  }

  /**
   * Checks if content should be compressed based on size and type.
   *
   * @param contentLength content length in bytes
   * @param mimeType content MIME type
   * @param minSize minimum size threshold for compression
   * @return true if content should be compressed
   */
  public static boolean shouldCompress(long contentLength, String mimeType, int minSize) {
    return contentLength >= minSize && isCompressible(mimeType);
  }

  /**
   * Creates compression options suitable for HTTP responses.
   *
   * @param algorithm compression algorithm
   * @param fast true for fast compression (lower CPU), false for better ratio
   * @return compression options optimized for HTTP
   */
  public static CompressionOptions createHttpOptions(CompressionAlgorithm algorithm, boolean fast) {
    CompressionOptions.Builder builder =
        CompressionOptions.builder(algorithm).bufferSize(8192); // Smaller buffer for HTTP streams

    if (fast) {
      return CompressionOptions.fastFor(algorithm);
    } else {
      // Balanced compression for HTTP (not maximum to avoid high CPU usage)
      int defaultLevel =
          switch (algorithm) {
            case GZIP, DEFLATE -> 6;
            case BROTLI -> 4;
            case LZ4 -> 1;
            case SNAPPY -> 1;
            case ZSTD -> 3;
          };
      builder.level(defaultLevel);
    }

    return builder.build();
  }

  /** Parses a single encoding preference from Accept-Encoding header. */
  private static EncodingPreference parseEncodingPreference(String encoding) {
    if (encoding == null || encoding.isEmpty()) {
      return null;
    }

    double quality = 1.0; // default quality
    String algorithmName = encoding;

    // Check for quality value (e.g., "gzip;q=0.8")
    int qIndex = encoding.indexOf(";q=");
    if (qIndex > 0) {
      algorithmName = encoding.substring(0, qIndex).trim();
      String qValue = encoding.substring(qIndex + 3).trim();
      try {
        quality = Double.parseDouble(qValue);
        if (quality < 0 || quality > 1) {
          quality = 1.0; // Invalid quality, use default
        }
      } catch (NumberFormatException e) {
        quality = 1.0; // Invalid quality, use default
      }
    }

    // Skip identity and wildcard encodings for algorithm selection
    if ("identity".equals(algorithmName) || "*".equals(algorithmName)) {
      return null;
    }

    CompressionAlgorithm algorithm = CompressionAlgorithm.fromName(algorithmName);
    return new EncodingPreference(algorithm, quality);
  }

  /** Represents an encoding preference with quality value. */
  private record EncodingPreference(CompressionAlgorithm algorithm, double quality) {}
}
