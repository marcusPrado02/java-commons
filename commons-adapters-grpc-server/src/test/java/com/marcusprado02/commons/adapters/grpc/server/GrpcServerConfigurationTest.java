package com.marcusprado02.commons.adapters.grpc.server;

import io.grpc.ServerInterceptor;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class GrpcServerConfigurationTest {

  @Test
  void shouldCreateMinimalConfiguration() {
    GrpcServerConfiguration config = GrpcServerConfiguration.builder()
        .port(9090)
        .build();

    assertEquals(9090, config.port());
    assertEquals(4 * 1024 * 1024, config.maxInboundMessageSize());
    assertEquals(8 * 1024, config.maxInboundMetadataSize());
    assertEquals(Duration.ofMinutes(2), config.keepAliveTime());
    assertEquals(Duration.ofSeconds(20), config.keepAliveTimeout());
    assertFalse(config.enableReflection());
    assertTrue(config.enableHealthCheck());
    assertFalse(config.enableMetrics());
    assertTrue(config.interceptors().isEmpty());
  }

  @Test
  void shouldCreateDevelopmentConfiguration() {
    GrpcServerConfiguration config = GrpcServerConfiguration.forDevelopment().build();

    assertEquals(9090, config.port());
    assertEquals(16 * 1024 * 1024, config.maxInboundMessageSize());
    assertTrue(config.enableReflection());
    assertTrue(config.enableHealthCheck());
    assertFalse(config.enableMetrics());
  }

  @Test
  void shouldCreateProductionConfiguration() {
    GrpcServerConfiguration config = GrpcServerConfiguration.forProduction().build();

    assertEquals(9090, config.port());
    assertEquals(4 * 1024 * 1024, config.maxInboundMessageSize());
    assertEquals(Duration.ofMinutes(1), config.keepAliveTime());
    assertEquals(Duration.ofMinutes(10), config.maxConnectionIdle());
    assertFalse(config.enableReflection());
    assertTrue(config.enableHealthCheck());
    assertTrue(config.enableMetrics());
  }

  @Test
  void shouldConfigureAllSettings() {
    ServerInterceptor interceptor = mock(ServerInterceptor.class);

    GrpcServerConfiguration config = GrpcServerConfiguration.builder()
        .port(8080)
        .maxInboundMessageSize(8 * 1024 * 1024)
        .maxInboundMetadataSize(16 * 1024)
        .keepAliveTime(Duration.ofSeconds(30))
        .keepAliveTimeout(Duration.ofSeconds(10))
        .maxConnectionIdle(Duration.ofMinutes(1))
        .maxConnectionAge(Duration.ofMinutes(30))
        .handshakeTimeout(Duration.ofSeconds(15))
        .enableReflection(true)
        .enableHealthCheck(false)
        .enableMetrics(true)
        .addInterceptor(interceptor)
        .build();

    assertEquals(8080, config.port());
    assertEquals(8 * 1024 * 1024, config.maxInboundMessageSize());
    assertEquals(16 * 1024, config.maxInboundMetadataSize());
    assertEquals(Duration.ofSeconds(30), config.keepAliveTime());
    assertEquals(Duration.ofSeconds(10), config.keepAliveTimeout());
    assertEquals(Duration.ofMinutes(1), config.maxConnectionIdle());
    assertEquals(Duration.ofMinutes(30), config.maxConnectionAge());
    assertEquals(Duration.ofSeconds(15), config.handshakeTimeout());
    assertTrue(config.enableReflection());
    assertFalse(config.enableHealthCheck());
    assertTrue(config.enableMetrics());
    assertEquals(1, config.interceptors().size());
  }

  @Test
  void shouldFailWhenPortTooLow() {
    assertThrows(IllegalArgumentException.class, () ->
        GrpcServerConfiguration.builder()
            .port(0)
            .build()
    );
  }

  @Test
  void shouldFailWhenPortTooHigh() {
    assertThrows(IllegalArgumentException.class, () ->
        GrpcServerConfiguration.builder()
            .port(65536)
            .build()
    );
  }

  @Test
  void shouldFailWhenPortNegative() {
    assertThrows(IllegalArgumentException.class, () ->
        GrpcServerConfiguration.builder()
            .port(-1)
            .build()
    );
  }

  @Test
  void shouldFailWhenMaxMessageSizeZero() {
    assertThrows(IllegalArgumentException.class, () ->
        GrpcServerConfiguration.builder()
            .maxInboundMessageSize(0)
            .build()
    );
  }

  @Test
  void shouldFailWhenMaxMessageSizeNegative() {
    assertThrows(IllegalArgumentException.class, () ->
        GrpcServerConfiguration.builder()
            .maxInboundMessageSize(-1)
            .build()
    );
  }

  @Test
  void shouldFailWhenMaxMetadataSizeZero() {
    assertThrows(IllegalArgumentException.class, () ->
        GrpcServerConfiguration.builder()
            .maxInboundMetadataSize(0)
            .build()
    );
  }

  @Test
  void shouldFailWhenKeepAliveTimeZero() {
    assertThrows(IllegalArgumentException.class, () ->
        GrpcServerConfiguration.builder()
            .keepAliveTime(Duration.ZERO)
            .build()
    );
  }

  @Test
  void shouldFailWhenKeepAliveTimeNegative() {
    assertThrows(IllegalArgumentException.class, () ->
        GrpcServerConfiguration.builder()
            .keepAliveTime(Duration.ofSeconds(-1))
            .build()
    );
  }

  @Test
  void shouldFailWhenKeepAliveTimeoutZero() {
    assertThrows(IllegalArgumentException.class, () ->
        GrpcServerConfiguration.builder()
            .keepAliveTimeout(Duration.ZERO)
            .build()
    );
  }

  @Test
  void shouldAllowValidPortRange() {
    assertDoesNotThrow(() -> {
      GrpcServerConfiguration.builder().port(1).build();
      GrpcServerConfiguration.builder().port(8080).build();
      GrpcServerConfiguration.builder().port(65535).build();
    });
  }

  @Test
  void shouldAddMultipleInterceptors() {
    ServerInterceptor interceptor1 = mock(ServerInterceptor.class);
    ServerInterceptor interceptor2 = mock(ServerInterceptor.class);

    GrpcServerConfiguration config = GrpcServerConfiguration.builder()
        .addInterceptor(interceptor1)
        .addInterceptor(interceptor2)
        .build();

    assertEquals(2, config.interceptors().size());
  }

  @Test
  void shouldIgnoreNullInterceptor() {
    GrpcServerConfiguration config = GrpcServerConfiguration.builder()
        .addInterceptor(null)
        .build();

    assertTrue(config.interceptors().isEmpty());
  }

  @Test
  void shouldReturnImmutableInterceptorList() {
    GrpcServerConfiguration config = GrpcServerConfiguration.builder().build();

    assertThrows(UnsupportedOperationException.class, () ->
        config.interceptors().add(mock(ServerInterceptor.class))
    );
  }
}
