package com.marcusprado02.commons.kernel.result;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public sealed interface Option<T> permits Option.Some, Option.None {

  boolean isPresent();

  default boolean isEmpty() {
    return !isPresent();
  }

  T get();

  static <T> Option<T> some(T value) {
    return new Some<>(Objects.requireNonNull(value));
  }

  static <T> Option<T> none() {
    return new None<>();
  }

  static <T> Option<T> ofNullable(T value) {
    return value == null ? none() : some(value);
  }

  default T orElse(T fallback) {
    return isPresent() ? get() : fallback;
  }

  default T orElseGet(Supplier<T> fallback) {
    return isPresent() ? get() : fallback.get();
  }

  default <U> Option<U> map(Function<T, U> fn) {
    Objects.requireNonNull(fn);
    return isPresent() ? Option.ofNullable(fn.apply(get())) : Option.none();
  }

  record Some<T>(T value) implements Option<T> {
    @Override
    public boolean isPresent() {
      return true;
    }

    @Override
    public T get() {
      return value;
    }
  }

  final class None<T> implements Option<T> {
    @Override
    public boolean isPresent() {
      return false;
    }

    @Override
    public T get() {
      throw new NoSuchElementException("Option is empty");
    }
  }
}
