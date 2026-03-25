package com.marcusprado02.commons.app.backup.filesystem;

import com.marcusprado02.commons.app.backup.*;
import com.marcusprado02.commons.kernel.errors.ErrorCategory;
import com.marcusprado02.commons.kernel.errors.ErrorCode;
import com.marcusprado02.commons.kernel.errors.Problem;
import com.marcusprado02.commons.kernel.errors.Severity;
import com.marcusprado02.commons.kernel.result.Result;
import java.io.*;
import java.nio.file.*;
import java.util.concurrent.ForkJoinPool;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Filesystem backup service implementation.
 *
 * <p>Backs up directories and files to ZIP archives with optional compression, AES-256-GCM
 * encryption, and integrity checks. Supports full, incremental, and differential backup strategies.
 */
public final class FilesystemBackupService implements BackupService, RestoreService {

  private static final Logger logger = LoggerFactory.getLogger(FilesystemBackupService.class);

  private static final String ENCRYPTION_ALGORITHM = "AES/GCM/NoPadding";
  private static final String KEY_ALGORITHM = "AES";
  private static final String KEY_DERIVATION = "PBKDF2WithHmacSHA256";
  private static final int GCM_IV_LENGTH = 12;
  private static final int GCM_TAG_LENGTH = 128;
  private static final int KEY_LENGTH = 256;
  private static final int PBKDF2_ITERATIONS = 65536;
  private static final byte[] SALT = "commons-backup-salt-v1".getBytes();

  private final Path sourcePath;
  private final Map<String, BackupMetadata> backups = new ConcurrentHashMap<>();
  private final Map<String, Map<String, Long>> fileModificationTimes = new ConcurrentHashMap<>();

  private FilesystemBackupService(Path sourcePath) {
    this.sourcePath = sourcePath;
  }

  /**
   * Creates a filesystem backup service for a source directory.
   *
   * @param sourcePath source directory to backup
   * @return filesystem backup service
   */
  public static FilesystemBackupService forSource(Path sourcePath) {
    return new FilesystemBackupService(sourcePath);
  }

