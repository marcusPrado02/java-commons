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
import com.marcusprado02.commons.ports.compression.CompressionPort;
import com.marcusprado02.commons.ports.compression.CompressionResult;
import net.jpountz.lz4.LZ4BlockInputStream;
import net.jpountz.lz4.LZ4BlockOutputStream;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xerial.snappy.SnappyInputStream;
import org.xerial.snappy.SnappyOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterInputStream;
import java.util.zip.DeflaterOutputStream;

/**
 * Multi-algorithm compression adapter supporting GZIP, DEFLATE, Brotli, LZ4, ZSTD, and Snappy.
 *
 * <p>This implementation provides comprehensive compression support with proper error handling,
 * performance metrics, and validation capabilities for all supported algorithms.
 *
 * @author Marcus Prado
 * @since 1.0.0
 */
public class MultiAlgorithmCompressionAdapter implements CompressionPort {

    private static final Logger logger = LoggerFactory.getLogger(MultiAlgorithmCompressionAdapter.class);

    private final boolean brotliAvailable;
    private final LZ4Factory lz4Factory;

    /**
     * Creates a new multi-algorithm compression adapter.
     * Initializes all compression libraries and logs availability.
     */
    public MultiAlgorithmCompressionAdapter() {
        this.brotliAvailable = initializeBrotli();
        this.lz4Factory = LZ4Factory.fastestInstance();

        logger.info("MultiAlgorithmCompressionAdapter initialized");
        logger.debug("Brotli available: {}", brotliAvailable);
        logger.debug("LZ4 factory: {}", lz4Factory.getClass().getSimpleName());
    }

    @Override
    public Result<CompressionResult> compress(InputStream input, OutputStream output, CompressionOptions options) {
        if (input == null || output == null || options == null) {
            return Result.error(Problem.of(
                ErrorCode.of("COMPRESSION_INVALID_ARGS"),
                ErrorCategory.INVALID_INPUT,
                Severity.ERROR,
                "Input stream, output stream, and options cannot be null",
                null
            ));
        }

        Instant start = Instant.now();
        long originalSize = 0;
        long compressedSize = 0;

        try {
            switch (options.algorithm()) {
                case GZIP -> {
                    var result = compressGzip(input, output, options);
                    if (!result.isOk()) return result;
                    originalSize = result.getValue().originalSize();
                    compressedSize = result.getValue().compressedSize();
                }
                case DEFLATE -> {
                    var result = compressDeflate(input, output, options);
                    if (!result.isOk()) return result;
                    originalSize = result.getValue().originalSize();
                    compressedSize = result.getValue().compressedSize();
                }
                case BROTLI -> {
                    if (!brotliAvailable) {
                        return Result.error(createUnsupportedAlgorithmError(options.algorithm()));
                    }
                    var result = compressBrotli(input, output, options);
                    if (!result.isOk()) return result;
                    originalSize = result.getValue().originalSize();
                    compressedSize = result.getValue().compressedSize();
                }
                case LZ4 -> {
                    var result = compressLZ4(input, output, options);
                    if (!result.isOk()) return result;
                    originalSize = result.getValue().originalSize();
                    compressedSize = result.getValue().compressedSize();
                }
                case ZSTD -> {
                    var result = compressZstd(input, output, options);
                    if (!result.isOk()) return result;
                    originalSize = result.getValue().originalSize();
                    compressedSize = result.getValue().compressedSize();
                }
                case SNAPPY -> {
                    var result = compressSnappy(input, output, options);
                    if (!result.isOk()) return result;
                    originalSize = result.getValue().originalSize();
                    compressedSize = result.getValue().compressedSize();
                }
                default -> {
                    return Result.error(createUnsupportedAlgorithmError(options.algorithm()));
                }
            }

            Duration processingTime = Duration.between(start, Instant.now());

            CompressionResult result = CompressionResult.builder(options.algorithm())
                .originalSize(originalSize)
                .compressedSize(compressedSize)
                .processingTime(processingTime)
                .checksumVerified(options.checksumEnabled())
                .build();

            return Result.ok(result);

        } catch (IOException e) {
            logger.error("Compression failed for algorithm {}", options.algorithm(), e);
            return Result.error(Problem.of(
                ErrorCode.of("COMPRESSION_IO_ERROR"),
                ErrorCategory.INFRASTRUCTURE,
                Severity.ERROR,
                "I/O error during compression: " + e.getMessage(),
                e
            ));
        }
    }

