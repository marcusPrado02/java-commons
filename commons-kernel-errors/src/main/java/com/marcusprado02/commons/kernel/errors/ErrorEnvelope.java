package com.marcusprado02.commons.kernel.errors;

import java.util.Map;

public record ErrorEnvelope(Problem problem, String correlationId, Map<String, Object> context) {
  public static ErrorEnvelope of(Problem problem) {
    return new ErrorEnvelope(problem, null, Map.of());
  }
}
