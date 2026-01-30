package com.marcusprado02.commons.kernel.errors;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record Problem(
    ErrorCode code,
    ErrorCategory category,
    Severity severity,
    String message,
    List<ProblemDetail> details,
    Map<String, Object> meta,
    Instant timestamp) {
  public Problem {
    Objects.requireNonNull(code, "code");
    Objects.requireNonNull(category, "category");
    Objects.requireNonNull(severity, "severity");
    Objects.requireNonNull(message, "message");
    details = details == null ? List.of() : List.copyOf(details);
    meta = meta == null ? Map.of() : Map.copyOf(meta);
    timestamp = timestamp == null ? Instant.now() : timestamp;
  }

  public static Problem of(
      ErrorCode code, ErrorCategory category, Severity severity, String message) {
    return new Problem(code, category, severity, message, List.of(), Map.of(), Instant.now());
  }
}
