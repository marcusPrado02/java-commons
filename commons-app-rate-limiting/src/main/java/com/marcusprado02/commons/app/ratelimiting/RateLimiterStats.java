package com.marcusprado02.commons.app.ratelimiting;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Statistics about rate limiter usage and performance.
 *
 * <p>This record provides metrics about rate limiter operations, including request counts, bucket
 * usage, and performance information. These statistics are useful for monitoring, alerting, and
 * optimization.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * RateLimiterStats stats = rateLimiter.getStats();
 *
 * // Check rejection rate
 * double rejectionRate = stats.getRejectionRate();
 * if (rejectionRate > 0.1) {
 *     log.warn("High rejection rate: {}%", rejectionRate * 100);
 * }
 *
 * // Monitor performance
 * log.info("Average response time: {}ms", stats.getAverageResponseTimeMs());
 * }</pre>
 */
public record RateLimiterStats(
    long totalRequests,
    long allowedRequests,
    long rejectedRequests,
    long activeBuckets,
    double averageResponseTimeMs,
    Instant lastResetTime) {

  /**
   * Creates empty statistics.
   *
   * @return empty statistics record
   */
  public static RateLimiterStats empty() {
    return new RateLimiterStats(0, 0, 0, 0, 0.0, Instant.now());
  }

  /**
   * Creates a builder for rate limiter statistics.
   *
   * @return new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Gets the rejection rate as a percentage (0.0 to 1.0).
   *
   * @return rejection rate
   */
  public double getRejectionRate() {
    if (totalRequests == 0) {
      return 0.0;
    }
    return (double) rejectedRequests / totalRequests;
  }

  /**
   * Gets the allow rate as a percentage (0.0 to 1.0).
   *
   * @return allow rate
   */
  public double getAllowRate() {
    return 1.0 - getRejectionRate();
  }

  /**
   * Returns whether any requests have been processed.
   *
   * @return true if total requests > 0
   */
  public boolean hasActivity() {
    return totalRequests > 0;
  }

  /**
   * Mutable builder for collecting rate limiter statistics.
   */
  public static class Builder {
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong allowedRequests = new AtomicLong(0);
    private final AtomicLong rejectedRequests = new AtomicLong(0);
    private final AtomicLong activeBuckets = new AtomicLong(0);
    private volatile double averageResponseTimeMs = 0.0;
    private volatile Instant lastResetTime = Instant.now();

    private Builder() {}

    /**
     * Records a successful (allowed) request.
     *
     * @return this builder
     */
    public Builder recordAllowed() {
      totalRequests.incrementAndGet();
      allowedRequests.incrementAndGet();
      return this;
    }

    /**
     * Records a rejected request.
     *
     * @return this builder
     */
    public Builder recordRejected() {
      totalRequests.incrementAndGet();
      rejectedRequests.incrementAndGet();
      return this;
    }

    /**
     * Sets the number of active buckets.
     *
     * @param count number of active buckets
     * @return this builder
     */
    public Builder activeBuckets(long count) {
      this.activeBuckets.set(count);
      return this;
    }

    /**
     * Updates the average response time.
     *
     * @param responseTimeMs new response time measurement
     * @return this builder
     */
    public Builder updateResponseTime(double responseTimeMs) {
      // Simple exponential moving average
      this.averageResponseTimeMs = (this.averageResponseTimeMs * 0.9) + (responseTimeMs * 0.1);
      return this;
    }

    /**
     * Resets all counters.
     *
     * @return this builder
     */
    public Builder reset() {
      totalRequests.set(0);
      allowedRequests.set(0);
      rejectedRequests.set(0);
      activeBuckets.set(0);
      averageResponseTimeMs = 0.0;
      lastResetTime = Instant.now();
      return this;
    }

    /**
     * Builds an immutable statistics snapshot.
     *
     * @return statistics record
     */
    public RateLimiterStats build() {
      return new RateLimiterStats(
          totalRequests.get(),
          allowedRequests.get(),
          rejectedRequests.get(),
          activeBuckets.get(),
          averageResponseTimeMs,
          lastResetTime);
    }

    /**
     * Gets current total requests.
     */
    public long getTotalRequests() {
      return totalRequests.get();
    }

    /**
     * Gets current allowed requests.
     */
    public long getAllowedRequests() {
      return allowedRequests.get();
    }

    /**
     * Gets current rejected requests.
     */
    public long getRejectedRequests() {
      return rejectedRequests.get();
    }

    /**
     * Gets current active buckets count.
     */
    public long getActiveBuckets() {
      return activeBuckets.get();
    }
  }
}
