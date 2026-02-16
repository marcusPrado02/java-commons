package com.marcusprado02.commons.app.webhooks;

import java.time.Duration;
import java.util.Objects;

final class ExponentialBackoffRetryPolicy implements RetryPolicy {

  private final int maxRetries;
  private final Duration initialDelay;
  private final double multiplier;
  private final Duration maxDelay;

  ExponentialBackoffRetryPolicy(int maxRetries, Duration initialDelay) {
    this(maxRetries, initialDelay, 2.0, Duration.ofHours(1));
  }

  ExponentialBackoffRetryPolicy(
      int maxRetries, Duration initialDelay, double multiplier, Duration maxDelay) {
    if (maxRetries < 0) {
      throw new IllegalArgumentException("maxRetries must be >= 0");
    }
    Objects.requireNonNull(initialDelay, "initialDelay cannot be null");
    if (multiplier <= 1.0) {
      throw new IllegalArgumentException("multiplier must be > 1.0");
    }
    Objects.requireNonNull(maxDelay, "maxDelay cannot be null");

    this.maxRetries = maxRetries;
    this.initialDelay = initialDelay;
    this.multiplier = multiplier;
    this.maxDelay = maxDelay;
  }

  @Override
  public int getMaxRetries() {
    return maxRetries;
  }

  @Override
  public Duration getRetryDelay(int attemptNumber) {
    if (attemptNumber <= 0) {
      throw new IllegalArgumentException("attemptNumber must be > 0");
    }

    double delaySeconds = initialDelay.toSeconds() * Math.pow(multiplier, attemptNumber - 1);
    long cappedSeconds = Math.min((long) delaySeconds, maxDelay.toSeconds());
    return Duration.ofSeconds(cappedSeconds);
  }
}
