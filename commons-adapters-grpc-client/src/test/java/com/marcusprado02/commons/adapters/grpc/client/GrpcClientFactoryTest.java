package com.marcusprado02.commons.adapters.grpc.client;

import io.grpc.*;
import io.grpc.stub.AbstractStub;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class GrpcClientFactoryTest {

  private GrpcClientConfiguration developmentConfig;
  private GrpcClientConfiguration productionConfig;

  @BeforeEach
  void setUp() {
    developmentConfig = GrpcClientConfiguration.forDevelopment("localhost", 9090);
    productionConfig = GrpcClientConfiguration.forProduction("grpc.example.com", 443);
  }

  @Test
  void shouldCreateChannelForDevelopment() {
    ManagedChannel channel = GrpcClientFactory.createChannel(developmentConfig);

    assertNotNull(channel);
    assertFalse(channel.isShutdown());
    assertFalse(channel.isTerminated());

    // Cleanup
    channel.shutdownNow();
  }

  @Test
  void shouldCreateChannelForProduction() {
    ManagedChannel channel = GrpcClientFactory.createChannel(productionConfig);

    assertNotNull(channel);
    assertFalse(channel.isShutdown());
    assertFalse(channel.isTerminated());

    // Cleanup
    channel.shutdownNow();
  }

  @Test
  void shouldCreateChannelWithCustomConfiguration() {
    var config =
        GrpcClientConfiguration.builder()
            .host("api.example.com")
            .port(8443)
            .callTimeout(Duration.ofSeconds(60))
            .idleTimeout(Duration.ofMinutes(15))
            .maxInboundMessageSize(8 * 1024 * 1024)
            .enableTls(true)
            .userAgent("test-client/1.0")
            .build();

    ManagedChannel channel = GrpcClientFactory.createChannel(config);

    assertNotNull(channel);
    assertFalse(channel.isShutdown());

    // Cleanup
    channel.shutdownNow();
  }

  @Test
  void shouldThrowWhenConfigurationIsNull() {
    assertThrows(
        NullPointerException.class,
        () -> GrpcClientFactory.createChannel(null));
  }

  @Test
  void shouldShutdownChannelGracefully() throws InterruptedException {
    ManagedChannel channel = GrpcClientFactory.createChannel(developmentConfig);

    boolean terminated = GrpcClientFactory.shutdownChannel(channel, 5, TimeUnit.SECONDS);

    assertTrue(terminated);
    assertTrue(channel.isShutdown());
    assertTrue(channel.isTerminated());
  }

  @Test
  void shouldShutdownChannelImmediately() {
    ManagedChannel channel = GrpcClientFactory.createChannel(developmentConfig);

    GrpcClientFactory.shutdownChannelNow(channel);

    assertTrue(channel.isShutdown());
  }

  @Test
  void shouldThrowWhenShuttingDownNullChannel() {
    assertThrows(
        NullPointerException.class,
        () -> GrpcClientFactory.shutdownChannel(null, 5, TimeUnit.SECONDS));
  }

  @Test
  void shouldThrowWhenShuttingDownNullChannelImmediately() {
    assertThrows(
        NullPointerException.class,
        () -> GrpcClientFactory.shutdownChannelNow(null));
  }

  @Test
  void shouldCreateStubWithConfiguration() {
    ManagedChannel channel = GrpcClientFactory.createChannel(developmentConfig);

    // Create a test stub (using CallOptions as a simple stub-like example)
    TestStub stub = GrpcClientFactory.createStub(
        channel,
        ch -> new TestStub(ch),
        developmentConfig
    );

    assertNotNull(stub);
    assertNotNull(stub.getChannel());

    // Cleanup
    channel.shutdownNow();
  }

  @Test
  void shouldCreateStubWithoutConfiguration() {
    ManagedChannel channel = GrpcClientFactory.createChannel(developmentConfig);

    TestStub stub = GrpcClientFactory.createStub(
        channel,
        ch -> new TestStub(ch)
    );

    assertNotNull(stub);

    // Cleanup
    channel.shutdownNow();
  }

  @Test
  void shouldThrowWhenCreatingStubWithNullChannel() {
    assertThrows(
        NullPointerException.class,
        () -> GrpcClientFactory.<TestStub>createStub(null, ch -> new TestStub(ch)));
  }

  @Test
  void shouldThrowWhenCreatingStubWithNullFactory() {
    ManagedChannel channel = GrpcClientFactory.createChannel(developmentConfig);

    assertThrows(
        NullPointerException.class,
        () -> GrpcClientFactory.<TestStub>createStub(channel, null));

    // Cleanup
    channel.shutdownNow();
  }

  @Test
  void shouldThrowWhenCreatingStubWithNullConfiguration() {
    ManagedChannel channel = GrpcClientFactory.createChannel(developmentConfig);

    assertThrows(
        NullPointerException.class,
        () -> GrpcClientFactory.<TestStub>createStub(channel, ch -> new TestStub(ch), null));

    // Cleanup
    channel.shutdownNow();
  }

  // Test stub class for demonstration
  private static class TestStub extends AbstractStub<TestStub> {
    protected TestStub(Channel channel) {
      super(channel);
    }

    protected TestStub(Channel channel, CallOptions callOptions) {
      super(channel, callOptions);
    }

    @Override
    protected TestStub build(Channel channel, CallOptions callOptions) {
      return new TestStub(channel, callOptions);
    }
  }
}
