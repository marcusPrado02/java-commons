package com.marcusprado02.commons.ports.servicediscovery;

import java.util.Map;
import java.util.Objects;

/**
 * Represents a service instance in the service registry.
 *
 * <p>Contains all metadata necessary to communicate with a service, including host, port, service
 * name, instance ID, and additional metadata tags.
 *
 * <p>Example:
 *
 * <pre>{@code
 * ServiceInstance instance = ServiceInstance.builder()
 *     .serviceId("payment-service")
 *     .instanceId("payment-01")
 *     .host("192.168.1.100")
 *     .port(8080)
 *     .secure(true)
 *     .metadata(Map.of("version", "1.2.0", "region", "us-east"))
 *     .build();
 * }</pre>
 *
 * @param serviceId the logical service name (e.g., "payment-service")
 * @param instanceId unique identifier for this instance
 * @param host the hostname or IP address
 * @param port the service port
 * @param secure whether the service uses HTTPS/TLS
 * @param metadata additional service metadata (tags, version, etc.)
 * @author Marcus Prado
 * @since 1.0.0
 */
public record ServiceInstance(
    String serviceId,
    String instanceId,
    String host,
    int port,
    boolean secure,
    Map<String, String> metadata) {

  public ServiceInstance {
    Objects.requireNonNull(serviceId, "serviceId cannot be null");
    Objects.requireNonNull(instanceId, "instanceId cannot be null");
    Objects.requireNonNull(host, "host cannot be null");
    if (port <= 0 || port > 65535) {
      throw new IllegalArgumentException("port must be between 1 and 65535");
    }
    metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
  }

  /**
   * Returns the base URI for this service instance.
   *
   * @return the complete URI (e.g., "https://192.168.1.100:8080")
   */
  public String getUri() {
    String scheme = secure ? "https" : "http";
    return String.format("%s://%s:%d", scheme, host, port);
  }

  /**
   * Gets a metadata value by key.
   *
   * @param key the metadata key
   * @return the value, or null if not present
   */
  public String getMetadata(String key) {
    return metadata.get(key);
  }

  /**
   * Checks if metadata contains a specific key.
   *
   * @param key the metadata key
   * @return true if present
   */
  public boolean hasMetadata(String key) {
    return metadata.containsKey(key);
  }

  /**
   * Creates a new builder.
   *
   * @return a builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder for ServiceInstance. */
  public static class Builder {

    private String serviceId;
    private String instanceId;
    private String host;
    private int port;
    private boolean secure = false;
    private Map<String, String> metadata = Map.of();

    public Builder serviceId(String serviceId) {
      this.serviceId = serviceId;
      return this;
    }

    public Builder instanceId(String instanceId) {
      this.instanceId = instanceId;
      return this;
    }

    public Builder host(String host) {
      this.host = host;
      return this;
    }

    public Builder port(int port) {
      this.port = port;
      return this;
    }

    public Builder secure(boolean secure) {
      this.secure = secure;
      return this;
    }

    public Builder metadata(Map<String, String> metadata) {
      this.metadata = metadata != null ? metadata : Map.of();
      return this;
    }

    public Builder addMetadata(String key, String value) {
      if (this.metadata.isEmpty()) {
        this.metadata = new java.util.HashMap<>();
      } else if (!(this.metadata instanceof java.util.HashMap)) {
        this.metadata = new java.util.HashMap<>(this.metadata);
      }
      ((java.util.HashMap<String, String>) this.metadata).put(key, value);
      return this;
    }

    public ServiceInstance build() {
      return new ServiceInstance(serviceId, instanceId, host, port, secure, metadata);
    }
  }
}
