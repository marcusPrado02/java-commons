package com.marcusprado02.commons.adapters.compression;

import com.aayushatharva.brotli4j.Brotli4jLoader;
import com.aayushatharva.brotli4j.decoder.BrotliInputStream;
import com.aayushatharva.brotli4j.encoder.BrotliOutputStream;
import com.github.luben.zstd.ZstdInputStream;
import com.github.luben.zstd.ZstdOutputStream;
import com.marcusprado02.commons.kernel.errors.ErrorCategory;
import com.marcusprado02.commons.kernel.errors.ErrorCode;
import com.marcusprado02.commons.kernel.errors.Problem;
import com.marcusprado02.commons.kernel.errors.Severity;
import com.marcusprado02.commons.kernel.result.Result;
import com.marcusprado02.commons.ports.compression.CompressionAlgorithm;
import com.marcusprado02.commons.ports.compression.CompressionOptions;
import com.marcusprado02.commons.ports.compression.CompressionResult;
import com.marcusprado02.commons.ports.compression.StreamingCompressionPort;
import net.jpountz.lz4.LZ4BlockInputStream;
import net.jpountz.lz4.LZ4BlockOutputStream;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xerial.snappy.SnappyInputStream;
import org.xerial.snappy.SnappyOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterInputStream;
import java.util.zip.DeflaterOutputStream;

/**
 * Streaming compression adapter supporting incremental compression and decompression.
 *
 * <p>This implementation provides memory-efficient compression for large datasets
 * by processing data in chunks without loading everything into memory.
 *
 * @author Marcus Prado
 * @since 1.0.0
 */
public class StreamingCompressionAdapter implements StreamingCompressionPort {

    private static final Logger logger = LoggerFactory.getLogger(StreamingCompressionAdapter.class);

    private final boolean brotliAvailable;
    private final LZ4Factory lz4Factory;

    /**
     * Creates a new streaming compression adapter.
     */
    public StreamingCompressionAdapter() {
        this.brotliAvailable = initializeBrotli();
        this.lz4Factory = LZ4Factory.fastestInstance();

        logger.info("StreamingCompressionAdapter initialized");
        logger.debug("Brotli available: {}", brotliAvailable);
    }

    @Override
    public Result<StreamingCompressor> createCompressor(OutputStream output, CompressionOptions options) {
        if (output == null || options == null) {
            return Result.fail(Problem.of(
                ErrorCode.of("COMPRESSION_INVALID_ARGS"),
                ErrorCategory.VALIDATION,
                Severity.ERROR,
                "Output stream and options cannot be null"
            ));
        }

        try {
            StreamingCompressor compressor = switch (options.algorithm()) {
                case GZIP -> new GzipStreamingCompressor(output, options);
                case DEFLATE -> new DeflateStreamingCompressor(output, options);
                case BROTLI -> {
                    if (!brotliAvailable) {
                        throw new UnsupportedOperationException("Brotli not available");
                    }
                    yield new BrotliStreamingCompressor(output, options);
                }
                case LZ4 -> new LZ4StreamingCompressor(output, options, lz4Factory);
                case ZSTD -> new ZstdStreamingCompressor(output, options);
                case SNAPPY -> new SnappyStreamingCompressor(output, options);
            };

            return Result.ok(compressor);

        } catch (Exception e) {
            logger.error("Failed to create compressor for algorithm {}", options.algorithm(), e);
            return Result.fail(Problem.of(
                ErrorCode.of("COMPRESSION_CREATION_ERROR"),
                ErrorCategory.TECHNICAL,
                Severity.ERROR,
                "Failed to create compressor: " + e.getMessage()
            ));
        }
    }

