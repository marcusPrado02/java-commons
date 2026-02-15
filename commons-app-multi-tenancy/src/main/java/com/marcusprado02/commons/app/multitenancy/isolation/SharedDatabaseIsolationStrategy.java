package com.marcusprado02.commons.app.multitenancy.isolation;

import javax.sql.DataSource;

/**
 * Shared database tenant isolation strategy.
 *
 * <p>All tenants share the same database and schema. Isolation is achieved through application-level
 * filtering using tenant ID in queries.
 *
 * <p>This strategy provides the least isolation but is the most resource-efficient.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * SharedDatabaseIsolationStrategy strategy = new SharedDatabaseIsolationStrategy(dataSource);
 *
 * // Get same DataSource for all tenants
 * DataSource result = strategy.getDataSource();
 *
 * // Current tenant ID can be used in queries:
 * // SELECT * FROM users WHERE tenant_id = ?
 * String tenantId = TenantContextHolder.getCurrentTenantId();
 * }</pre>
 */
public class SharedDatabaseIsolationStrategy implements DataSourceProvider {

  private final DataSource dataSource;

  public SharedDatabaseIsolationStrategy(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public DataSource getDataSource() {
    return dataSource;
  }
}
