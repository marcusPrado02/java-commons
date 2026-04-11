package com.marcusprado02.commons.kernel.result;

import static org.junit.jupiter.api.Assertions.*;

import com.marcusprado02.commons.kernel.errors.ErrorCategory;
import com.marcusprado02.commons.kernel.errors.ErrorCode;
import com.marcusprado02.commons.kernel.errors.Problem;
import com.marcusprado02.commons.kernel.errors.Severity;
import java.util.List;
import org.junit.jupiter.api.Test;

class ResultCollectorsTest {

  private static final Problem PROBLEM =
      Problem.of(ErrorCode.of("TEST.ERR"), ErrorCategory.VALIDATION, Severity.ERROR, "error");

  // ---- firstFailure ----

  @Test
  void firstFailure_all_ok_returns_ok() {
    List<Result<Void>> results = List.of(Result.ok(null), Result.ok(null));
    Result<Void> out = ResultCollectors.firstFailure(results);
    assertTrue(out.isOk());
  }

  @Test
  void firstFailure_one_fail_returns_that_failure() {
    List<Result<Void>> results = List.of(Result.ok(null), Result.fail(PROBLEM));
    Result<Void> out = ResultCollectors.firstFailure(results);
    assertTrue(out.isFail());
    assertEquals(PROBLEM, out.problemOrNull());
  }

  @Test
  void firstFailure_empty_list_returns_ok() {
    Result<Void> out = ResultCollectors.firstFailure(List.of());
    assertTrue(out.isOk());
  }

  @Test
  void firstFailure_null_list_throws() {
    assertThrows(NullPointerException.class, () -> ResultCollectors.firstFailure(null));
  }

  // ---- allOrFirstFailure ----

  @Test
  void allOrFirstFailure_all_ok_returns_values() {
    List<Result<String>> results = List.of(Result.ok("a"), Result.ok("b"), Result.ok("c"));
    Result<List<String>> out = ResultCollectors.allOrFirstFailure(results);
    assertTrue(out.isOk());
    assertEquals(List.of("a", "b", "c"), out.getOrNull());
  }

  @Test
  void allOrFirstFailure_one_fail_returns_failure() {
    List<Result<String>> results = List.of(Result.ok("a"), Result.fail(PROBLEM), Result.ok("c"));
    Result<List<String>> out = ResultCollectors.allOrFirstFailure(results);
    assertTrue(out.isFail());
    assertEquals(PROBLEM, out.problemOrNull());
  }

  @Test
  void allOrFirstFailure_empty_list_returns_empty_ok() {
    Result<List<String>> out = ResultCollectors.allOrFirstFailure(List.of());
    assertTrue(out.isOk());
    assertTrue(out.getOrNull().isEmpty());
  }

  @Test
  void allOrFirstFailure_null_list_throws() {
    assertThrows(NullPointerException.class, () -> ResultCollectors.allOrFirstFailure(null));
  }
}
