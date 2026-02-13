package com.marcusprado02.commons.app.resilience;

import java.time.Duration;

public record CachePolicy(int maxSize, Duration ttl) {

  public CachePolicy {
    if (maxSize <= 0) {
      throw new IllegalArgumentException("maxSize must be > 0");
    }
    if (ttl == null || ttl.isNegative() || ttl.isZero()) {
      throw new IllegalArgumentException("ttl must be > 0");
    }
  }
}
