package com.marcusprado02.commons.app.outbox.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class OutboxProcessorConfigTest {

  @Test
  void shouldCreateDefaultConfig() {
    OutboxProcessorConfig config = OutboxProcessorConfig.defaults();

    assertEquals(OutboxProcessorConfig.DEFAULT_BATCH_SIZE, config.batchSize());
    assertEquals(OutboxProcessorConfig.DEFAULT_MAX_ATTEMPTS, config.maxAttempts());
    assertEquals(OutboxProcessorConfig.DEFAULT_INITIAL_BACKOFF, config.initialBackoff());
    assertEquals(OutboxProcessorConfig.DEFAULT_MAX_BACKOFF, config.maxBackoff());
    assertEquals(OutboxProcessorConfig.DEFAULT_BACKOFF_MULTIPLIER, config.backoffMultiplier());
    assertFalse(config.useCircuitBreaker());
  }

  @Test
  void shouldCreateCustomConfig() {
    OutboxProcessorConfig config =
        new OutboxProcessorConfig(50, 3, Duration.ofSeconds(2), Duration.ofMinutes(1), 3.0, true);

    assertEquals(50, config.batchSize());
    assertEquals(3, config.maxAttempts());
    assertEquals(Duration.ofSeconds(2), config.initialBackoff());
    assertEquals(Duration.ofMinutes(1), config.maxBackoff());
    assertEquals(3.0, config.backoffMultiplier());
  }

  @Test
  void shouldRejectInvalidBatchSize() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new OutboxProcessorConfig(
                0, 5, Duration.ofSeconds(1), Duration.ofMinutes(5), 2.0, false));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            new OutboxProcessorConfig(
                -1, 5, Duration.ofSeconds(1), Duration.ofMinutes(5), 2.0, false));
  }

  @Test
  void shouldRejectInvalidMaxAttempts() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new OutboxProcessorConfig(
                100, 0, Duration.ofSeconds(1), Duration.ofMinutes(5), 2.0, false));
  }

  @Test
  void shouldRejectInvalidBackoff() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new OutboxProcessorConfig(100, 5, null, Duration.ofMinutes(5), 2.0, false));

    assertThrows(
        IllegalArgumentException.class,
        () -> new OutboxProcessorConfig(100, 5, Duration.ZERO, Duration.ofMinutes(5), 2.0, false));
  }

  @Test
  void shouldRejectInvalidBackoffMultiplier() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new OutboxProcessorConfig(100, 5, Duration.ofSeconds(1), Duration.ofMinutes(5), 1.0, false));

    assertThrows(
        IllegalArgumentException.class,
        () -> new OutboxProcessorConfig(100, 5, Duration.ofSeconds(1), Duration.ofMinutes(5), 0.5, false));
  }
}
