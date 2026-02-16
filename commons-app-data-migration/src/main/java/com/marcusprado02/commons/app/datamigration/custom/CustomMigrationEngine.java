package com.marcusprado02.commons.app.datamigration.custom;

import com.marcusprado02.commons.app.datamigration.MigrationEngine;
import com.marcusprado02.commons.app.datamigration.MigrationInfo;
import com.marcusprado02.commons.app.datamigration.MigrationResult;
import com.marcusprado02.commons.kernel.errors.ErrorCategory;
import com.marcusprado02.commons.kernel.errors.ErrorCode;
import com.marcusprado02.commons.kernel.errors.Problem;
import com.marcusprado02.commons.kernel.errors.Severity;
import com.marcusprado02.commons.kernel.result.Result;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Migration engine for executing custom Java-based migrations.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * MigrationEngine engine = CustomMigrationEngine.builder()
 *     .dataSource(dataSource)
 *     .migration(new V1_0__CreateUsers())
 *     .migration(new V1_1__InsertAdminUser())
 *     .migration(new V1_2__CreateProducts())
 *     .build();
 *
 * Result<MigrationResult> result = engine.migrate();
 * }</pre>
 *
 * @see CustomMigration
 * @see MigrationEngine
 */
public class CustomMigrationEngine implements MigrationEngine {

  private static final Logger logger = LoggerFactory.getLogger(CustomMigrationEngine.class);
  private static final String HISTORY_TABLE = "migration_history";

  private final DataSource dataSource;
  private final List<CustomMigration> migrations;

