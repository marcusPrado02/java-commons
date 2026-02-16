package com.marcusprado02.commons.app.datamigration.liquibase;

import com.marcusprado02.commons.app.datamigration.MigrationEngine;
import com.marcusprado02.commons.app.datamigration.MigrationInfo;
import com.marcusprado02.commons.app.datamigration.MigrationResult;
import com.marcusprado02.commons.kernel.errors.ErrorCategory;
import com.marcusprado02.commons.kernel.errors.ErrorCode;
import com.marcusprado02.commons.kernel.errors.Problem;
import com.marcusprado02.commons.kernel.errors.Severity;
import com.marcusprado02.commons.kernel.result.Result;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.sql.DataSource;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.RanChangeSet;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.ResourceAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Liquibase-based migration engine implementation.
 *
 * <p>Wraps Liquibase library to provide a unified migration API.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Create with defaults
 * MigrationEngine engine = LiquibaseMigrationEngine.create(dataSource);
 *
 * // Create with custom configuration
 * MigrationEngine engine = LiquibaseMigrationEngine.builder()
 *     .dataSource(dataSource)
 *     .changeLogFile("db/changelog/db.changelog-master.xml")
 *     .contexts("development", "test")
 *     .labels("feature-1", "feature-2")
 *     .build();
 *
 * // Run migrations
 * Result<MigrationResult> result = engine.migrate();
 * }</pre>
 *
 * @see MigrationEngine
 */
public class LiquibaseMigrationEngine implements MigrationEngine {

  private static final Logger logger = LoggerFactory.getLogger(LiquibaseMigrationEngine.class);

  private final DataSource dataSource;
  private final String changeLogFile;
  private final ResourceAccessor resourceAccessor;
  private final Contexts contexts;
  private final LabelExpression labels;

  /**
   * Creates a Liquibase migration engine.
   *
   * @param dataSource the data source
   * @param changeLogFile path to the changelog file
   * @param resourceAccessor resource accessor for loading changelogs
   * @param contexts migration contexts
   * @param labels migration labels
   */
  public LiquibaseMigrationEngine(
      DataSource dataSource,
      String changeLogFile,
      ResourceAccessor resourceAccessor,
      Contexts contexts,
      LabelExpression labels) {
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource cannot be null");
    this.changeLogFile = Objects.requireNonNull(changeLogFile, "changeLogFile cannot be null");
    this.resourceAccessor =
        Objects.requireNonNull(resourceAccessor, "resourceAccessor cannot be null");
    this.contexts = contexts;
    this.labels = labels;
  }

  /**
   * Creates a Liquibase migration engine with default configuration.
   *
   * @param dataSource the data source
   * @return migration engine
   */
  public static LiquibaseMigrationEngine create(DataSource dataSource) {
    return builder().dataSource(dataSource).build();
  }

  /**
   * Creates a new builder for configuring Liquibase.
   *
   * @return builder
   */
  public static Builder builder() {
    return new Builder();
  }

