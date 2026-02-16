package com.marcusprado02.commons.app.webhooks;

import java.time.Duration;

final class NoRetryPolicy implements RetryPolicy {

  @Override
  public int getMaxRetries() {
    return 0;
  }

  @Override
  public Duration getRetryDelay(int attemptNumber) {
    return Duration.ZERO;
  }
}
