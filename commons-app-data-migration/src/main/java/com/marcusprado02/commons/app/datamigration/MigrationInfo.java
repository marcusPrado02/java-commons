package com.marcusprado02.commons.app.datamigration;

import java.time.Instant;

/**
 * Information about a single database migration.
 *
 * @param version the migration version
 * @param description the migration description
 * @param type the migration type (SQL, JAVA, etc.)
 * @param state the current state of the migration
 * @param installedOn when the migration was installed (null if pending)
 * @param executionTime execution time in milliseconds (null if pending)
 * @param checksum the migration file checksum
 */
public record MigrationInfo(
    String version,
    String description,
    String type,
    MigrationState state,
    Instant installedOn,
    Integer executionTime,
    String checksum) {

  public MigrationInfo {
    java.util.Objects.requireNonNull(version, "version cannot be null");
    java.util.Objects.requireNonNull(description, "description cannot be null");
    java.util.Objects.requireNonNull(type, "type cannot be null");
    java.util.Objects.requireNonNull(state, "state cannot be null");
  }

  /**
   * Creates pending migration info.
   *
   * @param version migration version
   * @param description migration description
   * @param type migration type
   * @param checksum migration checksum
   * @return migration info
   */
  public static MigrationInfo pending(
      String version, String description, String type, String checksum) {
    return new MigrationInfo(
        version, description, type, MigrationState.PENDING, null, null, checksum);
  }

  /**
   * Creates applied migration info.
   *
   * @param version migration version
   * @param description migration description
   * @param type migration type
   * @param installedOn installation timestamp
   * @param executionTime execution time in milliseconds
   * @param checksum migration checksum
   * @return migration info
   */
  public static MigrationInfo success(
      String version,
      String description,
      String type,
      Instant installedOn,
      int executionTime,
      String checksum) {
    return new MigrationInfo(
        version, description, type, MigrationState.SUCCESS, installedOn, executionTime, checksum);
  }

  /**
   * Creates failed migration info.
   *
   * @param version migration version
   * @param description migration description
   * @param type migration type
   * @param installedOn installation timestamp
   * @param checksum migration checksum
   * @return migration info
   */
  public static MigrationInfo failed(
      String version, String description, String type, Instant installedOn, String checksum) {
    return new MigrationInfo(
        version, description, type, MigrationState.FAILED, installedOn, null, checksum);
  }

  /** Whether this migration has been applied. */
  public boolean isApplied() {
    return state == MigrationState.SUCCESS;
  }

  /** Whether this migration is pending. */
  public boolean isPending() {
    return state == MigrationState.PENDING;
  }

  /** Whether this migration failed. */
  public boolean isFailed() {
    return state == MigrationState.FAILED;
  }
}
