package com.marcusprado02.commons.app.backup.filesystem;

import static org.assertj.core.api.Assertions.*;

import com.marcusprado02.commons.app.backup.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

class FilesystemBackupServiceTest {

  @TempDir Path tempDir;

  private Path sourceDir;
  private Path backupDir;
  private FilesystemBackupService service;

  @BeforeEach
  void setUp() throws IOException {
    sourceDir = tempDir.resolve("source");
    backupDir = tempDir.resolve("backups");
    Files.createDirectories(sourceDir);
    Files.createDirectories(backupDir);

    // Create some test files
    Files.writeString(sourceDir.resolve("file1.txt"), "Content 1");
    Files.writeString(sourceDir.resolve("file2.txt"), "Content 2");
    var subdir = sourceDir.resolve("subdir");
    Files.createDirectories(subdir);
    Files.writeString(subdir.resolve("file3.txt"), "Content 3");

    service = FilesystemBackupService.forSource(sourceDir);
  }

  @Test
  void shouldCreateFilesystemBackupService() {
    assertThat(service).isNotNull();
  }

  @Test
  void shouldCreateFullBackup() {
    // Given
    var config =
        BackupConfiguration.builder()
            .destinationPath(backupDir.toString())
            .compressionEnabled(true)
            .build();

    // When
    var result = service.createFullBackup("Test Backup", config);

    // Then
    assertThat(result.isOk()).isTrue();
    var metadata = result.getOrNull();
    assertThat(metadata.id()).isNotEmpty();
    assertThat(metadata.name()).isEqualTo("Test Backup");
    assertThat(metadata.type()).isEqualTo(BackupMetadata.BackupType.FULL);
    assertThat(metadata.source()).isEqualTo(sourceDir.toString());
    assertThat(metadata.status()).isEqualTo(BackupMetadata.BackupStatus.COMPLETED);
    assertThat(metadata.size()).isGreaterThan(0);
    assertThat(metadata.checksum()).isPresent();
    assertThat(metadata.isComplete()).isTrue();
  }

  @Test
  void shouldListBackups() {
    // Given
    var config =
        BackupConfiguration.builder()
            .destinationPath(backupDir.toString())
            .compressionEnabled(true)
            .build();
    service.createFullBackup("Backup 1", config);
    service.createFullBackup("Backup 2", config);

    // When
    var result = service.listBackups();

    // Then
    assertThat(result.isOk()).isTrue();
    var backups = result.getOrNull();
    assertThat(backups).hasSize(2);
    assertThat(backups)
        .extracting(BackupMetadata::name)
        .containsExactlyInAnyOrder("Backup 1", "Backup 2");
  }

  @Test
  void shouldGetBackupById() {
    // Given
    var config =
        BackupConfiguration.builder()
            .destinationPath(backupDir.toString())
            .compressionEnabled(true)
            .build();
    var createResult = service.createFullBackup("Test Backup", config);
    var backupId = createResult.getOrNull().id();

    // When
    var result = service.getBackup(backupId);

    // Then
    assertThat(result.isOk()).isTrue();
    var metadata = result.getOrNull();
    assertThat(metadata.id()).isEqualTo(backupId);
    assertThat(metadata.name()).isEqualTo("Test Backup");
  }

  @Test
  void shouldFailToGetNonExistentBackup() {
    // When
    var result = service.getBackup("non-existent");

    // Then
    assertThat(result.isFail()).isTrue();
    var problem = result.problemOrNull();
    assertThat(problem.code().value()).isEqualTo("BACKUP.NOT_FOUND");
  }

  @Test
  void shouldVerifyBackupIntegrity() {
    // Given
    var config =
        BackupConfiguration.builder()
            .destinationPath(backupDir.toString())
            .compressionEnabled(true)
            .build();
    var createResult = service.createFullBackup("Test Backup", config);
    var backupId = createResult.getOrNull().id();

    // When
    var result = service.verifyBackup(backupId);

    // Then
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isTrue();
  }

  @Test
  void shouldDeleteBackup() {
    // Given
    var config =
        BackupConfiguration.builder()
            .destinationPath(backupDir.toString())
            .compressionEnabled(true)
            .build();
    var createResult = service.createFullBackup("Test Backup", config);
    var backupId = createResult.getOrNull().id();

    // When
    var deleteResult = service.deleteBackup(backupId);

    // Then
    assertThat(deleteResult.isOk()).isTrue();

    // Verify backup is gone
    var getResult = service.getBackup(backupId);
    assertThat(getResult.isFail()).isTrue();
  }

