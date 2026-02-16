package com.marcusprado02.commons.ports.servicediscovery;

import java.time.Duration;
import java.util.Objects;

/**
 * Represents a health check configuration for a service instance.
 *
 * <p>Defines how the service registry should monitor the health of a registered service. Supports
 * HTTP, TCP, and TTL-based health checks.
 *
 * <p>Example:
 *
 * <pre>{@code
 * HealthCheck httpCheck = HealthCheck.http("http://localhost:8080/health")
 *     .interval(Duration.ofSeconds(10))
 *     .timeout(Duration.ofSeconds(5))
 *     .build();
 *
 * HealthCheck tcpCheck = HealthCheck.tcp("localhost:8080")
 *     .interval(Duration.ofSeconds(30))
 *     .build();
 *
 * HealthCheck ttlCheck = HealthCheck.ttl(Duration.ofSeconds(30))
 *     .build();
 * }</pre>
 *
 * @param type the health check type
 * @param endpoint the check endpoint (URL for HTTP, host:port for TCP, null for TTL)
 * @param interval how often to perform the check
 * @param timeout maximum time to wait for a response
 * @param deregisterAfter time to wait before deregistering an unhealthy service
 * @author Marcus Prado
 * @since 1.0.0
 */
public record HealthCheck(
    Type type, String endpoint, Duration interval, Duration timeout, Duration deregisterAfter) {

  public HealthCheck {
    Objects.requireNonNull(type, "type cannot be null");
    Objects.requireNonNull(interval, "interval cannot be null");
    if (type != Type.TTL) {
      Objects.requireNonNull(endpoint, "endpoint cannot be null for " + type + " checks");
    }
  }

  /** Health check type. */
  public enum Type {
    /** HTTP health check - polls an HTTP endpoint. */
    HTTP,
    /** TCP health check - attempts to establish TCP connection. */
    TCP,
    /** TTL health check - requires service to send heartbeats. */
    TTL
  }

  /**
   * Creates an HTTP health check builder.
   *
   * @param url the HTTP URL to poll
   * @return builder instance
   */
  public static Builder http(String url) {
    return new Builder(Type.HTTP, url);
  }

  /**
   * Creates a TCP health check builder.
   *
   * @param hostPort the host:port to connect to
   * @return builder instance
   */
  public static Builder tcp(String hostPort) {
    return new Builder(Type.TCP, hostPort);
  }

  /**
   * Creates a TTL health check builder.
   *
   * @param ttl the time-to-live duration
   * @return builder instance
   */
  public static Builder ttl(Duration ttl) {
    return new Builder(Type.TTL, null).interval(ttl);
  }

  /** Builder for HealthCheck. */
  public static class Builder {

    private final Type type;
    private final String endpoint;
    private Duration interval = Duration.ofSeconds(10);
    private Duration timeout = Duration.ofSeconds(5);
    private Duration deregisterAfter = Duration.ofMinutes(1);

    private Builder(Type type, String endpoint) {
      this.type = type;
      this.endpoint = endpoint;
    }

    public Builder interval(Duration interval) {
      this.interval = interval;
      return this;
    }

    public Builder timeout(Duration timeout) {
      this.timeout = timeout;
      return this;
    }

    public Builder deregisterAfter(Duration deregisterAfter) {
      this.deregisterAfter = deregisterAfter;
      return this;
    }

    public HealthCheck build() {
      return new HealthCheck(type, endpoint, interval, timeout, deregisterAfter);
    }
  }
}