    @Override
    public Result<CompressionResult> decompress(InputStream input, OutputStream output, CompressionOptions options) {
        if (input == null || output == null || options == null) {
            return Result.error(Problem.of(
                ErrorCode.of("COMPRESSION_INVALID_ARGS"),
                ErrorCategory.INVALID_INPUT,
                Severity.ERROR,
                "Input stream, output stream, and options cannot be null",
                null
            ));
        }

        Instant start = Instant.now();
        long compressedSize = 0;
        long decompressedSize = 0;

        try {
            switch (options.algorithm()) {
                case GZIP -> {
                    var result = decompressGzip(input, output, options);
                    if (!result.isOk()) return result;
                    compressedSize = result.getValue().compressedSize();
                    decompressedSize = result.getValue().originalSize();
                }
                case DEFLATE -> {
                    var result = decompressDeflate(input, output, options);
                    if (!result.isOk()) return result;
                    compressedSize = result.getValue().compressedSize();
                    decompressedSize = result.getValue().originalSize();
                }
                case BROTLI -> {
                    if (!brotliAvailable) {
                        return Result.error(createUnsupportedAlgorithmError(options.algorithm()));
                    }
                    var result = decompressBrotli(input, output, options);
                    if (!result.isOk()) return result;
                    compressedSize = result.getValue().compressedSize();
                    decompressedSize = result.getValue().originalSize();
                }
                case LZ4 -> {
                    var result = decompressLZ4(input, output, options);
                    if (!result.isOk()) return result;
                    compressedSize = result.getValue().compressedSize();
                    decompressedSize = result.getValue().originalSize();
                }
                case ZSTD -> {
                    var result = decompressZstd(input, output, options);
                    if (!result.isOk()) return result;
                    compressedSize = result.getValue().compressedSize();
                    decompressedSize = result.getValue().originalSize();
                }
                case SNAPPY -> {
                    var result = decompressSnappy(input, output, options);
                    if (!result.isOk()) return result;
                    compressedSize = result.getValue().compressedSize();
                    decompressedSize = result.getValue().originalSize();
                }
                default -> {
                    return Result.error(createUnsupportedAlgorithmError(options.algorithm()));
                }
            }

            Duration processingTime = Duration.between(start, Instant.now());

            CompressionResult result = CompressionResult.builder(options.algorithm())
                .originalSize(decompressedSize)
                .compressedSize(compressedSize)
                .processingTime(processingTime)
                .checksumVerified(options.checksumEnabled())
                .build();

            return Result.ok(result);

        } catch (IOException e) {
            logger.error("Decompression failed for algorithm {}", options.algorithm(), e);
            return Result.error(Problem.of(
                ErrorCode.of("COMPRESSION_IO_ERROR"),
                ErrorCategory.INFRASTRUCTURE,
                Severity.ERROR,
                "I/O error during decompression: " + e.getMessage(),
                e
            ));
        }
    }

