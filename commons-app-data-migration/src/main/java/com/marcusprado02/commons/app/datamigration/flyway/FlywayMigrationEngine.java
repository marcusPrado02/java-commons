package com.marcusprado02.commons.app.datamigration.flyway;

import com.marcusprado02.commons.app.datamigration.MigrationEngine;
import com.marcusprado02.commons.app.datamigration.MigrationInfo;
import com.marcusprado02.commons.app.datamigration.MigrationResult;
import com.marcusprado02.commons.app.datamigration.MigrationState;
import com.marcusprado02.commons.kernel.errors.ErrorCategory;
import com.marcusprado02.commons.kernel.errors.ErrorCode;
import com.marcusprado02.commons.kernel.errors.Problem;
import com.marcusprado02.commons.kernel.errors.Severity;
import com.marcusprado02.commons.kernel.result.Result;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfoService;
import org.flywaydb.core.api.output.BaselineResult;
import org.flywaydb.core.api.output.CleanResult;
import org.flywaydb.core.api.output.MigrateResult;
import org.flywaydb.core.api.output.RepairResult;
import org.flywaydb.core.api.output.ValidateResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Flyway-based migration engine implementation.
 *
 * <p>Wraps Flyway library to provide a unified migration API.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Create with defaults
 * MigrationEngine engine = FlywayMigrationEngine.create(dataSource);
 *
 * // Create with custom configuration
 * MigrationEngine engine = FlywayMigrationEngine.builder()
 *     .dataSource(dataSource)
 *     .locations("classpath:db/migration", "filesystem:/migrations")
 *     .table("schema_version")
 *     .baselineVersion("1.0.0")
 *     .validateOnMigrate(true)
 *     .outOfOrder(false)
 *     .build();
 *
 * // Run migrations
 * Result<MigrationResult> result = engine.migrate();
 * }</pre>
 *
 * @see MigrationEngine
 */
public class FlywayMigrationEngine implements MigrationEngine {

  private static final Logger logger = LoggerFactory.getLogger(FlywayMigrationEngine.class);

  private final Flyway flyway;

  /**
   * Creates a Flyway migration engine.
   *
   * @param flyway configured Flyway instance
   */
  public FlywayMigrationEngine(Flyway flyway) {
    this.flyway = Objects.requireNonNull(flyway, "flyway cannot be null");
  }

  /**
   * Creates a Flyway migration engine with default configuration.
   *
   * @param dataSource the data source
   * @return migration engine
   */
  public static FlywayMigrationEngine create(DataSource dataSource) {
    Flyway flyway =
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load();
    return new FlywayMigrationEngine(flyway);
  }

  /**
   * Creates a new builder for configuring Flyway.
   *
   * @return builder
   */
  public static Builder builder() {
    return new Builder();
  }

