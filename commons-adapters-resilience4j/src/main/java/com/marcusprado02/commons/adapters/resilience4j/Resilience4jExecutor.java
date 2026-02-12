package com.marcusprado02.commons.adapters.resilience4j;

import com.marcusprado02.commons.app.resilience.ResilienceExecutor;
import com.marcusprado02.commons.app.resilience.ResiliencePolicySet;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.decorators.Decorators;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public final class Resilience4jExecutor implements ResilienceExecutor {

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
    Objects.requireNonNull(action, "action must not be null");

    String safeName = (name == null || name.isBlank()) ? "unnamed" : name.trim();
    ResiliencePolicySet safePolicies =
        (policies == null) ? new ResiliencePolicySet(null, null, null, null) : policies;

    Supplier<T> decorated =
        Decorators.ofSupplier(action)
            .withCircuitBreaker(buildCircuitBreaker(safeName, safePolicies))
            .withBulkhead(buildBulkhead(safeName, safePolicies))
            .withRetry(buildRetry(safeName, safePolicies))
            .decorate();

    // TimeLimiter funciona melhor com Future/async. Aqui fazemos um wrapper simples.
    TimeLimiter timeLimiter = buildTimeLimiter(safeName, safePolicies);
    if (timeLimiter != null) {
      try {
        @SuppressWarnings("unchecked")
        CompletableFuture<T> future =
            (CompletableFuture<T>)
                timeLimiter.executeFutureSupplier(() -> CompletableFuture.supplyAsync(decorated));
        return future.join();
      } catch (Exception e) {
        throw new RuntimeException("TimeLimiter execution failed", e);
      }
    }

    return decorated.get();
  }

  private CircuitBreaker buildCircuitBreaker(String name, ResiliencePolicySet policies) {
    if (policies.circuitBreaker() == null) {
      return CircuitBreaker.ofDefaults(name);
    }
    CircuitBreakerConfig cfg =
        CircuitBreakerConfig.custom()
            .failureRateThreshold(policies.circuitBreaker().failureRateThreshold())
            .slidingWindowSize(policies.circuitBreaker().slidingWindowSize())
            .build();
    return CircuitBreaker.of(name, cfg);
  }

  private Bulkhead buildBulkhead(String name, ResiliencePolicySet policies) {
    if (policies.bulkhead() == null) {
      return Bulkhead.ofDefaults(name);
    }
    BulkheadConfig cfg =
        BulkheadConfig.custom()
            .maxConcurrentCalls(policies.bulkhead().maxConcurrentCalls())
            .build();
    return Bulkhead.of(name, cfg);
  }

  private Retry buildRetry(String name, ResiliencePolicySet policies) {
    if (policies.retry() == null) {
      return Retry.ofDefaults(name);
    }
    RetryConfig cfg =
        RetryConfig.custom()
            .maxAttempts(policies.retry().maxAttempts())
            .waitDuration(safeDuration(policies.retry().initialBackoff(), Duration.ofMillis(100)))
            .build();
    return Retry.of(name, cfg);
  }

  private TimeLimiter buildTimeLimiter(String name, ResiliencePolicySet policies) {
    if (policies.timeout() == null || policies.timeout().timeout() == null) {
      return null;
    }
    TimeLimiterConfig cfg =
        TimeLimiterConfig.custom().timeoutDuration(policies.timeout().timeout()).build();
    return TimeLimiter.of(name, cfg);
  }

  private Duration safeDuration(Duration value, Duration fallback) {
    return (value == null || value.isNegative() || value.isZero()) ? fallback : value;
  }
}
