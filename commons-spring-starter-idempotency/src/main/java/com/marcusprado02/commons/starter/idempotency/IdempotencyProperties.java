package com.marcusprado02.commons.starter.idempotency;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "commons.idempotency")
public record IdempotencyProperties(
    Duration defaultTtl,
    Web web,
    Aop aop) {

  public record Web(
      boolean enabled,
      String headerName,
      DuplicateRequestStrategy onDuplicate,
      ResultRefStrategy resultRefStrategy) {

    public Web {
      if (headerName == null || headerName.isBlank()) {
        headerName = "Idempotency-Key";
      }
      if (onDuplicate == null) {
        onDuplicate = DuplicateRequestStrategy.CONFLICT;
      }
      if (resultRefStrategy == null) {
        resultRefStrategy = ResultRefStrategy.LOCATION_HEADER;
      }
    }
  }

  public record Aop(boolean enabled) {
    public Aop {
      // default stays false unless explicitly enabled via dependency + property
    }
  }

  public IdempotencyProperties {
    if (defaultTtl == null) {
      defaultTtl = Duration.ofMinutes(5);
    }

    if (web == null) {
      web = new Web(false, "Idempotency-Key", DuplicateRequestStrategy.CONFLICT, ResultRefStrategy.LOCATION_HEADER);
    }
    if (aop == null) {
      aop = new Aop(false);
    }
  }
}
