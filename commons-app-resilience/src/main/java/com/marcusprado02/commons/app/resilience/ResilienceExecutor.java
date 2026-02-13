package com.marcusprado02.commons.app.resilience;

import java.util.Objects;
import java.util.function.Supplier;

public interface ResilienceExecutor {

  void run(String name, ResiliencePolicySet policies, Runnable action);

  <T> T supply(String name, ResiliencePolicySet policies, Supplier<T> action);

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
