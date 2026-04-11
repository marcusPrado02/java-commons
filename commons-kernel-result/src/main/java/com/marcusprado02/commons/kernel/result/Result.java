package com.marcusprado02.commons.kernel.result;

import com.marcusprado02.commons.kernel.errors.Problem;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** Represents the outcome of an operation: either {@link Ok} with a value or {@link Fail}. */
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

  /**
   * Transforms the success value using the given function.
   *
   * @param fn mapping function
   * @param <U> target type
   * @return mapped Ok, or this Fail unchanged
   */
  default <U> Result<U> map(Function<T, U> fn) {
    Objects.requireNonNull(fn);
    if (isOk()) {
      return Result.ok(fn.apply(getOrNull()));
    }
    return Result.fail(problemOrNull());
  }

  /**
   * Chains this Result with a function that returns another Result.
   *
   * @param fn flat-mapping function
   * @param <U> target type
   * @return the chained Result, or this Fail unchanged
   */
  default <U> Result<U> flatMap(Function<T, Result<U>> fn) {
    Objects.requireNonNull(fn);
    if (isOk()) {
      return Objects.requireNonNull(fn.apply(getOrNull()));
    }
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
    if (isFail()) {
      return Result.fail(fn.apply(problemOrNull()));
    }
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
    if (isFail()) {
      return Result.ok(fn.apply(problemOrNull()));
    }
    return this;
  }

  /**
   * Recovers from a failure by applying a function that returns a Result.
   *
   * @param fn recovery function (Problem → {@code Result<T>})
   * @return recovered Result if this was Fail, otherwise returns this
   */
  default Result<T> recoverWith(Function<Problem, Result<T>> fn) {
    Objects.requireNonNull(fn);
    if (isFail()) {
      return Objects.requireNonNull(fn.apply(problemOrNull()));
    }
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
    if (isOk()) {
      action.accept(getOrNull());
    }
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
    if (isFail()) {
      action.accept(problemOrNull());
    }
    return this;
  }

  // ==== Async ====

  /**
   * Maps this Result asynchronously.
   *
   * <p>If this is Ok, applies {@code fn} on the common fork-join pool and wraps the outcome. If
   * this is Fail, returns an immediately-completed future with the same failure.
   *
   * @param fn async mapping function
   * @param <U> result type
   * @return future that completes with the mapped Result
   */
  default <U> CompletableFuture<Result<U>> mapAsync(Function<T, U> fn) {
    Objects.requireNonNull(fn);
    if (isFail()) {
      return CompletableFuture.completedFuture(Result.fail(problemOrNull()));
    }
    return CompletableFuture.supplyAsync(() -> Result.ok(fn.apply(getOrNull())));
  }

  /**
   * Chains this Result with an async operation that itself returns a {@code Result}.
   *
   * <p>If this is Ok, applies {@code fn} on the common fork-join pool. The function must return a
   * {@code CompletableFuture<Result<U>>}, allowing full async pipelines:
   *
   * <pre>{@code
   * Result.ok(userId)
   *     .flatMapAsync(id -> userRepository.findByIdAsync(id))
   *     .thenCompose(r -> r.flatMapAsync(user -> orderRepository.findByUserAsync(user)));
   * }</pre>
   *
   * @param fn async flat-mapping function returning a future of Result
   * @param <U> result type
   * @return future that completes with the chained Result
   */
  default <U> CompletableFuture<Result<U>> flatMapAsync(
      Function<T, CompletableFuture<Result<U>>> fn) {
    Objects.requireNonNull(fn);
    if (isFail()) {
      return CompletableFuture.completedFuture(Result.fail(problemOrNull()));
    }
    return CompletableFuture.supplyAsync(() -> fn.apply(getOrNull())).thenCompose(future -> future);
  }

  /**
   * Wraps this Result in a completed {@link CompletableFuture}.
   *
   * <p>Useful when an API requires a {@code CompletableFuture<Result<T>>} but the value is already
   * available.
   *
   * @return immediately-completed future containing this Result
   */
  default CompletableFuture<Result<T>> toFuture() {
    return CompletableFuture.completedFuture(this);
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

  /** Successful variant of {@link Result} holding the computed value. */
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

  /** Failed variant of {@link Result} holding the associated {@link Problem}. */
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