    @Override
    public Result<StreamingDecompressor> createDecompressor(InputStream input, CompressionOptions options) {
        if (input == null || options == null) {
            return Result.fail(Problem.of(
                ErrorCode.of("COMPRESSION_INVALID_ARGS"),
                ErrorCategory.VALIDATION,
                Severity.ERROR,
                "Input stream and options cannot be null"
            ));
        }

        try {
            StreamingDecompressor decompressor = switch (options.algorithm()) {
                case GZIP -> new GzipStreamingDecompressor(input, options);
                case DEFLATE -> new DeflateStreamingDecompressor(input, options);
                case BROTLI -> {
                    if (!brotliAvailable) {
                        throw new UnsupportedOperationException("Brotli not available");
                    }
                    yield new BrotliStreamingDecompressor(input, options);
                }
                case LZ4 -> new LZ4StreamingDecompressor(input, options, lz4Factory);
                case ZSTD -> new ZstdStreamingDecompressor(input, options);
                case SNAPPY -> new SnappyStreamingDecompressor(input, options);
            };

            return Result.ok(decompressor);

        } catch (Exception e) {
            logger.error("Failed to create decompressor for algorithm {}", options.algorithm(), e);
            return Result.fail(Problem.of(
                ErrorCode.of("COMPRESSION_CREATION_ERROR"),
                ErrorCategory.TECHNICAL,
                Severity.ERROR,
                "Failed to create decompressor: " + e.getMessage()
            ));
        }
    }

    private boolean initializeBrotli() {
        try {
            if (!Brotli4jLoader.isAvailable()) {
                Brotli4jLoader.ensureAvailability();
            }
            return Brotli4jLoader.isAvailable();
        } catch (Exception e) {
            logger.warn("Brotli compression not available: {}", e.getMessage());
            return false;
        }
    }

    // Abstract base compressor class
    abstract static class AbstractStreamingCompressor implements StreamingCompressor {
        protected final CompressionOptions options;
        protected final AtomicLong bytesWritten = new AtomicLong(0);
        protected final AtomicLong compressedBytes = new AtomicLong(0);
        protected final AtomicBoolean finished = new AtomicBoolean(false);
        protected final AtomicBoolean closed = new AtomicBoolean(false);
        protected final Instant startTime = Instant.now();

        protected AbstractStreamingCompressor(CompressionOptions options) {
            this.options = options;
        }

        @Override
        public Result<Void> write(byte[] data) {
            if (data == null) {
                return Result.error(createInvalidArgsError("Data cannot be null"));
            }
            return write(data, 0, data.length);
        }

        @Override
        public long getBytesWritten() {
            return bytesWritten.get();
        }

        @Override
        public long getCompressedBytes() {
            return compressedBytes.get();
        }

        @Override
        public CompressionResult getCurrentStats() {
            return CompressionResult.builder(options.algorithm())
                .originalSize(bytesWritten.get())
                .compressedSize(compressedBytes.get())
                .processingTime(Duration.between(startTime, Instant.now()))
                .checksumVerified(options.checksumEnabled())
                .build();
        }

        @Override
        public void close() {
            if (!closed.getAndSet(true)) {
                if (!finished.get()) {
                    finish(); // Try to finish gracefully
                }
                closeInternal();
            }
        }

        protected abstract void closeInternal();

        protected Problem createInvalidArgsError(String message) {
            return Problem.of(
                ErrorCode.of("COMPRESSION_INVALID_ARGS"),
                ErrorCategory.INVALID_INPUT,
                Severity.ERROR,
                message,
                null
            );
        }

        protected Problem createIOError(String message, Throwable cause) {
            return Problem.of(
                ErrorCode.of("COMPRESSION_IO_ERROR"),
                ErrorCategory.INFRASTRUCTURE,
                Severity.ERROR,
                message,
                cause
            );
        }
    }

    // GZIP Streaming Compressor
    static class GzipStreamingCompressor extends AbstractStreamingCompressor {
        private final GZIPOutputStream gzipOut;

        GzipStreamingCompressor(OutputStream output, CompressionOptions options) throws IOException {
            super(options);
            this.gzipOut = new GZIPOutputStream(output);
        }

        @Override
        public Result<Void> write(byte[] data, int offset, int length) {
            if (finished.get() || closed.get()) {
                return Result.error(createInvalidArgsError("Compressor is finished or closed"));
            }

            try {
                gzipOut.write(data, offset, length);
                bytesWritten.addAndGet(length);
                return Result.ok(null);
            } catch (IOException e) {
                return Result.error(createIOError("Failed to write data: " + e.getMessage(), e));
            }
        }

