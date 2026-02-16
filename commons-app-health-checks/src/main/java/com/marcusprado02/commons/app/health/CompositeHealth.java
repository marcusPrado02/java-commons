package com.marcusprado02.commons.app.health;

import java.time.Instant;
import java.util.Map;

/** Composite health check result containing multiple individual checks. */
public final class CompositeHealth {

  private final HealthStatus status;
  private final Map<String, Health> components;
  private final Instant timestamp;

  public CompositeHealth(HealthStatus status, Map<String, Health> components, Instant timestamp) {
    this.status = status;
    this.components = Map.copyOf(components);
    this.timestamp = timestamp;
  }

  public HealthStatus getStatus() {
    return status;
  }

  public Map<String, Health> getComponents() {
    return components;
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  @Override
  public String toString() {
    return String.format("CompositeHealth{status=%s, components=%d}", status, components.size());
  }
}
