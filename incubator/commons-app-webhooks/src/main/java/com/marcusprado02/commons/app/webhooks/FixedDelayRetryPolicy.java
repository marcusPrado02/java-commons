package com.marcusprado02.commons.app.webhooks;

import java.time.Duration;
import java.util.Objects;

final class FixedDelayRetryPolicy implements RetryPolicy {

  private final int maxRetries;
  private final Duration delay;

  FixedDelayRetryPolicy(int maxRetries, Duration delay) {
    if (maxRetries < 0) {
      throw new IllegalArgumentException("maxRetries must be >= 0");
    }
    this.maxRetries = maxRetries;
    this.delay = Objects.requireNonNull(delay, "delay cannot be null");
  }

  @Override
  public int getMaxRetries() {
    return maxRetries;
  }

  @Override
  public Duration getRetryDelay(int attemptNumber) {
    return delay;
  }
}
