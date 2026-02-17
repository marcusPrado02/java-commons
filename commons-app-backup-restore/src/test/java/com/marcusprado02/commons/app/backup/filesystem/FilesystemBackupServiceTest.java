package com.marcusprado02.commons.app.backup.filesystem;

import static org.assertj.core.api.Assertions.*;

import com.marcusprado02.commons.app.backup.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
}
