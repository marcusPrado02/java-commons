package com.marcusprado02.commons.app.ratelimiting;

import com.marcusprado02.commons.app.ratelimiting.impl.InMemoryRateLimiter;
import com.marcusprado02.commons.app.ratelimiting.impl.RedisRateLimiter;

import java.util.function.Supplier;

/**
 * Factory for creating different types of rate limiters.
 *
 * <p>This factory simplifies rate limiter creation by providing convenient methods for common
 * scenarios. It supports both in-memory and Redis-based implementations with sensible defaults.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Create in-memory rate limiter
 * RateLimiter rateLimiter = RateLimiterFactory.inMemory()
 *     .withConfig(RateLimitConfig.perMinute(100))
 *     .build();
 *
 * // Create Redis rate limiter
 * JedisPool jedisPool = new JedisPool("localhost", 6379);
 * RateLimiter rateLimiter = RateLimiterFactory.redis(jedisPool)
 *     .withConfig(RateLimitConfig.perSecond(10))
 *     .withKeyPrefix("api_rate_limit")
 *     .build();
 * }</pre>
 */
public final class RateLimiterFactory {

  private RateLimiterFactory() {
    // Utility class
  }

  /**
   * Creates a builder for in-memory rate limiters.
   *
   * @return builder for configuring in-memory rate limiters
   */
  public static InMemoryBuilder inMemory() {
    return new InMemoryBuilder();
  }

  /**
   * Creates a builder for Redis-based rate limiters.
   *
   * @param jedisPoolSupplier supplier for Jedis pool instances
   * @return builder for configuring Redis rate limiters
   */
  public static RedisBuilder redis(Supplier<redis.clients.jedis.JedisPool> jedisPoolSupplier) {
    return new RedisBuilder(jedisPoolSupplier);
  }

  /**
   * Creates a builder for Redis-based rate limiters using a direct pool instance.
   *
   * @param jedisPool direct Jedis pool instance
   * @return builder for configuring Redis rate limiters
   */
  public static RedisBuilder redis(redis.clients.jedis.JedisPool jedisPool) {
    return new RedisBuilder(() -> jedisPool);
  }

  /**
   * Builder for creating in-memory rate limiters.
   */
  public static class InMemoryBuilder {
    private RateLimitConfig config;

    /**
     * Sets the rate limiting configuration.
     *
     * @param config the configuration
     * @return this builder
     */
    public InMemoryBuilder withConfig(RateLimitConfig config) {
      this.config = config;
      return this;
    }

    /**
     * Builds the in-memory rate limiter.
     *
     * @return configured rate limiter
     * @throws IllegalStateException if configuration is missing
     */
    public RateLimiter build() {
      if (config == null) {
        throw new IllegalStateException("Configuration is required");
      }
      return new InMemoryRateLimiter(config);
    }
  }

  /**
   * Builder for creating Redis-based rate limiters.
   */
  public static class RedisBuilder {
    private final Supplier<redis.clients.jedis.JedisPool> jedisPoolSupplier;
    private RateLimitConfig config;
    private String keyPrefix = "rate_limiter";

    RedisBuilder(Supplier<redis.clients.jedis.JedisPool> jedisPoolSupplier) {
      this.jedisPoolSupplier = jedisPoolSupplier;
    }

    /**
     * Sets the rate limiting configuration.
     *
     * @param config the configuration
     * @return this builder
     */
    public RedisBuilder withConfig(RateLimitConfig config) {
      this.config = config;
      return this;
    }

    /**
     * Sets the Redis key prefix.
     *
     * @param keyPrefix the key prefix for Redis keys
     * @return this builder
     */
    public RedisBuilder withKeyPrefix(String keyPrefix) {
      this.keyPrefix = keyPrefix;
      return this;
    }

    /**
     * Builds the Redis-based rate limiter.
     *
     * @return configured rate limiter
     * @throws IllegalStateException if configuration is missing
     */
    public RateLimiter build() {
      if (config == null) {
        throw new IllegalStateException("Configuration is required");
      }
      return new RedisRateLimiter(jedisPoolSupplier, config, keyPrefix);
    }
  }
}
