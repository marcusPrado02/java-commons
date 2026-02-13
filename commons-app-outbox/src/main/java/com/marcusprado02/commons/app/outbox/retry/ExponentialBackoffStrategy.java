package com.marcusprado02.commons.app.outbox.retry;

import com.marcusprado02.commons.app.outbox.config.OutboxProcessorConfig;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Exponential backoff strategy for retry delays.
 *
 * <p>Calculates next retry time based on attempt count: delay = initialBackoff * (multiplier ^
 * attempts), capped at maxBackoff.
 */
public final class ExponentialBackoffStrategy {

  private final Duration initialBackoff;
  private final Duration maxBackoff;
  private final double multiplier;

  public ExponentialBackoffStrategy(OutboxProcessorConfig config) {
    this(config.initialBackoff(), config.maxBackoff(), config.backoffMultiplier());
  }

  public ExponentialBackoffStrategy(Duration initialBackoff, Duration maxBackoff, double multiplier) {
    this.initialBackoff = Objects.requireNonNull(initialBackoff, "initialBackoff must not be null");
    this.maxBackoff = Objects.requireNonNull(maxBackoff, "maxBackoff must not be null");
    this.multiplier = multiplier;
  }

  /**
   * Calculate next retry time based on current attempt count.
   *
   * @param currentAttempt Current attempt number (0-based)
   * @param baseTime Base time to add delay to
   * @return Next retry time
   */
  public Instant calculateNextRetry(int currentAttempt, Instant baseTime) {
    long delayMillis = calculateDelayMillis(currentAttempt);
    return baseTime.plusMillis(delayMillis);
  }

  /**
   * Calculate delay in milliseconds for given attempt.
   *
   * @param attempt Attempt number (0-based)
   * @return Delay in milliseconds
   */
  public long calculateDelayMillis(int attempt) {
    if (attempt <= 0) {
      return initialBackoff.toMillis();
    }

    long delay = (long) (initialBackoff.toMillis() * Math.pow(multiplier, attempt));
    long maxDelayMillis = maxBackoff.toMillis();
    return Math.min(delay, maxDelayMillis);
  }
}