        @Override
        public Result<Void> flush() {
            try {
                gzipOut.flush();
                return Result.ok(null);
            } catch (IOException e) {
                return Result.error(createIOError("Failed to flush: " + e.getMessage(), e));
            }
        }

        @Override
        public Result<CompressionResult> finish() {
            if (finished.getAndSet(true)) {
                return Result.ok(getCurrentStats());
            }

            try {
                gzipOut.finish();
                return Result.ok(getCurrentStats());
            } catch (IOException e) {
                return Result.error(createIOError("Failed to finish compression: " + e.getMessage(), e));
            }
        }

        @Override
        protected void closeInternal() {
            try {
                gzipOut.close();
            } catch (IOException e) {
                logger.warn("Error closing GZIP compressor", e);
            }
        }
    }

    // Similar implementations for other algorithms...
    // For brevity, I'll implement a few key ones and use similar patterns

    // LZ4 Streaming Compressor
    static class LZ4StreamingCompressor extends AbstractStreamingCompressor {
        private final LZ4BlockOutputStream lz4Out;

        LZ4StreamingCompressor(OutputStream output, CompressionOptions options, LZ4Factory factory) throws IOException {
            super(options);
            LZ4Compressor compressor = factory.fastCompressor();
            this.lz4Out = new LZ4BlockOutputStream(output, options.bufferSize(), compressor);
        }

        @Override
        public Result<Void> write(byte[] data, int offset, int length) {
            if (finished.get() || closed.get()) {
                return Result.error(createInvalidArgsError("Compressor is finished or closed"));
            }

            try {
                lz4Out.write(data, offset, length);
                bytesWritten.addAndGet(length);
                return Result.ok(null);
            } catch (IOException e) {
                return Result.error(createIOError("Failed to write data: " + e.getMessage(), e));
            }
        }

        @Override
        public Result<Void> flush() {
            try {
                lz4Out.flush();
                return Result.ok(null);
            } catch (IOException e) {
                return Result.error(createIOError("Failed to flush: " + e.getMessage(), e));
            }
        }

        @Override
        public Result<CompressionResult> finish() {
            if (finished.getAndSet(true)) {
                return Result.ok(getCurrentStats());
            }

            try {
                lz4Out.finish();
                return Result.ok(getCurrentStats());
            } catch (IOException e) {
                return Result.error(createIOError("Failed to finish compression: " + e.getMessage(), e));
            }
        }

        @Override
        protected void closeInternal() {
            try {
                lz4Out.close();
            } catch (IOException e) {
                logger.warn("Error closing LZ4 compressor", e);
            }
        }
    }

    // DEFLATE, Brotli, ZSTD, Snappy compressors follow similar patterns...
    // I'll implement them with the same structure

    static class DeflateStreamingCompressor extends AbstractStreamingCompressor {
        private final DeflaterOutputStream deflaterOut;

        DeflateStreamingCompressor(OutputStream output, CompressionOptions options) throws IOException {
            super(options);
            this.deflaterOut = new DeflaterOutputStream(output);
        }

        @Override
        public Result<Void> write(byte[] data, int offset, int length) {
            if (finished.get() || closed.get()) {
                return Result.error(createInvalidArgsError("Compressor is finished or closed"));
            }

            try {
                deflaterOut.write(data, offset, length);
                bytesWritten.addAndGet(length);
                return Result.ok(null);
            } catch (IOException e) {
                return Result.error(createIOError("Failed to write data: " + e.getMessage(), e));
            }
        }

        @Override
        public Result<Void> flush() {
            try {
                deflaterOut.flush();
                return Result.ok(null);
            } catch (IOException e) {
                return Result.error(createIOError("Failed to flush: " + e.getMessage(), e));
            }
        }

        @Override
        public Result<CompressionResult> finish() {
            if (finished.getAndSet(true)) {
                return Result.ok(getCurrentStats());
            }

            try {
                deflaterOut.finish();
                return Result.ok(getCurrentStats());
            } catch (IOException e) {
                return Result.error(createIOError("Failed to finish compression: " + e.getMessage(), e));
            }
        }

