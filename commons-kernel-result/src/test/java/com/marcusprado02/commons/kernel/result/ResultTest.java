package com.marcusprado02.commons.kernel.result;

import static org.junit.jupiter.api.Assertions.*;

import com.marcusprado02.commons.kernel.errors.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class ResultTest {

  private static final Problem NOT_FOUND =
      Problem.of(
          ErrorCode.of("TEST.NOT_FOUND"), ErrorCategory.NOT_FOUND, Severity.ERROR, "Not found");

  private static final Problem VALIDATION_ERROR =
      Problem.of(ErrorCode.of("TEST.INVALID"), ErrorCategory.VALIDATION, Severity.ERROR, "Invalid");

  // ---- ok / fail basics ----

  @Test
  void ok_should_be_ok_and_not_fail() {
    Result<String> r = Result.ok("hello");
    assertTrue(r.isOk());
    assertFalse(r.isFail());
    assertEquals("hello", r.getOrNull());
    assertNull(r.problemOrNull());
  }

  @Test
  void fail_should_be_fail_and_not_ok() {
    Result<String> r = Result.fail(NOT_FOUND);
    assertFalse(r.isOk());
    assertTrue(r.isFail());
    assertNull(r.getOrNull());
    assertEquals(NOT_FOUND, r.problemOrNull());
  }

  @Test
  void fail_requires_non_null_problem() {
    assertThrows(NullPointerException.class, () -> Result.fail(null));
  }

  // ---- getOrElse ----

  @Test
  void getOrElse_should_return_value_when_ok() {
    assertEquals("real", Result.ok("real").getOrElse("fallback"));
  }

  @Test
  void getOrElse_should_return_fallback_when_fail() {
    assertEquals("fallback", Result.<String>fail(NOT_FOUND).getOrElse("fallback"));
  }

  @Test
  void getOrElseGet_should_invoke_supplier_on_fail() {
    AtomicBoolean invoked = new AtomicBoolean(false);
    String result =
        Result.<String>fail(NOT_FOUND)
            .getOrElseGet(
                () -> {
                  invoked.set(true);
                  return "computed";
                });
    assertTrue(invoked.get());
    assertEquals("computed", result);
  }

  // ---- map ----

  @Test
  void map_should_transform_value_when_ok() {
    Result<Integer> r = Result.ok("hello").map(String::length);
    assertTrue(r.isOk());
    assertEquals(5, r.getOrNull());
  }

  @Test
  void map_should_propagate_failure() {
    Result<Integer> r = Result.<String>fail(NOT_FOUND).map(String::length);
    assertTrue(r.isFail());
    assertEquals(NOT_FOUND, r.problemOrNull());
  }

  // ---- flatMap ----

  @Test
  void flatMap_should_chain_ok_results() {
    Result<Integer> r = Result.ok("hello").flatMap(s -> Result.ok(s.length()));
    assertEquals(5, r.getOrNull());
  }

  @Test
  void flatMap_should_short_circuit_on_first_failure() {
    Result<Integer> r = Result.<String>fail(NOT_FOUND).flatMap(s -> Result.ok(s.length()));
    assertTrue(r.isFail());
    assertEquals(NOT_FOUND, r.problemOrNull());
  }

  @Test
  void flatMap_should_return_inner_failure() {
    Result<Integer> r = Result.ok("hello").flatMap(s -> Result.fail(VALIDATION_ERROR));
    assertTrue(r.isFail());
    assertEquals(VALIDATION_ERROR, r.problemOrNull());
  }

  // ---- mapError ----

  @Test
  void mapError_should_transform_problem_when_fail() {
    Problem enriched =
        Problem.of(ErrorCode.of("ENRICHED"), ErrorCategory.NOT_FOUND, Severity.ERROR, "enriched");
    Result<String> r = Result.<String>fail(NOT_FOUND).mapError(p -> enriched);
    assertEquals(enriched, r.problemOrNull());
  }

  @Test
  void mapError_should_not_affect_ok() {
    Result<String> ok = Result.ok("value");
    Result<String> r = ok.mapError(p -> VALIDATION_ERROR);
    assertTrue(r.isOk());
    assertEquals("value", r.getOrNull());
  }

  // ---- orElse / orElseGet ----

  @Test
  void orElse_should_return_self_when_ok() {
    Result<String> ok = Result.ok("a");
    assertSame(ok, ok.orElse(Result.ok("b")));
  }

  @Test
  void orElse_should_return_fallback_when_fail() {
    Result<String> fallback = Result.ok("fallback");
    assertSame(fallback, Result.<String>fail(NOT_FOUND).orElse(fallback));
  }

  // ---- recover ----

  @Test
  void recover_should_turn_failure_into_success() {
    Result<String> r = Result.<String>fail(NOT_FOUND).recover(p -> "default-" + p.code().value());
    assertTrue(r.isOk());
    assertEquals("default-TEST.NOT_FOUND", r.getOrNull());
  }

  @Test
  void recover_should_not_change_ok() {
    Result<String> r = Result.ok("original").recover(p -> "recovered");
    assertEquals("original", r.getOrNull());
  }

  // ---- recoverWith ----

  @Test
  void recoverWith_should_allow_async_style_recovery() {
    Result<String> r = Result.<String>fail(NOT_FOUND).recoverWith(p -> Result.ok("recovered"));
    assertTrue(r.isOk());
    assertEquals("recovered", r.getOrNull());
  }

  // ---- peek / peekError ----

  @Test
  void peek_should_invoke_consumer_when_ok() {
    List<String> captured = new ArrayList<>();
    Result.ok("hello").peek(captured::add);
    assertEquals(List.of("hello"), captured);
  }

  @Test
  void peek_should_not_invoke_consumer_when_fail() {
    List<String> captured = new ArrayList<>();
    Result.<String>fail(NOT_FOUND).peek(captured::add);
    assertTrue(captured.isEmpty());
  }

  @Test
  void peekError_should_invoke_consumer_when_fail() {
    AtomicReference<Problem> captured = new AtomicReference<>();
    Result.<String>fail(NOT_FOUND).peekError(captured::set);
    assertEquals(NOT_FOUND, captured.get());
  }

  @Test
  void peekError_should_not_invoke_consumer_when_ok() {
    AtomicReference<Problem> captured = new AtomicReference<>();
    Result.ok("hello").peekError(captured::set);
    assertNull(captured.get());
  }

  // ---- fold ----

  @Test
  void fold_should_apply_success_function_when_ok() {
    String result = Result.ok("hello").fold(p -> "error", String::toUpperCase);
    assertEquals("HELLO", result);
  }

  @Test
  void fold_should_apply_error_function_when_fail() {
    String result = Result.<String>fail(NOT_FOUND).fold(p -> "error:" + p.code().value(), s -> s);
    assertEquals("error:TEST.NOT_FOUND", result);
  }

  // ---- filter ----

  @Test
  void filter_should_pass_when_predicate_holds() {
    Result<Integer> r = Result.ok(10).filter(n -> n > 5, () -> VALIDATION_ERROR);
    assertTrue(r.isOk());
    assertEquals(10, r.getOrNull());
  }

  @Test
  void filter_should_fail_when_predicate_fails() {
    Result<Integer> r = Result.ok(3).filter(n -> n > 5, () -> VALIDATION_ERROR);
    assertTrue(r.isFail());
    assertEquals(VALIDATION_ERROR, r.problemOrNull());
  }

  @Test
  void filter_should_propagate_existing_failure() {
    Result<Integer> r = Result.<Integer>fail(NOT_FOUND).filter(n -> n > 5, () -> VALIDATION_ERROR);
    assertTrue(r.isFail());
    assertEquals(NOT_FOUND, r.problemOrNull()); // original failure preserved
  }

  // ---- async ----

  @Test
  void mapAsync_should_resolve_with_mapped_value() throws Exception {
    var future = Result.ok("hello").mapAsync(String::length);
    Result<Integer> r = future.get();
    assertTrue(r.isOk());
    assertEquals(5, r.getOrNull());
  }

  @Test
  void mapAsync_should_propagate_failure_without_executing_fn() throws Exception {
    AtomicBoolean fnCalled = new AtomicBoolean(false);
    var future =
        Result.<String>fail(NOT_FOUND)
            .mapAsync(
                s -> {
                  fnCalled.set(true);
                  return s.length();
                });
    Result<Integer> r = future.get();
    assertTrue(r.isFail());
    assertFalse(fnCalled.get());
  }

  @Test
  void toFuture_should_wrap_result_in_completed_future() throws Exception {
    Result<String> original = Result.ok("wrapped");
    Result<String> fromFuture = original.toFuture().get();
    assertSame(original, fromFuture);
  }
}
