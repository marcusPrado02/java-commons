package com.marcusprado02.commons.ports.compression;

import com.marcusprado02.commons.kernel.result.Result;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Streaming compression interface for processing large data volumes.
 *
 * <p>This interface provides streaming compression capabilities that allow
 * processing of large datasets without loading everything into memory.
 * It supports both push and pull streaming patterns.
 *
 * @author Marcus Prado
 * @since 1.0.0
 */
public interface StreamingCompressionPort {

    /**
     * Creates a streaming compressor for the given options.
     *
     * @param output output stream for compressed data
     * @param options compression options
     * @return result containing streaming compressor or error
     */
    Result<StreamingCompressor> createCompressor(OutputStream output, CompressionOptions options);

    /**
     * Creates a streaming decompressor for the given options.
     *
     * @param input input stream containing compressed data
     * @param options compression options (algorithm must match)
     * @return result containing streaming decompressor or error
     */
    Result<StreamingDecompressor> createDecompressor(InputStream input, CompressionOptions options);

    /**
     * Streaming compressor interface for incremental compression.
     */
    interface StreamingCompressor extends AutoCloseable {

        /**
         * Compresses and writes a chunk of data.
         *
         * @param data data chunk to compress
         * @return result indicating success or error
         */
        Result<Void> write(byte[] data);

        /**
         * Compresses and writes a portion of data.
         *
         * @param data data array
         * @param offset offset in the array
         * @param length number of bytes to compress
         * @return result indicating success or error
         */
        Result<Void> write(byte[] data, int offset, int length);

        /**
         * Flushes any buffered data to the output stream.
         *
         * @return result indicating success or error
         */
        Result<Void> flush();

        /**
         * Finishes compression and writes any remaining data.
         * Must be called before closing.
         *
         * @return result containing final compression statistics
         */
        Result<CompressionResult> finish();

        /**
         * Closes the compressor and releases resources.
         * Automatically calls finish() if not already called.
         */
        @Override
        void close();

        /**
         * Gets current compression statistics.
         *
         * @return current statistics (may be incomplete if not finished)
         */
        CompressionResult getCurrentStats();

        /**
         * Gets the total bytes written to the compressor.
         *
         * @return bytes written
         */
        long getBytesWritten();

        /**
         * Gets the total compressed bytes produced.
         *
         * @return compressed bytes
         */
        long getCompressedBytes();
    }

    /**
     * Streaming decompressor interface for incremental decompression.
     */
    interface StreamingDecompressor extends AutoCloseable {

        /**
         * Reads and decompresses data into the provided buffer.
         *
         * @param buffer buffer to read into
         * @return result containing number of bytes read or error
         */
        Result<Integer> read(byte[] buffer);

        /**
         * Reads and decompresses data into a portion of the buffer.
         *
         * @param buffer buffer to read into
         * @param offset offset in the buffer
         * @param length maximum bytes to read
         * @return result containing number of bytes read or error
         */
        Result<Integer> read(byte[] buffer, int offset, int length);

        /**
         * Skips compressed data.
         *
         * @param n number of decompressed bytes to skip
         * @return result containing actual bytes skipped or error
         */
        Result<Long> skip(long n);

        /**
         * Checks if more data is available.
         *
         * @return result indicating data availability or error
         */
        Result<Boolean> hasMore();

        /**
         * Gets current decompression statistics.
         *
         * @return current statistics
         */
        CompressionResult getCurrentStats();

        /**
         * Gets the total compressed bytes read.
         *
         * @return compressed bytes read
         */
        long getCompressedBytesRead();

        /**
         * Gets the total decompressed bytes produced.
         *
         * @return decompressed bytes
         */
        long getDecompressedBytes();

        /**
         * Closes the decompressor and releases resources.
         */
        @Override
        void close();
    }
}
