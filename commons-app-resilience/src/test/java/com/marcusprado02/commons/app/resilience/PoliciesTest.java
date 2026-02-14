package com.marcusprado02.commons.app.resilience;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class PoliciesTest {

  @Test
  void bulkheadPolicyShouldValidate() {
    assertThrows(IllegalArgumentException.class, () -> new BulkheadPolicy(0, Duration.ZERO));
    assertThrows(
        IllegalArgumentException.class, () -> new BulkheadPolicy(1, Duration.ofSeconds(-1)));
    assertNotNull(new BulkheadPolicy(1, Duration.ZERO));
  }

  @Test
  void rateLimiterPolicyShouldValidate() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new RateLimiterPolicy(0, Duration.ofSeconds(1), Duration.ZERO));
    assertThrows(
        IllegalArgumentException.class,
        () -> new RateLimiterPolicy(1, Duration.ZERO, Duration.ZERO));
    assertThrows(
        IllegalArgumentException.class,
        () -> new RateLimiterPolicy(1, Duration.ofSeconds(1), Duration.ofSeconds(-1)));
  }

  @Test
  void cachePolicyShouldValidate() {
    assertThrows(IllegalArgumentException.class, () -> new CachePolicy(0, Duration.ofSeconds(1)));
    assertThrows(IllegalArgumentException.class, () -> new CachePolicy(10, Duration.ZERO));
  }
}
