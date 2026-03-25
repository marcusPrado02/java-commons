package com.marcusprado02.commons.kernel.result;

import static org.assertj.core.api.Assertions.*;

import com.marcusprado02.commons.kernel.errors.ErrorCategory;
import com.marcusprado02.commons.kernel.errors.ErrorCode;
import com.marcusprado02.commons.kernel.errors.Problem;
import com.marcusprado02.commons.kernel.errors.Severity;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.Test;

class ResultAsyncTest {

  private static final Problem PROBLEM =
      Problem.of(ErrorCode.of("TEST.FAIL"), ErrorCategory.TECHNICAL, Severity.ERROR, "failure");

  @Test
  void mapAsync_shouldApplyFunctionOnOkResult() throws Exception {
    Result<Integer> result = Result.ok(5);

    CompletableFuture<Result<String>> future =
        result.mapAsync(n -> "value-" + n);

    Result<String> mapped = future.get();
    assertThat(mapped.isOk()).isTrue();
    assertThat(mapped.getOrNull()).isEqualTo("value-5");
  }

  @Test
  void mapAsync_shouldPassThroughFailure() throws Exception {
    Result<Integer> result = Result.fail(PROBLEM);

    CompletableFuture<Result<String>> future = result.mapAsync(n -> "value-" + n);

    Result<String> mapped = future.get();
    assertThat(mapped.isFail()).isTrue();
    assertThat(mapped.problemOrNull()).isEqualTo(PROBLEM);
  }

  @Test
  void flatMapAsync_shouldChainAsyncOperationsOnOkResult() throws Exception {
    Result<Integer> result = Result.ok(3);

    CompletableFuture<Result<String>> future =
        result.flatMapAsync(
            n -> CompletableFuture.completedFuture(Result.ok("async-" + n)));

    Result<String> chained = future.get();
    assertThat(chained.isOk()).isTrue();
    assertThat(chained.getOrNull()).isEqualTo("async-3");
  }

  @Test
  void flatMapAsync_shouldPropagateFailureFromFunction() throws Exception {
    Result<Integer> result = Result.ok(3);

    CompletableFuture<Result<String>> future =
        result.flatMapAsync(
            n -> CompletableFuture.completedFuture(Result.fail(PROBLEM)));

    Result<String> chained = future.get();
    assertThat(chained.isFail()).isTrue();
    assertThat(chained.problemOrNull()).isEqualTo(PROBLEM);
  }

  @Test
  void flatMapAsync_shouldShortCircuitOnFailInput() throws Exception {
    Result<Integer> failResult = Result.fail(PROBLEM);
    boolean[] functionCalled = {false};

    CompletableFuture<Result<String>> future =
        failResult.flatMapAsync(
            n -> {
              functionCalled[0] = true;
              return CompletableFuture.completedFuture(Result.ok("should-not-reach"));
            });

    Result<String> chained = future.get();
    assertThat(chained.isFail()).isTrue();
    assertThat(functionCalled[0]).isFalse();
  }

  @Test
  void toFuture_shouldWrapOkResultInCompletedFuture() throws Exception {
    Result<String> result = Result.ok("hello");

    CompletableFuture<Result<String>> future = result.toFuture();

    assertThat(future.isDone()).isTrue();
    assertThat(future.get().getOrNull()).isEqualTo("hello");
  }

  @Test
  void toFuture_shouldWrapFailResultInCompletedFuture() throws Exception {
    Result<String> result = Result.fail(PROBLEM);

    CompletableFuture<Result<String>> future = result.toFuture();

    assertThat(future.isDone()).isTrue();
    assertThat(future.get().isFail()).isTrue();
  }

  @Test
  void asyncPipeline_shouldComposeMultipleAsyncSteps() throws Exception {
    // Simulates: userId → User → Order
    CompletableFuture<Result<String>> pipeline =
        Result.ok(42)
            .flatMapAsync(id -> CompletableFuture.completedFuture(Result.ok("user-" + id)))
            .thenCompose(r -> r.flatMapAsync(
                user -> CompletableFuture.completedFuture(Result.ok("order-for-" + user))));

    Result<String> finalResult = pipeline.get();
    assertThat(finalResult.isOk()).isTrue();
    assertThat(finalResult.getOrNull()).isEqualTo("order-for-user-42");
  }
}
