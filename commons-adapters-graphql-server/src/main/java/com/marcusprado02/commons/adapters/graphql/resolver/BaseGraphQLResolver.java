package com.marcusprado02.commons.adapters.graphql.resolver;

import com.marcusprado02.commons.kernel.errors.DomainException;
import com.marcusprado02.commons.kernel.errors.Problem;
import com.marcusprado02.commons.kernel.result.Result;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Base class for GraphQL resolvers with common utilities.
 *
 * <p>Provides utility methods for:
 *
 * <ul>
 *   <li>Converting {@link Result} to GraphQL responses
 *   <li>Handling async operations
 *   <li>Extracting arguments from {@link DataFetchingEnvironment}
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * @Controller
 * public class UserController extends BaseGraphQLResolver {
 *
 *     @QueryMapping
 *     public CompletableFuture<User> getUser(@Argument String id) {
 *         return async(() -> userService.findById(id));
 *     }
 *
 *     @MutationMapping
 *     public CompletableFuture<User> createUser(@Argument UserInput input) {
 *         return asyncResult(() -> userService.create(input));
 *     }
 * }
 * }</pre>
 */
public abstract class BaseGraphQLResolver {

  /**
   * Executes an async operation that returns a Result.
   *
   * <p>Unwraps the Result and throws DomainError if failed.
   *
   * @param operation operation that returns a Result
   * @param <T> result type
   * @return CompletableFuture with the result value
   */
  protected <T> CompletableFuture<T> asyncResult(ResultSupplier<T> operation) {
    return CompletableFuture.supplyAsync(
        () -> {
          Result<T> result = operation.get();
          if (result.isOk()) {
            return result.getOrNull();
          } else {
            Problem problem = result.problemOrNull();
            throw new DomainException(problem);
          }
        });
  }

  /**
   * Executes an async operation.
   *
   * @param operation operation to execute
   * @param <T> result type
   * @return CompletableFuture with the result
   */
  protected <T> CompletableFuture<T> async(Operation<T> operation) {
    return CompletableFuture.supplyAsync(operation::execute);
  }

  /**
   * Maps a Result to another type.
   *
   * @param result source result
   * @param mapper mapping function
   * @param <T> source type
   * @param <R> target type
   * @return mapped result
   */
  protected <T, R> Result<R> map(Result<T> result, Function<T, R> mapper) {
    return result.map(mapper);
  }

  /**
   * Functional interface for operations that return a Result.
   *
   * @param <T> result type
   */
  @FunctionalInterface
  protected interface ResultSupplier<T> {
    Result<T> get();
  }

  /**
   * Functional interface for operations.
   *
   * @param <T> result type
   */
  @FunctionalInterface
  protected interface Operation<T> {
    T execute();
  }
}