  /**
   * Creates a custom migration engine.
   *
   * @param dataSource the data source
   * @param migrations list of custom migrations
   */
  public CustomMigrationEngine(DataSource dataSource, List<CustomMigration> migrations) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource cannot be null");
    this.migrations = new ArrayList<>(migrations);
    this.migrations.sort(Comparator.comparing(CustomMigration::getVersion));
  }

  /**
   * Creates a new builder.
   *
   * @return builder
   */
  public static Builder builder() {
    return new Builder();
  }

  @Override
  public Result<MigrationResult> migrate() {
    try (Connection conn = dataSource.getConnection()) {
      ensureHistoryTableExists(conn);

      List<CustomMigration> pendingMigrations = getPendingMigrations(conn);

      if (pendingMigrations.isEmpty()) {
        logger.info("No pending migrations");
        return Result.ok(MigrationResult.ok(0, currentVersion()));
      }

      logger.info("Found {} pending migrations", pendingMigrations.size());

      int executed = 0;
      String lastVersion = "0";

      for (CustomMigration migration : pendingMigrations) {
        logger.info(
            "Executing migration {} - {}", migration.getVersion(), migration.getDescription());

        long startTime = System.currentTimeMillis();

        try {
          if (migration.isTransactional()) {
            conn.setAutoCommit(false);
          }

          migration.migrate(conn);
          long executionTime = System.currentTimeMillis() - startTime;

          recordMigration(conn, migration, (int) executionTime);

          if (migration.isTransactional()) {
            conn.commit();
          }

          executed++;
          lastVersion = migration.getVersion();

          logger.info("Migration {} completed in {}ms", migration.getVersion(), executionTime);

        } catch (Exception e) {
          if (migration.isTransactional()) {
            conn.rollback();
          }
          logger.error("Migration {} failed", migration.getVersion(), e);
          return Result.fail(
              Problem.of(
                  ErrorCode.of("MIGRATION_FAILED"),
                  ErrorCategory.TECHNICAL,
                  Severity.ERROR,
                  "Migration " + migration.getVersion() + " failed: " + e.getMessage()));
        } finally {
          if (migration.isTransactional()) {
            conn.setAutoCommit(true);
          }
        }
      }

      logger.info("Applied {} migrations. Final version: {}", executed, lastVersion);
      return Result.ok(MigrationResult.ok(executed, lastVersion));

    } catch (Exception e) {
      logger.error("Migration failed", e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("MIGRATION_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Migration error: " + e.getMessage()));
    }
  }

  @Override
  public Result<Void> validate() {
    try (Connection conn = dataSource.getConnection()) {
      ensureHistoryTableExists(conn);

      List<MigrationInfo> appliedMigrations = getAppliedMigrations(conn);

      for (MigrationInfo applied : appliedMigrations) {
        boolean found = migrations.stream().anyMatch(m -> m.getVersion().equals(applied.version()));

        if (!found) {
          return Result.fail(
              Problem.of(
                  ErrorCode.of("VALIDATION_FAILED"),
                  ErrorCategory.TECHNICAL,
                  Severity.ERROR,
                  "Applied migration " + applied.version() + " not found in available migrations"));
        }
      }

      logger.info("Validation successful");
      return Result.ok(null);

    } catch (Exception e) {
      logger.error("Validation failed", e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("VALIDATION_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Validation error: " + e.getMessage()));
    }
  }

  @Override
  public List<MigrationInfo> info() {
    try (Connection conn = dataSource.getConnection()) {
      ensureHistoryTableExists(conn);

      List<MigrationInfo> result = new ArrayList<>();
      List<String> appliedVersions = getAppliedVersions(conn);

      for (CustomMigration migration : migrations) {
        if (appliedVersions.contains(migration.getVersion())) {
          MigrationInfo applied = getAppliedMigrationInfo(conn, migration.getVersion());
          if (applied != null) {
            result.add(applied);
          }
        } else {
          result.add(
              MigrationInfo.pending(
                  migration.getVersion(), migration.getDescription(), "JAVA", null));
        }
      }

      return result;

    } catch (Exception e) {
      logger.error("Failed to get migration info", e);
      return List.of();
    }
  }

  @Override
  public Result<Void> clean() {
    try (Connection conn = dataSource.getConnection();
        Statement stmt = conn.createStatement()) {

      stmt.execute("DROP TABLE IF EXISTS " + HISTORY_TABLE);
      logger.info("Migration history table dropped");
      return Result.ok(null);

    } catch (Exception e) {
      logger.error("Clean failed", e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("CLEAN_FAILED"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Clean failed: " + e.getMessage()));
    }
  }

  @Override
  public Result<Void> repair() {
    // Custom migrations don't have checksums to repair
    logger.info("Repair not needed for custom migrations");
    return Result.ok(null);
  }

  @Override
  public Result<Void> baseline(String version, String description) {
    try (Connection conn = dataSource.getConnection()) {
      ensureHistoryTableExists(conn);

      String sql =
          "INSERT INTO "
              + HISTORY_TABLE
              + " (version, description, type, installed_on, execution_time, success) "
              + "VALUES (?, ?, 'BASELINE', ?, 0, true)";

      try (PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setString(1, version);
        stmt.setString(2, description);
        stmt.setTimestamp(3, Timestamp.from(Instant.now()));
        stmt.executeUpdate();
      }

      logger.info("Baseline created at version {}", version);
      return Result.ok(null);

    } catch (Exception e) {
      logger.error("Baseline failed", e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("BASELINE_FAILED"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Baseline failed: " + e.getMessage()));
    }
  }

  private void ensureHistoryTableExists(Connection conn) throws SQLException {
    String sql =
        "CREATE TABLE IF NOT EXISTS "
            + HISTORY_TABLE
            + " ("
            + "  id BIGINT AUTO_INCREMENT PRIMARY KEY,"
            + "  version VARCHAR(50) NOT NULL,"
            + "  description VARCHAR(200) NOT NULL,"
            + "  type VARCHAR(20) NOT NULL,"
            + "  installed_on TIMESTAMP NOT NULL,"
            + "  execution_time INT,"
            + "  success BOOLEAN NOT NULL"
            + ")";

    try (Statement stmt = conn.createStatement()) {
      stmt.execute(sql);
    }
  }

  private List<CustomMigration> getPendingMigrations(Connection conn) throws SQLException {
    List<String> appliedVersions = getAppliedVersions(conn);
    return migrations.stream().filter(m -> !appliedVersions.contains(m.getVersion())).toList();
  }

  private List<String> getAppliedVersions(Connection conn) throws SQLException {
    List<String> versions = new ArrayList<>();

    String sql = "SELECT version FROM " + HISTORY_TABLE + " WHERE success = true ORDER BY id";

    try (Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql)) {
      while (rs.next()) {
        versions.add(rs.getString("version"));
      }
    }

    return versions;
  }

  private List<MigrationInfo> getAppliedMigrations(Connection conn) throws SQLException {
    List<MigrationInfo> migrations = new ArrayList<>();

    String sql = "SELECT * FROM " + HISTORY_TABLE + " WHERE success = true ORDER BY id";

    try (Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql)) {
      while (rs.next()) {
        migrations.add(
            MigrationInfo.success(
                rs.getString("version"),
                rs.getString("description"),
                rs.getString("type"),
                rs.getTimestamp("installed_on").toInstant(),
                rs.getInt("execution_time"),
                null));
      }
    }

    return migrations;
  }

  private MigrationInfo getAppliedMigrationInfo(Connection conn, String version)
      throws SQLException {
    String sql = "SELECT * FROM " + HISTORY_TABLE + " WHERE version = ? AND success = true";

    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, version);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return MigrationInfo.success(
              rs.getString("version"),
              rs.getString("description"),
              rs.getString("type"),
              rs.getTimestamp("installed_on").toInstant(),
              rs.getInt("execution_time"),
              null);
        }
      }
    }

    return null;
  }

  private void recordMigration(Connection conn, CustomMigration migration, int executionTime)
      throws SQLException {
    String sql =
        "INSERT INTO "
            + HISTORY_TABLE
            + " (version, description, type, installed_on, execution_time, success) "
            + "VALUES (?, ?, 'JAVA', ?, ?, true)";

    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, migration.getVersion());
      stmt.setString(2, migration.getDescription());
      stmt.setTimestamp(3, Timestamp.from(Instant.now()));
      stmt.setInt(4, executionTime);
      stmt.executeUpdate();
    }
  }

  /** Builder for CustomMigrationEngine. */
  public static class Builder {
    private DataSource dataSource;
    private final List<CustomMigration> migrations = new ArrayList<>();

    public Builder dataSource(DataSource dataSource) {
      this.dataSource = dataSource;
      return this;
    }

    public Builder migration(CustomMigration migration) {
      this.migrations.add(migration);
      return this;
    }

    public Builder migrations(List<CustomMigration> migrations) {
      this.migrations.addAll(migrations);
      return this;
    }

    public CustomMigrationEngine build() {
      Objects.requireNonNull(dataSource, "dataSource is required");
      return new CustomMigrationEngine(dataSource, migrations);
    }
  }
}
