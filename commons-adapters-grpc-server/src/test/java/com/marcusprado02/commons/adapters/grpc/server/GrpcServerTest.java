package com.marcusprado02.commons.adapters.grpc.server;

import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;
import io.grpc.health.v1.HealthCheckResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GrpcServerTest {

  private GrpcServerConfiguration config;

  @BeforeEach
  void setUp() {
    // Use a random port for testing to avoid conflicts
    config = GrpcServerConfiguration.builder()
        .port(0) // Let the OS assign a random available port
        .enableHealthCheck(true)
        .enableReflection(true)
        .build();
  }

  @Test
  void shouldFailToStartWithoutServices() {
    GrpcServer server = new GrpcServer(config);

    assertThrows(IllegalStateException.class, server::start);
  }

  @Test
  void shouldStartAndStopServer() throws IOException, InterruptedException {
    GrpcServer server = new GrpcServer(config);

    // Add a mock service
    BindableService mockService = createMockService();
    server.addService(mockService);

    server.start();
    assertTrue(server.isRunning());
    assertTrue(server.getPort() > 0);

    server.shutdown();
    assertFalse(server.isRunning());
  }

  @Test
  void shouldAddMultipleServices() throws IOException, InterruptedException {
    GrpcServer server = new GrpcServer(config);

    // Add multiple mock services
    server.addService(createMockService());
    server.addService(createMockService());

    server.start();
    assertTrue(server.isRunning());

    server.shutdown();
  }

  @Test
  void shouldUpdateServiceHealth() throws IOException, InterruptedException {
    GrpcServer server = new GrpcServer(config);
    BindableService mockService = createMockService();
    server.addService(mockService);

    server.start();

    // Update service health
    assertDoesNotThrow(() -> server.setServiceHealth("test.Service", true));
    assertDoesNotThrow(() -> server.setServiceHealth("test.Service", false));

    server.shutdown();
  }

  @Test
  void shouldProvideMetricsInterceptor() {
    GrpcServerConfiguration configWithMetrics = GrpcServerConfiguration.builder()
        .port(0)
        .enableMetrics(true)
        .build();

    GrpcServer server = new GrpcServer(configWithMetrics);

    assertNotNull(server.getMetricsInterceptor());
  }

  @Test
  void shouldReturnNullMetricsWhenDisabled() {
    GrpcServerConfiguration configWithoutMetrics = GrpcServerConfiguration.builder()
        .port(0)
        .enableMetrics(false)
        .build();

    GrpcServer server = new GrpcServer(configWithoutMetrics);

    assertNull(server.getMetricsInterceptor());
  }

  @Test
  void shouldReturnNegativeOnePortBeforeStart() {
    GrpcServer server = new GrpcServer(config);
    server.addService(createMockService());

    assertEquals(-1, server.getPort());
  }

  @Test
  void shouldApplyConfigurationSettings() {
    GrpcServerConfiguration customConfig = GrpcServerConfiguration.builder()
        .port(0)
        .maxInboundMessageSize(8 * 1024 * 1024) // 8MB
        .maxInboundMetadataSize(16 * 1024) // 16KB
        .keepAliveTime(Duration.ofMinutes(1))
        .keepAliveTimeout(Duration.ofSeconds(10))
        .maxConnectionIdle(Duration.ofMinutes(5))
        .maxConnectionAge(Duration.ofMinutes(30))
        .handshakeTimeout(Duration.ofSeconds(15))
        .enableHealthCheck(true)
        .enableReflection(true)
        .enableMetrics(true)
        .build();

    GrpcServer server = new GrpcServer(customConfig);
    server.addService(createMockService());

    // Should not throw exception with custom config
    assertDoesNotThrow(() -> {
      server.start();
      server.shutdown();
    });
  }

  private BindableService createMockService() {
    BindableService mockService = mock(BindableService.class);
    ServerServiceDefinition mockDefinition = ServerServiceDefinition.builder("test.Service")
        .build();
    when(mockService.bindService()).thenReturn(mockDefinition);
    return mockService;
  }
}
