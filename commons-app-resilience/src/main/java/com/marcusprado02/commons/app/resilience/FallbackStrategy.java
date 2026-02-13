package com.marcusprado02.commons.app.resilience;

import java.util.Objects;
import java.util.function.Supplier;

@FunctionalInterface
public interface FallbackStrategy<T> {

  T recover(Throwable cause);

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
