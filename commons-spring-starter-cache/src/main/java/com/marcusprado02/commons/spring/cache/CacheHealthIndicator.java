package com.marcusprado02.commons.spring.cache;

import com.marcusprado02.commons.ports.cache.CachePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

/**
 * Health indicator for cache connectivity.
 *
 * <p>Verifies cache health by testing basic cache operations.
 */
@Component
@ConditionalOnClass(name = "org.springframework.boot.actuate.health.HealthIndicator")
public class CacheHealthIndicator implements HealthIndicator {

  private static final Logger log = LoggerFactory.getLogger(CacheHealthIndicator.class);
  private static final String HEALTH_CHECK_KEY = "health:check:ping";
  private static final String HEALTH_CHECK_VALUE = "pong";

  private final CachePort<String, Object> cachePort;

  public CacheHealthIndicator(CachePort<String, Object> cachePort) {
    this.cachePort = cachePort;
  }

  @Override
  public Health health() {
    try {
      // Test cache write
      cachePort.put(HEALTH_CHECK_KEY, HEALTH_CHECK_VALUE);

      // Test cache read
      Object value = cachePort.get(HEALTH_CHECK_KEY).orElse(null);

      if (HEALTH_CHECK_VALUE.equals(value)) {
        // Cleanup
        cachePort.remove(HEALTH_CHECK_KEY);

        return Health.up()
            .withDetail("type", getCacheType())
            .withDetail("status", "Cache read/write successful")
            .build();
      } else {
        return Health.down()
            .withDetail("type", getCacheType())
            .withDetail("error", "Cache read failed: expected 'pong', got: " + value)
            .build();
      }
    } catch (Exception e) {
      log.warn("Cache health check failed", e);
      return Health.down()
          .withDetail("type", getCacheType())
          .withDetail("error", e.getMessage())
          .build();
    }
  }

  private String getCacheType() {
    return cachePort.getClass().getSimpleName();
  }
}
