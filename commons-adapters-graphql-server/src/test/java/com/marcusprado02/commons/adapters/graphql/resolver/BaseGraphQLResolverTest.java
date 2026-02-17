package com.marcusprado02.commons.adapters.graphql.resolver;

import com.marcusprado02.commons.kernel.errors.DomainException;
import com.marcusprado02.commons.kernel.errors.ErrorCategory;
import com.marcusprado02.commons.kernel.errors.ErrorCode;
import com.marcusprado02.commons.kernel.errors.Problem;
import com.marcusprado02.commons.kernel.errors.Severity;
import com.marcusprado02.commons.kernel.result.Result;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

class BaseGraphQLResolverTest {

  private final TestResolver resolver = new TestResolver();

  @Test
  void shouldExecuteAsyncResultWithSuccess() throws Exception {
    CompletableFuture<String> future = resolver.asyncResult(() -> Result.ok("test"));

    String result = future.get();
    assertEquals("test", result);
  }

  @Test
  void shouldExecuteAsyncResultWithFailure() {
    ErrorCode errorCode = ErrorCode.of("TEST_ERROR");
    Problem problem = Problem.of(
        errorCode,
        ErrorCategory.BUSINESS,
        Severity.ERROR,
        "Test error"
    );

    CompletableFuture<String> future = resolver.asyncResult(() -> Result.fail(problem));

    ExecutionException exception = assertThrows(ExecutionException.class, future::get);
    assertTrue(exception.getCause() instanceof DomainException);
    assertEquals("Test error", exception.getCause().getMessage());
  }

  @Test
  void shouldExecuteAsync() throws Exception {
    CompletableFuture<String> future = resolver.async(() -> "async result");

    String result = future.get();
    assertEquals("async result", result);
  }

  @Test
  void shouldMapResult() {
    Result<Integer> source = Result.ok(42);

    Result<String> mapped = resolver.map(source, Object::toString);

    assertTrue(mapped.isOk());
    assertEquals("42", mapped.getOrNull());
  }

  @Test
  void shouldMapFailureResult() {
    ErrorCode errorCode = ErrorCode.of("TEST_ERROR");
    Problem problem = Problem.of(
        errorCode,
        ErrorCategory.BUSINESS,
        Severity.ERROR,
        "Test error"
    );
    Result<Integer> source = Result.fail(problem);

    Result<String> mapped = resolver.map(source, Object::toString);

    assertTrue(mapped.isFail());
    assertEquals("Test error", mapped.problemOrNull().message());
  }

  @Test
  void shouldHandleNullInMap() {
    Result<String> source = Result.ok(null);

    Result<Integer> mapped = resolver.map(source, value -> value == null ? 0 : value.length());

    assertTrue(mapped.isOk());
    assertEquals(0, mapped.getOrNull());
  }

  // Test implementation
  private static class TestResolver extends BaseGraphQLResolver {
    // Expose protected methods for testing
    @Override
    public <T> CompletableFuture<T> asyncResult(ResultSupplier<T> operation) {
      return super.asyncResult(operation);
    }

    @Override
    public <T> CompletableFuture<T> async(Operation<T> operation) {
      return super.async(operation);
    }

    @Override
    public <T, R> Result<R> map(Result<T> result, java.util.function.Function<T, R> mapper) {
      return super.map(result, mapper);
    }
  }
}
