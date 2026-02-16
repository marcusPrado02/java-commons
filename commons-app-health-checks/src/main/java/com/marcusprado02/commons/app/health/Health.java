package com.marcusprado02.commons.app.health;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Result of a health check.
 *
 * <p>Contains the status and optional details about the health check.
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * Health health = Health.up()
 *     .withDetail("version", "1.0.0")
 *     .withDetail("connections", 5)
 *     .build();
 *
 * Health health = Health.down()
 *     .withException(new IOException("Connection failed"))
 *     .build();
 * }</pre>
 */
public final class Health {

  private final HealthStatus status;
  private final Map<String, Object> details;
  private final Instant timestamp;

  private Health(Builder builder) {
    this.status = Objects.requireNonNull(builder.status, "status cannot be null");
    this.details = Collections.unmodifiableMap(new LinkedHashMap<>(builder.details));
    this.timestamp = builder.timestamp != null ? builder.timestamp : Instant.now();
  }

  /**
   * Creates a health check with UP status.
   *
   * @return builder for UP status
   */
  public static Builder up() {
    return new Builder(HealthStatus.UP);
  }

  /**
   * Creates a health check with DOWN status.
   *
   * @return builder for DOWN status
   */
  public static Builder down() {
    return new Builder(HealthStatus.DOWN);
  }

  /**
   * Creates a health check with DEGRADED status.
   *
   * @return builder for DEGRADED status
   */
  public static Builder degraded() {
    return new Builder(HealthStatus.DEGRADED);
  }

  /**
   * Creates a health check with UNKNOWN status.
   *
   * @return builder for UNKNOWN status
   */
  public static Builder unknown() {
    return new Builder(HealthStatus.UNKNOWN);
  }

  /**
   * Creates a health check with the specified status.
   *
   * @param status the health status
   * @return builder
   */
  public static Builder status(HealthStatus status) {
    return new Builder(status);
  }

  public HealthStatus getStatus() {
    return status;
  }

  public Map<String, Object> getDetails() {
    return details;
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  @Override
  public String toString() {
    return String.format("Health{status=%s, details=%s}", status, details);
  }

  public static final class Builder {
    private final HealthStatus status;
    private final Map<String, Object> details = new LinkedHashMap<>();
    private Instant timestamp;

    private Builder(HealthStatus status) {
      this.status = status;
    }

    /**
     * Adds a detail to the health check.
     *
     * @param key the detail key
     * @param value the detail value
     * @return this builder
     */
    public Builder withDetail(String key, Object value) {
      this.details.put(key, value);
      return this;
    }

    /**
     * Adds multiple details to the health check.
     *
     * @param details the details to add
     * @return this builder
     */
    public Builder withDetails(Map<String, ?> details) {
      this.details.putAll(details);
      return this;
    }

    /**
     * Adds exception details to the health check.
     *
     * @param exception the exception
     * @return this builder
     */
    public Builder withException(Throwable exception) {
      this.details.put("error", exception.getClass().getName());
      this.details.put("message", exception.getMessage());
      return this;
    }

    /**
     * Sets a custom timestamp.
     *
     * @param timestamp the timestamp
     * @return this builder
     */
    public Builder timestamp(Instant timestamp) {
      this.timestamp = timestamp;
      return this;
    }

    /**
     * Builds the health check result.
     *
     * @return health check
     */
    public Health build() {
      return new Health(this);
    }
  }
}