        @Override
        protected void closeInternal() {
            try {
                deflaterOut.close();
            } catch (IOException e) {
                logger.warn("Error closing DEFLATE compressor", e);
            }
        }
    }

    static class BrotliStreamingCompressor extends AbstractStreamingCompressor {
        private final BrotliOutputStream brotliOut;

        BrotliStreamingCompressor(OutputStream output, CompressionOptions options) throws IOException {
            super(options);
            this.brotliOut = new BrotliOutputStream(output);
        }

        @Override
        public Result<Void> write(byte[] data, int offset, int length) {
            if (finished.get() || closed.get()) {
                return Result.error(createInvalidArgsError("Compressor is finished or closed"));
            }

            try {
                brotliOut.write(data, offset, length);
                bytesWritten.addAndGet(length);
                return Result.ok(null);
            } catch (IOException e) {
                return Result.error(createIOError("Failed to write data: " + e.getMessage(), e));
            }
        }

        @Override
        public Result<Void> flush() {
            try {
                brotliOut.flush();
                return Result.ok(null);
            } catch (IOException e) {
                return Result.error(createIOError("Failed to flush: " + e.getMessage(), e));
            }
        }

        @Override
        public Result<CompressionResult> finish() {
            if (finished.getAndSet(true)) {
                return Result.ok(getCurrentStats());
            }

            try {
                brotliOut.flush();
                return Result.ok(getCurrentStats());
            } catch (IOException e) {
                return Result.error(createIOError("Failed to finish compression: " + e.getMessage(), e));
            }
        }

        @Override
        protected void closeInternal() {
            try {
                brotliOut.close();
            } catch (IOException e) {
                logger.warn("Error closing Brotli compressor", e);
            }
        }
    }

    static class ZstdStreamingCompressor extends AbstractStreamingCompressor {
        private final ZstdOutputStream zstdOut;

        ZstdStreamingCompressor(OutputStream output, CompressionOptions options) throws IOException {
            super(options);
            this.zstdOut = new ZstdOutputStream(output, options.level());
        }

        @Override
        public Result<Void> write(byte[] data, int offset, int length) {
            if (finished.get() || closed.get()) {
                return Result.error(createInvalidArgsError("Compressor is finished or closed"));
            }

            try {
                zstdOut.write(data, offset, length);
                bytesWritten.addAndGet(length);
                return Result.ok(null);
            } catch (IOException e) {
                return Result.error(createIOError("Failed to write data: " + e.getMessage(), e));
            }
        }

        @Override
        public Result<Void> flush() {
            try {
                zstdOut.flush();
                return Result.ok(null);
            } catch (IOException e) {
                return Result.error(createIOError("Failed to flush: " + e.getMessage(), e));
            }
        }

        @Override
        public Result<CompressionResult> finish() {
            if (finished.getAndSet(true)) {
                return Result.ok(getCurrentStats());
            }

            try {
                zstdOut.flush();
                return Result.ok(getCurrentStats());
            } catch (IOException e) {
                return Result.error(createIOError("Failed to finish compression: " + e.getMessage(), e));
            }
        }

        @Override
        protected void closeInternal() {
            try {
                zstdOut.close();
            } catch (IOException e) {
                logger.warn("Error closing ZSTD compressor", e);
            }
        }
    }

    static class SnappyStreamingCompressor extends AbstractStreamingCompressor {
        private final SnappyOutputStream snappyOut;

        SnappyStreamingCompressor(OutputStream output, CompressionOptions options) throws IOException {
            super(options);
            this.snappyOut = new SnappyOutputStream(output);
        }

        @Override
        public Result<Void> write(byte[] data, int offset, int length) {
            if (finished.get() || closed.get()) {
                return Result.error(createInvalidArgsError("Compressor is finished or closed"));
            }

            try {
                snappyOut.write(data, offset, length);
                bytesWritten.addAndGet(length);
                return Result.ok(null);
            } catch (IOException e) {
                return Result.error(createIOError("Failed to write data: " + e.getMessage(), e));
            }
        }

