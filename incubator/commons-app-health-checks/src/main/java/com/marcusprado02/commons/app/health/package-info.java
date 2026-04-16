/**
 * Advanced health check system for the commons library.
 *
 * <p>This module provides comprehensive health checking capabilities with support for custom
 * indicators, dependency monitoring, degraded state, and health aggregation.
 *
 * <h2>Core Concepts</h2>
 *
 * <ul>
 *   <li>{@link com.marcusprado02.commons.app.health.Health} - Health check result
 *   <li>{@link com.marcusprado02.commons.app.health.HealthStatus} - Status enum (UP, DOWN,
 *       DEGRADED, UNKNOWN)
 *   <li>{@link com.marcusprado02.commons.app.health.HealthIndicator} - Interface for health checks
 *   <li>{@link com.marcusprado02.commons.app.health.HealthAggregator} - Aggregates multiple checks
 *   <li>{@link com.marcusprado02.commons.app.health.CompositeHealth} - Aggregated health result
 * </ul>
 *
 * <h2>Quick Start</h2>
 *
 * <h3>Creating Health Checks</h3>
 *
 * <pre>{@code
 * // Simple health check
 * Health health = Health.up()
 *     .withDetail("version", "1.0.0")
 *     .build();
 *
 * // Health check with error
 * Health health = Health.down()
 *     .withException(new IOException("Connection failed"))
 *     .build();
 *
 * // Degraded state
 * Health health = Health.degraded()
 *     .withDetail("message", "Running with reduced capacity")
 *     .build();
 * }</pre>
 *
 * <h3>Custom Health Indicators</h3>
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
 *
 *     @Override
 *     public boolean isCritical() {
 *         return true; // Database is critical
 *     }
 * }
 * }</pre>
 *
 * <h3>Aggregating Health Checks</h3>
 *
 * <pre>{@code
 * HealthAggregator aggregator = new HealthAggregator(
 *     List.of(
 *         new DatabaseHealthIndicator(),
 *         new CacheHealthIndicator(),
 *         new DiskSpaceHealthIndicator()
 *     )
 * );
 *
 * CompositeHealth health = aggregator.aggregate();
 * System.out.println("Overall status: " + health.getStatus());
 *
 * health.getComponents().forEach((name, componentHealth) -> {
 *     System.out.println(name + ": " + componentHealth.getStatus());
 * });
 * }</pre>
 *
 * <h2>Built-in Health Indicators</h2>
 *
 * <p>The module includes several ready-to-use health indicators:
 *
 * <ul>
 *   <li>{@link com.marcusprado02.commons.app.health.indicators.PingHealthIndicator} - Simple
 *       liveness check
 *   <li>{@link com.marcusprado02.commons.app.health.indicators.DiskSpaceHealthIndicator} - Disk
 *       space monitoring
 *   <li>{@link com.marcusprado02.commons.app.health.indicators.MemoryHealthIndicator} - JVM memory
 *       monitoring
 *   <li>{@link com.marcusprado02.commons.app.health.indicators.UrlHealthIndicator} - HTTP endpoint
 *       connectivity
 * </ul>
 *
 * <h2>Health Status Levels</h2>
 *
 * <ul>
 *   <li>{@code UP} - Component is healthy and functioning normally
 *   <li>{@code DEGRADED} - Component is functioning but with reduced capabilities
 *   <li>{@code DOWN} - Component is not functioning properly
 *   <li>{@code UNKNOWN} - Health status cannot be determined
 * </ul>
 *
 * <h2>Aggregation Strategies</h2>
 *
 * <p>The module supports different strategies for aggregating health checks:
 *
 * <ul>
 *   <li>{@code WORST_STATUS} (default) - Overall status is the worst of all checks, with critical
 *       checks taking priority
 *   <li>{@code ALL_UP} - Overall status is UP only if all checks are UP
 * </ul>
 *
 * @see com.marcusprado02.commons.app.health.Health
 * @see com.marcusprado02.commons.app.health.HealthIndicator
 * @see com.marcusprado02.commons.app.health.HealthAggregator
 */
package com.marcusprado02.commons.app.health;
