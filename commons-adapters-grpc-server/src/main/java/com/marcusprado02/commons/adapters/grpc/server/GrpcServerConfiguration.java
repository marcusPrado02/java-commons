package com.marcusprado02.commons.adapters.grpc.server;

import io.grpc.ServerInterceptor;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Configuration for gRPC server.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * GrpcServerConfiguration config = GrpcServerConfiguration.builder()
 *     .port(9090)
 *     .maxInboundMessageSize(4 * 1024 * 1024) // 4MB
 *     .enableReflection(true)
 *     .enableHealthCheck(true)
 *     .build();
 * }</pre>
 */
public record GrpcServerConfiguration(
    int port,
    int maxInboundMessageSize,
    int maxInboundMetadataSize,
    Duration keepAliveTime,
    Duration keepAliveTimeout,
    Duration maxConnectionIdle,
    Duration maxConnectionAge,
    Duration handshakeTimeout,
    boolean enableReflection,
    boolean enableHealthCheck,
    boolean enableMetrics,
    List<ServerInterceptor> interceptors
) {

  private static final int DEFAULT_PORT = 9090;
  private static final int DEFAULT_MAX_MESSAGE_SIZE = 4 * 1024 * 1024; // 4MB
  private static final int DEFAULT_MAX_METADATA_SIZE = 8 * 1024; // 8KB
  private static final Duration DEFAULT_KEEP_ALIVE_TIME = Duration.ofMinutes(2);
  private static final Duration DEFAULT_KEEP_ALIVE_TIMEOUT = Duration.ofSeconds(20);
  private static final Duration DEFAULT_MAX_CONNECTION_IDLE = Duration.ofMinutes(5);
  private static final Duration DEFAULT_MAX_CONNECTION_AGE = Duration.ofHours(1);
  private static final Duration DEFAULT_HANDSHAKE_TIMEOUT = Duration.ofSeconds(20);

  public GrpcServerConfiguration {
    if (port < 1 || port > 65535) {
      throw new IllegalArgumentException("Port must be between 1 and 65535");
    }

    if (maxInboundMessageSize <= 0) {
      throw new IllegalArgumentException("Max inbound message size must be positive");
    }

    if (maxInboundMetadataSize <= 0) {
      throw new IllegalArgumentException("Max inbound metadata size must be positive");
    }

    Objects.requireNonNull(keepAliveTime, "Keep alive time cannot be null");
    Objects.requireNonNull(keepAliveTimeout, "Keep alive timeout cannot be null");
    Objects.requireNonNull(maxConnectionIdle, "Max connection idle cannot be null");
    Objects.requireNonNull(maxConnectionAge, "Max connection age cannot be null");
    Objects.requireNonNull(handshakeTimeout, "Handshake timeout cannot be null");

    if (keepAliveTime.isNegative() || keepAliveTime.isZero()) {
      throw new IllegalArgumentException("Keep alive time must be positive");
    }

    if (keepAliveTimeout.isNegative() || keepAliveTimeout.isZero()) {
      throw new IllegalArgumentException("Keep alive timeout must be positive");
    }

    interceptors = interceptors == null ? List.of() : List.copyOf(interceptors);
  }

  /**
   * Creates a builder for development configuration with lenient settings.
   *
   * @return builder with development defaults
   */
  public static Builder forDevelopment() {
    return builder()
        .port(9090)
        .maxInboundMessageSize(16 * 1024 * 1024) // 16MB for dev
        .enableReflection(true)
        .enableHealthCheck(true)
        .enableMetrics(false);
  }

  /**
   * Creates a builder for production configuration with strict settings.
   *
   * @return builder with production defaults
   */
  public static Builder forProduction() {
    return builder()
        .port(9090)
        .maxInboundMessageSize(4 * 1024 * 1024) // 4MB
        .keepAliveTime(Duration.ofMinutes(1))
        .maxConnectionIdle(Duration.ofMinutes(10))
        .enableReflection(false) // Disable reflection in prod
        .enableHealthCheck(true)
        .enableMetrics(true);
  }

  /**
   * Creates a new builder with default values.
   *
   * @return new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for {@link GrpcServerConfiguration}.
   */
  public static class Builder {
    private int port = DEFAULT_PORT;
    private int maxInboundMessageSize = DEFAULT_MAX_MESSAGE_SIZE;
    private int maxInboundMetadataSize = DEFAULT_MAX_METADATA_SIZE;
    private Duration keepAliveTime = DEFAULT_KEEP_ALIVE_TIME;
    private Duration keepAliveTimeout = DEFAULT_KEEP_ALIVE_TIMEOUT;
    private Duration maxConnectionIdle = DEFAULT_MAX_CONNECTION_IDLE;
    private Duration maxConnectionAge = DEFAULT_MAX_CONNECTION_AGE;
    private Duration handshakeTimeout = DEFAULT_HANDSHAKE_TIMEOUT;
    private boolean enableReflection = false;
    private boolean enableHealthCheck = true;
    private boolean enableMetrics = false;
    private final List<ServerInterceptor> interceptors = new ArrayList<>();

    /**
     * Sets the server port.
     *
     * @param port port number (1-65535)
     * @return this builder
     */
    public Builder port(int port) {
      this.port = port;
      return this;
    }

    /**
     * Sets the maximum inbound message size.
     *
     * @param maxInboundMessageSize max size in bytes
     * @return this builder
     */
    public Builder maxInboundMessageSize(int maxInboundMessageSize) {
      this.maxInboundMessageSize = maxInboundMessageSize;
      return this;
    }

    /**
     * Sets the maximum inbound metadata size.
     *
     * @param maxInboundMetadataSize max size in bytes
     * @return this builder
     */
    public Builder maxInboundMetadataSize(int maxInboundMetadataSize) {
      this.maxInboundMetadataSize = maxInboundMetadataSize;
      return this;
    }

    /**
     * Sets the keep alive time.
     *
     * @param keepAliveTime duration between keep alive pings
     * @return this builder
     */
    public Builder keepAliveTime(Duration keepAliveTime) {
      this.keepAliveTime = keepAliveTime;
      return this;
    }

    /**
     * Sets the keep alive timeout.
     *
     * @param keepAliveTimeout timeout for keep alive response
     * @return this builder
     */
    public Builder keepAliveTimeout(Duration keepAliveTimeout) {
      this.keepAliveTimeout = keepAliveTimeout;
      return this;
    }

    /**
     * Sets the maximum connection idle time.
     *
     * @param maxConnectionIdle max idle duration before closing
     * @return this builder
     */
    public Builder maxConnectionIdle(Duration maxConnectionIdle) {
      this.maxConnectionIdle = maxConnectionIdle;
      return this;
    }

    /**
     * Sets the maximum connection age.
     *
     * @param maxConnectionAge max connection lifetime
     * @return this builder
     */
    public Builder maxConnectionAge(Duration maxConnectionAge) {
      this.maxConnectionAge = maxConnectionAge;
      return this;
    }

    /**
     * Sets the handshake timeout.
     *
     * @param handshakeTimeout timeout for TLS handshake
     * @return this builder
     */
    public Builder handshakeTimeout(Duration handshakeTimeout) {
      this.handshakeTimeout = handshakeTimeout;
      return this;
    }

    /**
     * Enables or disables reflection service.
     *
     * @param enableReflection true to enable
     * @return this builder
     */
    public Builder enableReflection(boolean enableReflection) {
      this.enableReflection = enableReflection;
      return this;
    }

    /**
     * Enables or disables health check service.
     *
     * @param enableHealthCheck true to enable
     * @return this builder
     */
    public Builder enableHealthCheck(boolean enableHealthCheck) {
      this.enableHealthCheck = enableHealthCheck;
      return this;
    }

    /**
     * Enables or disables metrics collection.
     *
     * @param enableMetrics true to enable
     * @return this builder
     */
    public Builder enableMetrics(boolean enableMetrics) {
      this.enableMetrics = enableMetrics;
      return this;
    }

    /**
     * Adds a server interceptor.
     *
     * @param interceptor interceptor to add
     * @return this builder
     */
    public Builder addInterceptor(ServerInterceptor interceptor) {
      if (interceptor != null) {
        this.interceptors.add(interceptor);
      }
      return this;
    }

    /**
     * Adds multiple server interceptors.
     *
     * @param interceptors interceptors to add
     * @return this builder
     */
    public Builder addInterceptors(List<ServerInterceptor> interceptors) {
      if (interceptors != null) {
        this.interceptors.addAll(interceptors);
      }
      return this;
    }

    /**
     * Builds the configuration.
     *
     * @return new configuration instance
     * @throws IllegalArgumentException if validation fails
     */
    public GrpcServerConfiguration build() {
      return new GrpcServerConfiguration(
          port,
          maxInboundMessageSize,
          maxInboundMetadataSize,
          keepAliveTime,
          keepAliveTimeout,
          maxConnectionIdle,
          maxConnectionAge,
          handshakeTimeout,
          enableReflection,
          enableHealthCheck,
          enableMetrics,
          interceptors
      );
    }
  }
}
