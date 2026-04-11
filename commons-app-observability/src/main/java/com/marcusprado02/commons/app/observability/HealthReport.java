package com.marcusprado02.commons.app.observability;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Aggregated health report for a specific check type (liveness or readiness). */
public record HealthReport(
    HealthCheckType type, HealthStatus status, List<HealthCheckResult> checks, Instant checkedAt) {

  /** Validates health report fields and creates defensive copies. */
  public HealthReport {
    Objects.requireNonNull(type, "type must not be null");
    Objects.requireNonNull(status, "status must not be null");
    Objects.requireNonNull(checkedAt, "checkedAt must not be null");
    checks = (checks == null) ? List.of() : Collections.unmodifiableList(checks);
  }
}
