package com.marcusprado02.commons.app.health.indicators;

import com.marcusprado02.commons.app.health.Health;
import com.marcusprado02.commons.app.health.HealthIndicator;

/**
 * Health indicator for memory usage.
 *
 * <p>Monitors JVM memory usage and reports DEGRADED or DOWN based on thresholds.
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * // Warn at 80%, fail at 95%
 * HealthIndicator memoryHealth = new MemoryHealthIndicator(0.80, 0.95);
 * }</pre>
 */
public final class MemoryHealthIndicator implements HealthIndicator {

  private final double warningThreshold;
  private final double criticalThreshold;

  /**
   * Creates a memory health indicator.
   *
   * @param warningThreshold percentage (0.0-1.0) to trigger DEGRADED
   * @param criticalThreshold percentage (0.0-1.0) to trigger DOWN
   */
  public MemoryHealthIndicator(double warningThreshold, double criticalThreshold) {
    this.warningThreshold = warningThreshold;
    this.criticalThreshold = criticalThreshold;
  }

  /** Creates with default thresholds (80% warning, 95% critical). */
  public MemoryHealthIndicator() {
    this(0.80, 0.95);
  }

  @Override
  public String name() {
    return "memory";
  }

  @Override
  public Health check() {
    Runtime runtime = Runtime.getRuntime();
    long maxMemory = runtime.maxMemory();
    long totalMemory = runtime.totalMemory();
    long freeMemory = runtime.freeMemory();
    long usedMemory = totalMemory - freeMemory;

    double usagePercentage = (double) usedMemory / maxMemory;

    Health.Builder builder;

    if (usagePercentage >= criticalThreshold) {
      builder = Health.down();
    } else if (usagePercentage >= warningThreshold) {
      builder = Health.degraded();
    } else {
      builder = Health.up();
    }

    return builder
        .withDetail("used", formatBytes(usedMemory))
        .withDetail("max", formatBytes(maxMemory))
        .withDetail("usage", String.format("%.2f%%", usagePercentage * 100))
        .build();
  }

  @Override
  public boolean isCritical() {
    return false;
  }

  private String formatBytes(long bytes) {
    long mb = bytes / (1024 * 1024);
    return mb + " MB";
  }
}
