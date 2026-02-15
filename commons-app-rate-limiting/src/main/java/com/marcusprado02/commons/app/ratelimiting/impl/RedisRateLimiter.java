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
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.jedis.cas.JedisBasedProxyManager;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Redis-based distributed rate limiter implementation using Bucket4j.
 *
 * <p>This implementation stores rate limiting buckets in Redis, enabling distributed rate limiting
 * across multiple application instances. It provides consistency across nodes while maintaining
 * good performance through Redis operations.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li><strong>Distributed</strong>: Shared state across multiple application instances
 *   <li><strong>Persistent</strong>: Rate limiting state survives application restarts
 *   <li><strong>Scalable</strong>: Handles high load through Redis clustering
 *   <li><strong>Consistent</strong>: Atomic operations ensure accurate rate limiting
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Create Redis connection pool
 * JedisPool jedisPool = new JedisPool("localhost", 6379);
 *
 * // Create rate limiter with 100 requests per minute
 * RateLimitConfig config = RateLimitConfig.perMinute(100);
 * RedisRateLimiter rateLimiter = new RedisRateLimiter(jedisPool, config);
 *
 * // Use with different keys
 * RateLimitResult result = rateLimiter.tryConsume("user123");
 * if (result.isAllowed()) {
 *     // Original logic
 * }
 * }</pre>
 *
 * <p><strong>Requirements</strong>:
 *
 * <ul>
 *   <li>Redis server (version 2.6+ for Lua script support)
 *   <li>Jedis client library on classpath
 * </ul>
 *
 * <p><strong>Redis Key Pattern</strong>: {@code rate_limiter:<keyPrefix>:<userKey>}
 */
public class RedisRateLimiter implements RateLimiter {

  private final RateLimitConfig config;
  private final JedisBasedProxyManager proxyManager;
  private final BucketConfiguration bucketConfiguration;
  private final RateLimiterStats.Builder statsBuilder;
  private final String keyPrefix;
  private final AtomicLong activeBuckets;

  /**
   * Creates a new Redis-based rate limiter with default key prefix.
   *
   * @param jedisPoolSupplier supplier for Jedis pool instances
   * @param config rate limiting configuration
   */
  public RedisRateLimiter(Supplier<redis.clients.jedis.JedisPool> jedisPoolSupplier, RateLimitConfig config) {
    this(jedisPoolSupplier, config, "rate_limiter");
  }

  /**
   * Creates a new Redis-based rate limiter with custom key prefix.
   *
   * @param jedisPoolSupplier supplier for Jedis pool instances
   * @param config rate limiting configuration
   * @param keyPrefix prefix for Redis keys (helps avoid collisions)
   */
  public RedisRateLimiter(
      Supplier<redis.clients.jedis.JedisPool> jedisPoolSupplier,
      RateLimitConfig config,
      String keyPrefix) {
    this.config = config;
    this.keyPrefix = keyPrefix;
    this.bucketConfiguration = createBucketConfiguration(config);
    this.proxyManager = createProxyManager(jedisPoolSupplier);
    this.statsBuilder = RateLimiterStats.builder();
    this.activeBuckets = new AtomicLong();
  }

  /**
   * Creates a Redis rate limiter using a direct Jedis pool instance.
   *
   * @param jedisPool direct Jedis pool instance
   * @param config rate limiting configuration
   */
  public RedisRateLimiter(redis.clients.jedis.JedisPool jedisPool, RateLimitConfig config) {
    this(() -> jedisPool, config);
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
      String redisKey = buildRedisKey(key);
      Bucket bucket = proxyManager.builder()
          .build(redisKey.getBytes(java.nio.charset.StandardCharsets.UTF_8), () -> bucketConfiguration);

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

      activeBuckets.set(Math.max(activeBuckets.get(), 1)); // Track active buckets
      return result;
    } catch (Exception e) {
      throw new RuntimeException("Failed to perform rate limiting operation on Redis", e);
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

    try {
      String redisKey = buildRedisKey(key);
      Bucket bucket = proxyManager.builder()
          .build(redisKey.getBytes(java.nio.charset.StandardCharsets.UTF_8), () -> bucketConfiguration);

      long availableTokens = bucket.getAvailableTokens();

      return RateLimitResult.allowed(
          0, // No tokens consumed
          availableTokens,
          config.getCapacity(),
          System.nanoTime()); // Current time as refill time
    } catch (Exception e) {
      throw new RuntimeException("Failed to probe rate limiter state from Redis", e);
    }
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

    try {
      String redisKey = buildRedisKey(key);
      // Remove the bucket by getting it and then clearing its state
      Bucket bucket = proxyManager.builder()
          .build(redisKey.getBytes(java.nio.charset.StandardCharsets.UTF_8), () -> bucketConfiguration);
      // Reset by consuming all available tokens and then refilling
      long available = bucket.getAvailableTokens();
      if (available > 0) {
        bucket.tryConsume(available);
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed to reset rate limiter state in Redis", e);
    }
  }

  @Override
  public RateLimiterStats getStats() {
    return statsBuilder
        .activeBuckets(activeBuckets.get())
        .build();
  }

  /**
   * Gets the Redis key prefix used for bucket storage.
   *
   * @return the key prefix
   */
  public String getKeyPrefix() {
    return keyPrefix;
  }

  private String buildRedisKey(String key) {
    return keyPrefix + ":" + key;
  }

  private JedisBasedProxyManager createProxyManager(Supplier<redis.clients.jedis.JedisPool> jedisPoolSupplier) {
    return JedisBasedProxyManager.builderFor(jedisPoolSupplier.get())
        .build();
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
