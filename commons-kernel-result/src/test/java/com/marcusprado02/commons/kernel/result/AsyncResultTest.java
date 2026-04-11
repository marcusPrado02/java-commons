package com.marcusprado02.commons.kernel.result;

import static org.junit.jupiter.api.Assertions.*;

import com.marcusprado02.commons.kernel.errors.ErrorCategory;
import com.marcusprado02.commons.kernel.errors.ErrorCode;
import com.marcusprado02.commons.kernel.errors.Problem;
import com.marcusprado02.commons.kernel.errors.Severity;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class AsyncResultTest {

  private static final Problem PROBLEM =
      Problem.of(ErrorCode.of("TEST.ERR"), ErrorCategory.TECHNICAL, Severity.ERROR, "error");

  // ---- factory methods ----

  @Test
  void ok_creates_completed_ok_result() {
    Result<String> r = AsyncResult.ok("hello").join();
    assertTrue(r.isOk());
    assertEquals("hello", r.getOrNull());
  }

  @Test
  void fail_creates_completed_fail_result() {
    Result<String> r = AsyncResult.<String>fail(PROBLEM).join();
    assertTrue(r.isFail());
    assertEquals(PROBLEM, r.problemOrNull());
  }

  @Test
  void of_result_wraps_result() {
    Result<Integer> inner = Result.ok(7);
    Result<Integer> r = AsyncResult.of(inner).join();
    assertTrue(r.isOk());
    assertEquals(7, r.getOrNull());
  }

  @Test
  void of_future_wraps_future() throws Exception {
    CompletableFuture<Result<String>> future = CompletableFuture.completedFuture(Result.ok("x"));
    Result<String> r = AsyncResult.of(future).join();
    assertTrue(r.isOk());
    assertEquals("x", r.getOrNull());
  }

  @Test
  void supply_executes_supplier_async() {
    AsyncResult<String> ar = AsyncResult.supply(() -> Result.ok("supplied"));
    assertTrue(ar.join().isOk());
    assertEquals("supplied", ar.join().getOrNull());
  }

  // ---- map ----

  @Test
  void map_transforms_ok_value() {
    Result<String> r = AsyncResult.ok(5).map(n -> "v" + n).join();
    assertTrue(r.isOk());
    assertEquals("v5", r.getOrNull());
  }

  @Test
  void map_passes_through_failure() {
    Result<String> r = AsyncResult.<Integer>fail(PROBLEM).map(n -> "v" + n).join();
    assertTrue(r.isFail());
  }

  // ---- flatMap ----

  @Test
  void flatMap_chains_ok_results() {
    Result<String> r = AsyncResult.ok(3).flatMap(n -> Result.ok("flat-" + n)).join();
    assertTrue(r.isOk());
    assertEquals("flat-3", r.getOrNull());
  }

  @Test
  void flatMapAsync_chains_ok_results() {
    Result<String> r = AsyncResult.ok(3).flatMapAsync(n -> AsyncResult.ok("async-" + n)).join();
    assertTrue(r.isOk());
    assertEquals("async-3", r.getOrNull());
  }

  @Test
  void flatMapAsync_short_circuits_on_failure() {
    AtomicBoolean called = new AtomicBoolean(false);
    Result<String> r =
        AsyncResult.<Integer>fail(PROBLEM)
            .flatMapAsync(
                n -> {
                  called.set(true);
                  return AsyncResult.ok("reached");
                })
            .join();
    assertTrue(r.isFail());
    assertFalse(called.get());
  }

  // ---- mapError ----

  @Test
  void mapError_transforms_problem_on_failure() {
    Problem mapped =
        Problem.of(ErrorCode.of("NEW"), ErrorCategory.VALIDATION, Severity.WARNING, "new");
    Result<String> r = AsyncResult.<String>fail(PROBLEM).mapError(p -> mapped).join();
    assertTrue(r.isFail());
    assertEquals(mapped, r.problemOrNull());
  }

  // ---- recover ----

  @Test
  void recover_converts_failure_to_ok() {
    Result<String> r = AsyncResult.<String>fail(PROBLEM).recover(p -> "recovered").join();
    assertTrue(r.isOk());
    assertEquals("recovered", r.getOrNull());
  }

  @Test
  void recoverWith_converts_failure_to_ok_result() {
    Result<String> r =
        AsyncResult.<String>fail(PROBLEM).recoverWith(p -> Result.ok("recWith")).join();
    assertTrue(r.isOk());
    assertEquals("recWith", r.getOrNull());
  }

  @Test
  void recoverWithAsync_converts_failure_to_ok() {
    Result<String> r =
        AsyncResult.<String>fail(PROBLEM).recoverWithAsync(p -> AsyncResult.ok("asyncRec")).join();
    assertTrue(r.isOk());
    assertEquals("asyncRec", r.getOrNull());
  }

  @Test
  void recoverWithAsync_leaves_ok_unchanged() {
    Result<String> r =
        AsyncResult.ok("original").recoverWithAsync(p -> AsyncResult.ok("should-not-reach")).join();
    assertTrue(r.isOk());
    assertEquals("original", r.getOrNull());
  }

  // ---- peek / peekError ----

  @Test
  void peek_executes_on_ok() {
    AtomicBoolean called = new AtomicBoolean(false);
    AsyncResult.ok("val").peek(v -> called.set(true)).join();
    assertTrue(called.get());
  }

  @Test
  void peekError_executes_on_failure() {
    AtomicBoolean called = new AtomicBoolean(false);
    AsyncResult.<String>fail(PROBLEM).peekError(p -> called.set(true)).join();
    assertTrue(called.get());
  }

  // ---- toCompletableFuture / toCompletableFutureOfValue ----

  @Test
  void toCompletableFuture_returns_underlying_future() throws Exception {
    CompletableFuture<Result<String>> cf = AsyncResult.ok("x").toCompletableFuture();
    assertTrue(cf.isDone());
    assertTrue(cf.get().isOk());
  }

  @Test
  void toCompletableFutureOfValue_returns_value_on_ok() throws Exception {
    String val = AsyncResult.ok("direct").toCompletableFutureOfValue().get();
    assertEquals("direct", val);
  }

  @Test
  void toCompletableFutureOfValue_throws_on_failure() {
    assertThrows(
        Exception.class,
        () -> AsyncResult.<String>fail(PROBLEM).toCompletableFutureOfValue().get());
  }
}
