package com.marcusprado02.commons.app.multitenancy.isolation;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Database-per-tenant isolation strategy with connection pooling.
 *
 * <p>Each tenant has a separate database with its own connection pool. This provides the strongest
 * isolation but requires more resources.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * DatabaseIsolationStrategy strategy = new DatabaseIsolationStrategy(
 *     tenantId -> "jdbc:postgresql://localhost:5432/tenant_" + tenantId,
 *     "username",
 *     "password");
 *
 * // Apply isolation (creates/gets connection pool for tenant)
 * strategy.applyIsolation("tenant123");
 *
 * // Get DataSource for current tenant
 * DataSource dataSource = strategy.getCurrentDataSource();
 *
 * // Cleanup
 * strategy.removeIsolation();
 * }</pre>
 */
public class DatabaseIsolationStrategy implements DataSourceProvider, TenantIsolationStrategy {

  private static final ThreadLocal<String> currentTenant = new ThreadLocal<>();
  private final Function<String, String> urlGenerator;
  private final String username;
  private final String password;
  private final HikariConfig baseConfig;
  private final Map<String, HikariDataSource> dataSources = new ConcurrentHashMap<>();

  public DatabaseIsolationStrategy(
      Function<String, String> urlGenerator, String username, String password) {
    this.urlGenerator = urlGenerator;
    this.username = username;
    this.password = password;
    this.baseConfig = createBaseConfig();
  }

  public DatabaseIsolationStrategy(
      Function<String, String> urlGenerator,
      String username,
      String password,
      HikariConfig baseConfig) {
    this.urlGenerator = urlGenerator;
    this.username = username;
    this.password = password;
    this.baseConfig = baseConfig;
  }

  @Override
  public TenantIsolationStrategy.IsolationType getIsolationType() {
    return TenantIsolationStrategy.IsolationType.DATABASE_PER_TENANT;
  }

  @Override
  public void applyIsolation(String tenantId) {
    currentTenant.set(tenantId);
    // Create DataSource if it doesn't exist
    getDataSource(tenantId);
  }

  @Override
  public void removeIsolation() {
    currentTenant.remove();
  }

  @Override
  public DataSource getDataSource() {
    String tenantId = currentTenant.get();
    if (tenantId == null) {
      throw new IllegalStateException("No tenant context available");
    }
    return getDataSource(tenantId);
  }

  /**
   * Gets the DataSource for the specified tenant.
   *
   * @param tenantId tenant ID
   * @return DataSource for the tenant
   */
  public DataSource getDataSource(String tenantId) {
    return dataSources.computeIfAbsent(tenantId, this::createDataSource);
  }

  /**
   * Gets the DataSource for the current tenant.
   *
   * @return DataSource for current tenant or null if no tenant is active
   */
  public DataSource getCurrentDataSource() {
    String tenantId = currentTenant.get();
    return tenantId != null ? getDataSource(tenantId) : null;
  }

  /**
   * Closes and removes the DataSource for the specified tenant.
   *
   * @param tenantId tenant ID
   */
  public void removeTenant(String tenantId) {
    HikariDataSource dataSource = dataSources.remove(tenantId);
    if (dataSource != null) {
      dataSource.close();
    }
  }

  /**
   * Closes all DataSources.
   */
  public void shutdown() {
    dataSources.values().forEach(HikariDataSource::close);
    dataSources.clear();
  }

  /**
   * Gets statistics for all tenant pools.
   *
   * @return pool statistics by tenant ID
   */
  public Map<String, PoolStats> getPoolStats() {
    Map<String, PoolStats> stats = new ConcurrentHashMap<>();
    dataSources.forEach(
        (tenantId, dataSource) -> {
          stats.put(
              tenantId,
              new PoolStats(
                  dataSource.getHikariPoolMXBean().getTotalConnections(),
                  dataSource.getHikariPoolMXBean().getActiveConnections(),
                  dataSource.getHikariPoolMXBean().getIdleConnections()));
        });
    return stats;
  }

  private HikariDataSource createDataSource(String tenantId) {
    HikariConfig config = new HikariConfig();

    // Copy base configuration
    if (baseConfig != null) {
      config.copyStateTo(config);
    }

    // Set tenant-specific URL
    config.setJdbcUrl(urlGenerator.apply(tenantId));
    config.setUsername(username);
    config.setPassword(password);

    // Set pool name for monitoring
    config.setPoolName("tenant-" + tenantId);

    return new HikariDataSource(config);
  }

  private HikariConfig createBaseConfig() {
    HikariConfig config = new HikariConfig();
    config.setMaximumPoolSize(10);
    config.setMinimumIdle(2);
    config.setConnectionTimeout(30_000);
    config.setIdleTimeout(600_000);
    config.setMaxLifetime(1_800_000);
    return config;
  }

  /**
   * Connection pool statistics.
   *
   * @param totalConnections total connections in pool
   * @param activeConnections active connections
   * @param idleConnections idle connections
   */
  public record PoolStats(int totalConnections, int activeConnections, int idleConnections) {}
}
