package com.marcusprado02.commons.app.resilience;

import java.util.Objects;
import java.util.function.Supplier;

/** Executes operations with resilience policies applied (retry, timeout, circuit breaker, etc.). */
public interface ResilienceExecutor {

  /**
   * Executes a runnable with resilience policies applied.
   *
   * @param name operation name for metrics and logging
   * @param policies the resilience policies to apply
   * @param action the action to execute
   */
  void run(String name, ResiliencePolicySet policies, Runnable action);

  <T> T supply(String name, ResiliencePolicySet policies, Supplier<T> action);

  /**
   * Executes a supplier with resilience policies and a fallback on failure.
   *
   * @param name operation name for metrics/logging
   * @param policies resilience policy set to apply
   * @param action supplier to execute
   * @param fallback fallback strategy invoked if the action fails
   * @param <T> return type
   * @return the action result, or fallback value on failure
   */
  default <T> T supply(
      String name, ResiliencePolicySet policies, Supplier<T> action, FallbackStrategy<T> fallback) {
    Objects.requireNonNull(action, "action must not be null");
    FallbackStrategy<T> safeFallback = (fallback == null) ? FallbackStrategy.none() : fallback;
    try {
      return supply(name, policies, action);
    } catch (Throwable t) {
      return safeFallback.recover(t);
    }
  }

  default <T> T supplyCached(
      String name, ResiliencePolicySet policies, Object cacheKey, Supplier<T> action) {
    return supplyCached(name, policies, cacheKey, action, FallbackStrategy.none());
  }

  default <T> T supplyCached(
      String name,
      ResiliencePolicySet policies,
      Object cacheKey,
      Supplier<T> action,
      FallbackStrategy<T> fallback) {
    // Default: ignore caching (implementations may override).
    return supply(name, policies, action, fallback);
  }
}
