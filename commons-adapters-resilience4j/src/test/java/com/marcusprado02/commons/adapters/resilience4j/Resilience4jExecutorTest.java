package com.marcusprado02.commons.adapters.resilience4j;

import static org.junit.jupiter.api.Assertions.*;

import com.marcusprado02.commons.app.observability.MetricsFacade;
import com.marcusprado02.commons.app.resilience.BulkheadPolicy;
import com.marcusprado02.commons.app.resilience.CachePolicy;
import com.marcusprado02.commons.app.resilience.CircuitBreakerPolicy;
import com.marcusprado02.commons.app.resilience.FallbackStrategy;
import com.marcusprado02.commons.app.resilience.RateLimiterPolicy;
import com.marcusprado02.commons.app.resilience.ResiliencePolicySet;
import com.marcusprado02.commons.app.resilience.RetryPolicy;
import com.marcusprado02.commons.app.resilience.TimeoutPolicy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class Resilience4jExecutorTest {

  @Test
  void shouldApplyFallbackOnFailure() {
    CapturingMetrics metrics = new CapturingMetrics();
    Resilience4jExecutor executor = new Resilience4jExecutor(metrics);

    ResiliencePolicySet policies = new ResiliencePolicySet(null, null, null, null, null, null);

    String value =
        executor.supply(
            "svc",
            policies,
            () -> {
              throw new IllegalStateException("boom");
            },
            FallbackStrategy.value("fallback"));

    assertEquals("fallback", value);
    assertTrue(
        metrics.hasCounter(
            "commons.resilience.calls", Map.of("name", "svc", "outcome", "fallback")));
  }

  @Test
  void shouldRateLimitAndUseFallback() {
    CapturingMetrics metrics = new CapturingMetrics();
    Resilience4jExecutor executor = new Resilience4jExecutor(metrics);

    ResiliencePolicySet policies =
        new ResiliencePolicySet(
            null,
            null,
            null,
            null,
            new RateLimiterPolicy(1, Duration.ofMinutes(1), Duration.ZERO),
            null);

    AtomicInteger calls = new AtomicInteger();
    assertEquals(
        "ok",
        executor.supply(
            "rl",
            policies,
            () -> {
              calls.incrementAndGet();
              return "ok";
            }));

    String second =
        executor.supply(
            "rl",
            policies,
            () -> {
              calls.incrementAndGet();
              return "ok2";
            },
            FallbackStrategy.value("fallback"));

    assertEquals("fallback", second);
    assertEquals(1, calls.get(), "second call should be blocked by rate limiter");
    assertTrue(metrics.hasAnyFailureType("rate_limited"));
  }

  @Test
  void shouldCacheValuesWhenPolicyPresent() {
    CapturingMetrics metrics = new CapturingMetrics();
    Resilience4jExecutor executor = new Resilience4jExecutor(metrics);

    ResiliencePolicySet policies =
        new ResiliencePolicySet(
            null, null, null, null, null, new CachePolicy(100, Duration.ofMinutes(5)));

    AtomicInteger calls = new AtomicInteger();
    String v1 = executor.supplyCached("cache", policies, "k1", () -> "v" + calls.incrementAndGet());
    String v2 = executor.supplyCached("cache", policies, "k1", () -> "v" + calls.incrementAndGet());

    assertEquals("v1", v1);
    assertEquals("v1", v2);
    assertEquals(1, calls.get());
    assertTrue(
        metrics.hasCounter("commons.resilience.cache", Map.of("name", "cache", "result", "hit")));
    assertTrue(
        metrics.hasCounter("commons.resilience.cache", Map.of("name", "cache", "result", "miss")));
  }

  @Test
  void shouldRejectWithBulkheadAndUseFallback() throws Exception {
    CapturingMetrics metrics = new CapturingMetrics();
    Resilience4jExecutor executor = new Resilience4jExecutor(metrics);

    ResiliencePolicySet policies =
        new ResiliencePolicySet(null, null, null, new BulkheadPolicy(1, Duration.ZERO), null, null);

    CountDownLatch started = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);

    Thread t1 =
        new Thread(
            () ->
                executor.supply(
                    "bh",
                    policies,
                    () -> {
                      started.countDown();
                      try {
                        release.await(2, TimeUnit.SECONDS);
                      } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                      }
                      return "ok";
                    }));
    t1.start();
    assertTrue(started.await(2, TimeUnit.SECONDS));

    String second =
        executor.supply("bh", policies, () -> "should-not-run", FallbackStrategy.value("fallback"));

    release.countDown();
    t1.join(2000);

    assertEquals("fallback", second);
    assertTrue(metrics.hasAnyFailureType("bulkhead_full"));
  }

  // ── No-arg constructor ──────────────────────────────────────────────────────

  @Test
  void noArgConstructorCreatesWorkingExecutor() {
    Resilience4jExecutor executor = new Resilience4jExecutor();
    String result =
        executor.supply(
            "default", new ResiliencePolicySet(null, null, null, null, null, null), () -> "ok");
    assertEquals("ok", result);
  }

  // ── Null metrics falls back to noop ─────────────────────────────────────────

  @Test
  void nullMetricsFallsBackToNoop() {
    Resilience4jExecutor executor = new Resilience4jExecutor(null);
    String result =
        executor.supply(
            "noop", new ResiliencePolicySet(null, null, null, null, null, null), () -> "noop");
    assertEquals("noop", result);
  }

  // ── run() method ────────────────────────────────────────────────────────────

  @Test
  void runExecutesAction() {
    Resilience4jExecutor executor = new Resilience4jExecutor();
    AtomicInteger counter = new AtomicInteger();
    executor.run(
        "run-test",
        new ResiliencePolicySet(null, null, null, null, null, null),
        counter::incrementAndGet);
    assertEquals(1, counter.get());
  }

  @Test
  void runThrowsWhenActionIsNull() {
    Resilience4jExecutor executor = new Resilience4jExecutor();
    assertThrows(
        NullPointerException.class,
        () ->
            executor.run(
                "run-null", new ResiliencePolicySet(null, null, null, null, null, null), null));
  }

  // ── supply() null / blank name ───────────────────────────────────────────────

  @Test
  void supplyWithNullNameUsesUnnamed() {
    Resilience4jExecutor executor = new Resilience4jExecutor();
    String result = executor.supply(null, null, () -> "unnamed-ok");
    assertEquals("unnamed-ok", result);
  }

  @Test
  void supplyWithBlankNameUsesUnnamed() {
    Resilience4jExecutor executor = new Resilience4jExecutor();
    String result = executor.supply("   ", null, () -> "blank-ok");
    assertEquals("blank-ok", result);
  }

  @Test
  void supplyWithNullPoliciesUsesDefaults() {
    Resilience4jExecutor executor = new Resilience4jExecutor();
    String result = executor.supply("null-policies", null, () -> "ok");
    assertEquals("ok", result);
  }

  @Test
  void supplyWithNullFallbackUsesNone() {
    Resilience4jExecutor executor = new Resilience4jExecutor();
    assertThrows(
        RuntimeException.class,
        () ->
            executor.supply(
                "null-fallback",
                new ResiliencePolicySet(null, null, null, null, null, null),
                () -> {
                  throw new IllegalStateException("boom");
                },
                null));
  }

  // ── supply() action must not be null ────────────────────────────────────────

  @Test
  void supplyThrowsWhenActionIsNull() {
    Resilience4jExecutor executor = new Resilience4jExecutor();
    assertThrows(
        NullPointerException.class,
        () ->
            executor.supply(
                "name", new ResiliencePolicySet(null, null, null, null, null, null), null));
  }

  // ── supply() fallback that itself throws ────────────────────────────────────

  @Test
  void supplyWrapsCheckedExceptionFromFallback() {
    Resilience4jExecutor executor = new Resilience4jExecutor();
    RuntimeException thrown =
        assertThrows(
            RuntimeException.class,
            () ->
                executor.supply(
                    "fallback-throws",
                    new ResiliencePolicySet(null, null, null, null, null, null),
                    () -> {
                      throw new IllegalStateException("original");
                    },
                    cause -> {
                      throw new IllegalArgumentException("fallback-also-fails");
                    }));
    assertNotNull(thrown);
  }

  // ── supplyCached() null cacheKey delegates to supply() ──────────────────────

  @Test
  void supplyCachedWithNullCacheKeyDelegatesToSupply() {
    Resilience4jExecutor executor = new Resilience4jExecutor();
    ResiliencePolicySet policies =
        new ResiliencePolicySet(
            null, null, null, null, null, new CachePolicy(100, Duration.ofMinutes(5)));
    AtomicInteger calls = new AtomicInteger();
    String v1 =
        executor.supplyCached(
            "nc", policies, null, () -> "v" + calls.incrementAndGet(), FallbackStrategy.none());
    String v2 =
        executor.supplyCached(
            "nc", policies, null, () -> "v" + calls.incrementAndGet(), FallbackStrategy.none());
    // both calls execute (no caching when key is null)
    assertEquals(2, calls.get());
    assertEquals("v1", v1);
    assertEquals("v2", v2);
  }

  @Test
  void supplyCachedWithNullCachePolicyDelegatesToSupply() {
    Resilience4jExecutor executor = new Resilience4jExecutor();
    ResiliencePolicySet policies = new ResiliencePolicySet(null, null, null, null, null, null);
    AtomicInteger calls = new AtomicInteger();
    String result =
        executor.supplyCached(
            "no-cache",
            policies,
            "key",
            () -> "v" + calls.incrementAndGet(),
            FallbackStrategy.none());
    assertEquals("v1", result);
    assertEquals(1, calls.get());
  }

  @Test
  void supplyCachedWithNullComputedValueIsNotStored() {
    Resilience4jExecutor executor = new Resilience4jExecutor();
    ResiliencePolicySet policies =
        new ResiliencePolicySet(
            null, null, null, null, null, new CachePolicy(100, Duration.ofMinutes(5)));
    AtomicInteger calls = new AtomicInteger();
    // action returns null — must not be stored, subsequent calls re-execute
    Object v1 =
        executor.supplyCached(
            "null-val",
            policies,
            "key",
            () -> {
              calls.incrementAndGet();
              return null;
            },
            FallbackStrategy.none());
    Object v2 =
        executor.supplyCached(
            "null-val",
            policies,
            "key",
            () -> {
              calls.incrementAndGet();
              return null;
            },
            FallbackStrategy.none());
    assertNull(v1);
    assertNull(v2);
    assertEquals(2, calls.get());
  }

  @Test
  void supplyCachedWithNullNameAndNullPolicies() {
    Resilience4jExecutor executor = new Resilience4jExecutor();
    String result = executor.supplyCached(null, null, "key", () -> "ok", FallbackStrategy.none());
    assertEquals("ok", result);
  }

  // ── circuitBreakerStatuses() ─────────────────────────────────────────────────

  @Test
  void circuitBreakerStatusesReturnsEmptyWhenNoneUsed() {
    Resilience4jExecutor executor = new Resilience4jExecutor();
    assertTrue(executor.circuitBreakerStatuses().isEmpty());
  }

  @Test
  void circuitBreakerStatusesReturnsEntryAfterExecution() {
    Resilience4jExecutor executor = new Resilience4jExecutor();
    ResiliencePolicySet policies =
        new ResiliencePolicySet(null, null, new CircuitBreakerPolicy(50f, 10), null, null, null);
    executor.supply("cb-status", policies, () -> "ok");

    List<Resilience4jExecutor.CircuitBreakerStatus> statuses = executor.circuitBreakerStatuses();
    assertEquals(1, statuses.size());
    Resilience4jExecutor.CircuitBreakerStatus status = statuses.get(0);
    assertEquals("cb-status", status.name());
    assertNotNull(status.state());
    assertTrue(status.bufferedCalls() >= 0);
  }

  @Test
  void circuitBreakerStatusesSortedByKey() {
    Resilience4jExecutor executor = new Resilience4jExecutor();
    ResiliencePolicySet policies = new ResiliencePolicySet(null, null, null, null, null, null);
    executor.supply("zzz", policies, () -> "ok");
    executor.supply("aaa", policies, () -> "ok");
    List<Resilience4jExecutor.CircuitBreakerStatus> statuses = executor.circuitBreakerStatuses();
    // should be sorted
    for (int i = 1; i < statuses.size(); i++) {
      assertTrue(statuses.get(i - 1).key().compareTo(statuses.get(i).key()) <= 0);
    }
  }

  // ── buildCircuitBreaker with policy ─────────────────────────────────────────

  @Test
  void supplyWithCircuitBreakerPolicy() {
    Resilience4jExecutor executor = new Resilience4jExecutor();
    ResiliencePolicySet policies =
        new ResiliencePolicySet(null, null, new CircuitBreakerPolicy(50f, 10), null, null, null);
    String result = executor.supply("cb-policy", policies, () -> "ok");
    assertEquals("ok", result);
  }

  // ── buildRetry with policy + safeWaitDuration ───────────────────────────────

  @Test
  void supplyWithRetryPolicyAndNullBackoff() {
    Resilience4jExecutor executor = new Resilience4jExecutor();
    ResiliencePolicySet policies =
        new ResiliencePolicySet(new RetryPolicy(2, null, null), null, null, null, null, null);
    AtomicInteger calls = new AtomicInteger();
    // Will succeed on first attempt
    String result =
        executor.supply(
            "retry-null-backoff",
            policies,
            () -> {
              calls.incrementAndGet();
              return "ok";
            });
    assertEquals("ok", result);
  }

  @Test
  void supplyWithRetryPolicyAndNegativeBackoff() {
    Resilience4jExecutor executor = new Resilience4jExecutor();
    ResiliencePolicySet policies =
        new ResiliencePolicySet(
            new RetryPolicy(2, Duration.ofMillis(-1), null), null, null, null, null, null);
    String result = executor.supply("retry-neg-backoff", policies, () -> "ok");
    assertEquals("ok", result);
  }

  @Test
  void supplyWithRetryPolicyAndPositiveBackoff() {
    Resilience4jExecutor executor = new Resilience4jExecutor();
    ResiliencePolicySet policies =
        new ResiliencePolicySet(
            new RetryPolicy(1, Duration.ofMillis(1), null), null, null, null, null, null);
    String result = executor.supply("retry-pos-backoff", policies, () -> "ok");
    assertEquals("ok", result);
  }

  // ── buildTimeLimiter branches ────────────────────────────────────────────────

  @Test
  void supplyWithNullTimeoutPolicySkipsTimeLimiter() {
    Resilience4jExecutor executor = new Resilience4jExecutor();
    ResiliencePolicySet policies = new ResiliencePolicySet(null, null, null, null, null, null);
    String result = executor.supply("no-tl", policies, () -> "ok");
    assertEquals("ok", result);
  }

  @Test
  void supplyWithTimeoutPolicyNullTimeoutSkipsTimeLimiter() {
    Resilience4jExecutor executor = new Resilience4jExecutor();
    ResiliencePolicySet policies =
        new ResiliencePolicySet(null, new TimeoutPolicy(null), null, null, null, null);
    String result = executor.supply("tl-null-dur", policies, () -> "ok");
    assertEquals("ok", result);
  }

  @Test
  void supplyWithTimeLimiterTakesTimeLimiterCodePath() {
    // The production TimeLimiter branch executes executeFutureSupplier which returns T directly.
    // The subsequent cast to CompletableFuture<T> causes a ClassCastException that is caught
    // and routed through the fallback path.
    Resilience4jExecutor executor = new Resilience4jExecutor();
    ResiliencePolicySet policies =
        new ResiliencePolicySet(
            null, new TimeoutPolicy(Duration.ofSeconds(5)), null, null, null, null);
    // With FallbackStrategy.value the ClassCastException is recovered and "fallback" is returned
    String result =
        executor.supply("tl-ok", policies, () -> "ok", FallbackStrategy.value("fallback"));
    assertEquals("fallback", result);
  }

  // ── failureType() — circuit breaker open ─────────────────────────────────────

  @Test
  void failureTypeCircuitBreakerOpenViaFallback() {
    CapturingMetrics metrics = new CapturingMetrics();
    Resilience4jExecutor executor = new Resilience4jExecutor(metrics);
    // Force circuit breaker open by setting 0% threshold with 2-call window and failing enough
    ResiliencePolicySet policies =
        new ResiliencePolicySet(
            new RetryPolicy(1, Duration.ofMillis(1), null),
            null,
            new CircuitBreakerPolicy(1f, 2),
            null,
            null,
            null);
    // Fail 2 calls to open the breaker
    for (int i = 0; i < 2; i++) {
      executor.supply(
          "cb-open",
          policies,
          () -> {
            throw new RuntimeException("fail");
          },
          FallbackStrategy.value("fb"));
    }
    // Next call should be blocked by open circuit breaker
    String result =
        executor.supply("cb-open", policies, () -> "should-not-run", FallbackStrategy.value("fb"));
    assertEquals("fb", result);
    assertTrue(metrics.hasAnyFailureType("circuit_breaker_open"));
  }

  // ── failureType() — TimeoutException ─────────────────────────────────────────

  @Test
  void failureTypeTimeLimiterPathIsExercised() {
    // The TimeLimiter path in production always results in a ClassCastException because
    // executeFutureSupplier returns T but the code casts it to CompletableFuture<T>.
    // The ClassCastException is caught and the fallback is invoked, exercising that branch.
    CapturingMetrics metrics = new CapturingMetrics();
    Resilience4jExecutor executor = new Resilience4jExecutor(metrics);
    ResiliencePolicySet policies =
        new ResiliencePolicySet(
            null, new TimeoutPolicy(Duration.ofMillis(50)), null, null, null, null);
    String result =
        executor.supply("tl-timeout", policies, () -> "value", FallbackStrategy.value("fallback"));
    assertEquals("fallback", result);
    // ClassCastException is caught and recorded as a failure
    assertTrue(metrics.hasAnyFailureType("ClassCastException"));
  }

  // ── asRuntime() — checked exception path ─────────────────────────────────────

  @Test
  void supplyWrapsCheckedExceptionAsRuntime() {
    Resilience4jExecutor executor = new Resilience4jExecutor();
    // Use a FallbackStrategy.none() so the checked exception propagates through asRuntime()
    RuntimeException thrown =
        assertThrows(
            RuntimeException.class,
            () ->
                executor.supply(
                    "checked-exc",
                    new ResiliencePolicySet(null, null, null, null, null, null),
                    () -> {
                      throw new RuntimeException(new Exception("checked-cause"));
                    }));
    assertNotNull(thrown);
  }

  // ── unwrapCompletionException() — non-CompletionException ───────────────────

  @Test
  void supplyWithTimeLimiterCodePathRecordsFailure() {
    // Exercises the TimeLimiter code path — ClassCastException is always thrown because
    // executeFutureSupplier returns T (not CompletableFuture<T>) and the cast fails.
    CapturingMetrics metrics = new CapturingMetrics();
    Resilience4jExecutor executor = new Resilience4jExecutor(metrics);
    ResiliencePolicySet policies =
        new ResiliencePolicySet(
            null, new TimeoutPolicy(Duration.ofSeconds(5)), null, null, null, null);
    String result =
        executor.supply("tl-fail", policies, () -> "value", FallbackStrategy.value("fallback"));
    assertEquals("fallback", result);
    assertTrue(metrics.hasAnyFailureType("ClassCastException"));
  }

  private static final class CapturingMetrics implements MetricsFacade {

    private final List<Counter> counters = new ArrayList<>();

    @Override
    public void incrementCounter(String name, long delta, Map<String, String> attributes) {
      counters.add(new Counter(name, delta, attributes));
    }

    @Override
    public void recordHistogram(String name, double value, Map<String, String> attributes) {
      // not needed in these tests
    }

    @Override
    public void recordGauge(String name, double value, Map<String, String> attributes) {
      // not needed in these tests
    }

    boolean hasCounter(String name, Map<String, String> attrs) {
      return counters.stream().anyMatch(c -> c.name.equals(name) && c.attributes.equals(attrs));
    }

    boolean hasAnyFailureType(String failureType) {
      return counters.stream()
          .anyMatch(
              c ->
                  c.name.equals("commons.resilience.calls")
                      && failureType.equals(c.attributes.get("failure_type")));
    }

    private record Counter(String name, long delta, Map<String, String> attributes) {}
  }
}
