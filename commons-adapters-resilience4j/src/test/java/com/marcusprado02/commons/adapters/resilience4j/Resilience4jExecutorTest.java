package com.marcusprado02.commons.adapters.resilience4j;

import static org.junit.jupiter.api.Assertions.*;

import com.marcusprado02.commons.app.observability.MetricsFacade;
import com.marcusprado02.commons.app.resilience.BulkheadPolicy;
import com.marcusprado02.commons.app.resilience.CachePolicy;
import com.marcusprado02.commons.app.resilience.FallbackStrategy;
import com.marcusprado02.commons.app.resilience.RateLimiterPolicy;
import com.marcusprado02.commons.app.resilience.ResiliencePolicySet;
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
