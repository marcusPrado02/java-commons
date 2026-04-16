package com.marcusprado02.commons.app.health.indicators;

import com.marcusprado02.commons.app.health.Health;
import com.marcusprado02.commons.app.health.HealthIndicator;

/**
 * Simple ping health indicator that always returns UP.
 *
 * <p>Useful as a basic liveness check.
 */
public final class PingHealthIndicator implements HealthIndicator {

  @Override
  public String name() {
    return "ping";
  }

  @Override
  public Health check() {
    return Health.up().build();
  }

  @Override
  public boolean isCritical() {
    return false;
  }
}