    @Override
    public Result<byte[]> compress(byte[] data, CompressionOptions options) {
        if (data == null || options == null) {
            return Result.error(Problem.of(
                ErrorCode.of("COMPRESSION_INVALID_ARGS"),
                ErrorCategory.INVALID_INPUT,
                Severity.ERROR,
                "Data and options cannot be null",
                null
            ));
        }

        try (ByteArrayInputStream input = new ByteArrayInputStream(data);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            Result<CompressionResult> result = compress(input, output, options);
            if (!result.isOk()) {
                return Result.error(result.getProblem());
            }

            return Result.ok(output.toByteArray());

        } catch (IOException e) {
            logger.error("Failed to compress byte array", e);
            return Result.error(Problem.of(
                ErrorCode.of("COMPRESSION_IO_ERROR"),
                ErrorCategory.INFRASTRUCTURE,
                Severity.ERROR,
                "Failed to compress byte array: " + e.getMessage(),
                e
            ));
        }
    }

    @Override
    public Result<byte[]> decompress(byte[] compressedData, CompressionOptions options) {
        if (compressedData == null || options == null) {
            return Result.error(Problem.of(
                ErrorCode.of("COMPRESSION_INVALID_ARGS"),
                ErrorCategory.INVALID_INPUT,
                Severity.ERROR,
                "Compressed data and options cannot be null",
                null
            ));
        }

        try (ByteArrayInputStream input = new ByteArrayInputStream(compressedData);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            Result<CompressionResult> result = decompress(input, output, options);
            if (!result.isOk()) {
                return Result.error(result.getProblem());
            }

            return Result.ok(output.toByteArray());

        } catch (IOException e) {
            logger.error("Failed to decompress byte array", e);
            return Result.error(Problem.of(
                ErrorCode.of("COMPRESSION_IO_ERROR"),
                ErrorCategory.INFRASTRUCTURE,
                Severity.ERROR,
                "Failed to decompress byte array: " + e.getMessage(),
                e
            ));
        }
    }

    @Override
    public Result<Double> getCompressionRatio(byte[] data, CompressionOptions options) {
        if (data == null || options == null) {
            return Result.fail(Problem.of(
                ErrorCode.of("COMPRESSION_INVALID_ARGS"),
                ErrorCategory.VALIDATION,
                Severity.ERROR,
                "Data and options cannot be null"
            ));
        }

        Result<byte[]> compressed = compress(data, options);
        if (!compressed.isOk()) {
            return Result.error(compressed.getProblem());
        }

        if (data.length == 0) {
            return Result.ok(1.0);
        }

        double ratio = (double) compressed.getValue().length / data.length;
        return Result.ok(ratio);
    }

    @Override
    public Result<Boolean> isCompressed(byte[] data, CompressionAlgorithm algorithm) {
        if (data == null || algorithm == null) {
            return Result.fail(Problem.of(
                ErrorCode.of("COMPRESSION_INVALID_ARGS"),
                ErrorCategory.VALIDATION,
                Severity.ERROR,
                "Data and algorithm cannot be null"
            ));
        }

        try {
            boolean isCompressed = switch (algorithm) {
                case GZIP -> isGzipCompressed(data);
                case DEFLATE -> isDeflateCompressed(data);
                case BROTLI -> isBrotliCompressed(data);
                case LZ4 -> isLZ4Compressed(data);
                case ZSTD -> isZstdCompressed(data);
                case SNAPPY -> isSnappyCompressed(data);
            };

            return Result.ok(isCompressed);

        } catch (Exception e) {
            logger.debug("Error checking if data is compressed with {}", algorithm, e);
            return Result.ok(false); // If we can't determine, assume not compressed
        }
    }

    @Override
    public CompressionAlgorithm[] getSupportedAlgorithms() {
        if (brotliAvailable) {
            return CompressionAlgorithm.values();
        } else {
            // Return all except Brotli if not available
            return Arrays.stream(CompressionAlgorithm.values())
                    .filter(alg -> alg != CompressionAlgorithm.BROTLI)
                    .toArray(CompressionAlgorithm[]::new);
        }
    }

    @Override
    public int getDefaultCompressionLevel(CompressionAlgorithm algorithm) {
        return switch (algorithm) {
            case GZIP, DEFLATE -> 6;
            case BROTLI -> 6;
            case LZ4 -> 1;
            case SNAPPY -> 1;
            case ZSTD -> 3;
        };
    }

