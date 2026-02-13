package com.marcusprado02.commons.starter.outbox;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "commons.outbox")
public record OutboxProperties(
    Processing processing, Scheduling scheduling, Retry retry, Health health) {

  public OutboxProperties {
    if (processing == null) {
      processing = new Processing(100, false);
    }
    if (scheduling == null) {
      scheduling = new Scheduling(false, Duration.ofSeconds(2));
    }
    if (retry == null) {
      retry = new Retry(5, Duration.ofSeconds(1), Duration.ofMinutes(5), 2.0);
    }
    if (health == null) {
      health = new Health(true, 1000, 100);
    }
  }

  public record Processing(int batchSize, boolean useCircuitBreaker) {
    public Processing {
      if (batchSize <= 0) {
        batchSize = 100;
      }
    }
  }

  public record Scheduling(boolean enabled, Duration fixedDelay) {
    public Scheduling {
      if (fixedDelay == null || fixedDelay.isNegative() || fixedDelay.isZero()) {
        fixedDelay = Duration.ofSeconds(2);
      }
    }
  }

  public record Retry(
      int maxAttempts, Duration initialBackoff, Duration maxBackoff, double backoffMultiplier) {
    public Retry {
      if (maxAttempts <= 0) {
        maxAttempts = 5;
      }
      if (initialBackoff == null || initialBackoff.isNegative() || initialBackoff.isZero()) {
        initialBackoff = Duration.ofSeconds(1);
      }
      if (maxBackoff == null || maxBackoff.isNegative() || maxBackoff.isZero()) {
        maxBackoff = Duration.ofMinutes(5);
      }
      if (backoffMultiplier <= 1.0) {
        backoffMultiplier = 2.0;
      }
    }
  }

  public record Health(boolean enabled, long warningThreshold, long errorThreshold) {
    public Health {
      if (warningThreshold <= 0) {
        warningThreshold = 1000;
      }
      if (errorThreshold <= 0) {
        errorThreshold = 100;
      }
    }
  }
}
