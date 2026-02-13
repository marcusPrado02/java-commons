package com.marcusprado02.commons.app.outbox.config;

import java.time.Duration;

/**
 * Configuration for OutboxProcessor behavior.
 *
 * @param batchSize Maximum number of messages to process in one batch
 * @param maxAttempts Maximum number of retry attempts before moving to DEAD
 * @param initialBackoff Initial backoff duration for exponential backoff retry
 * @param maxBackoff Maximum backoff duration for exponential backoff retry
 * @param backoffMultiplier Multiplier for exponential backoff (e.g., 2.0 for doubling)
 * @param useCircuitBreaker Whether to use circuit breaker for publishing
 */
public record OutboxProcessorConfig(
    int batchSize,
    int maxAttempts,
    Duration initialBackoff,
    Duration maxBackoff,
    double backoffMultiplier,
    boolean useCircuitBreaker) {

  public static final int DEFAULT_BATCH_SIZE = 100;
  public static final int DEFAULT_MAX_ATTEMPTS = 5;
  public static final Duration DEFAULT_INITIAL_BACKOFF = Duration.ofSeconds(1);
  public static final Duration DEFAULT_MAX_BACKOFF = Duration.ofMinutes(5);
  public static final double DEFAULT_BACKOFF_MULTIPLIER = 2.0;

  public OutboxProcessorConfig {
    if (batchSize <= 0) {
      throw new IllegalArgumentException("batchSize must be positive");
    }
    if (maxAttempts <= 0) {
      throw new IllegalArgumentException("maxAttempts must be positive");
    }
    if (initialBackoff == null || initialBackoff.isNegative() || initialBackoff.isZero()) {
      throw new IllegalArgumentException("initialBackoff must be positive");
    }
    if (maxBackoff == null || maxBackoff.isNegative() || maxBackoff.isZero()) {
      throw new IllegalArgumentException("maxBackoff must be positive");
    }
    if (backoffMultiplier <= 1.0) {
      throw new IllegalArgumentException("backoffMultiplier must be > 1.0");
    }
  }

  public static OutboxProcessorConfig defaults() {
    return new OutboxProcessorConfig(
        DEFAULT_BATCH_SIZE,
        DEFAULT_MAX_ATTEMPTS,
        DEFAULT_INITIAL_BACKOFF,
        DEFAULT_MAX_BACKOFF,
        DEFAULT_BACKOFF_MULTIPLIER,
        false);
  }
}
