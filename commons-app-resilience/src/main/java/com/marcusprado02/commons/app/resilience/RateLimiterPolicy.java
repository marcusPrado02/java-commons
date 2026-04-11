package com.marcusprado02.commons.app.resilience;

import java.time.Duration;

/** Configuration policy for rate limiting a resource or operation. */
public record RateLimiterPolicy(int limitForPeriod, Duration refreshPeriod, Duration timeout) {

  /** Validates rate limiter policy fields. */
  public RateLimiterPolicy {
    if (limitForPeriod <= 0) {
      throw new IllegalArgumentException("limitForPeriod must be > 0");
    }
    if (refreshPeriod == null || refreshPeriod.isNegative() || refreshPeriod.isZero()) {
      throw new IllegalArgumentException("refreshPeriod must be > 0");
    }
    timeout = (timeout == null) ? Duration.ZERO : timeout;
    if (timeout.isNegative()) {
      throw new IllegalArgumentException("timeout must be >= 0");
    }
  }
}