        @Override
        public Result<Void> flush() {
            try {
                snappyOut.flush();
                return Result.ok(null);
            } catch (IOException e) {
                return Result.error(createIOError("Failed to flush: " + e.getMessage(), e));
            }
        }

        @Override
        public Result<CompressionResult> finish() {
            if (finished.getAndSet(true)) {
                return Result.ok(getCurrentStats());
            }

            try {
                snappyOut.flush();
                return Result.ok(getCurrentStats());
            } catch (IOException e) {
                return Result.error(createIOError("Failed to finish compression: " + e.getMessage(), e));
            }
        }

        @Override
        protected void closeInternal() {
            try {
                snappyOut.close();
            } catch (IOException e) {
                logger.warn("Error closing Snappy compressor", e);
            }
        }
    }

    // Abstract base decompressor class
    abstract static class AbstractStreamingDecompressor implements StreamingDecompressor {
        protected final CompressionOptions options;
        protected final AtomicLong compressedBytesRead = new AtomicLong(0);
        protected final AtomicLong decompressedBytes = new AtomicLong(0);
        protected final AtomicBoolean closed = new AtomicBoolean(false);
        protected final Instant startTime = Instant.now();

        protected AbstractStreamingDecompressor(CompressionOptions options) {
            this.options = options;
        }

        @Override
        public Result<Integer> read(byte[] buffer) {
            if (buffer == null) {
                return Result.error(createInvalidArgsError("Buffer cannot be null"));
            }
            return read(buffer, 0, buffer.length);
        }

        @Override
        public long getCompressedBytesRead() {
            return compressedBytesRead.get();
        }

        @Override
        public long getDecompressedBytes() {
            return decompressedBytes.get();
        }

        @Override
        public CompressionResult getCurrentStats() {
            return CompressionResult.builder(options.algorithm())
                .originalSize(decompressedBytes.get())
                .compressedSize(compressedBytesRead.get())
                .processingTime(Duration.between(startTime, Instant.now()))
                .checksumVerified(options.checksumEnabled())
                .build();
        }

        @Override
        public void close() {
            if (!closed.getAndSet(true)) {
                closeInternal();
            }
        }

        protected abstract void closeInternal();

        protected Problem createInvalidArgsError(String message) {
            return Problem.of(
                ErrorCode.of("COMPRESSION_INVALID_ARGS"),
                ErrorCategory.INVALID_INPUT,
                Severity.ERROR,
                message,
                null
            );
        }

        protected Problem createIOError(String message, Throwable cause) {
            return Problem.of(
                ErrorCode.of("COMPRESSION_IO_ERROR"),
                ErrorCategory.INFRASTRUCTURE,
                Severity.ERROR,
                message,
                cause
            );
        }
    }

    // Decompressor implementations
    static class GzipStreamingDecompressor extends AbstractStreamingDecompressor {
        private final GZIPInputStream gzipIn;

        GzipStreamingDecompressor(InputStream input, CompressionOptions options) throws IOException {
            super(options);
            this.gzipIn = new GZIPInputStream(input);
        }

        @Override
        public Result<Integer> read(byte[] buffer, int offset, int length) {
            if (closed.get()) {
                return Result.error(createInvalidArgsError("Decompressor is closed"));
            }

            try {
                int bytesRead = gzipIn.read(buffer, offset, length);
                if (bytesRead > 0) {
                    decompressedBytes.addAndGet(bytesRead);
                }
                return Result.ok(bytesRead);
            } catch (IOException e) {
                return Result.error(createIOError("Failed to read data: " + e.getMessage(), e));
            }
        }

        @Override
        public Result<Long> skip(long n) {
            try {
                long skipped = gzipIn.skip(n);
                return Result.ok(skipped);
            } catch (IOException e) {
                return Result.error(createIOError("Failed to skip data: " + e.getMessage(), e));
            }
        }

        @Override
        public Result<Boolean> hasMore() {
            try {
                int available = gzipIn.available();
                return Result.ok(available > 0);
            } catch (IOException e) {
                return Result.error(createIOError("Failed to check availability: " + e.getMessage(), e));
            }
        }