  @Override
  public Result<BackupMetadata> createFullBackup(String name, BackupConfiguration configuration) {
    logger.info("Creating full backup '{}' for '{}'", name, sourcePath);

    try {
      var backupId = UUID.randomUUID().toString();
      var backupFile = resolveBackupFile(configuration.destinationPath(), backupId, configuration);

      var modTimes = collectFileModificationTimes();
      fileModificationTimes.put(backupId, modTimes);

      long size = createZipBackup(sourcePath, backupFile, configuration);
      var checksum = calculateChecksum(backupFile);

      var metadata =
          BackupMetadata.builder()
              .id(backupId)
              .name(name)
              .type(BackupMetadata.BackupType.FULL)
              .source(sourcePath.toString())
              .location(backupFile.toString())
              .size(size)
              .status(BackupMetadata.BackupStatus.COMPLETED)
              .createdAt(Instant.now())
              .completedAt(Instant.now())
              .checksum(checksum)
              .metadata(
                  Map.of(
                      "fileCount", String.valueOf(modTimes.size()),
                      "encrypted", String.valueOf(configuration.encryptionEnabled())))
              .build();

      backups.put(backupId, metadata);
      logger.info("Full backup '{}' completed: {} bytes", name, size);
      return Result.ok(metadata);

    } catch (Exception e) {
      logger.error("Failed to create full backup: {}", e.getMessage(), e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("BACKUP.FULL_BACKUP_FAILED"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to create full backup: " + e.getMessage()));
    }
  }

  @Override
  public Result<BackupMetadata> createIncrementalBackup(
      String name, String parentBackupId, BackupConfiguration configuration) {
    logger.info("Creating incremental backup '{}' from parent '{}'", name, parentBackupId);

    var parentOpt = Optional.ofNullable(backups.get(parentBackupId));
    if (parentOpt.isEmpty()) {
      return Result.fail(
          Problem.of(
              ErrorCode.of("BACKUP.PARENT_NOT_FOUND"),
              ErrorCategory.NOT_FOUND,
              Severity.ERROR,
              "Parent backup not found: " + parentBackupId));
    }

    var parentModTimes = fileModificationTimes.get(parentBackupId);
    if (parentModTimes == null) {
      return Result.fail(
          Problem.of(
              ErrorCode.of("BACKUP.NO_PARENT_METADATA"),
              ErrorCategory.BUSINESS,
              Severity.ERROR,
              "Parent backup metadata not found"));
    }

    return createChangedFilesBackup(
        name, parentBackupId, parentModTimes, BackupMetadata.BackupType.INCREMENTAL, configuration);
  }

  @Override
  public Result<BackupMetadata> createDifferentialBackup(
      String name, String fullBackupId, BackupConfiguration configuration) {
    logger.info("Creating differential backup '{}' from full backup '{}'", name, fullBackupId);

    var fullBackupOpt = Optional.ofNullable(backups.get(fullBackupId));
    if (fullBackupOpt.isEmpty()) {
      return Result.fail(
          Problem.of(
              ErrorCode.of("BACKUP.FULL_BACKUP_NOT_FOUND"),
              ErrorCategory.NOT_FOUND,
              Severity.ERROR,
              "Full backup not found: " + fullBackupId));
    }

    var fullBackup = fullBackupOpt.get();
    if (fullBackup.type() != BackupMetadata.BackupType.FULL) {
      return Result.fail(
          Problem.of(
              ErrorCode.of("BACKUP.NOT_A_FULL_BACKUP"),
              ErrorCategory.BUSINESS,
              Severity.ERROR,
              "Backup '" + fullBackupId + "' is not a full backup"));
    }

    var fullModTimes = fileModificationTimes.get(fullBackupId);
    if (fullModTimes == null) {
      return Result.fail(
          Problem.of(
              ErrorCode.of("BACKUP.NO_FULL_METADATA"),
              ErrorCategory.BUSINESS,
              Severity.ERROR,
              "Full backup file-state metadata not found"));
    }

    return createChangedFilesBackup(
        name, fullBackupId, fullModTimes, BackupMetadata.BackupType.DIFFERENTIAL, configuration);
  }

  @Override
  public Result<List<BackupMetadata>> listBackups() {
    return Result.ok(new ArrayList<>(backups.values()));
  }

  @Override
  public Result<BackupMetadata> getBackup(String backupId) {
    var metadata = backups.get(backupId);
    if (metadata == null) {
      return Result.fail(
          Problem.of(
              ErrorCode.of("BACKUP.NOT_FOUND"),
              ErrorCategory.NOT_FOUND,
              Severity.ERROR,
              "Backup not found: " + backupId));
    }
    return Result.ok(metadata);
  }

  @Override
  public Result<Void> deleteBackup(String backupId) {
    var metadata = backups.remove(backupId);
    if (metadata == null) {
      return Result.fail(
          Problem.of(
              ErrorCode.of("BACKUP.NOT_FOUND"),
              ErrorCategory.NOT_FOUND,
              Severity.ERROR,
              "Backup not found: " + backupId));
    }

    try {
      Files.deleteIfExists(Paths.get(metadata.location()));
      fileModificationTimes.remove(backupId);
      logger.info("Backup '{}' deleted", backupId);
      return Result.ok(null);
    } catch (IOException e) {
      logger.error("Failed to delete backup file: {}", e.getMessage(), e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("BACKUP.DELETE_FAILED"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to delete backup: " + e.getMessage()));
    }
  }

  @Override
  public Result<Boolean> verifyBackup(String backupId) {
    var metadataResult = getBackup(backupId);
    if (metadataResult.isFail()) {
      return Result.fail(metadataResult.problemOrNull());
    }

    var metadata = metadataResult.getOrNull();
    try {
      var backupFile = Paths.get(metadata.location());
      var actualChecksum = calculateChecksum(backupFile);
      var expectedChecksum = metadata.checksum().orElse("");

      boolean valid = actualChecksum.equals(expectedChecksum);
      if (valid) {
        logger.info("Backup '{}' integrity verified", backupId);
      } else {
        logger.warn("Backup '{}' integrity check failed", backupId);
      }
      return Result.ok(valid);

    } catch (IOException e) {
      logger.error("Failed to verify backup: {}", e.getMessage(), e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("BACKUP.VERIFICATION_FAILED"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to verify backup: " + e.getMessage()));
    }
  }

  @Override
  public Result<RestoreResult> restore(String backupId, RestoreConfiguration configuration) {
    logger.info("Restoring backup '{}'", backupId);

    var metadataResult = getBackup(backupId);
    if (metadataResult.isFail()) {
      return Result.fail(metadataResult.problemOrNull());
    }

    var metadata = metadataResult.getOrNull();

    if (configuration.verifyIntegrity()) {
      var verifyResult = verifyBackup(backupId);
      if (verifyResult.isFail() || Boolean.FALSE.equals(verifyResult.getOrNull())) {
        return Result.fail(
            Problem.of(
                ErrorCode.of("RESTORE.INTEGRITY_CHECK_FAILED"),
                ErrorCategory.BUSINESS,
                Severity.ERROR,
                "Backup integrity check failed"));
      }
    }

    try {
      var start = Instant.now();
      var targetPath = Paths.get(configuration.targetPath());
      var backupFile = Paths.get(metadata.location());
      boolean encrypted =
          Boolean.parseBoolean(metadata.metadata().getOrDefault("encrypted", "false"));

      long filesRestored;
      if (encrypted) {
        if (configuration.decryptionKey() == null || configuration.decryptionKey().isBlank()) {
          return Result.fail(
              Problem.of(
                  ErrorCode.of("RESTORE.ENCRYPTION_KEY_MISSING"),
                  ErrorCategory.BUSINESS,
                  Severity.ERROR,
                  "Backup is encrypted but no decryption key provided"));
        }
        filesRestored =
            extractEncryptedZip(backupFile, targetPath, configuration.overwriteExisting(),
                configuration.decryptionKey());
      } else {
        filesRestored = extractZip(backupFile, targetPath, configuration.overwriteExisting());
      }

      var duration = java.time.Duration.between(start, Instant.now());
      var result =
          RestoreResult.builder()
              .backupId(backupId)
              .targetPath(targetPath.toString())
              .filesRestored(filesRestored)
              .bytesRestored(metadata.size())
              .duration(duration)
              .completedAt(Instant.now())
              .metadata(Map.of("backupType", metadata.type().toString()))
              .build();

      logger.info("Restore completed: {} files in {}", filesRestored, duration);
      return Result.ok(result);

    } catch (Exception e) {
      logger.error("Failed to restore backup: {}", e.getMessage(), e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("RESTORE.FAILED"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to restore backup: " + e.getMessage()));
    }
  }

  @Override
  public Result<RestoreResult> restorePointInTime(
      String pointInTime, RestoreConfiguration configuration) {
    Instant target;
    try {
      target = Instant.parse(pointInTime);
    } catch (Exception e) {
      return Result.fail(
          Problem.of(
              ErrorCode.of("RESTORE.INVALID_POINT_IN_TIME"),
              ErrorCategory.BUSINESS,
              Severity.ERROR,
              "Invalid point-in-time format. Use ISO-8601 (e.g. 2026-01-01T12:00:00Z)"));
    }

    // Find the most recent completed backup whose createdAt <= target
    var candidate =
        backups.values().stream()
            .filter(m -> m.status() == BackupMetadata.BackupStatus.COMPLETED)
            .filter(m -> !m.createdAt().isAfter(target))
            .max(Comparator.comparing(BackupMetadata::createdAt));

    if (candidate.isEmpty()) {
      return Result.fail(
          Problem.of(
              ErrorCode.of("RESTORE.NO_BACKUP_FOR_POINT_IN_TIME"),
              ErrorCategory.NOT_FOUND,
              Severity.ERROR,
              "No completed backup found on or before " + pointInTime));
    }

    logger.info(
        "Restoring point-in-time '{}' using backup '{}'", pointInTime, candidate.get().id());
    return restore(candidate.get().id(), configuration);
  }

  @Override
  public Result<Void> validateRestore(String backupId, RestoreConfiguration configuration) {
    var metadataResult = getBackup(backupId);
    if (metadataResult.isFail()) {
      return Result.fail(metadataResult.problemOrNull());
    }

    var metadata = metadataResult.getOrNull();
    var backupFile = Paths.get(metadata.location());

    if (!Files.exists(backupFile)) {
      return Result.fail(
          Problem.of(
              ErrorCode.of("RESTORE.BACKUP_FILE_NOT_FOUND"),
              ErrorCategory.NOT_FOUND,
              Severity.ERROR,
              "Backup file not found: " + backupFile));
    }

    return Result.ok(null);
  }

  // -------------------------------------------------------------------------
  // Private helpers
  // -------------------------------------------------------------------------

  private Result<BackupMetadata> createChangedFilesBackup(
      String name,
      String referenceBackupId,
      Map<String, Long> referenceModTimes,
      BackupMetadata.BackupType type,
      BackupConfiguration configuration) {

    try {
      var backupId = UUID.randomUUID().toString();
      var backupFile = resolveBackupFile(configuration.destinationPath(), backupId, configuration);

      var currentModTimes = collectFileModificationTimes();
      var changedFiles =
          currentModTimes.entrySet().stream()
              .filter(
                  e ->
                      !referenceModTimes.containsKey(e.getKey())
                          || !referenceModTimes.get(e.getKey()).equals(e.getValue()))
              .map(e -> Paths.get(e.getKey()))
              .collect(Collectors.toList());

      logger.info("Found {} changed files for {} backup", changedFiles.size(), type);

      long size = createSelectiveZipBackup(changedFiles, backupFile, configuration);
      var checksum = calculateChecksum(backupFile);

      fileModificationTimes.put(backupId, currentModTimes);

      var metadata =
          BackupMetadata.builder()
              .id(backupId)
              .name(name)
              .type(type)
              .source(sourcePath.toString())
              .location(backupFile.toString())
              .size(size)
              .status(BackupMetadata.BackupStatus.COMPLETED)
              .createdAt(Instant.now())
              .completedAt(Instant.now())
              .parentBackupId(referenceBackupId)
              .checksum(checksum)
              .metadata(
                  Map.of(
                      "fileCount", String.valueOf(changedFiles.size()),
                      "referenceBackupId", referenceBackupId,
                      "encrypted", String.valueOf(configuration.encryptionEnabled())))
              .build();

      backups.put(backupId, metadata);
      logger.info("{} backup '{}' completed: {} bytes", type, name, size);
      return Result.ok(metadata);

    } catch (Exception e) {
      logger.error("Failed to create {} backup: {}", type, e.getMessage(), e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("BACKUP." + type + "_BACKUP_FAILED"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to create " + type.toString().toLowerCase() + " backup: " + e.getMessage()));
    }
  }

  private Path resolveBackupFile(
      String destinationPath, String backupId, BackupConfiguration configuration) {
    String extension = configuration.encryptionEnabled() ? ".zip.enc" : ".zip";
    return Paths.get(destinationPath, backupId + extension);
  }

  private Map<String, Long> collectFileModificationTimes() throws IOException {
    var modTimes = new HashMap<String, Long>();
    Files.walkFileTree(
        sourcePath,
        new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            modTimes.put(file.toString(), attrs.lastModifiedTime().toMillis());
            return FileVisitResult.CONTINUE;
          }
        });
    return modTimes;
  }

  private long createZipBackup(Path source, Path outFile, BackupConfiguration config)
      throws Exception {
    Files.createDirectories(outFile.getParent());

    // Always use parallel-read path for consistency
    var zipBytes = zipToBytes(source, config.compressionEnabled());

    if (config.encryptionEnabled()) {
      encryptToFile(zipBytes, outFile, config.encryptionKey());
    } else {
      Files.write(outFile, zipBytes);
    }

    return Files.size(outFile);
  }

  private long createSelectiveZipBackup(
      List<Path> files, Path outFile, BackupConfiguration config) throws Exception {
    Files.createDirectories(outFile.getParent());

    var zipBytes = selectiveZipToBytes(files, config.compressionEnabled());

    if (config.encryptionEnabled()) {
      encryptToFile(zipBytes, outFile, config.encryptionKey());
    } else {
      Files.write(outFile, zipBytes);
    }

    return Files.size(outFile);
  }

  private byte[] zipToBytes(Path source, boolean compress) throws IOException {
    // Collect all regular files first
    var allFiles = new ArrayList<Path>();
    Files.walkFileTree(
        source,
        new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            allFiles.add(file);
            return FileVisitResult.CONTINUE;
          }
        });
    return writeZipFromPaths(allFiles, source, compress);
  }

  private byte[] selectiveZipToBytes(List<Path> files, boolean compress) throws IOException {
    var regularFiles = files.stream().filter(Files::isRegularFile).toList();
    return writeZipFromPaths(regularFiles, sourcePath, compress);
  }

  /**
   * Reads file bytes in parallel (when there are many files), then writes the ZIP sequentially.
   * ZipOutputStream is not thread-safe so the write phase is always sequential.
   */
  private byte[] writeZipFromPaths(List<Path> files, Path root, boolean compress)
      throws IOException {
    // Read files in parallel using ForkJoinPool — improves throughput for many small files
    List<byte[]> fileContents;
    try {
      fileContents =
          ForkJoinPool.commonPool()
              .submit(
                  () ->
                      files.parallelStream()
                          .map(
                              file -> {
                                try {
                                  return Files.readAllBytes(file);
                                } catch (IOException e) {
                                  throw new UncheckedIOException(e);
                                }
                              })
                          .toList())
              .get();
    } catch (Exception e) {
      Throwable cause = e.getCause() != null ? e.getCause() : e;
      if (cause instanceof UncheckedIOException uio) throw uio.getCause();
      throw new IOException("Parallel file read failed: " + cause.getMessage(), cause);
    }

    // Write ZIP sequentially (ZipOutputStream is not thread-safe)
    var baos = new ByteArrayOutputStream();
    try (var zos = new ZipOutputStream(baos)) {
      zos.setLevel(compress ? 9 : 0);
      for (int i = 0; i < files.size(); i++) {
        var entryName = root.relativize(files.get(i)).toString();
        zos.putNextEntry(new ZipEntry(entryName));
        zos.write(fileContents.get(i));
        zos.closeEntry();
      }
    }
    return baos.toByteArray();
  }

  /**
   * Encrypts {@code plaintext} with AES-256-GCM using a key derived from {@code password} via
   * PBKDF2. Output format: [12-byte IV][ciphertext+tag].
   */
  private void encryptToFile(byte[] plaintext, Path outFile, String password) throws Exception {
    var key = deriveKey(password);
    var iv = new byte[GCM_IV_LENGTH];
    new SecureRandom().nextBytes(iv);

    var cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
    cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
    var ciphertext = cipher.doFinal(plaintext);

    try (var out = Files.newOutputStream(outFile)) {
      out.write(iv);
      out.write(ciphertext);
    }
  }

  /**
   * Decrypts a file produced by {@link #encryptToFile}. Returns the decrypted ZIP bytes.
   */
  private byte[] decryptFromFile(Path encFile, String password) throws Exception {
    var raw = Files.readAllBytes(encFile);
    var iv = Arrays.copyOfRange(raw, 0, GCM_IV_LENGTH);
    var ciphertext = Arrays.copyOfRange(raw, GCM_IV_LENGTH, raw.length);

    var key = deriveKey(password);
    var cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
    cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
    return cipher.doFinal(ciphertext);
  }

  private SecretKey deriveKey(String password) throws Exception {
    var factory = SecretKeyFactory.getInstance(KEY_DERIVATION);
    var spec = new PBEKeySpec(password.toCharArray(), SALT, PBKDF2_ITERATIONS, KEY_LENGTH);
    var tmp = factory.generateSecret(spec);
    return new SecretKeySpec(tmp.getEncoded(), KEY_ALGORITHM);
  }

  private long extractZip(Path zipFile, Path targetDir, boolean overwrite) throws IOException {
    Files.createDirectories(targetDir);
    long count = 0;

    try (var fs = FileSystems.newFileSystem(zipFile, (ClassLoader) null)) {
      for (var rootDir : fs.getRootDirectories()) {
        try (var stream = Files.walk(rootDir)) {
          for (var entry : stream.collect(Collectors.toList())) {
            if (Files.isRegularFile(entry)) {
              var targetFile = targetDir.resolve(entry.toString().substring(1));
              Files.createDirectories(targetFile.getParent());
              if (overwrite || !Files.exists(targetFile)) {
                Files.copy(entry, targetFile, StandardCopyOption.REPLACE_EXISTING);
                count++;
              }
            }
          }
        }
      }
    }

    return count;
  }

  private long extractEncryptedZip(
      Path encFile, Path targetDir, boolean overwrite, String password) throws Exception {
    var zipBytes = decryptFromFile(encFile, password);
    var tmpZip = Files.createTempFile("commons-restore-", ".zip");
    try {
      Files.write(tmpZip, zipBytes);
      return extractZip(tmpZip, targetDir, overwrite);
    } finally {
      Files.deleteIfExists(tmpZip);
    }
  }

  private String calculateChecksum(Path file) throws IOException {
    try {
      var digest = MessageDigest.getInstance("SHA-256");
      try (var is = Files.newInputStream(file)) {
        var buffer = new byte[8192];
        int read;
        while ((read = is.read(buffer)) > 0) {
          digest.update(buffer, 0, read);
        }
      }
      return bytesToHex(digest.digest());
    } catch (Exception e) {
      throw new IOException("Failed to calculate checksum", e);
    }
  }

  private String bytesToHex(byte[] bytes) {
    var sb = new StringBuilder();
    for (byte b : bytes) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }
}
