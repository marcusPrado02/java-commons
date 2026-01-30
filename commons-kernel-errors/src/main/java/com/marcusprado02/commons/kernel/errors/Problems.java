package com.marcusprado02.commons.kernel.errors;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class Problems {

  private Problems() {}

  public static Problem validation(String code, String message) {
    return Problem.of(ErrorCode.of(code), ErrorCategory.VALIDATION, Severity.ERROR, message);
  }

  public static Problem business(String code, String message) {
    return Problem.of(ErrorCode.of(code), ErrorCategory.BUSINESS, Severity.ERROR, message);
  }

  public static Problem notFound(String code, String message) {
    return Problem.of(ErrorCode.of(code), ErrorCategory.NOT_FOUND, Severity.ERROR, message);
  }

  public static Problem conflict(String code, String message) {
    return Problem.of(ErrorCode.of(code), ErrorCategory.CONFLICT, Severity.ERROR, message);
  }

  public static Problem technical(String code, String message) {
    return Problem.of(ErrorCode.of(code), ErrorCategory.TECHNICAL, Severity.ERROR, message);
  }

  public static Problem withDetails(
      Problem base, List<ProblemDetail> details, Map<String, Object> meta) {
    Objects.requireNonNull(base, "base");
    return new Problem(
        base.code(),
        base.category(),
        base.severity(),
        base.message(),
        details,
        meta,
        base.timestamp());
  }
}
