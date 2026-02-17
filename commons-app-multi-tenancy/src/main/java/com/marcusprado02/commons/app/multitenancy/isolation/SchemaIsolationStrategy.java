package com.marcusprado02.commons.app.multitenancy.isolation;

import com.marcusprado02.commons.app.multitenancy.TenantContextHolder;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Schema-based tenant isolation strategy.
 *
 * <p>Each tenant uses a separate schema in the same database. The strategy switches the default
 * schema based on the current tenant context.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * SchemaIsolationStrategy strategy = new SchemaIsolationStrategy(
 *     dataSource,
 *     tenantId -> "tenant_" + tenantId);
 *
 * // Apply isolation
 * strategy.applyIsolation("tenant123"); // Sets schema to "tenant_tenant123"
 *
 * // Use database operations...
 *
 * // Cleanup
 * strategy.removeIsolation();
 * }</pre>
 */
public class SchemaIsolationStrategy implements DataSourceProvider, TenantIsolationStrategy {

  private final DataSource dataSource;
  private final Function<String, String> schemaNameGenerator;
  private final Map<String, String> schemaCache = new ConcurrentHashMap<>();
  private final ThreadLocal<String> currentSchema = new ThreadLocal<>();

  public SchemaIsolationStrategy(
      DataSource dataSource, Function<String, String> schemaNameGenerator) {
    this.dataSource = dataSource;
    this.schemaNameGenerator = schemaNameGenerator;
  }

  public SchemaIsolationStrategy(DataSource dataSource) {
    this(dataSource, tenantId -> "tenant_" + tenantId);
  }

  @Override
  public TenantIsolationStrategy.IsolationType getIsolationType() {
    return TenantIsolationStrategy.IsolationType.SCHEMA_PER_TENANT;
  }

  @Override
  public DataSource getDataSource() {
    String tenantId = TenantContextHolder.getCurrentTenantId();
    if (tenantId == null) {
      throw new IllegalStateException("No tenant context available");
    }
    return dataSource;
  }

  @Override
  public void applyIsolation(String tenantId) {
    String schemaName = getSchemaName(tenantId);
    currentSchema.set(schemaName);

    // Set schema for current connection
    try (Connection connection = dataSource.getConnection()) {
      setSchema(connection, schemaName);
    } catch (SQLException e) {
      throw new TenantIsolationException("Failed to set schema for tenant: " + tenantId, e);
    }
  }

  @Override
  public void removeIsolation() {
    currentSchema.remove();
  }

  /**
   * Gets the current schema name for the active tenant.
   *
   * @return schema name or null if no tenant is active
   */
  public String getCurrentSchema() {
    String tenantId = TenantContextHolder.getCurrentTenantId();
    return tenantId != null ? getSchemaName(tenantId) : null;
  }

  /**
   * Creates schema for the given tenant if it doesn't exist.
   *
   * @param tenantId tenant ID
   */
  public void createSchemaIfNotExists(String tenantId) {
    String schemaName = getSchemaName(tenantId);

    try (Connection connection = dataSource.getConnection()) {
      String sql = "CREATE SCHEMA IF NOT EXISTS " + schemaName;
      connection.createStatement().execute(sql);
    } catch (SQLException e) {
      throw new TenantIsolationException("Failed to create schema for tenant: " + tenantId, e);
    }
  }

  /**
   * Drops schema for the given tenant.
   *
   * @param tenantId tenant ID
   */
  public void dropSchema(String tenantId) {
    String schemaName = getSchemaName(tenantId);

    try (Connection connection = dataSource.getConnection()) {
      String sql = "DROP SCHEMA IF EXISTS " + schemaName + " CASCADE";
      connection.createStatement().execute(sql);
      schemaCache.remove(tenantId);
    } catch (SQLException e) {
      throw new TenantIsolationException("Failed to drop schema for tenant: " + tenantId, e);
    }
  }

  private String getSchemaName(String tenantId) {
    return schemaCache.computeIfAbsent(tenantId, schemaNameGenerator);
  }

  private void setSchema(Connection connection, String schemaName) throws SQLException {
    // Try different database-specific approaches
    try {
      // PostgreSQL, H2
      connection.createStatement().execute("SET search_path TO " + schemaName);
    } catch (SQLException e) {
      try {
        // MySQL
        connection.createStatement().execute("USE " + schemaName);
      } catch (SQLException e2) {
        try {
          // Standard SQL (newer versions)
          connection.setSchema(schemaName);
        } catch (SQLException e3) {
          throw new SQLException("Unable to set schema: " + schemaName, e3);
        }
      }
    }
  }

  /**
   * Exception thrown when tenant isolation operations fail.
   */
  public static class TenantIsolationException extends RuntimeException {
    public TenantIsolationException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
