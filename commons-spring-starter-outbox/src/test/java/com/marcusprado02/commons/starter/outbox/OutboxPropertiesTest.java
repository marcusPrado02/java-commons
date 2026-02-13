package com.marcusprado02.commons.starter.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class OutboxPropertiesTest {

  @Test
  void shouldValidateBatchSizePositive() {
    var processing = new OutboxProperties.Processing(10, false);
    assertThat(processing.batchSize()).isEqualTo(10);
  }

  @Test void shouldValidateRetryMaxAttemptsPositive() {
    var retry = new OutboxProperties.Retry(3, Duration.ofMillis(100), Duration.ofMillis(30000), 2.0);
    assertThat(retry.maxAttempts()).isEqualTo(3);
  }

  @Test
  void shouldValidateHealthThresholdsPositive() {
    var health = new OutboxProperties.Health(true, 100, 500);
    assertThat(health.warningThreshold()).isEqualTo(100);
    assertThat(health.errorThreshold()).isEqualTo(500);
  }
}
