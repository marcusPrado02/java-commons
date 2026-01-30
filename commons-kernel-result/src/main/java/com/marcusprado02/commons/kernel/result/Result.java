package com.marcusprado02.commons.kernel.result;

import com.marcusprado02.commons.kernel.errors.Problem;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public sealed interface Result<T> permits Result.Ok, Result.Fail {

  boolean isOk();

  default boolean isFail() {
    return !isOk();
  }

  T getOrNull();

  Problem problemOrNull();

  static <T> Result<T> ok(T value) {
    return new Ok<>(value);
  }

  static <T> Result<T> fail(Problem problem) {
    return new Fail<>(Objects.requireNonNull(problem));
  }

  default T getOrElse(T fallback) {
    return isOk() ? getOrNull() : fallback;
  }

  default T getOrElseGet(Supplier<T> fallback) {
    return isOk() ? getOrNull() : fallback.get();
  }

  default <U> Result<U> map(Function<T, U> fn) {
    Objects.requireNonNull(fn);
    if (isOk()) return Result.ok(fn.apply(getOrNull()));
    return Result.fail(problemOrNull());
  }

  default <U> Result<U> flatMap(Function<T, Result<U>> fn) {
    Objects.requireNonNull(fn);
    if (isOk()) return Objects.requireNonNull(fn.apply(getOrNull()));
    return Result.fail(problemOrNull());
  }

  record Ok<T>(T value) implements Result<T> {
    @Override
    public boolean isOk() {
      return true;
    }

    @Override
    public T getOrNull() {
      return value;
    }

    @Override
    public Problem problemOrNull() {
      return null;
    }
  }

  record Fail<T>(Problem problem) implements Result<T> {
    @Override
    public boolean isOk() {
      return false;
    }

    @Override
    public T getOrNull() {
      return null;
    }

    @Override
    public Problem problemOrNull() {
      return problem;
    }
  }
}
