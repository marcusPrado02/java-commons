package com.marcusprado02.commons.app.multitenancy.isolation;

import javax.sql.DataSource;

/**
 * Interface for strategies that provide tenant-specific DataSources.
 *
 * <p>This interface complements {@link TenantIsolationStrategy} for cases where database access
 * requires different DataSources per tenant or tenant-specific configurations.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Database-per-tenant strategy
 * DataSourceProvider provider = new DatabaseIsolationStrategy(urlGenerator, "user", "pass");
 * DataSource dataSource = provider.getDataSource();
 *
 * // Schema-per-tenant strategy
 * DataSourceProvider provider = new SchemaIsolationStrategy(sharedDataSource);
 * DataSource dataSource = provider.getDataSource();
 * }</pre>
 */
public interface DataSourceProvider {

  /**
   * Gets the DataSource for the current tenant context.
   *
   * <p>Uses the tenant ID from {@link com.marcusprado02.commons.app.multitenancy.TenantContextHolder}
   * to determine which DataSource to return.
   *
   * @return tenant-specific DataSource
   * @throws IllegalStateException if no tenant context is available
   */
  DataSource getDataSource();

  /**
   * Cleanup resources when the provider is no longer needed.
   *
   * <p>This method should close connection pools and release other resources.
   */
  default void destroy() {
    // Default implementation does nothing
  }
}
