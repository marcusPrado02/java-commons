package com.marcusprado02.commons.starter.idempotency;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "commons.idempotency")
public record IdempotencyProperties(Duration defaultTtl) {
  public IdempotencyProperties {
    if (defaultTtl == null) {
      defaultTtl = Duration.ofMinutes(5);
    }
  }
}