  @Test
  void shouldCreateIncrementalBackup() throws IOException, InterruptedException {
    // Given
    var config =
        BackupConfiguration.builder()
            .destinationPath(backupDir.toString())
            .compressionEnabled(true)
            .build();

    // Create full backup
    var fullResult = service.createFullBackup("Full Backup", config);
    var parentId = fullResult.getOrNull().id();

    // Modify some files
    Thread.sleep(10); // Ensure different modification times
    Files.writeString(sourceDir.resolve("file1.txt"), "Modified Content 1");
    Files.writeString(sourceDir.resolve("file4.txt"), "New File");

    // When
    var incrementalResult = service.createIncrementalBackup("Incremental Backup", parentId, config);

    // Then
    assertThat(incrementalResult.isOk()).isTrue();
    var metadata = incrementalResult.getOrNull();
    assertThat(metadata.type()).isEqualTo(BackupMetadata.BackupType.INCREMENTAL);
    assertThat(metadata.parentBackupId()).hasValue(parentId);
    assertThat(metadata.size()).isLessThan(fullResult.getOrNull().size());
  }

  @Test
  void shouldFailIncrementalBackupWithoutParent() {
    // Given
    var config =
        BackupConfiguration.builder()
            .destinationPath(backupDir.toString())
            .compressionEnabled(true)
            .build();

    // When
    var result = service.createIncrementalBackup("Incremental Backup", "non-existent", config);

    // Then
    assertThat(result.isFail()).isTrue();
    var problem = result.problemOrNull();
    assertThat(problem.code().value()).isEqualTo("BACKUP.PARENT_NOT_FOUND");
  }

  @Test
  void shouldRestoreBackup() throws IOException {
    // Given
    var config =
        BackupConfiguration.builder()
            .destinationPath(backupDir.toString())
            .compressionEnabled(true)
            .build();
    var createResult = service.createFullBackup("Test Backup", config);
    var backupId = createResult.getOrNull().id();

    var restoreDir = tempDir.resolve("restore");
    var restoreConfig =
        RestoreConfiguration.builder()
            .targetPath(restoreDir.toString())
            .overwriteExisting(true)
            .verifyIntegrity(true)
            .build();

    // When
    var restoreResult = service.restore(backupId, restoreConfig);

    // Then
    assertThat(restoreResult.isOk()).isTrue();
    var result = restoreResult.getOrNull();
    assertThat(result.backupId()).isEqualTo(backupId);
    assertThat(result.filesRestored()).isEqualTo(3);
    assertThat(result.isSuccess()).isTrue();

    // Verify files were restored
    assertThat(Files.exists(restoreDir.resolve("file1.txt"))).isTrue();
    assertThat(Files.exists(restoreDir.resolve("file2.txt"))).isTrue();
    assertThat(Files.exists(restoreDir.resolve("subdir/file3.txt"))).isTrue();
    assertThat(Files.readString(restoreDir.resolve("file1.txt"))).isEqualTo("Content 1");
  }

  @Test
  void shouldValidateRestore() {
    // Given
    var config =
        BackupConfiguration.builder()
            .destinationPath(backupDir.toString())
            .compressionEnabled(true)
            .build();
    var createResult = service.createFullBackup("Test Backup", config);
    var backupId = createResult.getOrNull().id();

    var restoreConfig =
        RestoreConfiguration.builder().targetPath(tempDir.resolve("restore").toString()).build();

    // When
    var result = service.validateRestore(backupId, restoreConfig);

    // Then
    assertThat(result.isOk()).isTrue();
  }

  @Test
  void shouldCreateBackupWithoutCompression() {
    // Given
    var config =
        BackupConfiguration.builder()
            .destinationPath(backupDir.toString())
            .compressionEnabled(false)
            .build();

    // When
    var result = service.createFullBackup("Uncompressed Backup", config);

    // Then
    assertThat(result.isOk()).isTrue();
    var metadata = result.getOrNull();
    assertThat(metadata.size()).isGreaterThan(0);
  }