  @Override
  public Result<MigrationResult> migrate() {
    try (Connection connection = dataSource.getConnection()) {
      logger.info("Starting database migration with Liquibase");

      Database database =
          DatabaseFactory.getInstance()
              .findCorrectDatabaseImplementation(new JdbcConnection(connection));

      try (Liquibase liquibase = new Liquibase(changeLogFile, resourceAccessor, database)) {
        int pendingChanges = liquibase.listUnrunChangeSets(contexts, labels).size();

        liquibase.update(contexts, labels);

        String currentVersion = getCurrentVersion(liquibase);

        logger.info("Migration completed. Applied {} migrations", pendingChanges);

        return Result.ok(MigrationResult.success(pendingChanges, currentVersion));
      }

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
    try (Connection connection = dataSource.getConnection()) {
      logger.info("Validating migrations");

      Database database =
          DatabaseFactory.getInstance()
              .findCorrectDatabaseImplementation(new JdbcConnection(connection));

      try (Liquibase liquibase = new Liquibase(changeLogFile, resourceAccessor, database)) {
        liquibase.validate();
        logger.info("Migration validation successful");
        return Result.ok(null);
      }

    } catch (Exception e) {
      logger.error("Validation failed", e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("VALIDATION_FAILED"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Migration validation failed: " + e.getMessage()));
    }
  }

  @Override
  public List<MigrationInfo> info() {
    try (Connection connection = dataSource.getConnection()) {
      Database database =
          DatabaseFactory.getInstance()
              .findCorrectDatabaseImplementation(new JdbcConnection(connection));

      try (Liquibase liquibase = new Liquibase(changeLogFile, resourceAccessor, database)) {
        List<MigrationInfo> migrations = new ArrayList<>();

        // Get executed migrations
        List<RanChangeSet> ranChangeSets = liquibase.getDatabase().getRanChangeSetList();
        for (RanChangeSet ran : ranChangeSets) {
          migrations.add(
              MigrationInfo.success(
                  ran.getId(),
                  ran.getDescription() != null ? ran.getDescription() : "",
                  "SQL",
                  ran.getDateExecuted().toInstant(),
                  ran.getExecType() != null ? 0 : 0, // Liquibase doesn't track execution time
                  ran.getLastCheckSum() != null ? ran.getLastCheckSum().toString() : null));
        }

        // Get pending migrations
        List<ChangeSet> unrunChangeSets = liquibase.listUnrunChangeSets(contexts, labels);
        for (ChangeSet changeSet : unrunChangeSets) {
          migrations.add(
              MigrationInfo.pending(
                  changeSet.getId(),
                  changeSet.getDescription() != null ? changeSet.getDescription() : "",
                  "SQL",
                  null));
        }

        return migrations;
      }

    } catch (Exception e) {
      logger.error("Failed to get migration info", e);
      return List.of();
    }
  }

  @Override
  public Result<Void> clean() {
    try (Connection connection = dataSource.getConnection()) {
      logger.warn("Cleaning database - all objects will be dropped!");

      Database database =
          DatabaseFactory.getInstance()
              .findCorrectDatabaseImplementation(new JdbcConnection(connection));

      try (Liquibase liquibase = new Liquibase(changeLogFile, resourceAccessor, database)) {
        liquibase.dropAll();
        logger.info("Database cleaned");
        return Result.ok(null);
      }

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
    try (Connection connection = dataSource.getConnection()) {
      logger.info("Repairing migration history");

      Database database =
          DatabaseFactory.getInstance()
              .findCorrectDatabaseImplementation(new JdbcConnection(connection));

      try (Liquibase liquibase = new Liquibase(changeLogFile, resourceAccessor, database)) {
        liquibase.clearCheckSums();
        logger.info("Repair completed - checksums cleared");
        return Result.ok(null);
      }

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
    try (Connection connection = dataSource.getConnection()) {
      logger.info("Creating baseline at version {} - {}", version, description);

      Database database =
          DatabaseFactory.getInstance()
              .findCorrectDatabaseImplementation(new JdbcConnection(connection));

      try (Liquibase liquibase = new Liquibase(changeLogFile, resourceAccessor, database)) {
        // Mark all changes as executed up to the baseline version
        liquibase.changeLogSync(contexts, labels);

        logger.info("Baseline created successfully");
        return Result.ok(null);
      }

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

  private String getCurrentVersion(Liquibase liquibase) {
    try {
      List<RanChangeSet> ranChangeSets = liquibase.getDatabase().getRanChangeSetList();
      if (ranChangeSets.isEmpty()) {
        return "0";
      }
      RanChangeSet last = ranChangeSets.get(ranChangeSets.size() - 1);
      return last.getId();
    } catch (Exception e) {
      return "0";
    }
  }

  /** Builder for LiquibaseMigrationEngine. */
  public static class Builder {
    private DataSource dataSource;
    private String changeLogFile = "db/changelog/db.changelog-master.xml";
    private ResourceAccessor resourceAccessor = new ClassLoaderResourceAccessor();
    private List<String> contextsList = new ArrayList<>();
    private List<String> labelsList = new ArrayList<>();

    public Builder dataSource(DataSource dataSource) {
      this.dataSource = dataSource;
      return this;
    }

    public Builder changeLogFile(String changeLogFile) {
      this.changeLogFile = changeLogFile;
      return this;
    }

    public Builder resourceAccessor(ResourceAccessor resourceAccessor) {
      this.resourceAccessor = resourceAccessor;
      return this;
    }

    public Builder contexts(String... contexts) {
      this.contextsList = List.of(contexts);
      return this;
    }

    public Builder labels(String... labels) {
      this.labelsList = List.of(labels);
      return this;
    }

    public LiquibaseMigrationEngine build() {
      Objects.requireNonNull(dataSource, "dataSource is required");

      Contexts contexts =
          contextsList.isEmpty()
              ? new Contexts()
              : new Contexts(contextsList.toArray(new String[0]));

      LabelExpression labels =
          labelsList.isEmpty()
              ? new LabelExpression()
              : new LabelExpression(String.join(",", labelsList));

      return new LiquibaseMigrationEngine(
          dataSource, changeLogFile, resourceAccessor, contexts, labels);
    }
  }
}
