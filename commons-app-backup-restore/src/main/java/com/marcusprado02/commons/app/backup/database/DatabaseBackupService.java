package com.marcusprado02.commons.app.backup.database;

import com.marcusprado02.commons.app.backup.BackupConfiguration;
import com.marcusprado02.commons.app.backup.BackupMetadata;
import com.marcusprado02.commons.app.backup.BackupMetadata.BackupStatus;
import com.marcusprado02.commons.app.backup.BackupMetadata.BackupType;
import com.marcusprado02.commons.app.backup.BackupService;
import com.marcusprado02.commons.app.backup.RestoreConfiguration;
import com.marcusprado02.commons.app.backup.RestoreResult;
import com.marcusprado02.commons.kernel.errors.ErrorCategory;
import com.marcusprado02.commons.kernel.errors.ErrorCode;
import com.marcusprado02.commons.kernel.errors.Problem;
import com.marcusprado02.commons.kernel.errors.Severity;
import com.marcusprado02.commons.kernel.result.Result;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JDBC-based database backup service.
 *
 * <p>Exports all tables of a database schema to a compressed SQL dump (ZIP file containing
 * {@code dump.sql}). Supports full, incremental (timestamp-based), and differential backups.
 *
 * <p>Configuration options (via {@link BackupConfiguration#options()}):
 *
 * <ul>
 *   <li>{@code db.schema} — schema/catalog name to back up (optional, defaults to connection schema)
 *   <li>{@code db.tables} — comma-separated list of tables (optional, defaults to all tables)
 *   <li>{@code db.timestamp_column} — column used for incremental filtering (default: {@code updated_at})
 *   <li>{@code db.since} — ISO-8601 instant for incremental cutoff (required for incremental)
 * </ul>
 *
 * <p>Usage:
 *
 * <pre>{@code
 * DataSource ds = ...; // your JDBC DataSource
 * DatabaseBackupService backup = DatabaseBackupService.create(ds);
 *
 * BackupConfiguration config = BackupConfiguration.builder()
 *     .destinationPath("/var/backups/db")
 *     .compressionEnabled(true)
 *     .option("db.schema", "public")
 *     .build();
 *
 * backup.createFullBackup("nightly-2026-03-25", config)
 *     .peek(meta -> log.info("Backup saved to {}", meta.location()));
 * }</pre>
 */
public class DatabaseBackupService implements BackupService {

  private static final Logger logger = LoggerFactory.getLogger(DatabaseBackupService.class);

  private static final String OPT_SCHEMA = "db.schema";
  private static final String OPT_TABLES = "db.tables";
  private static final String OPT_TIMESTAMP_COL = "db.timestamp_column";
  private static final String OPT_SINCE = "db.since";
  private static final String DEFAULT_TIMESTAMP_COL = "updated_at";

  private final DataSource dataSource;
  private final Map<String, BackupMetadata> store = new ConcurrentHashMap<>();

  private DatabaseBackupService(DataSource dataSource) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
  }

  public static DatabaseBackupService create(DataSource dataSource) {
    return new DatabaseBackupService(dataSource);
  }

  @Override
  public Result<BackupMetadata> createFullBackup(String name, BackupConfiguration config) {
    return runBackup(name, BackupType.FULL, null, config, null);
  }

  @Override
  public Result<BackupMetadata> createIncrementalBackup(
      String name, String parentBackupId, BackupConfiguration config) {
    return runBackup(name, BackupType.INCREMENTAL, parentBackupId, config, null);
  }

  @Override
  public Result<BackupMetadata> createDifferentialBackup(
      String name, String fullBackupId, BackupConfiguration config) {
    return runBackup(name, BackupType.DIFFERENTIAL, fullBackupId, config, null);
  }

  @Override
  public Result<List<BackupMetadata>> listBackups() {
    return Result.ok(List.copyOf(store.values()));
  }

  @Override
  public Result<BackupMetadata> getBackup(String backupId) {
    var meta = store.get(backupId);
    if (meta == null) {
      return Result.fail(
          Problem.of(
              ErrorCode.of("DB_BACKUP.NOT_FOUND"),
              ErrorCategory.NOT_FOUND,
              Severity.WARNING,
              "Backup not found: " + backupId));
    }
    return Result.ok(meta);
  }

  @Override
  public Result<Void> deleteBackup(String backupId) {
    var meta = store.remove(backupId);
    if (meta == null) {
      return Result.fail(
          Problem.of(
              ErrorCode.of("DB_BACKUP.NOT_FOUND"),
              ErrorCategory.NOT_FOUND,
              Severity.WARNING,
              "Backup not found: " + backupId));
    }
    try {
      Files.deleteIfExists(Paths.get(meta.location()));
    } catch (IOException e) {
      logger.warn("Could not delete backup file {}: {}", meta.location(), e.getMessage());
    }
    return Result.ok(null);
  }

  @Override
  public Result<Boolean> verifyBackup(String backupId) {
    var meta = store.get(backupId);
    if (meta == null) {
      return Result.fail(
          Problem.of(
              ErrorCode.of("DB_BACKUP.NOT_FOUND"),
              ErrorCategory.NOT_FOUND,
              Severity.WARNING,
              "Backup not found: " + backupId));
    }

    Path path = Paths.get(meta.location());
    if (!Files.exists(path)) {
      return Result.ok(false);
    }

    if (meta.checksum().isEmpty()) {
      return Result.ok(Files.exists(path));
    }

    try {
      String actual = sha256Hex(Files.readAllBytes(path));
      return Result.ok(actual.equals(meta.checksum().get()));
    } catch (Exception e) {
      return Result.fail(
          Problem.of(
              ErrorCode.of("DB_BACKUP.VERIFY_FAILED"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to verify backup: " + e.getMessage()));
    }
  }

  // -------------------------------------------------------------------------
  // Restore (delegated to SQL replay)
  // -------------------------------------------------------------------------

  /**
   * Restores a database backup by replaying the SQL dump via JDBC.
   *
   * @param backupId the backup to restore
   * @param config restore configuration
   * @return restore result
   */
  public Result<RestoreResult> restore(String backupId, RestoreConfiguration config) {
    Instant startAt = Instant.now();
    var meta = store.get(backupId);
    if (meta == null) {
      return Result.fail(
          Problem.of(
              ErrorCode.of("DB_BACKUP.NOT_FOUND"),
              ErrorCategory.NOT_FOUND,
              Severity.WARNING,
              "Backup not found: " + backupId));
    }

    Path path = Paths.get(meta.location());
    if (!Files.exists(path)) {
      return Result.fail(
          Problem.of(
              ErrorCode.of("DB_BACKUP.FILE_MISSING"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Backup file not found: " + path));
    }

    try (var zip = new ZipFile(path.toFile())) {
      var entry = zip.getEntry("dump.sql");
      if (entry == null) {
        return Result.fail(
            Problem.of(
                ErrorCode.of("DB_BACKUP.INVALID_FORMAT"),
                ErrorCategory.TECHNICAL,
                Severity.ERROR,
                "Backup archive does not contain dump.sql"));
      }

      String sql = new String(zip.getInputStream(entry).readAllBytes(), StandardCharsets.UTF_8);
      executeSql(sql);

      logger.info("Restored database backup: {} from {}", backupId, path);
      return Result.ok(
          RestoreResult.builder()
              .backupId(backupId)
              .targetPath(path.toString())
              .bytesRestored(meta.size())
              .duration(java.time.Duration.between(startAt, Instant.now()))
              .completedAt(Instant.now())
              .build());

    } catch (Exception e) {
      logger.error("Failed to restore database backup {}: {}", backupId, e.getMessage(), e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("DB_BACKUP.RESTORE_FAILED"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Database restore failed: " + e.getMessage()));
    }
  }

  // -------------------------------------------------------------------------
  // Internal
  // -------------------------------------------------------------------------

  private Result<BackupMetadata> runBackup(
      String name,
      BackupType type,
      String parentId,
      BackupConfiguration config,
      @SuppressWarnings("unused") Instant sinceOverride) {

    String id = UUID.randomUUID().toString();
    Instant startedAt = Instant.now();

    // Determine since timestamp for incremental/differential
    Instant since = null;
    if (type != BackupType.FULL) {
      String sinceStr = config.options().get(OPT_SINCE);
      if (sinceStr != null) {
        since = Instant.parse(sinceStr);
      } else if (parentId != null && store.containsKey(parentId)) {
        since = store.get(parentId).createdAt();
      }
    }

    Path destDir = Paths.get(config.destinationPath());
    Path outFile = destDir.resolve(id + ".sql.zip");

    try {
      Files.createDirectories(destDir);

      String schema = config.options().get(OPT_SCHEMA);
      Set<String> tableFilter = parseTableFilter(config.options().get(OPT_TABLES));
      String tsCol = config.options().getOrDefault(OPT_TIMESTAMP_COL, DEFAULT_TIMESTAMP_COL);

      long byteCount;
      try (var connection = dataSource.getConnection()) {
        if (schema != null) {
          connection.setCatalog(schema);
        }
        List<String> tables = listTables(connection, tableFilter);
        byteCount = writeSqlDump(connection, tables, outFile, since, tsCol, type);
      }

      String checksum = sha256Hex(Files.readAllBytes(outFile));

      var meta =
          BackupMetadata.builder()
              .id(id)
              .name(name)
              .type(type)
              .source(schema != null ? schema : "default")
              .location(outFile.toString())
              .size(byteCount)
              .status(BackupStatus.COMPLETED)
              .createdAt(startedAt)
              .completedAt(Instant.now())
              .parentBackupId(parentId)
              .checksum(checksum)
              .build();

      store.put(id, meta);
      logger.info("Database backup completed: {} -> {}", name, outFile);
      return Result.ok(meta);

    } catch (Exception e) {
      logger.error("Database backup failed for {}: {}", name, e.getMessage(), e);

      var failed =
          BackupMetadata.builder()
              .id(id)
              .name(name)
              .type(type)
              .source(config.options().getOrDefault(OPT_SCHEMA, "unknown"))
              .location(outFile.toString())
              .size(0)
              .status(BackupStatus.FAILED)
              .createdAt(startedAt)
              .error(e.getMessage())
              .build();

      store.put(id, failed);
      return Result.fail(
          Problem.of(
              ErrorCode.of("DB_BACKUP.FAILED"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Database backup failed: " + e.getMessage()));
    }
  }

  private List<String> listTables(Connection conn, Set<String> tableFilter) throws SQLException {
    List<String> tables = new ArrayList<>();
    try (ResultSet rs =
        conn.getMetaData().getTables(conn.getCatalog(), conn.getSchema(), "%", new String[]{"TABLE"})) {
      while (rs.next()) {
        String table = rs.getString("TABLE_NAME");
        if (tableFilter.isEmpty() || tableFilter.contains(table)) {
          tables.add(table);
        }
      }
    }
    return tables;
  }

  private long writeSqlDump(
      Connection conn,
      List<String> tables,
      Path outFile,
      Instant since,
      String tsCol,
      BackupType type)
      throws Exception {

    try (var fos = Files.newOutputStream(outFile);
        var zos = new ZipOutputStream(fos)) {

      zos.putNextEntry(new ZipEntry("dump.sql"));

      var writer = new PrintWriter(new OutputStreamWriter(zos, StandardCharsets.UTF_8), true);
      writer.println("-- commons-platform database backup");
      writer.println("-- type: " + type.name());
      writer.println("-- created: " + Instant.now());
      if (since != null) {
        writer.println("-- since: " + since);
      }
      writer.println();

      for (String table : tables) {
        dumpTable(conn, table, writer, since, tsCol, type);
      }

      writer.flush();
      zos.closeEntry();
    }

    return Files.size(outFile);
  }

  private void dumpTable(
      Connection conn,
      String table,
      PrintWriter out,
      Instant since,
      String tsCol,
      BackupType type)
      throws SQLException {

    // Build WHERE clause for incremental/differential backups
    String where = "";
    if (since != null && type != BackupType.FULL) {
      // Check if timestamp column exists
      if (hasColumn(conn, table, tsCol)) {
        where = " WHERE " + tsCol + " > '" + Timestamp.from(since) + "'";
      }
    }

    String sql = "SELECT * FROM " + quoteIdentifier(table) + where;

    out.println("-- Table: " + table);
    try (Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql)) {

      ResultSetMetaData meta = rs.getMetaData();
      int columnCount = meta.getColumnCount();

      while (rs.next()) {
        out.print("INSERT INTO " + quoteIdentifier(table) + " VALUES (");
        for (int i = 1; i <= columnCount; i++) {
          if (i > 1) out.print(", ");
          Object value = rs.getObject(i);
          if (value == null) {
            out.print("NULL");
          } else if (value instanceof Number) {
            out.print(value);
          } else {
            out.print("'" + value.toString().replace("'", "''") + "'");
          }
        }
        out.println(");");
      }
    }
    out.println();
  }

  private boolean hasColumn(Connection conn, String table, String column) {
    try (ResultSet rs = conn.getMetaData().getColumns(conn.getCatalog(), conn.getSchema(), table, column)) {
      return rs.next();
    } catch (SQLException e) {
      return false;
    }
  }

  private void executeSql(String sql) throws SQLException {
    try (Connection conn = dataSource.getConnection();
        Statement stmt = conn.createStatement()) {
      // Execute statement by statement
      for (String statement : sql.split(";")) {
        String trimmed = statement.trim();
        if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
          stmt.execute(trimmed);
        }
      }
    }
  }

  private static Set<String> parseTableFilter(String tables) {
    if (tables == null || tables.isBlank()) {
      return Set.of();
    }
    return new HashSet<>(Arrays.asList(tables.split("\\s*,\\s*")));
  }

  private static String quoteIdentifier(String name) {
    return "\"" + name.replace("\"", "\"\"") + "\"";
  }

  private static String sha256Hex(byte[] data) throws Exception {
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    byte[] digest = md.digest(data);
    StringBuilder sb = new StringBuilder(digest.length * 2);
    for (byte b : digest) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }
}
