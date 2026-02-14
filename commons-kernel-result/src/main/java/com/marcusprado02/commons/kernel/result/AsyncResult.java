package com.marcusprado02.commons.kernel.result;

import com.marcusprado02.commons.kernel.errors.Problem;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Represents an asynchronous computation that will produce a Result.
 *
 * <p>This class wraps a CompletableFuture&lt;Result&lt;T&gt;&gt; and provides railway-oriented
 * programming operations for async workflows.
 *
 * @param <T> value type
 */
public final class AsyncResult<T> {

  private final CompletableFuture<Result<T>> future;

  private AsyncResult(CompletableFuture<Result<T>> future) {
    this.future = Objects.requireNonNull(future);
  }

  // ==== Factory Methods ====

  /**
   * Creates an AsyncResult from a CompletableFuture of Result.
   *
   * @param future the completable future
   * @param <T> value type
   * @return async result
   */
  public static <T> AsyncResult<T> of(CompletableFuture<Result<T>> future) {
    return new AsyncResult<>(future);
  }

  /**
   * Creates an AsyncResult from a Result.
   *
   * @param result the result
   * @param <T> value type
   * @return async result (already completed)
   */
  public static <T> AsyncResult<T> of(Result<T> result) {
    return new AsyncResult<>(CompletableFuture.completedFuture(result));
  }

  /**
   * Creates a successful AsyncResult.
   *
   * @param value the value
   * @param <T> value type
   * @return async result with Ok
   */
  public static <T> AsyncResult<T> ok(T value) {
    return of(Result.ok(value));
  }

  /**
   * Creates a failed AsyncResult.
   *
   * @param problem the problem
   * @param <T> value type
   * @return async result with Fail
   */
  public static <T> AsyncResult<T> fail(Problem problem) {
    return of(Result.fail(problem));
  }

  /**
   * Creates an AsyncResult from a supplier that will be executed asynchronously.
   *
   * @param supplier the result supplier
   * @param <T> value type
   * @return async result
   */
  public static <T> AsyncResult<T> supply(Supplier<Result<T>> supplier) {
    return of(CompletableFuture.supplyAsync(supplier));
  }

  /**
   * Creates an AsyncResult from a supplier using a custom executor.
   *
   * @param supplier the result supplier
   * @param executor the executor
   * @param <T> value type
   * @return async result
   */
  public static <T> AsyncResult<T> supply(Supplier<Result<T>> supplier, Executor executor) {
    return of(CompletableFuture.supplyAsync(supplier, executor));
  }

  // ==== Mapping ====

  /**
   * Maps the success value asynchronously.
   *
   * @param fn mapping function
   * @param <U> new value type
   * @return new async result
   */
  public <U> AsyncResult<U> map(Function<T, U> fn) {
    Objects.requireNonNull(fn);
    return new AsyncResult<>(future.thenApply(result -> result.map(fn)));
  }

  /**
   * Maps the success value asynchronously using a custom executor.
   *
   * @param fn mapping function
   * @param executor the executor
   * @param <U> new value type
   * @return new async result
   */
  public <U> AsyncResult<U> map(Function<T, U> fn, Executor executor) {
    Objects.requireNonNull(fn);
    return new AsyncResult<>(future.thenApplyAsync(result -> result.map(fn), executor));
  }

  /**
   * FlatMaps the success value asynchronously.
   *
   * @param fn flat mapping function
   * @param <U> new value type
   * @return new async result
   */
  public <U> AsyncResult<U> flatMap(Function<T, Result<U>> fn) {
    Objects.requireNonNull(fn);
    return new AsyncResult<>(future.thenApply(result -> result.flatMap(fn)));
  }

  /**
   * FlatMaps the success value with another AsyncResult.
   *
   * @param fn flat mapping function returning AsyncResult
   * @param <U> new value type
   * @return new async result
   */
  public <U> AsyncResult<U> flatMapAsync(Function<T, AsyncResult<U>> fn) {
    Objects.requireNonNull(fn);
    return new AsyncResult<>(
        future.thenCompose(
            result -> {
              if (result.isOk()) {
                return fn.apply(result.getOrNull()).toCompletableFuture();
              }
              return CompletableFuture.completedFuture(Result.fail(result.problemOrNull()));
            }));
  }

  /**
   * Maps the error (Problem) to a new Problem.
   *
   * @param fn error mapping function
   * @return new async result with mapped error
   */
  public AsyncResult<T> mapError(Function<Problem, Problem> fn) {
    Objects.requireNonNull(fn);
    return new AsyncResult<>(future.thenApply(result -> result.mapError(fn)));
  }

  // ==== Recovery ====

  /**
   * Recovers from a failure asynchronously.
   *
   * @param fn recovery function
   * @return new async result
   */
  public AsyncResult<T> recover(Function<Problem, T> fn) {
    Objects.requireNonNull(fn);
    return new AsyncResult<>(future.thenApply(result -> result.recover(fn)));
  }

  /**
   * Recovers from a failure with another Result.
   *
   * @param fn recovery function returning Result
   * @return new async result
   */
  public AsyncResult<T> recoverWith(Function<Problem, Result<T>> fn) {
    Objects.requireNonNull(fn);
    return new AsyncResult<>(future.thenApply(result -> result.recoverWith(fn)));
  }

  /**
   * Recovers from a failure with another AsyncResult.
   *
   * @param fn recovery function returning AsyncResult
   * @return new async result
   */
  public AsyncResult<T> recoverWithAsync(Function<Problem, AsyncResult<T>> fn) {
    Objects.requireNonNull(fn);
    return new AsyncResult<>(
        future.thenCompose(
            result -> {
              if (result.isFail()) {
                return fn.apply(result.problemOrNull()).toCompletableFuture();
              }
              return CompletableFuture.completedFuture(result);
            }));
  }

  // ==== Side Effects ====

  /**
   * Performs a side effect if the result is Ok.
   *
   * @param action consumer for the value
   * @return this async result
   */
  public AsyncResult<T> peek(Consumer<T> action) {
    Objects.requireNonNull(action);
    return new AsyncResult<>(future.thenApply(result -> result.peek(action)));
  }

  /**
   * Performs a side effect if the result is Fail.
   *
   * @param action consumer for the Problem
   * @return this async result
   */
  public AsyncResult<T> peekError(Consumer<Problem> action) {
    Objects.requireNonNull(action);
    return new AsyncResult<>(future.thenApply(result -> result.peekError(action)));
  }

  // ==== Conversion ====

  /**
   * Returns the underlying CompletableFuture.
   *
   * @return completable future of result
   */
  public CompletableFuture<Result<T>> toCompletableFuture() {
    return future;
  }

  /**
   * Blocks and returns the Result.
   *
   * @return the result
   */
  public Result<T> join() {
    return future.join();
  }

  /**
   * Returns a CompletableFuture of the value (unwraps the Result).
   *
   * <p>If the Result is a Fail, the future will complete exceptionally with the Problem.
   *
   * @return completable future of value
   */
  public CompletableFuture<T> toCompletableFutureOfValue() {
    return future.thenApply(
        result -> {
          if (result.isOk()) return result.getOrNull();
          throw new ResultException(result.problemOrNull());
        });
  }

  /** Exception thrown when unwrapping a failed Result into a CompletableFuture. */
  public static class ResultException extends RuntimeException {
    private final Problem problem;

    public ResultException(Problem problem) {
      super(problem.message());
      this.problem = problem;
    }

    public Problem getProblem() {
      return problem;
    }
  }
}