        @Override
        protected void closeInternal() {
            try {
                gzipIn.close();
            } catch (IOException e) {
                logger.warn("Error closing GZIP decompressor", e);
            }
        }
    }

    // Similar decompressor implementations for other algorithms...
    // Following the same pattern as compressors

    static class LZ4StreamingDecompressor extends AbstractStreamingDecompressor {
        private final LZ4BlockInputStream lz4In;

        LZ4StreamingDecompressor(InputStream input, CompressionOptions options, LZ4Factory factory) throws IOException {
            super(options);
            LZ4FastDecompressor decompressor = factory.fastDecompressor();
            this.lz4In = new LZ4BlockInputStream(input, decompressor);
        }

        @Override
        public Result<Integer> read(byte[] buffer, int offset, int length) {
            if (closed.get()) {
                return Result.error(createInvalidArgsError("Decompressor is closed"));
            }

            try {
                int bytesRead = lz4In.read(buffer, offset, length);
                if (bytesRead > 0) {
                    decompressedBytes.addAndGet(bytesRead);
                }
                return Result.ok(bytesRead);
            } catch (IOException e) {
                return Result.error(createIOError("Failed to read data: " + e.getMessage(), e));
            }
        }

        @Override
        public Result<Long> skip(long n) {
            try {
                long skipped = lz4In.skip(n);
                return Result.ok(skipped);
            } catch (IOException e) {
                return Result.error(createIOError("Failed to skip data: " + e.getMessage(), e));
            }
        }

        @Override
        public Result<Boolean> hasMore() {
            try {
                int available = lz4In.available();
                return Result.ok(available > 0);
            } catch (IOException e) {
                return Result.error(createIOError("Failed to check availability: " + e.getMessage(), e));
            }
        }

        @Override
        protected void closeInternal() {
            try {
                lz4In.close();
            } catch (IOException e) {
                logger.warn("Error closing LZ4 decompressor", e);
            }
        }
    }

    // Additional decompressor implementations with similar patterns...
    static class DeflateStreamingDecompressor extends AbstractStreamingDecompressor {
        private final InflaterInputStream inflaterIn;

        DeflateStreamingDecompressor(InputStream input, CompressionOptions options) throws IOException {
            super(options);
            this.inflaterIn = new InflaterInputStream(input);
        }

        @Override
        public Result<Integer> read(byte[] buffer, int offset, int length) {
            if (closed.get()) {
                return Result.error(createInvalidArgsError("Decompressor is closed"));
            }

            try {
                int bytesRead = inflaterIn.read(buffer, offset, length);
                if (bytesRead > 0) {
                    decompressedBytes.addAndGet(bytesRead);
                }
                return Result.ok(bytesRead);
            } catch (IOException e) {
                return Result.error(createIOError("Failed to read data: " + e.getMessage(), e));
            }
        }

        @Override
        public Result<Long> skip(long n) {
            try {
                long skipped = inflaterIn.skip(n);
                return Result.ok(skipped);
            } catch (IOException e) {
                return Result.error(createIOError("Failed to skip data: " + e.getMessage(), e));
            }
        }

        @Override
        public Result<Boolean> hasMore() {
            try {
                int available = inflaterIn.available();
                return Result.ok(available > 0);
            } catch (IOException e) {
                return Result.error(createIOError("Failed to check availability: " + e.getMessage(), e));
            }
        }

        @Override
        protected void closeInternal() {
            try {
                inflaterIn.close();
            } catch (IOException e) {
                logger.warn("Error closing DEFLATE decompressor", e);
            }
        }
    }

    static class BrotliStreamingDecompressor extends AbstractStreamingDecompressor {
        private final BrotliInputStream brotliIn;

        BrotliStreamingDecompressor(InputStream input, CompressionOptions options) throws IOException {
            super(options);
            this.brotliIn = new BrotliInputStream(input);
        }