  @Override
  public Result<MigrationResult> migrate() {
    try {
      logger.info("Starting database migration with Flyway");
      MigrateResult result = flyway.migrate();

      List<String> warnings = new ArrayList<>();
      if (result.warnings != null && !result.warnings.isEmpty()) {
        result.warnings.forEach(warnings::add);
      }

      MigrationResult migrationResult =
          new MigrationResult(
              result.migrationsExecuted,
              result.targetSchemaVersion != null ? result.targetSchemaVersion : "0",
              result.success,
              warnings);

      logger.info(
          "Migration completed. Applied {} migrations. Target version: {}",
          result.migrationsExecuted,
          result.targetSchemaVersion);

      return Result.ok(migrationResult);

    } catch (Exception e) {
      logger.error("Migration failed", e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("MIGRATION_FAILED"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Database migration failed: " + e.getMessage()));
    }
  }

  @Override
  public Result<Void> validate() {
    try {
      logger.info("Validating migrations");
      ValidateResult result = flyway.validateWithResult();

      if (!result.validationSuccessful) {
        StringBuilder errorMsg = new StringBuilder("Migration validation failed:");
        if (result.invalidMigrations != null) {
          result.invalidMigrations.forEach(
              invalid -> errorMsg.append("\n  - ").append(invalid.errorDetails.errorMessage));
        }

        return Result.fail(
            Problem.of(
                ErrorCode.of("VALIDATION_FAILED"),
                ErrorCategory.TECHNICAL,
                Severity.ERROR,
                errorMsg.toString()));
      }

      logger.info("Migration validation successful");
      return Result.ok(null);

    } catch (Exception e) {
      logger.error("Validation failed", e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("VALIDATION_ERROR"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Migration validation error: " + e.getMessage()));
    }
  }

  @Override
  public List<MigrationInfo> info() {
    MigrationInfoService infoService = flyway.info();
    org.flywaydb.core.api.MigrationInfo[] migrations = infoService.all();

    return Arrays.stream(migrations)
        .map(
            m -> {
              MigrationState state = mapState(m.getState());
              Instant installedOn =
                  m.getInstalledOn() != null ? m.getInstalledOn().toInstant() : null;
              Integer executionTime = m.getExecutionTime();

              String checksum = m.getChecksum() != null ? String.valueOf(m.getChecksum()) : null;

              return new MigrationInfo(
                  m.getVersion() != null ? m.getVersion().getVersion() : "0",
                  m.getDescription(),
                  m.getType().toString(),
                  state,
                  installedOn,
                  executionTime,
                  checksum);
            })
        .toList();
  }

  @Override
  public Result<Void> clean() {
    try {
      logger.warn("Cleaning database - all objects will be dropped!");
      CleanResult result = flyway.clean();

      if (result.warnings != null && !result.warnings.isEmpty()) {
        result.warnings.forEach(w -> logger.warn("Clean warning: {}", w));
      }

      logger.info("Database cleaned. Dropped {} schemas", result.schemasCleaned.size());
      return Result.ok(null);

    } catch (Exception e) {
      logger.error("Clean failed", e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("CLEAN_FAILED"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Database clean failed: " + e.getMessage()));
    }
  }

  @Override
  public Result<Void> repair() {
    try {
      logger.info("Repairing migration history");
      RepairResult result = flyway.repair();

      logger.info(
          "Repair completed. Removed {} failed migrations", result.migrationsRemoved.size());
      return Result.ok(null);

    } catch (Exception e) {
      logger.error("Repair failed", e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("REPAIR_FAILED"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Migration repair failed: " + e.getMessage()));
    }
  }

  @Override
  public Result<Void> baseline(String version, String description) {
    try {
      logger.info("Creating baseline at version {} - {}", version, description);
      BaselineResult result = flyway.baseline();

      logger.info("Baseline created successfully at version {}", result.baselineVersion);
      return Result.ok(null);

    } catch (Exception e) {
      logger.error("Baseline failed", e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("BASELINE_FAILED"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Baseline creation failed: " + e.getMessage()));
    }
  }

  private MigrationState mapState(org.flywaydb.core.api.MigrationState flywayState) {
    return switch (flywayState) {
      case PENDING, ABOVE_TARGET, IGNORED -> MigrationState.PENDING;
      case SUCCESS -> MigrationState.SUCCESS;
      case FAILED -> MigrationState.FAILED;
      case BASELINE -> MigrationState.BASELINE;
      case OUTDATED, SUPERSEDED -> MigrationState.OUTDATED;
      default -> MigrationState.PENDING;
    };
  }

  /** Builder for FlywayMigrationEngine. */
  public static class Builder {
    private DataSource dataSource;
    private List<String> locations = List.of("classpath:db/migration");
    private String table = "flyway_schema_history";
    private String baselineVersion = "1";
    private String baselineDescription = "Initial baseline";
    private boolean validateOnMigrate = true;
    private boolean outOfOrder = false;
    private boolean cleanDisabled = false;

    public Builder dataSource(DataSource dataSource) {
      this.dataSource = dataSource;
      return this;
    }

    public Builder locations(String... locations) {
      this.locations = List.of(locations);
      return this;
    }

    public Builder table(String table) {
      this.table = table;
      return this;
    }

    public Builder baselineVersion(String baselineVersion) {
      this.baselineVersion = baselineVersion;
      return this;
    }

    public Builder baselineDescription(String baselineDescription) {
      this.baselineDescription = baselineDescription;
      return this;
    }

    public Builder validateOnMigrate(boolean validateOnMigrate) {
      this.validateOnMigrate = validateOnMigrate;
      return this;
    }

    public Builder outOfOrder(boolean outOfOrder) {
      this.outOfOrder = outOfOrder;
      return this;
    }

    public Builder cleanDisabled(boolean cleanDisabled) {
      this.cleanDisabled = cleanDisabled;
      return this;
    }

    public FlywayMigrationEngine build() {
      Objects.requireNonNull(dataSource, "dataSource is required");

      Flyway flyway =
          Flyway.configure()
              .dataSource(dataSource)
              .locations(locations.toArray(new String[0]))
              .table(table)
              .baselineVersion(baselineVersion)
              .baselineDescription(baselineDescription)
              .validateOnMigrate(validateOnMigrate)
              .outOfOrder(outOfOrder)
              .cleanDisabled(cleanDisabled)
              .load();

      return new FlywayMigrationEngine(flyway);
    }
  }
}
