package com.marcusprado02.commons.app.observability;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

public final class Metrics {
  private Metrics() {}

  public static final String REQUESTS_TOTAL = "requests.total";
  public static final String ERRORS_TOTAL = "requests.errors";
  public static final String LATENCY_MS = "requests.latency_ms";

  /**
   * Records a minimal set of SLI metrics for an operation.
   *
   * <p>Attributes (suggested): operation, outcome.
   */
  public static void recordRequest(
      MetricsFacade metrics, String operation, Duration latency, boolean success) {
    Objects.requireNonNull(metrics, "metrics must not be null");

    String safeOp = (operation == null || operation.isBlank()) ? "unknown" : operation.trim();
    String outcome = success ? "success" : "error";

    Map<String, String> attrs = Map.of("operation", safeOp, "outcome", outcome);
    metrics.incrementCounter(REQUESTS_TOTAL, 1, attrs);
    if (!success) {
      metrics.incrementCounter(ERRORS_TOTAL, 1, attrs);
    }
    if (latency != null) {
      metrics.recordHistogram(LATENCY_MS, latency.toMillis(), attrs);
    }
  }
}
