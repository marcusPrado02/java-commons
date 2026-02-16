/**
 * Database migration framework with support for Flyway, Liquibase, and custom migrations.
 *
 * <p>Key components:
 *
 * <ul>
 *   <li>{@link com.marcusprado02.commons.app.datamigration.MigrationEngine} - Main migration
 *       interface
 *   <li>{@link com.marcusprado02.commons.app.datamigration.flyway.FlywayMigrationEngine} - Flyway
 *       implementation
 *   <li>{@link com.marcusprado02.commons.app.datamigration.liquibase.LiquibaseMigrationEngine} -
 *       Liquibase implementation
 *   <li>{@link com.marcusprado02.commons.app.datamigration.custom.CustomMigrationEngine} - Custom
 *       Java migrations
 * </ul>
 *
 * <p><b>Quick Start with Flyway:</b>
 *
 * <pre>{@code
 * MigrationEngine engine = FlywayMigrationEngine.create(dataSource);
 * Result<MigrationResult> result = engine.migrate();
 *
 * if (result.isOk()) {
 *     MigrationResult migrationResult = result.getOrNull();
 *     System.out.println("Applied " + migrationResult.migrationsExecuted() + " migrations");
 * }
 * }</pre>
 *
 * <p><b>Quick Start with Liquibase:</b>
 *
 * <pre>{@code
 * MigrationEngine engine = LiquibaseMigrationEngine.builder()
 *     .dataSource(dataSource)
 *     .changeLogFile("db/changelog/db.changelog-master.xml")
 *     .build();
 *
 * Result<MigrationResult> result = engine.migrate();
 * }</pre>
 *
 * <p><b>Custom Migrations:</b>
 *
 * <pre>{@code
 * public class V1_2__InsertDefaultData implements CustomMigration {
 *     public void migrate(Connection conn) throws Exception {
 *         // Your migration logic
 *     }
 *
 *     public String getVersion() { return "1.2"; }
 *     public String getDescription() { return "Insert default data"; }
 * }
 *
 * MigrationEngine engine = CustomMigrationEngine.builder()
 *     .dataSource(dataSource)
 *     .migration(new V1_2__InsertDefaultData())
 *     .build();
 * }</pre>
 */
package com.marcusprado02.commons.app.datamigration;
