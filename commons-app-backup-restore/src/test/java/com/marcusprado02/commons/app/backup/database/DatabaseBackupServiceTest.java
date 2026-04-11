package com.marcusprado02.commons.app.backup.database;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.marcusprado02.commons.app.backup.BackupConfiguration;
import com.marcusprado02.commons.app.backup.BackupMetadata.BackupStatus;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class DatabaseBackupServiceTest {

  @Mock private DataSource dataSource;
  @Mock private Connection connection;
  @Mock private DatabaseMetaData dbMetaData;
  @Mock private ResultSet tableResultSet;

  private DatabaseBackupService service;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    service = DatabaseBackupService.create(dataSource);
  }

  @Test
  void shouldCreateServiceViaFactory() {
    assertThat(service).isNotNull();
  }

  @Test
  void shouldListBackupsWhenEmpty() {
    var result = service.listBackups();

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).isEmpty();
  }

  @Test
  void shouldReturnFailWhenBackupNotFound() {
    var result = service.getBackup("nonexistent");

    assertThat(result.isFail()).isTrue();
  }

  @Test
  void shouldReturnFailOnDeleteWhenBackupNotFound() {
    var result = service.deleteBackup("nonexistent");

    assertThat(result.isFail()).isTrue();
  }

  @Test
  void shouldReturnFailOnVerifyWhenBackupNotFound() {
    var result = service.verifyBackup("nonexistent");

    assertThat(result.isFail()).isTrue();
  }

  @Test
  void shouldReturnFailOnRestoreWhenBackupNotFound() {
    var restoreConfig =
        com.marcusprado02.commons.app.backup.RestoreConfiguration.builder()
            .targetPath("/restore")
            .build();

    var result = service.restore("nonexistent", restoreConfig);

    assertThat(result.isFail()).isTrue();
  }

  @Test
  void shouldCreateFullBackupSuccessfully(@TempDir Path tempDir) throws Exception {
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.getCatalog()).thenReturn(null);
    when(connection.getSchema()).thenReturn(null);
    when(connection.getMetaData()).thenReturn(dbMetaData);
    when(dbMetaData.getTables(any(), any(), eq("%"), eq(new String[] {"TABLE"})))
        .thenReturn(tableResultSet);
    when(tableResultSet.next()).thenReturn(false);

    var config = BackupConfiguration.builder().destinationPath(tempDir.toString()).build();

    var result = service.createFullBackup("test-backup", config);

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull().name()).isEqualTo("test-backup");
    assertThat(result.getOrNull().status()).isEqualTo(BackupStatus.COMPLETED);
  }

  @Test
  void shouldReturnFailedBackupWhenConnectionFails(@TempDir Path tempDir) throws Exception {
    when(dataSource.getConnection()).thenThrow(new SQLException("connection refused"));

    var config = BackupConfiguration.builder().destinationPath(tempDir.toString()).build();

    var result = service.createFullBackup("failing-backup", config);

    assertThat(result.isFail()).isTrue();
  }

  @Test
  void shouldCreateIncrementalBackupSuccessfully(@TempDir Path tempDir) throws Exception {
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.getCatalog()).thenReturn(null);
    when(connection.getSchema()).thenReturn(null);
    when(connection.getMetaData()).thenReturn(dbMetaData);
    when(dbMetaData.getTables(any(), any(), eq("%"), eq(new String[] {"TABLE"})))
        .thenReturn(tableResultSet);
    when(tableResultSet.next()).thenReturn(false);

    var config =
        BackupConfiguration.builder()
            .destinationPath(tempDir.toString())
            .option("db.since", "2026-01-01T00:00:00Z")
            .build();

    var result = service.createIncrementalBackup("incremental", "parent-id", config);

    assertThat(result.isOk()).isTrue();
  }

  @Test
  void shouldCreateDifferentialBackupSuccessfully(@TempDir Path tempDir) throws Exception {
    // First create a full backup to use as parent
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.getCatalog()).thenReturn(null);
    when(connection.getSchema()).thenReturn(null);
    when(connection.getMetaData()).thenReturn(dbMetaData);
    when(dbMetaData.getTables(any(), any(), eq("%"), eq(new String[] {"TABLE"})))
        .thenReturn(tableResultSet);
    when(tableResultSet.next()).thenReturn(false);

    var config = BackupConfiguration.builder().destinationPath(tempDir.toString()).build();
    service.createFullBackup("full", config);

    // Re-setup mocks for second call (ResultSet is consumed)
    when(connection.getMetaData()).thenReturn(dbMetaData);
    when(dbMetaData.getTables(any(), any(), eq("%"), eq(new String[] {"TABLE"})))
        .thenReturn(tableResultSet);
    when(tableResultSet.next()).thenReturn(false);

    var diffResult = service.createDifferentialBackup("differential", "some-full-id", config);

    assertThat(diffResult.isOk()).isTrue();
  }

  @Test
  void shouldVerifyBackupWithNoChecksum(@TempDir Path tempDir) throws Exception {
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.getCatalog()).thenReturn(null);
    when(connection.getSchema()).thenReturn(null);
    when(connection.getMetaData()).thenReturn(dbMetaData);
    when(dbMetaData.getTables(any(), any(), eq("%"), eq(new String[] {"TABLE"})))
        .thenReturn(tableResultSet);
    when(tableResultSet.next()).thenReturn(false);

    var config = BackupConfiguration.builder().destinationPath(tempDir.toString()).build();
    var createResult = service.createFullBackup("verify-test", config);
    assertThat(createResult.isOk()).isTrue();

    var backupId = createResult.getOrNull().id();
    var verifyResult = service.verifyBackup(backupId);

    assertThat(verifyResult.isOk()).isTrue();
  }

  @Test
  void shouldDeleteExistingBackup(@TempDir Path tempDir) throws Exception {
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.getCatalog()).thenReturn(null);
    when(connection.getSchema()).thenReturn(null);
    when(connection.getMetaData()).thenReturn(dbMetaData);
    when(dbMetaData.getTables(any(), any(), eq("%"), eq(new String[] {"TABLE"})))
        .thenReturn(tableResultSet);
    when(tableResultSet.next()).thenReturn(false);

    var config = BackupConfiguration.builder().destinationPath(tempDir.toString()).build();
    var createResult = service.createFullBackup("delete-me", config);
    var backupId = createResult.getOrNull().id();

    var deleteResult = service.deleteBackup(backupId);

    assertThat(deleteResult.isOk()).isTrue();
    assertThat(service.getBackup(backupId).isFail()).isTrue();
  }
}
