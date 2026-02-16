package com.marcusprado02.commons.app.datamigration.custom;

import java.sql.Connection;

/**
 * Interface for custom database migrations.
 *
 * <p>Allows executing Java-based migrations that can't be expressed in SQL.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * public class V1_2__InsertDefaultData implements CustomMigration {
 *
 *     @Override
 *     public void migrate(Connection connection) throws Exception {
 *         try (PreparedStatement stmt = connection.prepareStatement(
 *                 "INSERT INTO users (id, name, email) VALUES (?, ?, ?)")) {
 *             stmt.setLong(1, 1L);
 *             stmt.setString(2, "Admin");
 *             stmt.setString(3, "admin@example.com");
 *             stmt.executeUpdate();
 *         }
 *     }
 *
 *     @Override
 *     public String getVersion() {
 *         return "1.2";
 *     }
 *
 *     @Override
 *     public String getDescription() {
 *         return "Insert default data";
 *     }
 * }
 * }</pre>
 *
 * @see CustomMigrationEngine
 */
public interface CustomMigration {

  /**
   * Executes the migration.
   *
   * @param connection the database connection
   * @throws Exception if migration fails
   */
  void migrate(Connection connection) throws Exception;

  /**
   * Returns the migration version.
   *
   * @return version (e.g., "1.2", "2.0")
   */
  String getVersion();

  /**
   * Returns the migration description.
   *
   * @return description
   */
  String getDescription();

  /**
   * Whether this migration should run in a transaction.
   *
   * <p>Default is true. Set to false for migrations that can't run in a transaction (e.g., CREATE
   * INDEX CONCURRENTLY in PostgreSQL).
   *
   * @return true if should run in transaction
   */
  default boolean isTransactional() {
    return true;
  }
}
