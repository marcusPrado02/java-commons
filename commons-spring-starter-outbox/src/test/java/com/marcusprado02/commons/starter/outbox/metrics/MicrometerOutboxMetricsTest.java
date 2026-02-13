package com.marcusprado02.commons.starter.outbox.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class MicrometerOutboxMetricsTest {

  private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
  private final MicrometerOutboxMetrics metrics = new MicrometerOutboxMetrics(registry);

  @Test
  void shouldRecordPublished() {
    metrics.recordPublished("test.topic");
    metrics.recordPublished("test.topic");

    var counter = registry.find("outbox.published").tag("topic", "test.topic").counter();
    assertThat(counter).isNotNull();
    assertThat(counter.count()).isEqualTo(2.0);
  }

  @Test
  void shouldRecordFailed() {
    metrics.recordFailed("test.topic", "Connection timeout");

    var counter =
        registry
            .find("outbox.failed")
            .tag("topic", "test.topic")
            .tag("reason", "Connection timeout")
            .counter();
    assertThat(counter).isNotNull();
    assertThat(counter.count()).isEqualTo(1.0);
  }

  @Test
  void shouldRecordDead() {
    metrics.recordDead("test.topic");

    var counter = registry.find("outbox.dead").tag("topic", "test.topic").counter();
    assertThat(counter).isNotNull();
    assertThat(counter.count()).isEqualTo(1.0);
  }

  @Test
  void shouldRecordLatency() {
    metrics.recordLatency("test.topic", 150);

    var timer = registry.find("outbox.publish.latency").tag("topic", "test.topic").timer();
    assertThat(timer).isNotNull();
    assertThat(timer.count()).isEqualTo(1);
  }

  @Test
  void shouldRecordBatchProcessing() {
    metrics.recordBatchProcessing(10, 500);

    var timer = registry.find("outbox.batch.processing").tag("size", "10").timer();
    assertThat(timer).isNotNull();
    assertThat(timer.count()).isEqualTo(1);
  }
}
