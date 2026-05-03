package com.marcusprado02.commons.app.backup.database;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.marcusprado02.commons.app.backup.BackupConfiguration;
import com.marcusprado02.commons.app.backup.BackupMetadata.BackupStatus;
import com.marcusprado02.commons.app.backup.RestoreConfiguration;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class DatabaseBackupServiceBranchTest {

  @Mock private DataSource dataSource;
  @Mock private Connection connection;
  @Mock private DatabaseMetaData dbMetaData;
  @Mock private ResultSet tableResultSet;
  @Mock private ResultSet columnResultSet;
  @Mock private ResultSet rowResultSet;
  @Mock private ResultSetMetaData rsMetaData;
  @Mock private Statement statement;

  private DatabaseBackupService service;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    service = DatabaseBackupService.create(dataSource);
  }

  private void stubFullBackupConnection() throws Exception {
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.getCatalog()).thenReturn(null);
    when(connection.getSchema()).thenReturn(null);
    when(connection.getMetaData()).thenReturn(dbMetaData);
    when(dbMetaData.getTables(any(), any(), eq("%"), eq(new String[] {"TABLE"})))
        .thenReturn(tableResultSet);
    when(tableResultSet.next()).thenReturn(false);
  }

  // --- since resolved from parent store (sinceStr == null, parentId in store) ---

  @Test
  void shouldCreateIncrementalResolvingSinceFromParentStore(@TempDir Path tempDir)
      throws Exception {
    stubFullBackupConnection();

    var config = BackupConfiguration.builder().destinationPath(tempDir.toString()).build();
    var fullResult = service.createFullBackup("full", config);
    assertThat(fullResult.isOk()).isTrue();
    String parentId = fullResult.getOrNull().id();

    // Reset tableResultSet for the second call
    when(tableResultSet.next()).thenReturn(false);

    // Incremental with no db.since option — since must come from parent store
    var incConfig =
        BackupConfiguration.builder()
            .destinationPath(tempDir.toString())
            // intentionally no "db.since" option so branch sinceStr==null is taken
            .build();
    var incResult = service.createIncrementalBackup("inc", parentId, incConfig);

    assertThat(incResult.isOk()).isTrue();
    assertThat(incResult.getOrNull().type().name()).isEqualTo("INCREMENTAL");
    assertThat(incResult.getOrNull().parentBackupId()).contains(parentId);
  }

  // --- schema option → setCatalog called ---

  @Test
  void shouldCreateFullBackupWithSchemaOption(@TempDir Path tempDir) throws Exception {
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.getCatalog()).thenReturn("public");
    when(connection.getSchema()).thenReturn(null);
    when(connection.getMetaData()).thenReturn(dbMetaData);
    when(dbMetaData.getTables(any(), any(), eq("%"), eq(new String[] {"TABLE"})))
        .thenReturn(tableResultSet);
    when(tableResultSet.next()).thenReturn(false);

    var config =
        BackupConfiguration.builder()
            .destinationPath(tempDir.toString())
            .option("db.schema", "public")
            .build();

    var result = service.createFullBackup("schema-backup", config);

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull().source()).isEqualTo("public");
    verify(connection).setCatalog("public");
  }

  // --- table filter: non-empty, include/exclude paths ---

  @Test
  void shouldCreateFullBackupWithTableFilter(@TempDir Path tempDir) throws Exception {
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.getCatalog()).thenReturn(null);
    when(connection.getSchema()).thenReturn(null);
    when(connection.getMetaData()).thenReturn(dbMetaData);
    when(dbMetaData.getTables(any(), any(), eq("%"), eq(new String[] {"TABLE"})))
        .thenReturn(tableResultSet);
    // Two tables: "orders" (matches filter), "payments" (does not match)
    when(tableResultSet.next()).thenReturn(true, true, false);
    when(tableResultSet.getString("TABLE_NAME")).thenReturn("orders", "payments");

    // "orders" table: one row with one integer column
    when(connection.createStatement()).thenReturn(statement);
    when(statement.executeQuery(any())).thenReturn(rowResultSet);
    when(rowResultSet.getMetaData()).thenReturn(rsMetaData);
    when(rsMetaData.getColumnCount()).thenReturn(1);
    when(rowResultSet.next()).thenReturn(true, false);
    when(rowResultSet.getObject(1)).thenReturn(42);

    var config =
        BackupConfiguration.builder()
            .destinationPath(tempDir.toString())
            .option("db.tables", "orders")
            .build();

    var result = service.createFullBackup("filtered-backup", config);

    assertThat(result.isOk()).isTrue();
  }

  // --- dumpTable: null / Number / String values; hasColumn true (incremental with since) ---

  @Test
  void shouldDumpTableRowsWithNullNumberAndStringValues(@TempDir Path tempDir) throws Exception {
    // First, create a full backup to populate the parent store
    stubFullBackupConnection();
    var config = BackupConfiguration.builder().destinationPath(tempDir.toString()).build();
    var fullResult = service.createFullBackup("full", config);
    assertThat(fullResult.isOk()).isTrue();
    String parentId = fullResult.getOrNull().id();

    // Incremental backup with db.since: exercises since != null && type != FULL path
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.getCatalog()).thenReturn(null);
    when(connection.getSchema()).thenReturn(null);
    when(connection.getMetaData()).thenReturn(dbMetaData);
    when(dbMetaData.getTables(any(), any(), eq("%"), eq(new String[] {"TABLE"})))
        .thenReturn(tableResultSet);
    when(tableResultSet.next()).thenReturn(true, false);
    when(tableResultSet.getString("TABLE_NAME")).thenReturn("users");

    // hasColumn returns true (updated_at column exists)
    when(dbMetaData.getColumns(any(), any(), eq("users"), eq("updated_at")))
        .thenReturn(columnResultSet);
    when(columnResultSet.next()).thenReturn(true);

    // Row: null, integer, string with quote
    when(connection.createStatement()).thenReturn(statement);
    when(statement.executeQuery(any())).thenReturn(rowResultSet);
    when(rowResultSet.getMetaData()).thenReturn(rsMetaData);
    when(rsMetaData.getColumnCount()).thenReturn(3);
    when(rowResultSet.next()).thenReturn(true, false);
    when(rowResultSet.getObject(1)).thenReturn(null);
    when(rowResultSet.getObject(2)).thenReturn(42);
    when(rowResultSet.getObject(3)).thenReturn("O'Brien");

    var incConfig =
        BackupConfiguration.builder()
            .destinationPath(tempDir.toString())
            .option("db.since", "2026-01-01T00:00:00Z")
            .build();

    var result = service.createIncrementalBackup("dump-test", parentId, incConfig);

    assertThat(result.isOk()).isTrue();
  }

  // --- hasColumn returns false (rs.next() = false) ---

  @Test
  void shouldDumpTableWhenHasColumnReturnsFalse(@TempDir Path tempDir) throws Exception {
    // Full backup setup for parent
    stubFullBackupConnection();
    var config = BackupConfiguration.builder().destinationPath(tempDir.toString()).build();
    var fullResult = service.createFullBackup("full2", config);
    String parentId = fullResult.getOrNull().id();

    // Incremental: hasColumn returns false → no WHERE clause
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.getCatalog()).thenReturn(null);
    when(connection.getSchema()).thenReturn(null);
    when(connection.getMetaData()).thenReturn(dbMetaData);
    when(dbMetaData.getTables(any(), any(), eq("%"), eq(new String[] {"TABLE"})))
        .thenReturn(tableResultSet);
    when(tableResultSet.next()).thenReturn(true, false);
    when(tableResultSet.getString("TABLE_NAME")).thenReturn("events");

    // hasColumn: no "updated_at" column
    when(dbMetaData.getColumns(any(), any(), eq("events"), eq("updated_at")))
        .thenReturn(columnResultSet);
    when(columnResultSet.next()).thenReturn(false);

    when(connection.createStatement()).thenReturn(statement);
    when(statement.executeQuery(any())).thenReturn(rowResultSet);
    when(rowResultSet.getMetaData()).thenReturn(rsMetaData);
    when(rsMetaData.getColumnCount()).thenReturn(1);
    when(rowResultSet.next()).thenReturn(false);

    var incConfig =
        BackupConfiguration.builder()
            .destinationPath(tempDir.toString())
            .option("db.since", "2026-01-01T00:00:00Z")
            .build();

    var result = service.createIncrementalBackup("no-col", parentId, incConfig);

    assertThat(result.isOk()).isTrue();
  }

  // --- verifyBackup: file missing → false ---

  @Test
  void shouldVerifyBackupReturnsFalseWhenFileMissing(@TempDir Path tempDir) throws Exception {
    stubFullBackupConnection();
    var config = BackupConfiguration.builder().destinationPath(tempDir.toString()).build();
    var createResult = service.createFullBackup("verify-missing", config);
    String backupId = createResult.getOrNull().id();
    String location = createResult.getOrNull().location();

    Files.deleteIfExists(Path.of(location));

    var verifyResult = service.verifyBackup(backupId);

    assertThat(verifyResult.isOk()).isTrue();
    assertThat(verifyResult.getOrNull()).isFalse();
  }

  // --- verifyBackup: checksum mismatch → false ---

  @Test
  void shouldVerifyBackupReturnsFalseOnChecksumMismatch(@TempDir Path tempDir) throws Exception {
    stubFullBackupConnection();
    var config = BackupConfiguration.builder().destinationPath(tempDir.toString()).build();
    var createResult = service.createFullBackup("checksum-test", config);
    String backupId = createResult.getOrNull().id();
    String location = createResult.getOrNull().location();

    // Overwrite with different bytes → checksum will not match
    Files.write(Path.of(location), new byte[] {1, 2, 3});

    var verifyResult = service.verifyBackup(backupId);

    assertThat(verifyResult.isOk()).isTrue();
    assertThat(verifyResult.getOrNull()).isFalse();
  }

  // --- restore: file missing ---

  @Test
  void shouldRestoreFailsWhenBackupFileMissing(@TempDir Path tempDir) throws Exception {
    stubFullBackupConnection();
    var config = BackupConfiguration.builder().destinationPath(tempDir.toString()).build();
    var createResult = service.createFullBackup("restore-missing", config);
    String backupId = createResult.getOrNull().id();
    String location = createResult.getOrNull().location();

    Files.deleteIfExists(Path.of(location));

    var restoreConfig = RestoreConfiguration.builder().targetPath(tempDir.toString()).build();
    var result = service.restore(backupId, restoreConfig);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("DB_BACKUP.FILE_MISSING");
  }

  // --- restore: zip has no dump.sql entry ---

  @Test
  void shouldRestoreFailsWhenZipHasNoDumpSqlEntry(@TempDir Path tempDir) throws Exception {
    stubFullBackupConnection();
    var config = BackupConfiguration.builder().destinationPath(tempDir.toString()).build();
    var createResult = service.createFullBackup("no-dump", config);
    String backupId = createResult.getOrNull().id();
    String location = createResult.getOrNull().location();

    // Overwrite with a ZIP that has a different entry name
    var baos = new ByteArrayOutputStream();
    try (var zos = new ZipOutputStream(baos)) {
      zos.putNextEntry(new ZipEntry("other.sql"));
      zos.write("content".getBytes());
      zos.closeEntry();
    }
    Files.write(Path.of(location), baos.toByteArray());

    var restoreConfig = RestoreConfiguration.builder().targetPath(tempDir.toString()).build();
    var result = service.restore(backupId, restoreConfig);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("DB_BACKUP.INVALID_FORMAT");
  }

  // --- restore: success, exercising executeSql branches ---

  @Test
  void shouldRestoreSuccessfullyExecutingValidDump(@TempDir Path tempDir) throws Exception {
    stubFullBackupConnection();
    var config = BackupConfiguration.builder().destinationPath(tempDir.toString()).build();
    var createResult = service.createFullBackup("restore-ok", config);
    String backupId = createResult.getOrNull().id();
    String location = createResult.getOrNull().location();

    // Craft a ZIP with dump.sql containing: comment, blank line, real statement
    String sqlContent = "-- comment\n\nINSERT INTO \"t\" VALUES (1)";
    var baos = new ByteArrayOutputStream();
    try (var zos = new ZipOutputStream(baos)) {
      zos.putNextEntry(new ZipEntry("dump.sql"));
      zos.write(sqlContent.getBytes());
      zos.closeEntry();
    }
    Files.write(Path.of(location), baos.toByteArray());

    // Second dataSource.getConnection() call is for executeSql
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.createStatement()).thenReturn(statement);
    when(statement.execute(any())).thenReturn(false);

    var restoreConfig = RestoreConfiguration.builder().targetPath(tempDir.toString()).build();
    var result = service.restore(backupId, restoreConfig);

    assertThat(result.isOk()).isTrue();
  }

  // --- parseTableFilter: blank tables option → Set.of() ---

  @Test
  void shouldCreateFullBackupWithBlankTableFilter(@TempDir Path tempDir) throws Exception {
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
            .option("db.tables", "   ")
            .build();

    var result = service.createFullBackup("blank-filter", config);

    assertThat(result.isOk()).isTrue();
  }

  // --- failed backup stored in store with FAILED status ---

  @Test
  void shouldStoreFailedBackupWithFailedStatus(@TempDir Path tempDir) throws Exception {
    when(dataSource.getConnection()).thenThrow(new RuntimeException("disk full"));

    var config = BackupConfiguration.builder().destinationPath(tempDir.toString()).build();
    var result = service.createFullBackup("failing", config);

    assertThat(result.isFail()).isTrue();

    var list = service.listBackups();
    assertThat(list.isOk()).isTrue();
    assertThat(list.getOrNull()).hasSize(1).allMatch(m -> m.status() == BackupStatus.FAILED);
  }
}
