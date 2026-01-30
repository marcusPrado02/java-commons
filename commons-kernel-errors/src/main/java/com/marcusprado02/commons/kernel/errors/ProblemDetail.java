package com.marcusprado02.commons.kernel.errors;

import java.util.Map;

public record ProblemDetail(String field, String message, Map<String, Object> meta) {
  public static ProblemDetail of(String field, String message) {
    return new ProblemDetail(field, message, Map.of());
  }
}
