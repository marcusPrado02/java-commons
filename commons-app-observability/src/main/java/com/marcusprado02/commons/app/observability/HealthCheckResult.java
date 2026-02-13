package com.marcusprado02.commons.app.observability;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record HealthCheckResult(
    String name,
    HealthCheckType type,
    HealthStatus status,
    Map<String, Object> details,
    Instant checkedAt) {

  public HealthCheckResult {
    Objects.requireNonNull(name, "name must not be null");
    Objects.requireNonNull(type, "type must not be null");
    Objects.requireNonNull(status, "status must not be null");
    Objects.requireNonNull(checkedAt, "checkedAt must not be null");
    details = (details == null) ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(details));
  }

  public static HealthCheckResult up(String name, HealthCheckType type) {
    return new HealthCheckResult(name, type, HealthStatus.UP, Map.of(), Instant.now());
  }

  public static HealthCheckResult down(String name, HealthCheckType type, Map<String, Object> details) {
    return new HealthCheckResult(name, type, HealthStatus.DOWN, details, Instant.now());
  }

  public static HealthCheckResult degraded(
      String name, HealthCheckType type, Map<String, Object> details) {
    return new HealthCheckResult(name, type, HealthStatus.DEGRADED, details, Instant.now());
  }
}
