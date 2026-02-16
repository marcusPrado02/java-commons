package com.marcusprado02.commons.app.datamigration;

import com.marcusprado02.commons.kernel.result.Result;
import java.util.List;

/**
 * Interface for database migration operations.
 *
 * <p>Provides a unified API for executing database migrations regardless of the underlying
 * framework (Flyway, Liquibase, custom).
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * MigrationEngine engine = new FlywayMigrationEngine(dataSource);
 *
 * // Run migrations
 * Result<MigrationResult> result = engine.migrate();
 * if (result.isOk()) {
 *     MigrationResult migrationResult = result.getOrNull();
 *     System.out.println("Applied " + migrationResult.migrationsExecuted() + " migrations");
 * }
 *
 * // Get migration info
 * List<MigrationInfo> info = engine.info();
 * for (MigrationInfo migration : info) {
 *     System.out.println(migration.version() + " - " + migration.description());
 * }
 * }</pre>
 *
 * @see FlywayMigrationEngine
 * @see LiquibaseMigrationEngine
 */
public interface MigrationEngine {

  /**
   * Executes all pending migrations.
   *
   * @return result containing migration summary
   */
  Result<MigrationResult> migrate();

  /**
   * Validates the applied migrations against the available migrations.
   *
   * @return result indicating if migrations are valid
   */
  Result<Void> validate();

  /**
   * Retrieves information about all migrations.
   *
   * @return list of migration info
   */
  List<MigrationInfo> info();

  /**
   * Cleans the database by dropping all objects.
   *
   * <p><b>WARNING:</b> This will delete all data! Only use in development/test environments.
   *
   * @return result indicating success or failure
   */
  Result<Void> clean();

  /**
   * Repairs the migration history table.
   *
   * <p>Useful when migrations have been manually fixed or deleted.
   *
   * @return result indicating success or failure
   */
  Result<Void> repair();

  /**
   * Baselines the database at a specific version.
   *
   * <p>Useful for existing databases that need to start using migrations.
   *
   * @param version the baseline version
   * @param description description for the baseline
   * @return result indicating success or failure
   */
  Result<Void> baseline(String version, String description);

  /**
   * Checks if there are pending migrations to be applied.
   *
   * @return true if there are pending migrations
   */
  default boolean hasPendingMigrations() {
    return info().stream().anyMatch(m -> m.state() == MigrationState.PENDING);
  }

  /**
   * Returns the current schema version.
   *
   * @return current version or empty if no migrations applied
   */
  default String currentVersion() {
    return info().stream()
        .filter(m -> m.state() == MigrationState.SUCCESS)
        .reduce((first, second) -> second)
        .map(MigrationInfo::version)
        .orElse("0");
  }
}
