package com.marcusprado02.commons.app.health.indicators;

import com.marcusprado02.commons.app.health.Health;
import com.marcusprado02.commons.app.health.HealthIndicator;
import java.io.File;

/**
 * Health indicator for disk space.
 *
 * <p>Checks available disk space and reports DEGRADED or DOWN if below thresholds.
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * // Warn at 1GB, fail at 100MB
 * HealthIndicator diskHealth = new DiskSpaceHealthIndicator(
 *     new File("/"),
 *     1024 * 1024 * 1024L,      // 1GB warning
 *     100 * 1024 * 1024L         // 100MB critical
 * );
 * }</pre>
 */
public final class DiskSpaceHealthIndicator implements HealthIndicator {

  private final File path;
  private final long warningThreshold;
  private final long criticalThreshold;

  /**
   * Creates a disk space health indicator.
   *
   * @param path the path to check
   * @param warningThreshold bytes available to trigger DEGRADED
   * @param criticalThreshold bytes available to trigger DOWN
   */
  public DiskSpaceHealthIndicator(File path, long warningThreshold, long criticalThreshold) {
    this.path = path;
    this.warningThreshold = warningThreshold;
    this.criticalThreshold = criticalThreshold;
  }

  /**
   * Creates with default thresholds (1GB warning, 100MB critical).
   *
   * @param path the path to check
   */
  public DiskSpaceHealthIndicator(File path) {
    this(path, 1024 * 1024 * 1024L, 100 * 1024 * 1024L);
  }

  @Override
  public String name() {
    return "diskSpace";
  }

  @Override
  public Health check() {
    try {
      long freeSpace = path.getUsableSpace();
      long totalSpace = path.getTotalSpace();

      if (freeSpace < criticalThreshold) {
        return Health.down()
            .withDetail("path", path.getAbsolutePath())
            .withDetail("free", formatBytes(freeSpace))
            .withDetail("total", formatBytes(totalSpace))
            .withDetail("threshold", formatBytes(criticalThreshold))
            .build();
      }

      if (freeSpace < warningThreshold) {
        return Health.degraded()
            .withDetail("path", path.getAbsolutePath())
            .withDetail("free", formatBytes(freeSpace))
            .withDetail("total", formatBytes(totalSpace))
            .withDetail("threshold", formatBytes(warningThreshold))
            .build();
      }

      return Health.up()
          .withDetail("path", path.getAbsolutePath())
          .withDetail("free", formatBytes(freeSpace))
          .withDetail("total", formatBytes(totalSpace))
          .build();

    } catch (Exception e) {
      return Health.down().withException(e).build();
    }
  }

  private String formatBytes(long bytes) {
    if (bytes < 1024) {
      return bytes + " B";
    }
    if (bytes < 1024 * 1024) {
      return (bytes / 1024) + " KB";
    }
    if (bytes < 1024 * 1024 * 1024) {
      return (bytes / (1024 * 1024)) + " MB";
    }
    return (bytes / (1024 * 1024 * 1024)) + " GB";
  }
}
