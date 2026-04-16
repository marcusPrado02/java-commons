package com.marcusprado02.commons.app.health;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Aggregates health checks from multiple indicators.
 *
 * <p>Combines individual health checks into an overall system health status.
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * HealthAggregator aggregator = new HealthAggregator(
 *     List.of(
 *         new DatabaseHealthIndicator(),
 *         new CacheHealthIndicator(),
 *         new DiskSpaceHealthIndicator()
 *     )
 * );
 *
 * CompositeHealth health = aggregator.aggregate();
 * }</pre>
 */
public final class HealthAggregator {

  private final List<HealthIndicator> indicators;
  private final AggregationStrategy strategy;

  public HealthAggregator(List<HealthIndicator> indicators) {
    this(indicators, AggregationStrategy.WORST_STATUS);
  }

  public HealthAggregator(List<HealthIndicator> indicators, AggregationStrategy strategy) {
    this.indicators = List.copyOf(Objects.requireNonNull(indicators, "indicators cannot be null"));
    this.strategy = Objects.requireNonNull(strategy, "strategy cannot be null");
  }

  /**
   * Aggregates all health checks.
   *
   * @return composite health result
   */
  public CompositeHealth aggregate() {
    Map<String, Health> checks = new LinkedHashMap<>();

    for (HealthIndicator indicator : indicators) {
      try {
        Health health = indicator.check();
        checks.put(indicator.name(), health);
      } catch (Exception e) {
        Health errorHealth =
            Health.down()
                .withDetail("error", e.getClass().getName())
                .withDetail("message", e.getMessage())
                .build();
        checks.put(indicator.name(), errorHealth);
      }
    }

    HealthStatus overallStatus = strategy.aggregate(checks, indicators);

    return new CompositeHealth(overallStatus, checks, Instant.now());
  }

  /** Strategy for aggregating health statuses. */
  public enum AggregationStrategy {

    /**
     * Overall status is the worst of all statuses.
     *
     * <p>DOWN > UNKNOWN > DEGRADED > UP
     */
    WORST_STATUS {
      @Override
      public HealthStatus aggregate(Map<String, Health> checks, List<HealthIndicator> indicators) {
        HealthStatus worst = HealthStatus.UP;

        for (Map.Entry<String, Health> entry : checks.entrySet()) {
          HealthStatus status = entry.getValue().getStatus();

          // Find the corresponding indicator
          HealthIndicator indicator =
              indicators.stream()
                  .filter(i -> i.name().equals(entry.getKey()))
                  .findFirst()
                  .orElse(null);

          boolean isCritical = indicator != null && indicator.isCritical();

          // If critical check is DOWN or UNKNOWN, overall is DOWN
          if (isCritical && (status == HealthStatus.DOWN || status == HealthStatus.UNKNOWN)) {
            return HealthStatus.DOWN;
          }

          // Update worst status
          if (status == HealthStatus.DOWN && worst != HealthStatus.DOWN) {
            worst = HealthStatus.DOWN;
          } else if (status == HealthStatus.UNKNOWN
              && worst != HealthStatus.DOWN
              && worst != HealthStatus.UNKNOWN) {
            worst = HealthStatus.UNKNOWN;
          } else if (status == HealthStatus.DEGRADED
              && worst != HealthStatus.DOWN
              && worst != HealthStatus.UNKNOWN
              && worst != HealthStatus.DEGRADED) {
            worst = HealthStatus.DEGRADED;
          }
        }

        return worst;
      }
    },

    /** Overall status is UP only if all are UP. */
    ALL_UP {
      @Override
      public HealthStatus aggregate(Map<String, Health> checks, List<HealthIndicator> indicators) {
        boolean allUp = checks.values().stream().allMatch(h -> h.getStatus() == HealthStatus.UP);
        return allUp ? HealthStatus.UP : HealthStatus.DOWN;
      }
    };

    public abstract HealthStatus aggregate(
        Map<String, Health> checks, List<HealthIndicator> indicators);
  }
}
