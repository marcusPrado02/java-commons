package com.marcusprado02.commons.app.health;

/**
 * Health status of a component or system.
 *
 * <p>Represents the current state of a health check.
 */
public enum HealthStatus {

  /** The component is healthy and functioning normally. */
  UP,

  /** The component is experiencing issues and not functioning properly. */
  DOWN,

  /**
   * The component is functioning but with reduced capabilities or performance.
   *
   * <p>This state indicates that the system can continue operating but may have limitations.
   */
  DEGRADED,

  /** The health status cannot be determined. */
  UNKNOWN;

  /**
   * Checks if this status is considered healthy.
   *
   * @return true if UP or DEGRADED
   */
  public boolean isHealthy() {
    return this == UP || this == DEGRADED;
  }

  /**
   * Checks if this status indicates a problem.
   *
   * @return true if DOWN or UNKNOWN
   */
  public boolean isUnhealthy() {
    return this == DOWN || this == UNKNOWN;
  }
}