        @Override
        public Result<Integer> read(byte[] buffer, int offset, int length) {
            if (closed.get()) {
                return Result.error(createInvalidArgsError("Decompressor is closed"));
            }

            try {
                int bytesRead = brotliIn.read(buffer, offset, length);
                if (bytesRead > 0) {
                    decompressedBytes.addAndGet(bytesRead);
                }
                return Result.ok(bytesRead);
            } catch (IOException e) {
                return Result.error(createIOError("Failed to read data: " + e.getMessage(), e));
            }
        }

        @Override
        public Result<Long> skip(long n) {
            try {
                long skipped = brotliIn.skip(n);
                return Result.ok(skipped);
            } catch (IOException e) {
                return Result.error(createIOError("Failed to skip data: " + e.getMessage(), e));
            }
        }

        @Override
        public Result<Boolean> hasMore() {
            try {
                int available = brotliIn.available();
                return Result.ok(available > 0);
            } catch (IOException e) {
                return Result.error(createIOError("Failed to check availability: " + e.getMessage(), e));
            }
        }

        @Override
        protected void closeInternal() {
            try {
                brotliIn.close();
            } catch (IOException e) {
                logger.warn("Error closing Brotli decompressor", e);
            }
        }
    }

    static class ZstdStreamingDecompressor extends AbstractStreamingDecompressor {
        private final ZstdInputStream zstdIn;

        ZstdStreamingDecompressor(InputStream input, CompressionOptions options) throws IOException {
            super(options);
            this.zstdIn = new ZstdInputStream(input);
        }

        @Override
        public Result<Integer> read(byte[] buffer, int offset, int length) {
            if (closed.get()) {
                return Result.error(createInvalidArgsError("Decompressor is closed"));
            }

            try {
                int bytesRead = zstdIn.read(buffer, offset, length);
                if (bytesRead > 0) {
                    decompressedBytes.addAndGet(bytesRead);
                }
                return Result.ok(bytesRead);
            } catch (IOException e) {
                return Result.error(createIOError("Failed to read data: " + e.getMessage(), e));
            }
        }

        @Override
        public Result<Long> skip(long n) {
            try {
                long skipped = zstdIn.skip(n);
                return Result.ok(skipped);
            } catch (IOException e) {
                return Result.error(createIOError("Failed to skip data: " + e.getMessage(), e));
            }
        }

        @Override
        public Result<Boolean> hasMore() {
            try {
                int available = zstdIn.available();
                return Result.ok(available > 0);
            } catch (IOException e) {
                return Result.error(createIOError("Failed to check availability: " + e.getMessage(), e));
            }
        }

        @Override
        protected void closeInternal() {
            try {
                zstdIn.close();
            } catch (IOException e) {
                logger.warn("Error closing ZSTD decompressor", e);
            }
        }
    }

    static class SnappyStreamingDecompressor extends AbstractStreamingDecompressor {
        private final SnappyInputStream snappyIn;

        SnappyStreamingDecompressor(InputStream input, CompressionOptions options) throws IOException {
            super(options);
            this.snappyIn = new SnappyInputStream(input);
        }

        @Override
        public Result<Integer> read(byte[] buffer, int offset, int length) {
            if (closed.get()) {
                return Result.error(createInvalidArgsError("Decompressor is closed"));
            }

            try {
                int bytesRead = snappyIn.read(buffer, offset, length);
                if (bytesRead > 0) {
                    decompressedBytes.addAndGet(bytesRead);
                }
                return Result.ok(bytesRead);
            } catch (IOException e) {
                return Result.error(createIOError("Failed to read data: " + e.getMessage(), e));
            }
        }

        @Override
        public Result<Long> skip(long n) {
            try {
                long skipped = snappyIn.skip(n);
                return Result.ok(skipped);
            } catch (IOException e) {
                return Result.error(createIOError("Failed to skip data: " + e.getMessage(), e));
            }
        }

        @Override
        public Result<Boolean> hasMore() {
            try {
                int available = snappyIn.available();
                return Result.ok(available > 0);
            } catch (IOException e) {
                return Result.error(createIOError("Failed to check availability: " + e.getMessage(), e));
            }
        }

        @Override
        protected void closeInternal() {
            try {
                snappyIn.close();
            } catch (IOException e) {
                logger.warn("Error closing Snappy decompressor", e);
            }
        }
    }
}
