package com.marcusprado02.commons.starter.outbox.health;

import com.marcusprado02.commons.app.outbox.model.OutboxStatus;
import com.marcusprado02.commons.app.outbox.port.OutboxRepositoryPort;
import com.marcusprado02.commons.starter.outbox.OutboxProperties;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

/**
 * Health indicator for Outbox pattern processor.
 *
 * <p>Reports health based on pending and failed message counts:
 *
 * <ul>
 *   <li>DOWN: Failed count > errorThreshold
 *   <li>OUT_OF_SERVICE: Pending count > errorThreshold
 *   <li>DEGRADED: Failed or pending count > warningThreshold
 *   <li>UP: Otherwise
 * </ul>
 */
public final class OutboxHealthIndicator implements HealthIndicator {

  private final OutboxRepositoryPort repository;
  private final OutboxProperties properties;

  public OutboxHealthIndicator(OutboxRepositoryPort repository, OutboxProperties properties) {
    this.repository = Objects.requireNonNull(repository, "repository must not be null");
    this.properties = Objects.requireNonNull(properties, "properties must not be null");
  }

  @Override
  public Health health() {
    try {
      long pending = repository.countByStatus(OutboxStatus.PENDING);
      long processing = repository.countByStatus(OutboxStatus.PROCESSING);
      long published = repository.countByStatus(OutboxStatus.PUBLISHED);
      long failed = repository.countByStatus(OutboxStatus.FAILED);
      long dead = repository.countByStatus(OutboxStatus.DEAD);

      Map<String, Object> details = new LinkedHashMap<>();
      details.put("pending", pending);
      details.put("processing", processing);
      details.put("published", published);
      details.put("failed", failed);
      details.put("dead", dead);
      details.put("total", pending + processing + published + failed + dead);

      long warningThreshold = properties.health().warningThreshold();
      long errorThreshold = properties.health().errorThreshold();

      if (failed > errorThreshold) {
        return Health.down().withDetails(details).build();
      }

      if (pending > errorThreshold) {
        return Health.outOfService().withDetails(details).build();
      }

      if (failed > warningThreshold || pending > warningThreshold) {
        return Health.status("DEGRADED").withDetails(details).build();
      }

      return Health.up().withDetails(details).build();

    } catch (Exception ex) {
      return Health.down(ex).build();
    }
  }
}
