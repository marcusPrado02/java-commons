package com.marcusprado02.commons.app.health;

/**
 * Interface for health check indicators.
 *
 * <p>Implementations perform health checks for specific components or dependencies.
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * public class DatabaseHealthIndicator implements HealthIndicator {
 *     private final DataSource dataSource;
 *
 *     @Override
 *     public String name() {
 *         return "database";
 *     }
 *
 *     @Override
 *     public Health check() {
 *         try (Connection conn = dataSource.getConnection()) {
 *             return Health.up()
 *                 .withDetail("database", conn.getMetaData().getDatabaseProductName())
 *                 .build();
 *         } catch (Exception e) {
 *             return Health.down()
 *                 .withException(e)
 *                 .build();
 *         }
 *     }
 * }
 * }</pre>
 */
public interface HealthIndicator {

  /**
   * Gets the name of this health indicator.
   *
   * @return indicator name
   */
  String name();

  /**
   * Performs the health check.
   *
   * @return health check result
   */
  Health check();

  /**
   * Indicates whether this check is critical.
   *
   * <p>If a critical check fails, the overall system health should be DOWN.
   *
   * @return true if critical, default is true
   */
  default boolean isCritical() {
    return true;
  }
}
