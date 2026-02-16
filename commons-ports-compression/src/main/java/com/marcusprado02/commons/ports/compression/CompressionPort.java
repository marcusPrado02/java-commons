package com.marcusprado02.commons.ports.compression;

import com.marcusprado02.commons.kernel.result.Result;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Port interface for compression operations.
 * Provides unified abstraction for different compression algorithms.
 *
 * <p>This interface supports both streaming and in-memory compression operations,
 * allowing for flexible usage patterns depending on data size and performance requirements.
 *
 * @author Marcus Prado
 * @since 1.0.0
 */
public interface CompressionPort {

    /**
     * Compresses data from input stream to output stream.
     *
     * @param input the input stream to compress
     * @param output the output stream for compressed data
     * @param options compression options
     * @return result indicating success or error details
     */
    Result<CompressionResult> compress(InputStream input, OutputStream output, CompressionOptions options);

    /**
     * Decompresses data from input stream to output stream.
     *
     * @param input the input stream containing compressed data
     * @param output the output stream for decompressed data
     * @param options compression options (algorithm must match)
     * @return result indicating success or error details
     */
    Result<CompressionResult> decompress(InputStream input, OutputStream output, CompressionOptions options);

    /**
     * Compresses byte array data.
     *
     * @param data the data to compress
     * @param options compression options
     * @return result containing compressed data or error details
     */
    Result<byte[]> compress(byte[] data, CompressionOptions options);

    /**
     * Decompresses byte array data.
     *
     * @param compressedData the compressed data
     * @param options compression options (algorithm must match)
     * @return result containing decompressed data or error details
     */
    Result<byte[]> decompress(byte[] compressedData, CompressionOptions options);

    /**
     * Gets compression ratio for given data using specified options.
     *
     * @param data the data to analyze
     * @param options compression options
     * @return result containing compression ratio (compressed size / original size)
     */
    Result<Double> getCompressionRatio(byte[] data, CompressionOptions options);

    /**
     * Validates if data is compressed with the specified algorithm.
     *
     * @param data the data to validate
     * @param algorithm the expected compression algorithm
     * @return result indicating if data is compressed with the algorithm
     */
    Result<Boolean> isCompressed(byte[] data, CompressionAlgorithm algorithm);

    /**
     * Gets supported compression algorithms by this implementation.
     *
     * @return array of supported algorithms
     */
    CompressionAlgorithm[] getSupportedAlgorithms();

    /**
     * Gets the default compression level for the specified algorithm.
     *
     * @param algorithm the compression algorithm
     * @return default compression level
     */
    int getDefaultCompressionLevel(CompressionAlgorithm algorithm);
}
