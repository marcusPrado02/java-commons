package com.marcusprado02.commons.app.resilience;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
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

  @Test
  void circuitBreakerPolicyAccessors() {
    CircuitBreakerPolicy policy = new CircuitBreakerPolicy(0.5f, 10);
    assertEquals(0.5f, policy.failureRateThreshold());
    assertEquals(10, policy.slidingWindowSize());
  }

  @Test
  void retryPolicyAccessors() {
    RetryPolicy policy = new RetryPolicy(3, Duration.ofMillis(100), Duration.ofSeconds(5));
    assertEquals(3, policy.maxAttempts());
    assertEquals(Duration.ofMillis(100), policy.initialBackoff());
    assertEquals(Duration.ofSeconds(5), policy.maxBackoff());
  }

  @Test
  void timeoutPolicyAccessors() {
    TimeoutPolicy policy = new TimeoutPolicy(Duration.ofSeconds(10));
    assertEquals(Duration.ofSeconds(10), policy.timeout());
  }

  @Test
  void resiliencePolicySetAccessors() {
    RetryPolicy retry = new RetryPolicy(1, Duration.ofMillis(10), Duration.ofMillis(100));
    ResiliencePolicySet policySet = new ResiliencePolicySet(retry, null, null, null, null, null);
    assertEquals(retry, policySet.retry());
    assertNull(policySet.timeout());
  }

  @Test
  void fallbackStrategyNoneRethrowsRuntimeException() {
    FallbackStrategy<String> fallback = FallbackStrategy.none();
    assertThrows(RuntimeException.class, () -> fallback.recover(new RuntimeException("boom")));
  }

  @Test
  void fallbackStrategyNoneRethrowsError() {
    FallbackStrategy<String> fallback = FallbackStrategy.none();
    assertThrows(Error.class, () -> fallback.recover(new Error("oops")));
  }

  @Test
  void fallbackStrategyNoneWrapsCheckedException() {
    FallbackStrategy<String> fallback = FallbackStrategy.none();
    RuntimeException ex =
        assertThrows(RuntimeException.class, () -> fallback.recover(new Exception("checked")));
    assertNotNull(ex.getCause());
  }

  @Test
  void fallbackStrategyValueReturnsConstant() {
    FallbackStrategy<String> fallback = FallbackStrategy.value("default");
    assertEquals("default", fallback.recover(new RuntimeException("x")));
  }

  @Test
  void fallbackStrategySupplierCallsSupplier() {
    FallbackStrategy<String> fallback = FallbackStrategy.supplier(() -> "from-supplier");
    assertEquals("from-supplier", fallback.recover(new RuntimeException("x")));
  }

  @Test
  void fallbackStrategySupplierRejectsNullSupplier() {
    assertThrows(NullPointerException.class, () -> FallbackStrategy.supplier(null));
  }

  @Test
  void noopExecutorRunsAction() {
    AtomicBoolean ran = new AtomicBoolean(false);
    new NoopResilienceExecutor()
        .run(
            "op", new ResiliencePolicySet(null, null, null, null, null, null), () -> ran.set(true));
    assertTrue(ran.get());
  }

  @Test
  void noopExecutorSuppliesValue() {
    String result =
        new NoopResilienceExecutor()
            .supply("op", new ResiliencePolicySet(null, null, null, null, null, null), () -> "ok");
    assertEquals("ok", result);
  }

  @Test
  void noopExecutorRejectsNullRunnable() {
    assertThrows(
        NullPointerException.class,
        () ->
            new NoopResilienceExecutor()
                .run("op", new ResiliencePolicySet(null, null, null, null, null, null), null));
  }
}
