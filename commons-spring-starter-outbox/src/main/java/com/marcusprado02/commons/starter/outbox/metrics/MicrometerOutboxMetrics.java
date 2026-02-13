package com.marcusprado02.commons.starter.outbox.metrics;

import com.marcusprado02.commons.app.outbox.metrics.OutboxMetrics;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.Objects;

/** Micrometer implementation of OutboxMetrics for Spring Boot applications. */
public final class MicrometerOutboxMetrics implements OutboxMetrics {

  private final MeterRegistry registry;

  public MicrometerOutboxMetrics(MeterRegistry registry) {
    this.registry = Objects.requireNonNull(registry, "registry must not be null");
  }

  @Override
  public void recordPublished(String topic) {
    Counter.builder("outbox.published")
        .tag("topic", topic)
        .description("Number of outbox messages successfully published")
        .register(registry)
        .increment();
  }

  @Override
  public void recordFailed(String topic, String reason) {
    Counter.builder("outbox.failed")
        .tag("topic", topic)
        .tag("reason", reason)
        .description("Number of outbox messages that failed to publish")
        .register(registry)
        .increment();
  }

  @Override
  public void recordDead(String topic) {
    Counter.builder("outbox.dead")
        .tag("topic", topic)
        .description("Number of outbox messages moved to dead letter queue")
        .register(registry)
        .increment();
  }

  @Override
  public void recordLatency(String topic, long durationMillis) {
    Timer.builder("outbox.publish.latency")
        .tag("topic", topic)
        .description("Time taken to publish outbox messages")
        .register(registry)
        .record(Duration.ofMillis(durationMillis));
  }

  @Override
  public void recordBatchProcessing(int batchSize, long durationMillis) {
    Timer.builder("outbox.batch.processing")
        .tag("size", String.valueOf(batchSize))
        .description("Time taken to process outbox message batch")
        .register(registry)
        .record(Duration.ofMillis(durationMillis));
  }
}
