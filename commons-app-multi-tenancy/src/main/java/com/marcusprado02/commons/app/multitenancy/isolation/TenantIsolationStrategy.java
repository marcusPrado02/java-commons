package com.marcusprado02.commons.app.multitenancy.isolation;

/**
 * Tenant isolation strategy.
 *
 * <p>Defines how data is isolated between tenants in a multi-tenant application.
 *
 * <p>Common strategies:
 *
 * <ul>
 *   <li><strong>Database per tenant</strong> - Each tenant has a separate database
 *   <li><strong>Schema per tenant</strong> - Each tenant has a separate schema in shared database
 *   <li><strong>Shared database with tenant ID</strong> - All tenants share database with tenant
 *       filtering
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Database per tenant
 * TenantIsolationStrategy strategy = new DatabaseIsolationStrategy(
 *     tenantId -> "jdbc:postgresql://localhost:5432/tenant_" + tenantId);
 *
 * // Schema per tenant
 * TenantIsolationStrategy strategy = new SchemaIsolationStrategy(
 *     "jdbc:postgresql://localhost:5432/shared_db",
 *     tenantId -> "tenant_" + tenantId);
 *
 * // Shared database
 * TenantIsolationStrategy strategy = new SharedDatabaseIsolationStrategy();
 * }</pre>
 */
public interface TenantIsolationStrategy {

  /**
   * Gets the isolation type.
   *
   * @return isolation type
   */
  IsolationType getIsolationType();

  /**
   * Applies tenant isolation for the given tenant.
   *
   * @param tenantId tenant ID
   */
  void applyIsolation(String tenantId);

  /**
   * Removes tenant isolation (cleanup).
   */
  void removeIsolation();

  enum IsolationType {
    /** Each tenant has a separate database. */
    DATABASE_PER_TENANT,

    /** Each tenant has a separate schema in shared database. */
    SCHEMA_PER_TENANT,

    /** All tenants share database with tenant ID filtering. */
    SHARED_DATABASE,

    /** Custom isolation strategy. */
    CUSTOM
  }
}
