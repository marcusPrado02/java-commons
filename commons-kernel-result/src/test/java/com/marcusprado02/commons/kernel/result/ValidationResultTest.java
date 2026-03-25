package com.marcusprado02.commons.kernel.result;

import static org.junit.jupiter.api.Assertions.*;

import com.marcusprado02.commons.kernel.errors.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class ValidationResultTest {

  private static Problem problem(String code) {
    return Problem.of(ErrorCode.of(code), ErrorCategory.VALIDATION, Severity.ERROR, code);
  }

  // ---- valid / invalid basics ----

  @Test
  void valid_should_be_valid_with_value() {
    ValidationResult<String> r = ValidationResult.valid("ok");
    assertTrue(r.isValid());
    assertFalse(r.isInvalid());
    assertEquals("ok", r.getValue());
    assertTrue(r.getProblems().isEmpty());
  }

  @Test
  void invalid_single_should_not_be_valid() {
    Problem p = problem("FIELD.REQUIRED");
    ValidationResult<String> r = ValidationResult.invalid(p);
    assertFalse(r.isValid());
    assertTrue(r.isInvalid());
    assertEquals(1, r.getProblems().size());
    assertEquals(p, r.getProblems().get(0));
  }

  @Test
  void invalid_multiple_should_aggregate_all_errors() {
    Problem p1 = problem("FIELD.REQUIRED");
    Problem p2 = problem("FIELD.FORMAT");
    ValidationResult<String> r = ValidationResult.invalid(List.of(p1, p2));
    assertEquals(2, r.getProblems().size());
    assertTrue(r.isInvalid());
  }

  @Test
  void invalid_requires_non_null_problem() {
    assertThrows(NullPointerException.class, () -> ValidationResult.invalid((Problem) null));
  }

  @Test
  void invalid_with_empty_list_should_throw() {
    assertThrows(
        IllegalArgumentException.class, () -> ValidationResult.invalid(List.of()));
  }

  // ---- of(Result) ----

  @Test
  void of_ok_result_should_be_valid() {
    ValidationResult<String> vr = ValidationResult.of(Result.ok("hello"));
    assertTrue(vr.isValid());
    assertEquals("hello", vr.getValue());
  }

  @Test
  void of_fail_result_should_be_invalid() {
    Problem p = problem("ERR");
    ValidationResult<String> vr = ValidationResult.of(Result.fail(p));
    assertTrue(vr.isInvalid());
    assertEquals(p, vr.getProblems().get(0));
  }

  // ---- toResult ----

  @Test
  void toResult_on_valid_should_return_ok() {
    ValidationResult<String> vr = ValidationResult.valid("value");
    Result<String> r = vr.toResult();
    assertTrue(r.isOk());
    assertEquals("value", r.getOrNull());
  }

  @Test
  void toResult_on_single_invalid_should_return_fail_with_that_problem() {
    Problem p = problem("CODE1");
    ValidationResult<String> vr = ValidationResult.invalid(p);
    Result<String> r = vr.toResult();
    assertTrue(r.isFail());
    assertEquals(p, r.problemOrNull());
  }

  @Test
  void toResult_on_multiple_invalid_should_aggregate_into_multiple_errors_code() {
    Problem p1 = problem("CODE1");
    Problem p2 = problem("CODE2");
    ValidationResult<String> vr = ValidationResult.invalid(List.of(p1, p2));
    Result<String> r = vr.toResult();
    assertTrue(r.isFail());
    assertEquals("VALIDATION.MULTIPLE_ERRORS", r.problemOrNull().code().value());
  }

  // ---- map ----

  @Test
  void map_should_transform_value_when_valid() {
    ValidationResult<Integer> r = ValidationResult.valid("hello").map(String::length);
    assertTrue(r.isValid());
    assertEquals(5, r.getValue());
  }

  @Test
  void map_should_propagate_errors_when_invalid() {
    Problem p = problem("ERR");
    ValidationResult<Integer> r = ValidationResult.<String>invalid(p).map(String::length);
    assertTrue(r.isInvalid());
    assertEquals(p, r.getProblems().get(0));
  }

  // ---- combine ----

  @Test
  void combine_two_valid_results_should_produce_valid() {
    ValidationResult<String> r1 = ValidationResult.valid("hello");
    ValidationResult<Integer> r2 = ValidationResult.valid(5);
    ValidationResult<String> combined = r1.combine(r2, s -> n -> s + "-" + n);
    assertTrue(combined.isValid());
    assertEquals("hello-5", combined.getValue());
  }

  @Test
  void combine_should_collect_all_errors_from_both() {
    Problem p1 = problem("ERR1");
    Problem p2 = problem("ERR2");
    ValidationResult<String> invalid1 = ValidationResult.invalid(p1);
    ValidationResult<Integer> invalid2 = ValidationResult.invalid(p2);
    ValidationResult<String> combined = invalid1.combine(invalid2, s -> n -> s);
    assertTrue(combined.isInvalid());
    assertEquals(2, combined.getProblems().size());
  }

  // ---- andThen ----

  @Test
  void andThen_should_chain_validation_when_valid() {
    ValidationResult<String> base = ValidationResult.valid("hello");
    ValidationResult<String> result =
        base.andThen(
            s -> s.length() > 3
                ? ValidationResult.valid(s.toUpperCase())
                : ValidationResult.invalid(problem("TOO_SHORT")));
    assertTrue(result.isValid());
    assertEquals("HELLO", result.getValue());
  }

  @Test
  void andThen_on_invalid_should_preserve_original_errors() {
    Problem p = problem("EXISTING");
    ValidationResult<String> base = ValidationResult.invalid(p);
    ValidationResult<String> result =
        base.andThen(s -> ValidationResult.valid(s.toUpperCase()));
    assertTrue(result.isInvalid());
    assertEquals(1, result.getProblems().size());
    assertEquals(p, result.getProblems().get(0));
  }

  // ---- Builder ----

  @Test
  void builder_with_all_ok_results_should_produce_valid() {
    Problem p = problem("ERR");
    ValidationResult<String> vr =
        ValidationResult.builder()
            .add(Result.ok("a"))
            .add(Result.ok("b"))
            .build("final value");
    assertTrue(vr.isValid());
    assertEquals("final value", vr.getValue());
  }

  @Test
  void builder_with_failures_should_aggregate_all_errors() {
    Problem p1 = problem("ERR1");
    Problem p2 = problem("ERR2");
    ValidationResult<String> vr =
        ValidationResult.builder()
            .add(Result.fail(p1))
            .add(Result.ok("ok"))
            .add(Result.fail(p2))
            .build("value");
    assertTrue(vr.isInvalid());
    assertEquals(2, vr.getProblems().size());
  }
}
