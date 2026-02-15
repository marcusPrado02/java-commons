package com.marcusprado02.commons.app.ratelimiting.impl;

import com.marcusprado02.commons.app.ratelimiting.RateLimitConfig;
import com.marcusprado02.commons.app.ratelimiting.RateLimitResult;
import com.marcusprado02.commons.app.ratelimiting.RateLimiter;
import com.marcusprado02.commons.app.ratelimiting.RateLimiterStats;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import io.github.bucket4j.local.LocalBucketBuilder;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory rate limiter implementation using Bucket4j.
 *
 * <p>This implementation stores rate limiting buckets in local memory using ConcurrentHashMap. It
 * provides high performance for single-node applications but does not share state across multiple
 * application instances.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li><strong>High Performance</strong>: Local memory operations with minimal latency
 *   <li><strong>Thread Safety</strong>: Concurrent access using lock-free algorithms
 *   <li><strong>Automatic Cleanup</strong>: Optional periodic cleanup of unused buckets
 *   <li><strong>Statistics</strong>: Built-in metrics collection
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Create rate limiter with 100 requests per minute
 * RateLimitConfig config = RateLimitConfig.perMinute(100);
 * InMemoryRateLimiter rateLimiter = new InMemoryRateLimiter(config);
 *
 * // Use with different keys
 * RateLimitResult result = rateLimiter.tryConsume("user123");
 * if (result.isAllowed()) {
 *     // Original logic
 * }
 * }</pre>
 *
 * <p><strong>Note</strong>: This implementation is suitable for single-node deployments. For
 * distributed rate limiting across multiple nodes, use {@code RedisRateLimiter}.
 */
public class InMemoryRateLimiter implements RateLimiter {

  private final RateLimitConfig config;
  private final BucketConfiguration bucketConfiguration;
  private final ConcurrentMap<String, Bucket> buckets;
  private final RateLimiterStats.Builder statsBuilder;

  /**
   * Creates a new in-memory rate limiter with the specified configuration.
   *
   * @param config rate limiting configuration
   */
  public InMemoryRateLimiter(RateLimitConfig config) {
    this.config = config;
    this.buckets = new ConcurrentHashMap<>();
    this.statsBuilder = RateLimiterStats.builder();
    this.bucketConfiguration = createBucketConfiguration(config);
  }

  @Override
  public RateLimitResult tryConsume(String key, long tokens) {
    if (key == null || key.isEmpty()) {
      throw new IllegalArgumentException("Key cannot be null or empty");
    }
    if (tokens <= 0) {
      throw new IllegalArgumentException("Tokens must be positive");
    }

    long startTime = System.nanoTime();
    try {
      Bucket bucket = getBucket(key);
      ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(tokens);

      RateLimitResult result;
      if (probe.isConsumed()) {
        result = RateLimitResult.allowed(
            tokens,
            probe.getRemainingTokens(),
            config.getCapacity(),
            probe.getNanosToWaitForRefill());
        statsBuilder.recordAllowed();
      } else {
        result = RateLimitResult.rejected(
            probe.getRemainingTokens(),
            config.getCapacity(),
            probe.getNanosToWaitForRefill());
        statsBuilder.recordRejected();
      }

      return result;
    } finally {
      long duration = System.nanoTime() - startTime;
      statsBuilder.updateResponseTime(duration / 1_000_000.0); // Convert to milliseconds
    }
  }

  @Override
  public RateLimitResult probe(String key) {
    if (key == null || key.isEmpty()) {
      throw new IllegalArgumentException("Key cannot be null or empty");
    }

    Bucket bucket = getBucket(key);
    long availableTokens = bucket.getAvailableTokens();

    return RateLimitResult.allowed(
        0, // No tokens consumed
        availableTokens,
        config.getCapacity(),
        System.nanoTime()); // Current time as refill time
  }

  @Override
  public RateLimitConfig getConfig() {
    return config;
  }

  @Override
  public void reset(String key) {
    if (key == null || key.isEmpty()) {
      throw new IllegalArgumentException("Key cannot be null or empty");
    }
    buckets.remove(key);
  }

  @Override
  public RateLimiterStats getStats() {
    return statsBuilder
        .activeBuckets(buckets.size())
        .build();
  }

  /**
   * Gets the total number of active buckets.
   *
   * @return number of buckets currently stored in memory
   */
  public int getBucketCount() {
    return buckets.size();
  }

  /**
   * Clears all buckets from memory.
   *
   * <p>This method removes all rate limiting state. Use with caution as it effectively resets
   * rate limiting for all keys.
   */
  public void clearAll() {
    buckets.clear();
  }

  private Bucket getBucket(String key) {
    return buckets.computeIfAbsent(key, k ->
        new LocalBucketBuilder()
            .addLimit(createBandwidth())
            .build());
  }

  private BucketConfiguration createBucketConfiguration(RateLimitConfig config) {
    return BucketConfiguration.builder()
        .addLimit(createBandwidth())
        .build();
  }

  private Bandwidth createBandwidth() {
    return Bandwidth.classic(config.getCapacity(),
                           Refill.greedy(config.getRefillRate(), config.getRefillPeriod()));
  }
}
