package com.marcusprado02.commons.adapters.resilience4j;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.marcusprado02.commons.app.observability.MetricsFacade;
import com.marcusprado02.commons.app.resilience.CachePolicy;
import com.marcusprado02.commons.app.resilience.FallbackStrategy;
import com.marcusprado02.commons.app.resilience.RateLimiterPolicy;
import com.marcusprado02.commons.app.resilience.ResilienceExecutor;
import com.marcusprado02.commons.app.resilience.ResiliencePolicySet;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.decorators.Decorators;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

public final class Resilience4jExecutor implements ResilienceExecutor {

  private static final String METRIC_CALLS = "commons.resilience.calls";
  private static final String METRIC_CACHE = "commons.resilience.cache";

  private final MetricsFacade metrics;
  private final ConcurrentMap<String, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, Bulkhead> bulkheads = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, RateLimiter> rateLimiters = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, Retry> retries = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, TimeLimiter> timeLimiters = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, com.github.benmanes.caffeine.cache.Cache<Object, Object>>
      caches = new ConcurrentHashMap<>();

  public Resilience4jExecutor() {
    this(MetricsFacade.noop());
  }

  public Resilience4jExecutor(MetricsFacade metrics) {
    this.metrics = (metrics == null) ? MetricsFacade.noop() : metrics;
  }

  public List<CircuitBreakerStatus> circuitBreakerStatuses() {
    return circuitBreakers.entrySet().stream()
        .map(
            entry -> {
              CircuitBreaker circuitBreaker = entry.getValue();
              CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
              return new CircuitBreakerStatus(
                  entry.getKey(),
                  circuitBreaker.getName(),
                  circuitBreaker.getState().name(),
                  metrics.getFailureRate(),
                  metrics.getNumberOfBufferedCalls(),
                  metrics.getNumberOfFailedCalls(),
                  metrics.getNumberOfSuccessfulCalls());
            })
        .sorted(Comparator.comparing(CircuitBreakerStatus::key))
        .toList();
  }

  public record CircuitBreakerStatus(
      String key,
      String name,
      String state,
      float failureRate,
      int bufferedCalls,
      int failedCalls,
      int successfulCalls) {}

  @Override
  public void run(String name, ResiliencePolicySet policies, Runnable action) {
    Objects.requireNonNull(action, "action must not be null");
    supply(
        name,
        policies,
        () -> {
          action.run();
          return null;
        });
  }

  @Override
  public <T> T supply(String name, ResiliencePolicySet policies, Supplier<T> action) {
    return supply(name, policies, action, FallbackStrategy.none());
  }

  @Override
  public <T> T supply(
      String name, ResiliencePolicySet policies, Supplier<T> action, FallbackStrategy<T> fallback) {
    Objects.requireNonNull(action, "action must not be null");

    String safeName = (name == null || name.isBlank()) ? "unnamed" : name.trim();
    ResiliencePolicySet safePolicies =
        (policies == null) ? new ResiliencePolicySet(null, null, null, null, null, null) : policies;
    FallbackStrategy<T> safeFallback = (fallback == null) ? FallbackStrategy.none() : fallback;

    Supplier<T> decorated =
        Decorators.ofSupplier(action)
            .withCircuitBreaker(buildCircuitBreaker(safeName, safePolicies))
            .withBulkhead(buildBulkhead(safeName, safePolicies))
            .withRateLimiter(buildRateLimiter(safeName, safePolicies))
            .withRetry(buildRetry(safeName, safePolicies))
            .decorate();

    // TimeLimiter funciona melhor com Future/async. Aqui fazemos um wrapper simples.
    TimeLimiter timeLimiter = buildTimeLimiter(safeName, safePolicies);
    try {
      T result;
      if (timeLimiter != null) {
        @SuppressWarnings("unchecked")
        CompletableFuture<T> future =
            (CompletableFuture<T>)
                timeLimiter.executeFutureSupplier(() -> CompletableFuture.supplyAsync(decorated));
        result = future.join();
      } else {
        result = decorated.get();
      }

      metrics.incrementCounter(METRIC_CALLS, 1, Map.of("name", safeName, "outcome", "success"));
      return result;
    } catch (Throwable t) {
      metrics.incrementCounter(
          METRIC_CALLS,
          1,
          Map.of("name", safeName, "outcome", "failure", "failure_type", failureType(t)));

      try {
        T fallbackValue = safeFallback.recover(unwrapCompletionException(t));
        metrics.incrementCounter(METRIC_CALLS, 1, Map.of("name", safeName, "outcome", "fallback"));
        return fallbackValue;
      } catch (Throwable fallbackError) {
        throw asRuntime(unwrapCompletionException(fallbackError));
      }
    }
  }

