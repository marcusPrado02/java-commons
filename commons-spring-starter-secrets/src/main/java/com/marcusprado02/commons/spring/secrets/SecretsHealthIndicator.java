package com.marcusprado02.commons.spring.secrets;

import com.marcusprado02.commons.ports.secrets.SecretKey;
import com.marcusprado02.commons.ports.secrets.SecretStorePort;
import com.marcusprado02.commons.ports.secrets.SecretValue;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

/**
 * Health indicator for secret store connectivity.
 *
 * <p>Verifies secret store health by testing basic secret operations.
 */
@Component
@ConditionalOnClass(name = "org.springframework.boot.actuate.health.HealthIndicator")
public class SecretsHealthIndicator implements HealthIndicator {

  private static final Logger log = LoggerFactory.getLogger(SecretsHealthIndicator.class);
  private static final String HEALTH_CHECK_KEY = "health-check-" + UUID.randomUUID();
  private static final String HEALTH_CHECK_VALUE = "ok";

  private final SecretStorePort secretStorePort;

  public SecretsHealthIndicator(SecretStorePort secretStorePort) {
    this.secretStorePort = secretStorePort;
  }

  @Override
  public Health health() {
    try {
      // Test secret write
      SecretKey key = SecretKey.of(HEALTH_CHECK_KEY);
      SecretValue value = SecretValue.of(HEALTH_CHECK_VALUE);
      secretStorePort.put(key, value);

      // Test secret read
      String retrievedValue = secretStorePort.get(key).map(SecretValue::asString).orElse(null);

      if (HEALTH_CHECK_VALUE.equals(retrievedValue)) {
        // Cleanup
        secretStorePort.delete(key);

        return Health.up()
            .withDetail("type", getSecretStoreType())
            .withDetail("status", "Secret store read/write successful")
            .build();
      } else {
        return Health.down()
            .withDetail("type", getSecretStoreType())
            .withDetail("error", "Secret read failed: expected 'ok', got: " + value)
            .build();
      }
    } catch (Exception e) {
      log.warn("Secret store health check failed", e);
      return Health.down()
          .withDetail("type", getSecretStoreType())
          .withDetail("error", e.getMessage())
          .build();
    }
  }

  private String getSecretStoreType() {
    return secretStorePort.getClass().getSimpleName();
  }
}
