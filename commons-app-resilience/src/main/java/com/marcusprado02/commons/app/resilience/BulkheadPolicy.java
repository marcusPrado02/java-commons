package com.marcusprado02.commons.app.resilience;

import java.time.Duration;

/** Configuration policy for limiting concurrent calls via a bulkhead pattern. */
public record BulkheadPolicy(int maxConcurrentCalls, Duration maxWaitDuration) {

  /** Validates bulkhead policy fields and applies defaults for optional values. */
  public BulkheadPolicy {
    if (maxConcurrentCalls <= 0) {
      throw new IllegalArgumentException("maxConcurrentCalls must be > 0");
    }
    maxWaitDuration = (maxWaitDuration == null) ? Duration.ZERO : maxWaitDuration;
    if (maxWaitDuration.isNegative()) {
      throw new IllegalArgumentException("maxWaitDuration must be >= 0");
    }
  }

  public static BulkheadPolicy of(int maxConcurrentCalls) {
    return new BulkheadPolicy(maxConcurrentCalls, Duration.ZERO);
  }
}
