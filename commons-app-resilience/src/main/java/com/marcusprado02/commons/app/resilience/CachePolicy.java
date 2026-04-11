package com.marcusprado02.commons.app.resilience;

import java.time.Duration;

/** Configuration policy for caching operation results with a size limit and time-to-live. */
public record CachePolicy(int maxSize, Duration ttl) {

  /** Validates cache policy fields. */
  public CachePolicy {
    if (maxSize <= 0) {
      throw new IllegalArgumentException("maxSize must be > 0");
    }
    if (ttl == null || ttl.isNegative() || ttl.isZero()) {
      throw new IllegalArgumentException("ttl must be > 0");
    }
  }
}
