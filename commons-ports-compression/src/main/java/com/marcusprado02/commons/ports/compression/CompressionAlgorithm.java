package com.marcusprado02.commons.ports.compression;

/**
 * Enumeration of supported compression algorithms.
 *
 * <p>Each algorithm has different performance and compression characteristics:
 * <ul>
 *   <li><b>GZIP</b>: Good balance between speed and compression ratio, widely supported</li>
 *   <li><b>DEFLATE</b>: Similar to GZIP but without headers, used in ZIP files</li>
 *   <li><b>BROTLI</b>: Superior compression ratio, especially for text/HTML, slower compression</li>
 *   <li><b>LZ4</b>: Extremely fast compression/decompression, lower compression ratio</li>
 *   <li><b>ZSTD</b>: Great balance of speed and compression, Facebook's Zstandard</li>
 *   <li><b>SNAPPY</b>: Fast compression focused on speed over ratio, used by Google</li>
 * </ul>
 *
 * @author Marcus Prado
 * @since 1.0.0
 */
public enum CompressionAlgorithm {

    /**
     * GZIP compression algorithm (RFC 1952).
     * Good balance between compression ratio and speed.
     * Widely supported across platforms and libraries.
     */
    GZIP("gzip", "application/gzip", new String[]{"gz", "gzip"}),

    /**
     * DEFLATE compression algorithm (RFC 1951).
     * Similar to GZIP but without the header and checksum.
     * Used in ZIP files and HTTP compression.
     */
    DEFLATE("deflate", "application/octet-stream", new String[]{"deflate"}),

    /**
     * Brotli compression algorithm (RFC 7932).
     * Superior compression ratio especially for text content.
     * Supported by modern browsers for HTTP compression.
     */
    BROTLI("br", "application/brotli", new String[]{"br", "brotli"}),

    /**
     * LZ4 compression algorithm.
     * Extremely fast compression and decompression.
     * Good choice when speed is more important than compression ratio.
     */
    LZ4("lz4", "application/lz4", new String[]{"lz4"}),

    /**
     * Zstandard (ZSTD) compression algorithm.
     * Great balance between speed and compression ratio.
     * Developed by Facebook, gaining widespread adoption.
     */
    ZSTD("zstd", "application/zstd", new String[]{"zst", "zstd"}),

    /**
     * Snappy compression algorithm.
     * Focuses on speed rather than compression ratio.
     * Developed by Google, used in BigTable and other systems.
     */
    SNAPPY("snappy", "application/snappy", new String[]{"snappy", "sz"});

    private final String algorithmName;
    private final String mimeType;
    private final String[] fileExtensions;

    CompressionAlgorithm(String algorithmName, String mimeType, String[] fileExtensions) {
        this.algorithmName = algorithmName;
        this.mimeType = mimeType;
        this.fileExtensions = fileExtensions.clone();
    }

    /**
     * Gets the algorithm name as used in HTTP headers and configuration.
     *
     * @return algorithm name
     */
    public String getAlgorithmName() {
        return algorithmName;
    }

    /**
     * Gets the MIME type for this compression algorithm.
     *
     * @return MIME type
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * Gets file extensions commonly used for this compression algorithm.
     *
     * @return array of file extensions (without dot)
     */
    public String[] getFileExtensions() {
        return fileExtensions.clone();
    }

    /**
     * Gets compression algorithm by name (case-insensitive).
     *
     * @param name algorithm name
     * @return compression algorithm or null if not found
     */
    public static CompressionAlgorithm fromName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }

        String normalizedName = name.trim().toLowerCase();
        for (CompressionAlgorithm algorithm : values()) {
            if (algorithm.algorithmName.equalsIgnoreCase(normalizedName)) {
                return algorithm;
            }
        }
        return null;
    }

    /**
     * Gets compression algorithm by MIME type.
     *
     * @param mimeType MIME type
     * @return compression algorithm or null if not found
     */
    public static CompressionAlgorithm fromMimeType(String mimeType) {
        if (mimeType == null || mimeType.trim().isEmpty()) {
            return null;
        }

        String normalizedMimeType = mimeType.trim().toLowerCase();
        for (CompressionAlgorithm algorithm : values()) {
            if (algorithm.mimeType.equalsIgnoreCase(normalizedMimeType)) {
                return algorithm;
            }
        }
        return null;
    }

    /**
     * Gets compression algorithm by file extension.
     *
     * @param extension file extension (with or without dot)
     * @return compression algorithm or null if not found
     */
    public static CompressionAlgorithm fromFileExtension(String extension) {
        if (extension == null || extension.trim().isEmpty()) {
            return null;
        }

        String normalizedExt = extension.trim().toLowerCase();
        if (normalizedExt.startsWith(".")) {
            normalizedExt = normalizedExt.substring(1);
        }

        for (CompressionAlgorithm algorithm : values()) {
            for (String ext : algorithm.fileExtensions) {
                if (ext.equalsIgnoreCase(normalizedExt)) {
                    return algorithm;
                }
            }
        }
        return null;
    }
}