  @Override
  public <T> T supplyCached(
      String name,
      ResiliencePolicySet policies,
      Object cacheKey,
      Supplier<T> action,
      FallbackStrategy<T> fallback) {
    Objects.requireNonNull(action, "action must not be null");

    String safeName = (name == null || name.isBlank()) ? "unnamed" : name.trim();
    ResiliencePolicySet safePolicies =
        (policies == null) ? new ResiliencePolicySet(null, null, null, null, null, null) : policies;
    CachePolicy cachePolicy = safePolicies.cache();
    if (cachePolicy == null || cacheKey == null) {
      return supply(safeName, safePolicies, action, fallback);
    }

    com.github.benmanes.caffeine.cache.Cache<Object, Object> cache =
        caches.computeIfAbsent(
            safeName,
            ignored ->
                Caffeine.newBuilder()
                    .maximumSize(cachePolicy.maxSize())
                    .expireAfterWrite(cachePolicy.ttl())
                    .build());

    Object cachedValue = cache.getIfPresent(cacheKey);
    if (cachedValue != null) {
      metrics.incrementCounter(METRIC_CACHE, 1, Map.of("name", safeName, "result", "hit"));
      @SuppressWarnings("unchecked")
      T typed = (T) cachedValue;
      return typed;
    }

    metrics.incrementCounter(METRIC_CACHE, 1, Map.of("name", safeName, "result", "miss"));
    T computed = supply(safeName, safePolicies, action, fallback);
    if (computed != null) {
      cache.put(cacheKey, computed);
    }
    return computed;
  }

  private CircuitBreaker buildCircuitBreaker(String name, ResiliencePolicySet policies) {
    String key = componentKey(name, policies.circuitBreaker());
    return circuitBreakers.computeIfAbsent(
        key,
        ignored -> {
          if (policies.circuitBreaker() == null) {
            return CircuitBreaker.ofDefaults(name);
          }
          CircuitBreakerConfig cfg =
              CircuitBreakerConfig.custom()
                  .failureRateThreshold(policies.circuitBreaker().failureRateThreshold())
                  .slidingWindowSize(policies.circuitBreaker().slidingWindowSize())
                  .build();
          return CircuitBreaker.of(name, cfg);
        });
  }

  private Bulkhead buildBulkhead(String name, ResiliencePolicySet policies) {
    String key = componentKey(name, policies.bulkhead());
    return bulkheads.computeIfAbsent(
        key,
        ignored -> {
          if (policies.bulkhead() == null) {
            return Bulkhead.ofDefaults(name);
          }
          BulkheadConfig cfg =
              BulkheadConfig.custom()
                  .maxConcurrentCalls(policies.bulkhead().maxConcurrentCalls())
                  .maxWaitDuration(policies.bulkhead().maxWaitDuration())
                  .build();
          return Bulkhead.of(name, cfg);
        });
  }

  private RateLimiter buildRateLimiter(String name, ResiliencePolicySet policies) {
    RateLimiterPolicy policy = policies.rateLimiter();
    String key = componentKey(name, policy);
    return rateLimiters.computeIfAbsent(
        key,
        ignored -> {
          if (policy == null) {
            return RateLimiter.ofDefaults(name);
          }
          RateLimiterConfig cfg =
              RateLimiterConfig.custom()
                  .limitForPeriod(policy.limitForPeriod())
                  .limitRefreshPeriod(policy.refreshPeriod())
                  .timeoutDuration(policy.timeout())
                  .build();
          return RateLimiter.of(name, cfg);
        });
  }

  private Retry buildRetry(String name, ResiliencePolicySet policies) {
    String key = componentKey(name, policies.retry());
    return retries.computeIfAbsent(
        key,
        ignored -> {
          if (policies.retry() == null) {
            return Retry.ofDefaults(name);
          }
          RetryConfig cfg =
              RetryConfig.custom()
                  .maxAttempts(policies.retry().maxAttempts())
                  .waitDuration(
                      safeWaitDuration(policies.retry().initialBackoff(), Duration.ofMillis(100)))
                  .build();
          return Retry.of(name, cfg);
        });
  }

  private TimeLimiter buildTimeLimiter(String name, ResiliencePolicySet policies) {
    if (policies.timeout() == null || policies.timeout().timeout() == null) {
      return null;
    }
    String key = componentKey(name, policies.timeout());
    return timeLimiters.computeIfAbsent(
        key,
        ignored -> {
          TimeLimiterConfig cfg =
              TimeLimiterConfig.custom().timeoutDuration(policies.timeout().timeout()).build();
          return TimeLimiter.of(name, cfg);
        });
  }

  private Duration safeWaitDuration(Duration value, Duration fallback) {
    return (value == null || value.isNegative()) ? fallback : value;
  }

  private String componentKey(String name, Object policy) {
    return name + "|" + ((policy == null) ? "default" : policy);
  }

  private String failureType(Throwable t) {
    Throwable unwrapped = unwrapCompletionException(t);
    if (unwrapped instanceof CallNotPermittedException) {
      return "circuit_breaker_open";
    }
    if (unwrapped instanceof BulkheadFullException) {
      return "bulkhead_full";
    }
    if (unwrapped instanceof RequestNotPermitted) {
      return "rate_limited";
    }
    if (unwrapped instanceof TimeoutException) {
      return "timeout";
    }
    return unwrapped.getClass().getSimpleName();
  }

  private Throwable unwrapCompletionException(Throwable t) {
    if (t instanceof java.util.concurrent.CompletionException ce && ce.getCause() != null) {
      return ce.getCause();
    }
    return t;
  }

  private RuntimeException asRuntime(Throwable t) {
    if (t instanceof RuntimeException runtimeException) {
      return runtimeException;
    }
    return new RuntimeException(t);
  }
}
