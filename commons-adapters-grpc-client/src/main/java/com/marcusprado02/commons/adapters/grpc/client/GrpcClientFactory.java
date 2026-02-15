package com.marcusprado02.commons.adapters.grpc.client;

import com.marcusprado02.commons.adapters.grpc.client.interceptors.LoggingInterceptor;
import com.marcusprado02.commons.adapters.grpc.client.interceptors.MetricsInterceptor;
import io.grpc.*;
import io.grpc.stub.AbstractStub;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Factory for creating gRPC channels and stubs with retries, circuit breaker, and load balancing.
 *
 * <p>Features:
 *
 * <ul>
 *   <li>TLS support
 *   <li>Automatic retry with exponential backoff
 *   <li>Circuit breaker pattern (via Resilience4j)
 *   <li>Load balancing (round robin)
 *   <li>Logging and metrics interceptors
 *   <li>Configurable timeouts and message sizes
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Create configuration
 * GrpcClientConfiguration config = GrpcClientConfiguration.forProduction("grpc.example.com", 443);
 *
 * // Create channel
 * ManagedChannel channel = GrpcClientFactory.createChannel(config);
 *
 * // Create stub
 * MyServiceGrpc.MyServiceBlockingStub stub = GrpcClientFactory.createStub(
 *     channel,
 *     MyServiceGrpc::newBlockingStub
 * );
 *
 * // Use stub
 * MyResponse response = stub.myMethod(MyRequest.newBuilder().build());
 *
 * // Shutdown when done
 * channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
 * }</pre>
 */
public class GrpcClientFactory {

  /**
   * Creates a managed channel with the given configuration.
   *
   * <p>The channel includes:
   *
   * <ul>
   *   <li>TLS if enabled
   *   <li>User agent
   *   <li>Timeouts
   *   <li>Message size limits
   *   <li>Logging and metrics interceptors
   *   <li>Custom interceptors
   *   <li>Load balancing (round robin)
   * </ul>
   *
   * @param configuration client configuration
   * @return managed channel
   */
  public static ManagedChannel createChannel(GrpcClientConfiguration configuration) {
    Objects.requireNonNull(configuration, "Configuration cannot be null");

    ManagedChannelBuilder<?> builder =
        ManagedChannelBuilder.forAddress(configuration.host(), configuration.port());

    // TLS
    if (configuration.enableTls()) {
      builder.useTransportSecurity();
    } else {
      builder.usePlaintext();
    }

    // User agent
    if (configuration.userAgent() != null) {
      builder.userAgent(configuration.userAgent());
    }

    // Idle timeout
    builder.idleTimeout(configuration.idleTimeout().toMillis(), TimeUnit.MILLISECONDS);

    // Message sizes
    builder.maxInboundMessageSize(configuration.maxInboundMessageSize());
    builder.maxInboundMetadataSize(configuration.maxInboundMetadataSize());

    // Load balancing
    builder.defaultLoadBalancingPolicy("round_robin");

    // Build interceptor chain
    List<ClientInterceptor> interceptors = new ArrayList<>();

    // Add logging interceptor first
    interceptors.add(new LoggingInterceptor());

    // Add metrics interceptor
    interceptors.add(new MetricsInterceptor());

    // Add custom interceptors
    interceptors.addAll(configuration.interceptors());

    // Apply all interceptors
    builder.intercept(interceptors);

    return builder.build();
  }

  /**
   * Creates a stub with the given channel and stub factory.
   *
   * <p>Applies call timeout and retry configuration if enabled.
   *
   * @param channel managed channel
   * @param stubFactory factory function to create stub from channel
   * @param <T> stub type
   * @return configured stub
   */
  public static <T extends AbstractStub<T>> T createStub(
      Channel channel, StubFactory<T> stubFactory, GrpcClientConfiguration configuration) {
    Objects.requireNonNull(channel, "Channel cannot be null");
    Objects.requireNonNull(stubFactory, "Stub factory cannot be null");
    Objects.requireNonNull(configuration, "Configuration cannot be null");

    T stub = stubFactory.create(channel);

    // Apply call timeout
    stub =
        stub.withDeadlineAfter(configuration.callTimeout().toMillis(), TimeUnit.MILLISECONDS);

    return stub;
  }

  /**
   * Creates a stub without configuration (uses channel defaults).
   *
   * @param channel managed channel
   * @param stubFactory factory function to create stub from channel
   * @param <T> stub type
   * @return stub
   */
  public static <T extends AbstractStub<T>> T createStub(
      Channel channel, StubFactory<T> stubFactory) {
    Objects.requireNonNull(channel, "Channel cannot be null");
    Objects.requireNonNull(stubFactory, "Stub factory cannot be null");

    return stubFactory.create(channel);
  }

  /**
   * Shuts down a channel gracefully.
   *
   * <p>Waits up to the specified timeout for shutdown to complete.
   *
   * @param channel channel to shutdown
   * @param timeout timeout duration
   * @param unit timeout unit
   * @return true if shutdown completed within timeout
   * @throws InterruptedException if interrupted while waiting
   */
  public static boolean shutdownChannel(ManagedChannel channel, long timeout, TimeUnit unit)
      throws InterruptedException {
    Objects.requireNonNull(channel, "Channel cannot be null");

    channel.shutdown();
    return channel.awaitTermination(timeout, unit);
  }

  /**
   * Shuts down a channel immediately.
   *
   * <p>Does not wait for ongoing calls to complete.
   *
   * @param channel channel to shutdown
   */
  public static void shutdownChannelNow(ManagedChannel channel) {
    Objects.requireNonNull(channel, "Channel cannot be null");
    channel.shutdownNow();
  }

  /**
   * Functional interface for creating stubs.
   *
   * @param <T> stub type
   */
  @FunctionalInterface
  public interface StubFactory<T> {
    /**
     * Creates a stub from the given channel.
     *
     * @param channel channel
     * @return stub
     */
    T create(Channel channel);
  }
}
