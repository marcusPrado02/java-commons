package com.marcusprado02.commons.app.resilience;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class ResilienceBranchTest {

  private static final ResiliencePolicySet EMPTY =
      new ResiliencePolicySet(null, null, null, null, null, null);

  private final NoopResilienceExecutor executor = new NoopResilienceExecutor();

  // --- ResilienceExecutor default supply(4-arg) ---

  @Test
  void supply_4arg_success_returns_value() {
    String result = executor.supply("op", EMPTY, () -> "ok", FallbackStrategy.value("fallback"));
    assertEquals("ok", result);
  }

  @Test
  void supply_4arg_failure_uses_non_null_fallback() {
    String result =
        executor.supply(
            "op",
            EMPTY,
            () -> {
              throw new RuntimeException("boom");
            },
            FallbackStrategy.value("recovered"));
    assertEquals("recovered", result);
  }

  @Test
  void supply_4arg_null_fallback_defaults_to_none_and_rethrows() {
    assertThrows(
        RuntimeException.class,
        () ->
            executor.supply(
                "op",
                EMPTY,
                () -> {
                  throw new RuntimeException("boom");
                },
                null));
  }

  @Test
  void supply_4arg_null_action_throws() {
    assertThrows(
        NullPointerException.class,
        () -> executor.supply("op", EMPTY, null, FallbackStrategy.value("x")));
  }

  // --- ResilienceExecutor.supplyCached (3-arg and 4-arg) ---

  @Test
  void supplyCached_3arg_returns_action_result() {
    String result = executor.supplyCached("op", EMPTY, "key", () -> "cached");
    assertEquals("cached", result);
  }

  @Test
  void supplyCached_4arg_success_returns_value() {
    String result =
        executor.supplyCached("op", EMPTY, "key", () -> "cached", FallbackStrategy.value("fb"));
    assertEquals("cached", result);
  }

  @Test
  void supplyCached_4arg_failure_uses_fallback() {
    String result =
        executor.supplyCached(
            "op",
            EMPTY,
            "key",
            () -> {
              throw new RuntimeException("err");
            },
            FallbackStrategy.value("fallback"));
    assertEquals("fallback", result);
  }

  // --- NoopResilienceExecutor null action ---

  @Test
  void noopExecutor_supply_null_action_throws() {
    assertThrows(NullPointerException.class, () -> executor.supply("op", EMPTY, null));
  }

  // --- BulkheadPolicy branches ---

  @Test
  void bulkheadPolicy_of_factory_creates_with_zero_wait() {
    BulkheadPolicy p = BulkheadPolicy.of(5);
    assertEquals(5, p.maxConcurrentCalls());
    assertEquals(Duration.ZERO, p.maxWaitDuration());
  }

  @Test
  void bulkheadPolicy_null_maxWaitDuration_defaults_to_zero() {
    BulkheadPolicy p = new BulkheadPolicy(3, null);
    assertEquals(Duration.ZERO, p.maxWaitDuration());
  }

  // --- CachePolicy branches ---

  @Test
  void cachePolicy_valid() {
    CachePolicy p = new CachePolicy(10, Duration.ofSeconds(30));
    assertEquals(10, p.maxSize());
    assertEquals(Duration.ofSeconds(30), p.ttl());
  }

  @Test
  void cachePolicy_null_ttl_throws() {
    assertThrows(IllegalArgumentException.class, () -> new CachePolicy(10, null));
  }

  @Test
  void cachePolicy_negative_ttl_throws() {
    assertThrows(IllegalArgumentException.class, () -> new CachePolicy(10, Duration.ofSeconds(-1)));
  }

  // --- RateLimiterPolicy branches ---

  @Test
  void rateLimiterPolicy_null_timeout_defaults_to_zero() {
    RateLimiterPolicy p = new RateLimiterPolicy(10, Duration.ofSeconds(1), null);
    assertEquals(Duration.ZERO, p.timeout());
  }

  @Test
  void rateLimiterPolicy_null_refreshPeriod_throws() {
    assertThrows(
        IllegalArgumentException.class, () -> new RateLimiterPolicy(10, null, Duration.ZERO));
  }

  @Test
  void rateLimiterPolicy_negative_refreshPeriod_throws() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new RateLimiterPolicy(10, Duration.ofSeconds(-1), Duration.ZERO));
  }

  @Test
  void rateLimiterPolicy_valid() {
    RateLimiterPolicy p = new RateLimiterPolicy(5, Duration.ofSeconds(1), Duration.ofMillis(500));
    assertEquals(5, p.limitForPeriod());
    assertEquals(Duration.ofSeconds(1), p.refreshPeriod());
    assertEquals(Duration.ofMillis(500), p.timeout());
  }
}
