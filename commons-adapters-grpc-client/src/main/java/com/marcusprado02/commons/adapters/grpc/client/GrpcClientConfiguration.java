package com.marcusprado02.commons.adapters.grpc.client;

import io.grpc.ClientInterceptor;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Configuration for gRPC client connections.
 *
 * <p>Supports connection settings, interceptors, retry policies, and circuit breaker configuration.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Development
 * GrpcClientConfiguration config = GrpcClientConfiguration.forDevelopment("localhost", 9090);
 *
 * // Production with retries
 * GrpcClientConfiguration config = GrpcClientConfiguration.builder()
 *     .host("grpc.example.com")
 *     .port(443)
 *     .enableTls(true)
 *     .maxRetries(3)
 *     .retryDelay(Duration.ofMillis(100))
 *     .callTimeout(Duration.ofSeconds(30))
 *     .build();
 * }</pre>
 */
public record GrpcClientConfiguration(
    String host,
    int port,
    Duration callTimeout,
    Duration idleTimeout,
    int maxInboundMessageSize,
    int maxInboundMetadataSize,
    boolean enableTls,
    boolean enableRetry,
    int maxRetries,
    Duration retryDelay,
    Duration maxRetryDelay,
    boolean enableCircuitBreaker,
    int circuitBreakerFailureThreshold,
    Duration circuitBreakerWaitDuration,
    List<ClientInterceptor> interceptors,
    String userAgent) {

  public GrpcClientConfiguration {
    Objects.requireNonNull(host, "Host cannot be null");
    if (host.isBlank()) {
      throw new IllegalArgumentException("Host cannot be blank");
    }

    if (port < 1 || port > 65535) {
      throw new IllegalArgumentException("Port must be between 1 and 65535, got: " + port);
    }

    Objects.requireNonNull(callTimeout, "Call timeout cannot be null");
    Objects.requireNonNull(idleTimeout, "Idle timeout cannot be null");

    if (callTimeout.isNegative() || callTimeout.isZero()) {
      throw new IllegalArgumentException("Call timeout must be positive");
    }

    if (idleTimeout.isNegative() || idleTimeout.isZero()) {
      throw new IllegalArgumentException("Idle timeout must be positive");
    }

    if (maxInboundMessageSize <= 0) {
      throw new IllegalArgumentException("Max inbound message size must be positive");
    }

    if (maxInboundMetadataSize <= 0) {
      throw new IllegalArgumentException("Max inbound metadata size must be positive");
    }

    if (maxRetries < 0) {
      throw new IllegalArgumentException("Max retries cannot be negative");
    }

    if (enableRetry) {
      Objects.requireNonNull(retryDelay, "Retry delay cannot be null when retry is enabled");
      Objects.requireNonNull(
          maxRetryDelay, "Max retry delay cannot be null when retry is enabled");

      if (retryDelay.isNegative() || retryDelay.isZero()) {
        throw new IllegalArgumentException("Retry delay must be positive");
      }

      if (maxRetryDelay.isNegative() || maxRetryDelay.isZero()) {
        throw new IllegalArgumentException("Max retry delay must be positive");
      }
    }

    if (enableCircuitBreaker) {
      if (circuitBreakerFailureThreshold <= 0) {
        throw new IllegalArgumentException("Circuit breaker failure threshold must be positive");
      }

      Objects.requireNonNull(
          circuitBreakerWaitDuration,
          "Circuit breaker wait duration cannot be null when circuit breaker is enabled");

      if (circuitBreakerWaitDuration.isNegative() || circuitBreakerWaitDuration.isZero()) {
        throw new IllegalArgumentException("Circuit breaker wait duration must be positive");
      }
    }

    interceptors = interceptors == null ? List.of() : List.copyOf(interceptors);
  }

  /**
   * Creates a builder for GrpcClientConfiguration.
   *
   * @return new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates a development configuration with default settings.
   *
   * <p>Defaults:
   *
   * <ul>
   *   <li>TLS: disabled
   *   <li>Call timeout: 30 seconds
   *   <li>Idle timeout: 5 minutes
   *   <li>Max message size: 16MB (lenient for development)
   *   <li>Retry: disabled
   *   <li>Circuit breaker: disabled
   * </ul>
   *
   * @param host server host
   * @param port server port
   * @return development configuration
   */
  public static GrpcClientConfiguration forDevelopment(String host, int port) {
    return builder()
        .host(host)
        .port(port)
        .maxInboundMessageSize(16 * 1024 * 1024) // 16MB for dev
        .build();
  }

  /**
   * Creates a production configuration with recommended settings.
   *
   * <p>Defaults:
   *
   * <ul>
   *   <li>TLS: enabled
   *   <li>Call timeout: 30 seconds
   *   <li>Idle timeout: 10 minutes
   *   <li>Max message size: 4MB (strict for production)
   *   <li>Retry: enabled (3 attempts with exponential backoff)
   *   <li>Circuit breaker: enabled (5 failures, 60s wait)
   * </ul>
   *
   * @param host server host
   * @param port server port
   * @return production configuration
   */
  public static GrpcClientConfiguration forProduction(String host, int port) {
    return builder()
        .host(host)
        .port(port)
        .enableTls(true)
        .callTimeout(Duration.ofSeconds(30))
        .idleTimeout(Duration.ofMinutes(10))
        .maxInboundMessageSize(4 * 1024 * 1024) // 4MB for prod
        .enableRetry(true)
        .maxRetries(3)
        .retryDelay(Duration.ofMillis(100))
        .maxRetryDelay(Duration.ofSeconds(5))
        .enableCircuitBreaker(true)
        .circuitBreakerFailureThreshold(5)
        .circuitBreakerWaitDuration(Duration.ofSeconds(60))
        .build();
  }

  /** Builder for GrpcClientConfiguration. */
  public static class Builder {
    private String host;
    private int port;
    private Duration callTimeout = Duration.ofSeconds(30);
    private Duration idleTimeout = Duration.ofMinutes(5);
    private int maxInboundMessageSize = 4 * 1024 * 1024; // 4MB
    private int maxInboundMetadataSize = 8 * 1024; // 8KB
    private boolean enableTls = false;
    private boolean enableRetry = false;
    private int maxRetries = 0;
    private Duration retryDelay = Duration.ofMillis(100);
    private Duration maxRetryDelay = Duration.ofSeconds(5);
    private boolean enableCircuitBreaker = false;
    private int circuitBreakerFailureThreshold = 5;
    private Duration circuitBreakerWaitDuration = Duration.ofSeconds(60);
    private List<ClientInterceptor> interceptors = new ArrayList<>();
    private String userAgent = "commons-grpc-client/0.1.0";

    /**
     * Sets the server host.
     *
     * @param host server host
     * @return this builder
     */
    public Builder host(String host) {
      this.host = host;
      return this;
    }

    /**
     * Sets the server port.
     *
     * @param port server port (1-65535)
     * @return this builder
     */
    public Builder port(int port) {
      this.port = port;
      return this;
    }

    /**
     * Sets the call timeout per RPC.
     *
     * @param callTimeout call timeout
     * @return this builder
     */
    public Builder callTimeout(Duration callTimeout) {
      this.callTimeout = callTimeout;
      return this;
    }

    /**
     * Sets the idle timeout for connections.
     *
     * @param idleTimeout idle timeout
     * @return this builder
     */
    public Builder idleTimeout(Duration idleTimeout) {
      this.idleTimeout = idleTimeout;
      return this;
    }

    /**
     * Sets the maximum inbound message size.
     *
     * @param maxInboundMessageSize max message size in bytes
     * @return this builder
     */
    public Builder maxInboundMessageSize(int maxInboundMessageSize) {
      this.maxInboundMessageSize = maxInboundMessageSize;
      return this;
    }

    /**
     * Sets the maximum inbound metadata size.
     *
     * @param maxInboundMetadataSize max metadata size in bytes
     * @return this builder
     */
    public Builder maxInboundMetadataSize(int maxInboundMetadataSize) {
      this.maxInboundMetadataSize = maxInboundMetadataSize;
      return this;
    }

    /**
     * Enables or disables TLS.
     *
     * @param enableTls true to enable TLS
     * @return this builder
     */
    public Builder enableTls(boolean enableTls) {
      this.enableTls = enableTls;
      return this;
    }

    /**
     * Enables or disables automatic retry.
     *
     * @param enableRetry true to enable retry
     * @return this builder
     */
    public Builder enableRetry(boolean enableRetry) {
      this.enableRetry = enableRetry;
      return this;
    }

    /**
     * Sets the maximum number of retry attempts.
     *
     * @param maxRetries max retries
     * @return this builder
     */
    public Builder maxRetries(int maxRetries) {
      this.maxRetries = maxRetries;
      return this;
    }

    /**
     * Sets the initial retry delay (exponential backoff).
     *
     * @param retryDelay retry delay
     * @return this builder
     */
    public Builder retryDelay(Duration retryDelay) {
      this.retryDelay = retryDelay;
      return this;
    }

    /**
     * Sets the maximum retry delay.
     *
     * @param maxRetryDelay max retry delay
     * @return this builder
     */
    public Builder maxRetryDelay(Duration maxRetryDelay) {
      this.maxRetryDelay = maxRetryDelay;
      return this;
    }

    /**
     * Enables or disables circuit breaker.
     *
     * @param enableCircuitBreaker true to enable circuit breaker
     * @return this builder
     */
    public Builder enableCircuitBreaker(boolean enableCircuitBreaker) {
      this.enableCircuitBreaker = enableCircuitBreaker;
      return this;
    }

    /**
     * Sets the circuit breaker failure threshold.
     *
     * @param circuitBreakerFailureThreshold failure threshold
     * @return this builder
     */
    public Builder circuitBreakerFailureThreshold(int circuitBreakerFailureThreshold) {
      this.circuitBreakerFailureThreshold = circuitBreakerFailureThreshold;
      return this;
    }

    /**
     * Sets the circuit breaker wait duration (time to wait before retry).
     *
     * @param circuitBreakerWaitDuration wait duration
     * @return this builder
     */
    public Builder circuitBreakerWaitDuration(Duration circuitBreakerWaitDuration) {
      this.circuitBreakerWaitDuration = circuitBreakerWaitDuration;
      return this;
    }

    /**
     * Adds a client interceptor.
     *
     * @param interceptor interceptor to add
     * @return this builder
     */
    public Builder addInterceptor(ClientInterceptor interceptor) {
      if (interceptor != null) {
        this.interceptors.add(interceptor);
      }
      return this;
    }

    /**
     * Adds multiple client interceptors.
     *
     * @param interceptors interceptors to add
     * @return this builder
     */
    public Builder addInterceptors(List<ClientInterceptor> interceptors) {
      if (interceptors != null) {
        this.interceptors.addAll(interceptors);
      }
      return this;
    }

    /**
     * Sets the user agent string.
     *
     * @param userAgent user agent
     * @return this builder
     */
    public Builder userAgent(String userAgent) {
      this.userAgent = userAgent;
      return this;
    }

    /**
     * Builds the configuration.
     *
     * @return GrpcClientConfiguration instance
     * @throws IllegalArgumentException if host or port not set
     */
    public GrpcClientConfiguration build() {
      if (host == null) {
        throw new IllegalArgumentException("Host must be set");
      }
      if (port == 0) {
        throw new IllegalArgumentException("Port must be set");
      }

      return new GrpcClientConfiguration(
          host,
          port,
          callTimeout,
          idleTimeout,
          maxInboundMessageSize,
          maxInboundMetadataSize,
          enableTls,
          enableRetry,
          maxRetries,
          retryDelay,
          maxRetryDelay,
          enableCircuitBreaker,
          circuitBreakerFailureThreshold,
          circuitBreakerWaitDuration,
          interceptors,
          userAgent);
    }
  }
}