    // GZIP compression methods
    private Result<CompressionResult> compressGzip(InputStream input, OutputStream output, CompressionOptions options) throws IOException {
        long originalSize = 0;
        long compressedSize = 0;

        try (GZIPOutputStream gzipOut = new GZIPOutputStream(output)) {
            byte[] buffer = new byte[options.bufferSize()];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                gzipOut.write(buffer, 0, bytesRead);
                originalSize += bytesRead;
            }
            gzipOut.finish();
            compressedSize = ((ByteArrayOutputStream) output).size();
        }

        return Result.ok(CompressionResult.builder(CompressionAlgorithm.GZIP)
                .originalSize(originalSize)
                .compressedSize(compressedSize)
                .build());
    }

    private Result<CompressionResult> decompressGzip(InputStream input, OutputStream output, CompressionOptions options) throws IOException {
        long compressedSize = 0;
        long decompressedSize = 0;

        try (GZIPInputStream gzipIn = new GZIPInputStream(input)) {
            byte[] buffer = new byte[options.bufferSize()];
            int bytesRead;
            while ((bytesRead = gzipIn.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
                decompressedSize += bytesRead;
            }
            compressedSize = ((ByteArrayInputStream) input).available();
        }

        return Result.ok(CompressionResult.builder(CompressionAlgorithm.GZIP)
                .originalSize(decompressedSize)
                .compressedSize(compressedSize)
                .build());
    }

    // DEFLATE compression methods
    private Result<CompressionResult> compressDeflate(InputStream input, OutputStream output, CompressionOptions options) throws IOException {
        long originalSize = 0;
        long compressedSize = 0;

        try (DeflaterOutputStream deflaterOut = new DeflaterOutputStream(output)) {
            byte[] buffer = new byte[options.bufferSize()];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                deflaterOut.write(buffer, 0, bytesRead);
                originalSize += bytesRead;
            }
            deflaterOut.finish();
            compressedSize = ((ByteArrayOutputStream) output).size();
        }

        return Result.ok(CompressionResult.builder(CompressionAlgorithm.DEFLATE)
                .originalSize(originalSize)
                .compressedSize(compressedSize)
                .build());
    }

    private Result<CompressionResult> decompressDeflate(InputStream input, OutputStream output, CompressionOptions options) throws IOException {
        long compressedSize = 0;
        long decompressedSize = 0;

        try (InflaterInputStream inflaterIn = new InflaterInputStream(input)) {
            byte[] buffer = new byte[options.bufferSize()];
            int bytesRead;
            while ((bytesRead = inflaterIn.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
                decompressedSize += bytesRead;
            }
            compressedSize = ((ByteArrayInputStream) input).available();
        }

        return Result.ok(CompressionResult.builder(CompressionAlgorithm.DEFLATE)
                .originalSize(decompressedSize)
                .compressedSize(compressedSize)
                .build());
    }

    // Brotli compression methods
    private Result<CompressionResult> compressBrotli(InputStream input, OutputStream output, CompressionOptions options) throws IOException {
        long originalSize = 0;
        long compressedSize = 0;

        try (BrotliOutputStream brotliOut = new BrotliOutputStream(output)) {
            byte[] buffer = new byte[options.bufferSize()];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                brotliOut.write(buffer, 0, bytesRead);
                originalSize += bytesRead;
            }
            brotliOut.flush();
            compressedSize = ((ByteArrayOutputStream) output).size();
        }

        return Result.ok(CompressionResult.builder(CompressionAlgorithm.BROTLI)
                .originalSize(originalSize)
                .compressedSize(compressedSize)
                .build());
    }

    private Result<CompressionResult> decompressBrotli(InputStream input, OutputStream output, CompressionOptions options) throws IOException {
        long compressedSize = 0;
        long decompressedSize = 0;

        try (BrotliInputStream brotliIn = new BrotliInputStream(input)) {
            byte[] buffer = new byte[options.bufferSize()];
            int bytesRead;
            while ((bytesRead = brotliIn.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
                decompressedSize += bytesRead;
            }
            compressedSize = ((ByteArrayInputStream) input).available();
        }

        return Result.ok(CompressionResult.builder(CompressionAlgorithm.BROTLI)
                .originalSize(decompressedSize)
                .compressedSize(compressedSize)
                .build());
    }

    // LZ4 compression methods
    private Result<CompressionResult> compressLZ4(InputStream input, OutputStream output, CompressionOptions options) throws IOException {
        long originalSize = 0;
        long compressedSize = 0;

        LZ4Compressor compressor = lz4Factory.fastCompressor();
        try (LZ4BlockOutputStream lz4Out = new LZ4BlockOutputStream(output, options.bufferSize(), compressor)) {
            byte[] buffer = new byte[options.bufferSize()];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                lz4Out.write(buffer, 0, bytesRead);
                originalSize += bytesRead;
            }
            lz4Out.finish();
            compressedSize = ((ByteArrayOutputStream) output).size();
        }

        return Result.ok(CompressionResult.builder(CompressionAlgorithm.LZ4)
                .originalSize(originalSize)
                .compressedSize(compressedSize)
                .build());
    }

    private Result<CompressionResult> decompressLZ4(InputStream input, OutputStream output, CompressionOptions options) throws IOException {
        long compressedSize = 0;
        long decompressedSize = 0;

        LZ4FastDecompressor decompressor = lz4Factory.fastDecompressor();
        try (LZ4BlockInputStream lz4In = new LZ4BlockInputStream(input, decompressor)) {
            byte[] buffer = new byte[options.bufferSize()];
            int bytesRead;
            while ((bytesRead = lz4In.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
                decompressedSize += bytesRead;
            }
            compressedSize = ((ByteArrayInputStream) input).available();
        }

        return Result.ok(CompressionResult.builder(CompressionAlgorithm.LZ4)
                .originalSize(decompressedSize)
                .compressedSize(compressedSize)
                .build());
    }

    // ZSTD compression methods
    private Result<CompressionResult> compressZstd(InputStream input, OutputStream output, CompressionOptions options) throws IOException {
        long originalSize = 0;
        long compressedSize = 0;

        try (ZstdOutputStream zstdOut = new ZstdOutputStream(output, options.level())) {
            byte[] buffer = new byte[options.bufferSize()];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                zstdOut.write(buffer, 0, bytesRead);
                originalSize += bytesRead;
            }
            zstdOut.flush();
            compressedSize = ((ByteArrayOutputStream) output).size();
        }

        return Result.ok(CompressionResult.builder(CompressionAlgorithm.ZSTD)
                .originalSize(originalSize)
                .compressedSize(compressedSize)
                .build());
    }

    private Result<CompressionResult> decompressZstd(InputStream input, OutputStream output, CompressionOptions options) throws IOException {
        long compressedSize = 0;
        long decompressedSize = 0;

        try (ZstdInputStream zstdIn = new ZstdInputStream(input)) {
            byte[] buffer = new byte[options.bufferSize()];
            int bytesRead;
            while ((bytesRead = zstdIn.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
                decompressedSize += bytesRead;
            }
            compressedSize = ((ByteArrayInputStream) input).available();
        }

        return Result.ok(CompressionResult.builder(CompressionAlgorithm.ZSTD)
                .originalSize(decompressedSize)
                .compressedSize(compressedSize)
                .build());
    }

    // Snappy compression methods
    private Result<CompressionResult> compressSnappy(InputStream input, OutputStream output, CompressionOptions options) throws IOException {
        long originalSize = 0;
        long compressedSize = 0;

        try (SnappyOutputStream snappyOut = new SnappyOutputStream(output)) {
            byte[] buffer = new byte[options.bufferSize()];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                snappyOut.write(buffer, 0, bytesRead);
                originalSize += bytesRead;
            }
            snappyOut.flush();
            compressedSize = ((ByteArrayOutputStream) output).size();
        }

        return Result.ok(CompressionResult.builder(CompressionAlgorithm.SNAPPY)
                .originalSize(originalSize)
                .compressedSize(compressedSize)
                .build());
    }

    private Result<CompressionResult> decompressSnappy(InputStream input, OutputStream output, CompressionOptions options) throws IOException {
        long compressedSize = 0;
        long decompressedSize = 0;

        try (SnappyInputStream snappyIn = new SnappyInputStream(input)) {
            byte[] buffer = new byte[options.bufferSize()];
            int bytesRead;
            while ((bytesRead = snappyIn.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
                decompressedSize += bytesRead;
            }
            compressedSize = ((ByteArrayInputStream) input).available();
        }

        return Result.ok(CompressionResult.builder(CompressionAlgorithm.SNAPPY)
                .originalSize(decompressedSize)
                .compressedSize(compressedSize)
                .build());
    }

    // Utility methods for initialization and validation
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

    private Problem createUnsupportedAlgorithmError(CompressionAlgorithm algorithm) {
        return Problem.of(
            ErrorCode.of("COMPRESSION_UNSUPPORTED_ALGORITHM"),
            ErrorCategory.VALIDATION,
            Severity.ERROR,
            "Compression algorithm not supported: " + algorithm
        );
    }

    // Signature detection methods
    private boolean isGzipCompressed(byte[] data) {
        return data.length >= 2 &&
               (data[0] & 0xFF) == 0x1F &&
               (data[1] & 0xFF) == 0x8B;
    }

    private boolean isDeflateCompressed(byte[] data) {
        // DEFLATE doesn't have a magic number, so we try to decompress
        if (data.length < 2) return false;
        try {
            ByteArrayInputStream input = new ByteArrayInputStream(data);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try (InflaterInputStream inflater = new InflaterInputStream(input)) {
                byte[] buffer = new byte[1024];
                inflater.read(buffer, 0, Math.min(buffer.length, 100)); // Try to read a small amount
                return true;
            }
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isBrotliCompressed(byte[] data) {
        // Brotli doesn't have a standard magic number, try to detect by attempting decompression
        if (data.length < 4) return false;
        try {
            ByteArrayInputStream input = new ByteArrayInputStream(data);
            try (BrotliInputStream brotliIn = new BrotliInputStream(input)) {
                byte[] buffer = new byte[1024];
                brotliIn.read(buffer, 0, Math.min(buffer.length, 100));
                return true;
            }
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isLZ4Compressed(byte[] data) {
        // LZ4 Block format magic number: 0x184D2204
        return data.length >= 4 &&
               (data[0] & 0xFF) == 0x04 &&
               (data[1] & 0xFF) == 0x22 &&
               (data[2] & 0xFF) == 0x4D &&
               (data[3] & 0xFF) == 0x18;
    }

    private boolean isZstdCompressed(byte[] data) {
        // ZSTD magic number: 0xFD2FB528
        return data.length >= 4 &&
               (data[0] & 0xFF) == 0x28 &&
               (data[1] & 0xFF) == 0xB5 &&
               (data[2] & 0xFF) == 0x2F &&
               (data[3] & 0xFF) == 0xFD;
    }

    private boolean isSnappyCompressed(byte[] data) {
        // Snappy doesn't have a magic number in raw format
        // We try to detect by attempting decompression
        if (data.length < 4) return false;
        try {
            ByteArrayInputStream input = new ByteArrayInputStream(data);
            try (SnappyInputStream snappyIn = new SnappyInputStream(input)) {
                byte[] buffer = new byte[1024];
                snappyIn.read(buffer, 0, Math.min(buffer.length, 100));
                return true;
            }
        } catch (Exception e) {
            return false;
        }
    }
}
