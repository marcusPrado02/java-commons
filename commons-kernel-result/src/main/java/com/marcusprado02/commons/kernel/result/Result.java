package com.marcusprado02.commons.kernel.result;

import com.marcusprado02.commons.kernel.errors.Problem;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
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

  // ==== Value Extraction ====

  default T getOrElse(T fallback) {
    return isOk() ? getOrNull() : fallback;
  }

  default T getOrElseGet(Supplier<T> fallback) {
    return isOk() ? getOrNull() : fallback.get();
  }

  // ==== Mapping ====

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

  /**
   * Maps the error (Problem) to a new Problem.
   *
   * @param fn error mapping function
   * @return Result with mapped error if this is a Fail, otherwise returns this
   */
  default Result<T> mapError(Function<Problem, Problem> fn) {
    Objects.requireNonNull(fn);
    if (isFail()) return Result.fail(fn.apply(problemOrNull()));
    return this;
  }

  // ==== Recovery ====

  /**
   * Returns this Result if Ok, otherwise returns the fallback Result.
   *
   * @param fallback fallback result
   * @return this or fallback
   */
  default Result<T> orElse(Result<T> fallback) {
    return isOk() ? this : fallback;
  }

  /**
   * Returns this Result if Ok, otherwise returns the result from the supplier.
   *
   * @param fallback fallback result supplier
   * @return this or supplied fallback
   */
  default Result<T> orElseGet(Supplier<Result<T>> fallback) {
    return isOk() ? this : fallback.get();
  }

  /**
   * Recovers from a failure by applying a function to the Problem.
   *
   * @param fn recovery function (Problem → T)
   * @return Result.ok with recovered value if this was Fail, otherwise returns this
   */
  default Result<T> recover(Function<Problem, T> fn) {
    Objects.requireNonNull(fn);
    if (isFail()) return Result.ok(fn.apply(problemOrNull()));
    return this;
  }

  /**
   * Recovers from a failure by applying a function that returns a Result.
   *
   * @param fn recovery function (Problem → Result<T>)
   * @return recovered Result if this was Fail, otherwise returns this
   */
  default Result<T> recoverWith(Function<Problem, Result<T>> fn) {
    Objects.requireNonNull(fn);
    if (isFail()) return Objects.requireNonNull(fn.apply(problemOrNull()));
    return this;
  }

  // ==== Side Effects ====

  /**
   * Performs a side effect if this is Ok.
   *
   * @param action consumer for the value
   * @return this Result (unchanged)
   */
  default Result<T> peek(Consumer<T> action) {
    Objects.requireNonNull(action);
    if (isOk()) action.accept(getOrNull());
    return this;
  }

  /**
   * Performs a side effect if this is Fail.
   *
   * @param action consumer for the Problem
   * @return this Result (unchanged)
   */
  default Result<T> peekError(Consumer<Problem> action) {
    Objects.requireNonNull(action);
    if (isFail()) action.accept(problemOrNull());
    return this;
  }

  // ==== Transformations ====

  /**
   * Folds this Result into a single value by applying one of two functions.
   *
   * @param onError function to apply if Fail
   * @param onSuccess function to apply if Ok
   * @param <U> result type
   * @return folded value
   */
  default <U> U fold(Function<Problem, U> onError, Function<T, U> onSuccess) {
    Objects.requireNonNull(onError);
    Objects.requireNonNull(onSuccess);
    return isOk() ? onSuccess.apply(getOrNull()) : onError.apply(problemOrNull());
  }

  /**
   * Filters the success value with a predicate.
   *
   * @param predicate predicate to test the value
   * @param errorSupplier supplier for Problem if predicate fails
   * @return this if Ok and predicate passes, Fail if predicate fails or already Fail
   */
  default Result<T> filter(Predicate<T> predicate, Supplier<Problem> errorSupplier) {
    Objects.requireNonNull(predicate);
    Objects.requireNonNull(errorSupplier);
    if (isOk() && !predicate.test(getOrNull())) {
      return Result.fail(errorSupplier.get());
    }
    return this;
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
