package com.marcusprado02.commons.kernel.errors;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ProblemsTest {

  @AfterEach
  void resetResolver() {
    Problems.setMessageResolver(I18nMessageResolver.noOp());
  }

  // ---- validation ----

  @Test
  void validation_should_create_problem_with_correct_category() {
    Problem p = Problems.validation("VALIDATION.REQUIRED_FIELD", "Field is required");

    assertEquals("VALIDATION.REQUIRED_FIELD", p.code().value());
    assertEquals(ErrorCategory.VALIDATION, p.category());
    assertEquals(Severity.ERROR, p.severity());
    assertEquals("Field is required", p.message());
  }

  @Test
  void validation_i18n_should_use_noOp_resolver_by_default() {
    Problem p =
        Problems.validation(
            "VALIDATION.REQUIRED_FIELD",
            "key.required",
            Locale.ENGLISH,
            "default message");

    assertEquals("default message", p.message());
  }

  // ---- business ----

  @Test
  void business_should_create_problem_with_business_category() {
    Problem p = Problems.business("BUSINESS.RULE_VIOLATION", "Insufficient balance");

    assertEquals(ErrorCategory.BUSINESS, p.category());
    assertEquals("Insufficient balance", p.message());
  }

  // ---- notFound ----

  @Test
  void notFound_should_create_problem_with_not_found_category() {
    Problem p = Problems.notFound("NOT_FOUND.ENTITY", "User not found");

    assertEquals(ErrorCategory.NOT_FOUND, p.category());
    assertEquals("NOT_FOUND.ENTITY", p.code().value());
  }

  // ---- conflict ----

  @Test
  void conflict_should_create_problem_with_conflict_category() {
    Problem p = Problems.conflict("CONFLICT.VERSION", "Stale version");

    assertEquals(ErrorCategory.CONFLICT, p.category());
  }

  // ---- unauthorized / forbidden ----

  @Test
  void unauthorized_should_create_problem_with_unauthorized_category() {
    Problem p = Problems.unauthorized("UNAUTHORIZED.TOKEN_EXPIRED", "Session expired");

    assertEquals(ErrorCategory.UNAUTHORIZED, p.category());
  }

  @Test
  void forbidden_should_create_problem_with_forbidden_category() {
    Problem p = Problems.forbidden("FORBIDDEN.ACCESS_DENIED", "Access denied");

    assertEquals(ErrorCategory.FORBIDDEN, p.category());
  }

  // ---- technical ----

  @Test
  void technical_should_create_problem_with_technical_category_and_critical_severity() {
    Problem p = Problems.technical("TECHNICAL.DATABASE_ERROR", "DB connection failed");

    assertEquals(ErrorCategory.TECHNICAL, p.category());
    assertEquals(Severity.CRITICAL, p.severity());
  }

  // ---- setMessageResolver ----

  @Test
  void setMessageResolver_should_affect_subsequent_i18n_creations() {
    Problems.setMessageResolver(
        (key, locale, defaultMsg, args) -> "RESOLVED: " + key);

    Problem p =
        Problems.validation("CODE", "my.key", Locale.ENGLISH, "default");

    assertEquals("RESOLVED: my.key", p.message());
  }

  @Test
  void setMessageResolver_null_should_throw() {
    assertThrows(NullPointerException.class, () -> Problems.setMessageResolver(null));
  }

  // ---- Problem record ----

  @Test
  void problem_of_should_set_timestamp() {
    Problem p =
        Problem.of(
            ErrorCode.of("X"), ErrorCategory.VALIDATION, Severity.ERROR, "msg");

    assertNotNull(p.timestamp());
    assertTrue(p.details().isEmpty());
    assertTrue(p.meta().isEmpty());
  }

  @Test
  void problem_requires_non_null_code() {
    assertThrows(
        NullPointerException.class,
        () -> Problem.of(null, ErrorCategory.VALIDATION, Severity.ERROR, "msg"));
  }

  @Test
  void problem_requires_non_null_message() {
    assertThrows(
        NullPointerException.class,
        () -> Problem.of(ErrorCode.of("X"), ErrorCategory.VALIDATION, Severity.ERROR, null));
  }

  // ---- ErrorCode ----

  @Test
  void errorCode_value_should_return_the_code_string() {
    ErrorCode code = ErrorCode.of("BACKUP.NOT_FOUND");
    assertEquals("BACKUP.NOT_FOUND", code.value());
  }
}
