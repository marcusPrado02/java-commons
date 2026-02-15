package com.marcusprado02.commons.adapters.grpc.server;

import com.marcusprado02.commons.adapters.grpc.server.interceptors.LoggingInterceptor;
import com.marcusprado02.commons.adapters.grpc.server.interceptors.MetricsInterceptor;
import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptor;
import io.grpc.ServerServiceDefinition;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.protobuf.services.HealthStatusManager;
import io.grpc.protobuf.services.ProtoReflectionService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * gRPC server wrapper with health check, reflection, and interceptor support.
 *
 * <p>Features:
 * <ul>
 *   <li>Automatic health check service</li>
 *   <li>Optional reflection service for debugging</li>
 *   <li>Configurable interceptors (logging, metrics, auth)</li>
 *   <li>Graceful shutdown with timeout</li>
 *   <li>Connection management (keep-alive, max age)</li>
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * GrpcServerConfiguration config = GrpcServerConfiguration.forProduction();
 *
 * GrpcServer server = new GrpcServer(config);
 * server.addService(new MyServiceImpl());
 * server.start();
 *
 * // Later...
 * server.shutdown();
 * }</pre>
 */
public class GrpcServer {

  private static final Logger logger = Logger.getLogger(GrpcServer.class.getName());

  private final GrpcServerConfiguration configuration;
  private final List<BindableService> services = new ArrayList<>();
  private final List<ServerServiceDefinition> serviceDefinitions = new ArrayList<>();
  private final HealthStatusManager healthStatusManager;
  private final MetricsInterceptor metricsInterceptor;

  private Server server;

  /**
   * Creates a new gRPC server with the given configuration.
   *
   * @param configuration server configuration
   */
  public GrpcServer(GrpcServerConfiguration configuration) {
    this.configuration = Objects.requireNonNull(configuration, "Configuration cannot be null");
    this.healthStatusManager = configuration.enableHealthCheck() ?
        new HealthStatusManager() : null;
    this.metricsInterceptor = configuration.enableMetrics() ?
        new MetricsInterceptor() : null;
  }

  /**
   * Adds a service to the server.
   *
   * @param service service implementation
   * @return this server for chaining
   */
  public GrpcServer addService(BindableService service) {
    Objects.requireNonNull(service, "Service cannot be null");
    this.services.add(service);
    return this;
  }

  /**
   * Adds a service definition to the server.
   *
   * @param serviceDefinition service definition
   * @return this server for chaining
   */
  public GrpcServer addService(ServerServiceDefinition serviceDefinition) {
    Objects.requireNonNull(serviceDefinition, "Service definition cannot be null");
    this.serviceDefinitions.add(serviceDefinition);
    return this;
  }

