package com.marcusprado02.commons.app.datamigration;

/**
 * Result of a migration execution.
 *
 * @param migrationsExecuted number of migrations successfully executed
 * @param targetVersion the final schema version after migration
 * @param success whether the migration was successful
 * @param warnings list of warnings encountered during migration
 */
public record MigrationResult(
    int migrationsExecuted,
    String targetVersion,
    boolean success,
    java.util.List<String> warnings) {

  public MigrationResult {
    java.util.Objects.requireNonNull(targetVersion, "targetVersion cannot be null");
    java.util.Objects.requireNonNull(warnings, "warnings cannot be null");
  }

  /**
   * Creates a successful migration result.
   *
   * @param migrationsExecuted number of migrations executed
   * @param targetVersion target version
   * @return migration result
   */
  public static MigrationResult success(int migrationsExecuted, String targetVersion) {
    return new MigrationResult(migrationsExecuted, targetVersion, true, java.util.List.of());
  }

  /**
   * Creates a successful migration result with warnings.
   *
   * @param migrationsExecuted number of migrations executed
   * @param targetVersion target version
   * @param warnings list of warnings
   * @return migration result
   */
  public static MigrationResult successWithWarnings(
      int migrationsExecuted, String targetVersion, java.util.List<String> warnings) {
    return new MigrationResult(migrationsExecuted, targetVersion, true, warnings);
  }

  /**
   * Creates a failed migration result.
   *
   * @param migrationsExecuted number of migrations executed before failure
   * @param targetVersion target version reached before failure
   * @return migration result
   */
  public static MigrationResult failure(int migrationsExecuted, String targetVersion) {
    return new MigrationResult(migrationsExecuted, targetVersion, false, java.util.List.of());
  }
}