  @Test
  void shouldCreateEncryptedFullBackupAndRestoreIt() throws IOException {
    // Given
    var config =
        BackupConfiguration.builder()
            .destinationPath(backupDir.toString())
            .compressionEnabled(true)
            .encryptionEnabled(true)
            .encryptionKey("s3cr3t-p@ssword!")
            .build();

    var createResult = service.createFullBackup("Encrypted Backup", config);
    assertThat(createResult.isOk()).isTrue();

    var metadata = createResult.getOrNull();
    assertThat(metadata.metadata()).containsEntry("encrypted", "true");
    assertThat(metadata.location()).endsWith(".zip.enc");

    var restoreDir = tempDir.resolve("restore-enc");
    var restoreConfig =
        RestoreConfiguration.builder()
            .targetPath(restoreDir.toString())
            .overwriteExisting(true)
            .verifyIntegrity(false)
            .decryptionKey("s3cr3t-p@ssword!")
            .build();

    // When
    var restoreResult = service.restore(metadata.id(), restoreConfig);

    // Then
    assertThat(restoreResult.isOk()).isTrue();
    assertThat(restoreResult.getOrNull().filesRestored()).isEqualTo(3);
    assertThat(Files.readString(restoreDir.resolve("file1.txt"))).isEqualTo("Content 1");
  }

  @Test
  void shouldFailRestoreEncryptedBackupWithoutKey() {
    // Given
    var config =
        BackupConfiguration.builder()
            .destinationPath(backupDir.toString())
            .encryptionEnabled(true)
            .encryptionKey("secret")
            .build();
    var createResult = service.createFullBackup("Encrypted Backup", config);
    var backupId = createResult.getOrNull().id();

    var restoreConfig =
        RestoreConfiguration.builder()
            .targetPath(tempDir.resolve("restore-no-key").toString())
            .verifyIntegrity(false)
            .build();

    // When
    var result = service.restore(backupId, restoreConfig);

    // Then
    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("RESTORE.ENCRYPTION_KEY_MISSING");
  }

  @Test
  void shouldCreateDifferentialBackup() throws IOException, InterruptedException {
    // Given
    var config =
        BackupConfiguration.builder()
            .destinationPath(backupDir.toString())
            .compressionEnabled(true)
            .build();

    var fullResult = service.createFullBackup("Full Backup", config);
    assertThat(fullResult.isOk()).isTrue();
    var fullId = fullResult.getOrNull().id();

    Thread.sleep(10);
    Files.writeString(sourceDir.resolve("file1.txt"), "Modified Content");
    Files.writeString(sourceDir.resolve("file5.txt"), "New File");

    // When
    var diffResult = service.createDifferentialBackup("Diff Backup", fullId, config);

    // Then
    assertThat(diffResult.isOk()).isTrue();
    var metadata = diffResult.getOrNull();
    assertThat(metadata.type()).isEqualTo(BackupMetadata.BackupType.DIFFERENTIAL);
    assertThat(metadata.parentBackupId()).hasValue(fullId);
    assertThat(metadata.size()).isLessThan(fullResult.getOrNull().size());
  }

  @Test
  void shouldFailDifferentialBackupWhenReferenceIsNotFull() throws IOException {
    // Given
    var config =
        BackupConfiguration.builder()
            .destinationPath(backupDir.toString())
            .compressionEnabled(true)
            .build();

    var fullResult = service.createFullBackup("Full Backup", config);
    var fullId = fullResult.getOrNull().id();
    var incrResult = service.createIncrementalBackup("Incr", fullId, config);
    var incrId = incrResult.getOrNull().id();

    // When — try to use an incremental as base for differential
    var result = service.createDifferentialBackup("Diff", incrId, config);

    // Then
    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("BACKUP.NOT_A_FULL_BACKUP");
  }

