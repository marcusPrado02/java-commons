package com.marcusprado02.commons.app.backup.filesystem;

import com.marcusprado02.commons.app.backup.*;
import com.marcusprado02.commons.kernel.errors.ErrorCategory;
import com.marcusprado02.commons.kernel.errors.ErrorCode;
import com.marcusprado02.commons.kernel.errors.Problem;
import com.marcusprado02.commons.kernel.errors.Severity;
import com.marcusprado02.commons.kernel.result.Result;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Filesystem backup service implementation.
 *
 * <p>Backs up directories and files to ZIP archives with optional compression and integrity checks.
 */
public final class FilesystemBackupService implements BackupService, RestoreService {

  private static final Logger logger = LoggerFactory.getLogger(FilesystemBackupService.class);

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
      var backupFile = Paths.get(configuration.destinationPath(), backupId + ".zip");

      // Record all file modification times
      var modTimes = collectFileModificationTimes();
      fileModificationTimes.put(backupId, modTimes);

      // Create backup
      long size = createZipBackup(sourcePath, backupFile, configuration.compressionEnabled());
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
              .metadata(Map.of("fileCount", String.valueOf(modTimes.size())))
              .build();

      backups.put(backupId, metadata);
      logger.info("Full backup '{}' completed: {} bytes", name, size);
      return Result.ok(metadata);

    } catch (IOException e) {
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

    try {
      var backupId = UUID.randomUUID().toString();
      var backupFile = Paths.get(configuration.destinationPath(), backupId + ".zip");

      // Get parent file modification times
      var parentModTimes = fileModificationTimes.get(parentBackupId);
      if (parentModTimes == null) {
        return Result.fail(
            Problem.of(
                ErrorCode.of("BACKUP.NO_PARENT_METADATA"),
                ErrorCategory.BUSINESS,
                Severity.ERROR,
                "Parent backup metadata not found"));
      }

      // Collect only changed files
      var currentModTimes = collectFileModificationTimes();
      var changedFiles =
          currentModTimes.entrySet().stream()
              .filter(
                  e ->
                      !parentModTimes.containsKey(e.getKey())
                          || !parentModTimes.get(e.getKey()).equals(e.getValue()))
              .map(e -> Paths.get(e.getKey()))
              .collect(Collectors.toList());

      logger.info("Found {} changed files for incremental backup", changedFiles.size());

      // Create incremental backup with only changed files
      long size =
          createSelectiveZipBackup(changedFiles, backupFile, configuration.compressionEnabled());
      var checksum = calculateChecksum(backupFile);

      fileModificationTimes.put(backupId, currentModTimes);

      var metadata =
          BackupMetadata.builder()
              .id(backupId)
              .name(name)
              .type(BackupMetadata.BackupType.INCREMENTAL)
              .source(sourcePath.toString())
              .location(backupFile.toString())
              .size(size)
              .status(BackupMetadata.BackupStatus.COMPLETED)
              .createdAt(Instant.now())
              .completedAt(Instant.now())
              .parentBackupId(parentBackupId)
              .checksum(checksum)
              .metadata(
                  Map.of(
                      "fileCount",
                      String.valueOf(changedFiles.size()),
                      "parentBackupId",
                      parentBackupId))
              .build();

      backups.put(backupId, metadata);
      logger.info("Incremental backup '{}' completed: {} bytes", name, size);
      return Result.ok(metadata);

    } catch (IOException e) {
      logger.error("Failed to create incremental backup: {}", e.getMessage(), e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("BACKUP.INCREMENTAL_BACKUP_FAILED"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to create incremental backup: " + e.getMessage()));
    }
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

    // Verify integrity if requested
    if (configuration.verifyIntegrity()) {
      var verifyResult = verifyBackup(backupId);
      if (verifyResult.isFail() || !verifyResult.getOrNull()) {
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

      // Extract ZIP
      long filesRestored = extractZip(backupFile, targetPath, configuration.overwriteExisting());
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

    } catch (IOException e) {
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
    return Result.fail(
        Problem.of(
            ErrorCode.of("RESTORE.NOT_IMPLEMENTED"),
            ErrorCategory.BUSINESS,
            Severity.WARNING,
            "Point-in-time restore not yet implemented"));
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

  // Helper methods

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

  private long createZipBackup(Path source, Path zipFile, boolean compress) throws IOException {
    Files.createDirectories(zipFile.getParent());

    try (var zos = new ZipOutputStream(Files.newOutputStream(zipFile))) {
      zos.setLevel(compress ? 9 : 0);

      Files.walkFileTree(
          source,
          new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                throws IOException {
              var zipEntry = new ZipEntry(source.relativize(file).toString());
              zos.putNextEntry(zipEntry);
              Files.copy(file, zos);
              zos.closeEntry();
              return FileVisitResult.CONTINUE;
            }
          });
    }

    return Files.size(zipFile);
  }

  private long createSelectiveZipBackup(List<Path> files, Path zipFile, boolean compress)
      throws IOException {
    Files.createDirectories(zipFile.getParent());

    try (var zos = new ZipOutputStream(Files.newOutputStream(zipFile))) {
      zos.setLevel(compress ? 9 : 0);

      for (var file : files) {
        if (Files.isRegularFile(file)) {
          var zipEntry = new ZipEntry(sourcePath.relativize(file).toString());
          zos.putNextEntry(zipEntry);
          Files.copy(file, zos);
          zos.closeEntry();
        }
      }
    }

    return Files.size(zipFile);
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