  /**
   * Starts the gRPC server.
   *
   * @throws IOException           if server fails to start
   * @throws IllegalStateException if server is already started
   */
  public void start() throws IOException {
    if (server != null) {
      throw new IllegalStateException("Server is already started");
    }

    if (services.isEmpty() && serviceDefinitions.isEmpty()) {
      throw new IllegalStateException("No services registered. Add at least one service.");
    }

    ServerBuilder<?> serverBuilder = ServerBuilder.forPort(configuration.port())
        .maxInboundMessageSize(configuration.maxInboundMessageSize())
        .maxInboundMetadataSize(configuration.maxInboundMetadataSize())
        .keepAliveTime(configuration.keepAliveTime().toMillis(), TimeUnit.MILLISECONDS)
        .keepAliveTimeout(configuration.keepAliveTimeout().toMillis(), TimeUnit.MILLISECONDS)
        .maxConnectionIdle(configuration.maxConnectionIdle().toMillis(), TimeUnit.MILLISECONDS)
        .maxConnectionAge(configuration.maxConnectionAge().toMillis(), TimeUnit.MILLISECONDS)
        .handshakeTimeout(configuration.handshakeTimeout().toMillis(), TimeUnit.MILLISECONDS);

    // Add built-in interceptors
    List<ServerInterceptor> allInterceptors = new ArrayList<>();

    // Add logging interceptor first (if enabled via configuration)
    if (configuration.enableMetrics() || !configuration.interceptors().isEmpty()) {
      allInterceptors.add(new LoggingInterceptor());
    }

    // Add metrics interceptor
    if (metricsInterceptor != null) {
      allInterceptors.add(metricsInterceptor);
    }

    // Add custom interceptors
    allInterceptors.addAll(configuration.interceptors());

    // Apply interceptors to server
    for (ServerInterceptor interceptor : allInterceptors) {
      serverBuilder.intercept(interceptor);
    }

    // Add user services
    for (BindableService service : services) {
      serverBuilder.addService(service);
      logger.log(Level.INFO, "Registered service: {0}",
          service.bindService().getServiceDescriptor().getName());

      // Update health status for this service
      if (healthStatusManager != null) {
        healthStatusManager.setStatus(
            service.bindService().getServiceDescriptor().getName(),
            HealthCheckResponse.ServingStatus.SERVING
        );
      }
    }

    // Add service definitions
    for (ServerServiceDefinition serviceDef : serviceDefinitions) {
      serverBuilder.addService(serviceDef);
      logger.log(Level.INFO, "Registered service definition: {0}",
          serviceDef.getServiceDescriptor().getName());

      // Update health status
      if (healthStatusManager != null) {
        healthStatusManager.setStatus(
            serviceDef.getServiceDescriptor().getName(),
            HealthCheckResponse.ServingStatus.SERVING
        );
      }
    }

    // Add health check service
    if (healthStatusManager != null) {
      serverBuilder.addService(healthStatusManager.getHealthService());
      logger.info("Health check service enabled");
    }

    // Add reflection service (for debugging)
    if (configuration.enableReflection()) {
      serverBuilder.addService(ProtoReflectionService.newInstance());
      logger.info("Reflection service enabled");
    }

    // Build and start server
    server = serverBuilder.build().start();

    logger.log(Level.INFO, "gRPC server started on port {0}", configuration.port());

    // Set overall health status to SERVING
    if (healthStatusManager != null) {
      healthStatusManager.setStatus("", HealthCheckResponse.ServingStatus.SERVING);
    }

    // Add shutdown hook
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      logger.info("Shutting down gRPC server via shutdown hook");
      try {
        GrpcServer.this.shutdown();
      } catch (InterruptedException e) {
        logger.log(Level.WARNING, "Interrupted during shutdown", e);
        Thread.currentThread().interrupt();
      }
    }));
  }

  /**
   * Shuts down the server gracefully.
   *
   * @throws InterruptedException if shutdown is interrupted
   */
  public void shutdown() throws InterruptedException {
    if (server == null) {
      return;
    }

    logger.info("Shutting down gRPC server");

    // Set health status to NOT_SERVING
    if (healthStatusManager != null) {
      healthStatusManager.enterTerminalState();
    }

    // Initiate graceful shutdown
    server.shutdown();

    // Wait for all calls to complete (with timeout)
    if (!server.awaitTermination(30, TimeUnit.SECONDS)) {
      logger.warning("Server did not terminate gracefully, forcing shutdown");
      server.shutdownNow();

      // Wait for forced shutdown
      if (!server.awaitTermination(5, TimeUnit.SECONDS)) {
        logger.severe("Server did not terminate after forced shutdown");
      }
    }

    logger.info("gRPC server shut down successfully");
  }

  /**
   * Blocks until the server is terminated.
   *
   * @throws InterruptedException if waiting is interrupted
   */
  public void awaitTermination() throws InterruptedException {
    if (server != null) {
      server.awaitTermination();
    }
  }

  /**
   * Gets the port the server is listening on.
   *
   * @return port number or -1 if not started
   */
  public int getPort() {
    return server != null ? server.getPort() : -1;
  }

  /**
   * Checks if the server is running.
   *
   * @return true if server is running
   */
  public boolean isRunning() {
    return server != null && !server.isShutdown();
  }

  /**
   * Gets the metrics interceptor for accessing metrics.
   *
   * @return metrics interceptor or null if metrics are disabled
   */
  public MetricsInterceptor getMetricsInterceptor() {
    return metricsInterceptor;
  }

  /**
   * Updates the health status of a specific service.
   *
   * @param serviceName service name
   * @param serving     true for SERVING, false for NOT_SERVING
   */
  public void setServiceHealth(String serviceName, boolean serving) {
    if (healthStatusManager != null) {
      HealthCheckResponse.ServingStatus status = serving ?
          HealthCheckResponse.ServingStatus.SERVING :
          HealthCheckResponse.ServingStatus.NOT_SERVING;

      healthStatusManager.setStatus(serviceName, status);
      logger.log(Level.INFO, "Updated health status for {0}: {1}", new Object[]{serviceName, status});
    }
  }
}
