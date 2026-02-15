package com.marcusprado02.commons.adapters.grpc.client;

import io.grpc.ClientInterceptor;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GrpcClientConfigurationTest {

  @Test
  void shouldCreateDevelopmentConfiguration() {
    var config = GrpcClientConfiguration.forDevelopment("localhost", 9090);

    assertEquals("localhost", config.host());
    assertEquals(9090, config.port());
    assertEquals(Duration.ofSeconds(30), config.callTimeout());
    assertEquals(Duration.ofMinutes(5), config.idleTimeout());
    assertEquals(16 * 1024 * 1024, config.maxInboundMessageSize()); // 16MB
    assertEquals(8 * 1024, config.maxInboundMetadataSize()); // 8KB
    assertFalse(config.enableTls());
    assertFalse(config.enableRetry());
    assertFalse(config.enableCircuitBreaker());
    assertTrue(config.interceptors().isEmpty());
  }

  @Test
  void shouldCreateProductionConfiguration() {
    var config = GrpcClientConfiguration.forProduction("grpc.example.com", 443);

    assertEquals("grpc.example.com", config.host());
    assertEquals(443, config.port());
    assertEquals(Duration.ofSeconds(30), config.callTimeout());
    assertEquals(Duration.ofMinutes(10), config.idleTimeout());
    assertEquals(4 * 1024 * 1024, config.maxInboundMessageSize()); // 4MB
    assertEquals(8 * 1024, config.maxInboundMetadataSize()); // 8KB
    assertTrue(config.enableTls());
    assertTrue(config.enableRetry());
    assertEquals(3, config.maxRetries());
    assertEquals(Duration.ofMillis(100), config.retryDelay());
    assertEquals(Duration.ofSeconds(5), config.maxRetryDelay());
    assertTrue(config.enableCircuitBreaker());
    assertEquals(5, config.circuitBreakerFailureThreshold());
    assertEquals(Duration.ofSeconds(60), config.circuitBreakerWaitDuration());
    assertTrue(config.interceptors().isEmpty());
  }

  @Test
  void shouldCreateCustomConfiguration() {
    var config =
        GrpcClientConfiguration.builder("api.example.com", 8443)
            .callTimeout(Duration.ofSeconds(60))
            .idleTimeout(Duration.ofMinutes(15))
            .maxInboundMessageSize(8 * 1024 * 1024) // 8MB
            .maxInboundMetadataSize(16 * 1024) // 16KB
            .enableTls(true)
            .enableRetry(true)
            .maxRetries(5)
            .retryDelay(Duration.ofMillis(200))
            .maxRetryDelay(Duration.ofSeconds(10))
            .enableCircuitBreaker(true)
            .circuitBreakerFailureThreshold(10)
            .circuitBreakerWaitDuration(Duration.ofSeconds(120))
            .userAgent("my-app/1.0.0")
            .build();

    assertEquals("api.example.com", config.host());
    assertEquals(8443, config.port());
    assertEquals(Duration.ofSeconds(60), config.callTimeout());
    assertEquals(Duration.ofMinutes(15), config.idleTimeout());
    assertEquals(8 * 1024 * 1024, config.maxInboundMessageSize());
    assertEquals(16 * 1024, config.maxInboundMetadataSize());
    assertTrue(config.enableTls());
    assertTrue(config.enableRetry());
    assertEquals(5, config.maxRetries());
    assertEquals(Duration.ofMillis(200), config.retryDelay());
    assertEquals(Duration.ofSeconds(10), config.maxRetryDelay());
    assertTrue(config.enableCircuitBreaker());
    assertEquals(10, config.circuitBreakerFailureThreshold());
    assertEquals(Duration.ofSeconds(120), config.circuitBreakerWaitDuration());
    assertEquals("my-app/1.0.0", config.userAgent());
  }

  @Test
  void shouldValidateNullHost() {
    var builder = GrpcClientConfiguration.builder(null, 9090);

    assertThrows(IllegalArgumentException.class, builder::build);
  }

  @Test
  void shouldValidateBlankHost() {
    var builder = GrpcClientConfiguration.builder("  ", 9090);

    assertThrows(IllegalArgumentException.class, builder::build);
  }

  @Test
  void shouldValidateMinPort() {
    var builder = GrpcClientConfiguration.builder("localhost", 0);

    assertThrows(IllegalArgumentException.class, builder::build);
  }

  @Test
  void shouldValidateMaxPort() {
    var builder = GrpcClientConfiguration.builder("localhost", 65536);

    assertThrows(IllegalArgumentException.class, builder::build);
  }

  @Test
  void shouldValidatePositiveCallTimeout() {
    var builder =
        GrpcClientConfiguration.builder("localhost", 9090)
            .callTimeout(Duration.ofMillis(0));

    assertThrows(IllegalArgumentException.class, builder::build);
  }

  @Test
  void shouldValidatePositiveIdleTimeout() {
    var builder =
        GrpcClientConfiguration.builder("localhost", 9090)
            .idleTimeout(Duration.ofMillis(-1));

    assertThrows(IllegalArgumentException.class, builder::build);
  }

  @Test
  void shouldValidatePositiveMaxInboundMessageSize() {
    var builder =
        GrpcClientConfiguration.builder("localhost", 9090).maxInboundMessageSize(0);

    assertThrows(IllegalArgumentException.class, builder::build);
  }

  @Test
  void shouldValidatePositiveMaxInboundMetadataSize() {
    var builder =
        GrpcClientConfiguration.builder("localhost", 9090).maxInboundMetadataSize(-1);

    assertThrows(IllegalArgumentException.class, builder::build);
  }

  @Test
  void shouldValidateRetrySettingsWhenEnabled() {
    var builder =
        GrpcClientConfiguration.builder("localhost", 9090)
            .enableRetry(true);

    // Missing retry settings
    assertThrows(IllegalArgumentException.class, builder::build);
  }

  @Test
  void shouldValidatePositiveMaxRetries() {
    var builder =
        GrpcClientConfiguration.builder("localhost", 9090)
            .enableRetry(true)
            .maxRetries(0)
            .retryDelay(Duration.ofMillis(100))
            .maxRetryDelay(Duration.ofSeconds(5));

    assertThrows(IllegalArgumentException.class, builder::build);
  }

  @Test
  void shouldValidatePositiveRetryDelay() {
    var builder =
        GrpcClientConfiguration.builder("localhost", 9090)
            .enableRetry(true)
            .maxRetries(3)
            .retryDelay(Duration.ofMillis(0))
            .maxRetryDelay(Duration.ofSeconds(5));

    assertThrows(IllegalArgumentException.class, builder::build);
  }

  @Test
  void shouldValidatePositiveMaxRetryDelay() {
    var builder =
        GrpcClientConfiguration.builder("localhost", 9090)
            .enableRetry(true)
            .maxRetries(3)
            .retryDelay(Duration.ofMillis(100))
            .maxRetryDelay(Duration.ofMillis(-1));

    assertThrows(IllegalArgumentException.class, builder::build);
  }

  @Test
  void shouldValidateCircuitBreakerSettingsWhenEnabled() {
    var builder =
        GrpcClientConfiguration.builder("localhost", 9090)
            .enableCircuitBreaker(true);

    // Missing circuit breaker settings
    assertThrows(IllegalArgumentException.class, builder::build);
  }

  @Test
  void shouldValidatePositiveCircuitBreakerFailureThreshold() {
    var builder =
        GrpcClientConfiguration.builder("localhost", 9090)
            .enableCircuitBreaker(true)
            .circuitBreakerFailureThreshold(0)
            .circuitBreakerWaitDuration(Duration.ofSeconds(60));

    assertThrows(IllegalArgumentException.class, builder::build);
  }

  @Test
  void shouldValidatePositiveCircuitBreakerWaitDuration() {
    var builder =
        GrpcClientConfiguration.builder("localhost", 9090)
            .enableCircuitBreaker(true)
            .circuitBreakerFailureThreshold(5)
            .circuitBreakerWaitDuration(Duration.ofMillis(0));

    assertThrows(IllegalArgumentException.class, builder::build);
  }

  @Test
  void shouldAcceptCustomInterceptors() {
    ClientInterceptor interceptor1 = (method, callOptions, next) -> next.newCall(method, callOptions);
    ClientInterceptor interceptor2 = (method, callOptions, next) -> next.newCall(method, callOptions);

    var config =
        GrpcClientConfiguration.builder("localhost", 9090)
            .interceptors(List.of(interceptor1, interceptor2))
            .build();

    assertEquals(2, config.interceptors().size());
    assertEquals(interceptor1, config.interceptors().get(0));
    assertEquals(interceptor2, config.interceptors().get(1));
  }

  @Test
  void shouldAcceptNullInterceptors() {
    var config =
        GrpcClientConfiguration.builder("localhost", 9090)
            .interceptors(null)
            .build();

    assertTrue(config.interceptors().isEmpty());
  }

  @Test
  void shouldAcceptNullUserAgent() {
    var config =
        GrpcClientConfiguration.builder("localhost", 9090)
            .userAgent(null)
            .build();

    assertNull(config.userAgent());
  }

  @Test
  void shouldAllowRetryWithoutCircuitBreaker() {
    var config =
        GrpcClientConfiguration.builder("localhost", 9090)
            .enableRetry(true)
            .maxRetries(3)
            .retryDelay(Duration.ofMillis(100))
            .maxRetryDelay(Duration.ofSeconds(5))
            .enableCircuitBreaker(false)
            .build();

    assertTrue(config.enableRetry());
    assertFalse(config.enableCircuitBreaker());
  }

  @Test
  void shouldAllowCircuitBreakerWithRetry() {
    var config =
        GrpcClientConfiguration.builder("localhost", 9090)
            .enableRetry(true)
            .maxRetries(3)
            .retryDelay(Duration.ofMillis(100))
            .maxRetryDelay(Duration.ofSeconds(5))
            .enableCircuitBreaker(true)
            .circuitBreakerFailureThreshold(5)
            .circuitBreakerWaitDuration(Duration.ofSeconds(60))
            .build();

    assertTrue(config.enableRetry());
    assertTrue(config.enableCircuitBreaker());
  }
}
