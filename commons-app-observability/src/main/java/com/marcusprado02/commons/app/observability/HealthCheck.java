package com.marcusprado02.commons.app.observability;

/** Framework-agnostic health check. */
public interface HealthCheck {

  String name();

  HealthCheckType type();

  HealthCheckResult check();
}
