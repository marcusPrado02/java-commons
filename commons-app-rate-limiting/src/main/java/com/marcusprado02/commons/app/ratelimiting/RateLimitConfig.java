package com.marcusprado02.commons.app.ratelimiting;

import java.time.Duration;
import java.util.Objects;

/**
 * Rate limiting configuration defining bucket capacity, refill rate, and time window.
 *
 * <p>This class defines the parameters for a token bucket algorithm:
 *
 * <ul>
 *   <li><strong>Capacity</strong>: Maximum number of tokens in the bucket
 *   <li><strong>Refill Rate</strong>: Number of tokens added per time period
 *   <li><strong>Refill Period</strong>: Time period for refill rate
 * </ul>
 *
 * <p>Example configurations:
 *
 * <pre>{@code
 * // 100 requests per hour
 * RateLimitConfig config = RateLimitConfig.builder()
 *     .capacity(100)
 *     .refillRate(100)
 *     .refillPeriod(Duration.ofHours(1))
 *     .build();
 *
 * // 10 requests per minute with burst of 20
 * RateLimitConfig config = RateLimitConfig.builder()
 *     .capacity(20)
 *     .refillRate(10)
 *     .refillPeriod(Duration.ofMinutes(1))
 *     .build();
 * }</pre>
 */
public class RateLimitConfig {

  private final long capacity;
  private final long refillRate;
  private final Duration refillPeriod;

  private RateLimitConfig(long capacity, long refillRate, Duration refillPeriod) {
    this.capacity = capacity;
    this.refillRate = refillRate;
    this.refillPeriod = refillPeriod;
  }

  /**
   * Creates a new builder for rate limit configuration.
   *
   * @return new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates a rate limit configuration with requests per time period.
   *
   * @param requestsPerPeriod number of requests allowed per period
   * @param period time period
   * @return rate limit configuration
   */
  public static RateLimitConfig of(long requestsPerPeriod, Duration period) {
    return builder()
        .capacity(requestsPerPeriod)
        .refillRate(requestsPerPeriod)
        .refillPeriod(period)
        .build();
  }

  /**
   * Creates a rate limit configuration with burst capacity.
   *
   * @param requestsPerPeriod steady rate of requests per period
   * @param period time period
   * @param burstCapacity maximum burst capacity
   * @return rate limit configuration
   */
  public static RateLimitConfig withBurst(long requestsPerPeriod, Duration period, long burstCapacity) {
    return builder()
        .capacity(burstCapacity)
        .refillRate(requestsPerPeriod)
        .refillPeriod(period)
        .build();
  }

  // Common configurations

  /**
   * 100 requests per hour.
   */
  public static RateLimitConfig perHour(long requests) {
    return of(requests, Duration.ofHours(1));
  }

  /**
   * Requests per minute.
   */
  public static RateLimitConfig perMinute(long requests) {
    return of(requests, Duration.ofMinutes(1));
  }

  /**
   * Requests per second.
   */
  public static RateLimitConfig perSecond(long requests) {
    return of(requests, Duration.ofSeconds(1));
  }

  /**
   * Gets the bucket capacity (maximum tokens).
   */
  public long getCapacity() {
    return capacity;
  }

  /**
   * Gets the refill rate (tokens per period).
   */
  public long getRefillRate() {
    return refillRate;
  }

  /**
   * Gets the refill period.
   */
  public Duration getRefillPeriod() {
    return refillPeriod;
  }

  /**
   * Calculates the refill rate per second.
   *
   * @return tokens per second
   */
  public double getRefillRatePerSecond() {
    return (double) refillRate / refillPeriod.getSeconds();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    RateLimitConfig that = (RateLimitConfig) o;
    return capacity == that.capacity &&
        refillRate == that.refillRate &&
        Objects.equals(refillPeriod, that.refillPeriod);
  }

  @Override
  public int hashCode() {
    return Objects.hash(capacity, refillRate, refillPeriod);
  }

  @Override
  public String toString() {
    return String.format("RateLimitConfig{capacity=%d, refillRate=%d, refillPeriod=%s}",
        capacity, refillRate, refillPeriod);
  }

  /**
   * Builder for {@link RateLimitConfig}.
   */
  public static class Builder {
    private long capacity;
    private long refillRate;
    private Duration refillPeriod;

    private Builder() {}

    /**
     * Sets the bucket capacity (maximum number of tokens).
     *
     * @param capacity bucket capacity (must be > 0)
     * @return this builder
     */
    public Builder capacity(long capacity) {
      if (capacity <= 0) {
        throw new IllegalArgumentException("Capacity must be positive");
      }
      this.capacity = capacity;
      return this;
    }

    /**
     * Sets the refill rate (tokens added per period).
     *
     * @param refillRate refill rate (must be > 0)
     * @return this builder
     */
    public Builder refillRate(long refillRate) {
      if (refillRate <= 0) {
        throw new IllegalArgumentException("Refill rate must be positive");
      }
      this.refillRate = refillRate;
      return this;
    }

    /**
     * Sets the refill period.
     *
     * @param refillPeriod refill period (must be positive)
     * @return this builder
     */
    public Builder refillPeriod(Duration refillPeriod) {
      if (refillPeriod == null || refillPeriod.isNegative() || refillPeriod.isZero()) {
        throw new IllegalArgumentException("Refill period must be positive");
      }
      this.refillPeriod = refillPeriod;
      return this;
    }

    /**
     * Builds the rate limit configuration.
     *
     * @return rate limit configuration
     * @throws IllegalStateException if required fields are not set
     */
    public RateLimitConfig build() {
      if (capacity <= 0) {
        throw new IllegalStateException("Capacity must be set and positive");
      }
      if (refillRate <= 0) {
        throw new IllegalStateException("Refill rate must be set and positive");
      }
      if (refillPeriod == null) {
        throw new IllegalStateException("Refill period must be set");
      }
      return new RateLimitConfig(capacity, refillRate, refillPeriod);
    }
  }
}