  @Test
  void shouldRestorePointInTime() throws IOException {
    // Given
    var config =
        BackupConfiguration.builder()
            .destinationPath(backupDir.toString())
            .compressionEnabled(true)
            .build();

    service.createFullBackup("Backup 1", config);
    var backupTime = Instant.now().plusSeconds(1).toString();

    var restoreDir = tempDir.resolve("restore-pit");
    var restoreConfig =
        RestoreConfiguration.builder()
            .targetPath(restoreDir.toString())
            .overwriteExisting(true)
            .verifyIntegrity(false)
            .build();

    // When
    var result = service.restorePointInTime(backupTime, restoreConfig);

    // Then
    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull().filesRestored()).isEqualTo(3);
  }

  @Test
  void shouldFailPointInTimeRestoreWhenNoBackupExists() {
    // Given
    var restoreConfig =
        RestoreConfiguration.builder()
            .targetPath(tempDir.resolve("restore-pit-fail").toString())
            .verifyIntegrity(false)
            .build();

    // When — target is in the past, before any backup was created
    var result = service.restorePointInTime("2000-01-01T00:00:00Z", restoreConfig);

    // Then
    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value())
        .isEqualTo("RESTORE.NO_BACKUP_FOR_POINT_IN_TIME");
  }

  // -----------------------------------------------------------------------
  // Large file tests (#17)
  // -----------------------------------------------------------------------

  @Test
  void shouldBackupAndRestoreLargeFile() throws IOException {
    // Create a 2 MB file in the source directory
    byte[] twoMb = new byte[2 * 1024 * 1024];
    for (int i = 0; i < twoMb.length; i++) {
      twoMb[i] = (byte) (i % 127);
    }
    Files.write(sourceDir.resolve("large-file.bin"), twoMb);

    var config =
        BackupConfiguration.builder()
            .destinationPath(backupDir.toString())
            .compressionEnabled(true)
            .build();

    // Backup should succeed and include the large file
    var backupResult = service.createFullBackup("Large File Backup", config);
    assertThat(backupResult.isOk()).isTrue();
    var backupId = backupResult.getOrNull().id();
    assertThat(backupResult.getOrNull().size()).isGreaterThan(0);

    // Restore should recover the large file with identical content
    var restoreDir = tempDir.resolve("restore-large");
    var restoreConfig =
        RestoreConfiguration.builder()
            .targetPath(restoreDir.toString())
            .overwriteExisting(true)
            .verifyIntegrity(true)
            .build();

    var restoreResult = service.restore(backupId, restoreConfig);
    assertThat(restoreResult.isOk()).isTrue();
    assertThat(restoreResult.getOrNull().filesRestored()).isEqualTo(4); // 3 original + large file

    byte[] restored = Files.readAllBytes(restoreDir.resolve("large-file.bin"));
    assertThat(restored).isEqualTo(twoMb);
  }

  @Test
  void shouldHandleBackupOfEmptyDirectory() throws IOException {
    var emptySourceDir = tempDir.resolve("empty-source");
    Files.createDirectories(emptySourceDir);

    var emptyService = FilesystemBackupService.forSource(emptySourceDir);
    var config =
        BackupConfiguration.builder()
            .destinationPath(backupDir.toString())
            .compressionEnabled(false)
            .build();

    var result = emptyService.createFullBackup("Empty Dir Backup", config);

    assertThat(result.isOk()).isTrue();
    var metadata = result.getOrNull();
    assertThat(metadata.status()).isEqualTo(BackupMetadata.BackupStatus.COMPLETED);
  }

  @Test
  @DisabledOnOs(OS.WINDOWS)
  void shouldHandlePartialFailureDueToUnreadableFile() throws IOException {
    // Create an unreadable file (Unix only)
    Path secretFile = sourceDir.resolve("secret.txt");
    Files.writeString(secretFile, "sensitive data");
    Files.setPosixFilePermissions(secretFile, Set.of()); // no read permission

    var config =
        BackupConfiguration.builder()
            .destinationPath(backupDir.toString())
            .compressionEnabled(false)
            .build();

    // The backup may succeed (skipping the unreadable file) or fail gracefully
    // Either outcome is acceptable — what must NOT happen is an unhandled exception
    var result = service.createFullBackup("Partial Backup", config);
    assertThat(result).isNotNull(); // result is always returned, never a thrown exception

    // Restore permissions to allow JUnit to clean up the @TempDir
    Files.setPosixFilePermissions(
        secretFile, Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
  }

  @Test
  void shouldFailPointInTimeRestoreWithInvalidFormat() {
    // Given
    var restoreConfig =
        RestoreConfiguration.builder()
            .targetPath(tempDir.resolve("restore-pit-invalid").toString())
            .verifyIntegrity(false)
            .build();

    // When
    var result = service.restorePointInTime("not-a-date", restoreConfig);

    // Then
    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("RESTORE.INVALID_POINT_IN_TIME");
  }
}
