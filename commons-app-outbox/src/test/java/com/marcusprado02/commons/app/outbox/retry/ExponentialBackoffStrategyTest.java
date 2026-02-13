package com.marcusprado02.commons.app.outbox.retry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.marcusprado02.commons.app.outbox.config.OutboxProcessorConfig;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ExponentialBackoffStrategyTest {

  @Test
  void shouldCalculateExponentialBackoffDelay() {
    Duration initial = Duration.ofSeconds(1);
    Duration max = Duration.ofMinutes(5);
    double multiplier = 2.0;

    ExponentialBackoffStrategy strategy = new ExponentialBackoffStrategy(initial, max, multiplier);

    assertEquals(1000, strategy.calculateDelayMillis(0));
    assertEquals(2000, strategy.calculateDelayMillis(1));
    assertEquals(4000, strategy.calculateDelayMillis(2));
    assertEquals(8000, strategy.calculateDelayMillis(3));
  }

  @Test
  void shouldCapDelayAtMaxBackoff() {
    Duration initial = Duration.ofSeconds(1);
    Duration max = Duration.ofSeconds(10);
    double multiplier = 2.0;

    ExponentialBackoffStrategy strategy = new ExponentialBackoffStrategy(initial, max, multiplier);

    long delay = strategy.calculateDelayMillis(10);
    assertEquals(10000, delay);
  }

  @Test
  void shouldCalculateNextRetryTime() {
    Duration initial = Duration.ofSeconds(1);
    Duration max = Duration.ofMinutes(5);
    double multiplier = 2.0;

    ExponentialBackoffStrategy strategy = new ExponentialBackoffStrategy(initial, max, multiplier);

    Instant base = Instant.parse("2026-02-13T10:00:00Z");
    Instant next = strategy.calculateNextRetry(1, base);

    assertEquals(Instant.parse("2026-02-13T10:00:02Z"), next);
  }

  @Test
  void shouldWorkWithDefaultConfig() {
    OutboxProcessorConfig config = OutboxProcessorConfig.defaults();
    ExponentialBackoffStrategy strategy = new ExponentialBackoffStrategy(config);

    long delay = strategy.calculateDelayMillis(0);
    assertTrue(delay > 0);
  }
}
