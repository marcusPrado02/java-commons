package com.marcusprado02.commons.app.observability;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Registry and evaluator for liveness/readiness checks.
 *
 * <p>Designed to be framework-agnostic; adapters/starters can expose reports via HTTP endpoints.
 */
public final class HealthChecks {

  private final List<HealthCheck> checks;
  private final Clock clock;

  public HealthChecks(List<HealthCheck> checks) {
    this(checks, Clock.systemUTC());
  }

  public HealthChecks(List<HealthCheck> checks, Clock clock) {
    this.checks =
        Collections.unmodifiableList(new ArrayList<>(checks == null ? List.of() : checks));
    this.clock = Objects.requireNonNull(clock, "clock must not be null");
  }

  public HealthReport liveness() {
    return evaluate(HealthCheckType.LIVENESS);
  }

  public HealthReport readiness() {
    return evaluate(HealthCheckType.READINESS);
  }

  public HealthReport evaluate(HealthCheckType type) {
    Objects.requireNonNull(type, "type must not be null");

    List<HealthCheckResult> results = new ArrayList<>();
    for (HealthCheck check : checks) {
      if (check == null || check.type() != type) {
        continue;
      }

      try {
        HealthCheckResult result = check.check();
        results.add(result);
      } catch (RuntimeException ex) {
        results.add(
            HealthCheckResult.down(
                check.name(),
                check.type(),
                java.util.Map.of("error", ex.getClass().getName(), "message", ex.getMessage())));
      }
    }

    HealthStatus overall = aggregate(results);
    Instant now = clock.instant();
    return new HealthReport(type, overall, results, now);
  }

  private static HealthStatus aggregate(List<HealthCheckResult> results) {
    boolean anyDegraded = false;

    for (HealthCheckResult r : results) {
      if (r.status() == HealthStatus.DOWN) {
        return HealthStatus.DOWN;
      }
      if (r.status() == HealthStatus.DEGRADED) {
        anyDegraded = true;
      }
    }

    return anyDegraded ? HealthStatus.DEGRADED : HealthStatus.UP;
  }
}
