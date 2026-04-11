package com.marcusprado02.commons.app.resilience;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Strategy for recovering from failures when resilience policies are exhausted.
 *
 * @param <T> the return type of the guarded operation
 */
@FunctionalInterface
public interface FallbackStrategy<T> {

  T recover(Throwable cause);

  /**
   * Returns a fallback that rethrows the original exception without recovery.
   *
   * @param <T> the return type of the guarded operation
   * @return a fallback strategy that always rethrows
   */
  static <T> FallbackStrategy<T> none() {
    return cause -> {
      if (cause instanceof RuntimeException runtimeException) {
        throw runtimeException;
      }
      if (cause instanceof Error error) {
        throw error;
      }
      throw new RuntimeException(cause);
    };
  }

  static <T> FallbackStrategy<T> value(T value) {
    return ignored -> value;
  }

  static <T> FallbackStrategy<T> supplier(Supplier<T> supplier) {
    Objects.requireNonNull(supplier, "supplier must not be null");
    return ignored -> supplier.get();
  }
}
