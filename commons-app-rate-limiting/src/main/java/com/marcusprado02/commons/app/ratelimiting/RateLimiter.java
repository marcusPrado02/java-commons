package com.marcusprado02.commons.app.ratelimiting;

import java.util.concurrent.CompletableFuture;

/**
 * Rate limiter interface for controlling request rates using token bucket algorithm.
 *
 * <p>This interface provides methods for consuming tokens from rate limiting buckets identified by
 * keys. It supports both synchronous and asynchronous operations.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li><strong>Token Consumption</strong>: Try to consume tokens without blocking
 *   <li><strong>Bucket Inspection</strong>: Get remaining capacity without consuming
 *   <li><strong>Async Support</strong>: Non-blocking operations for reactive applications
 *   <li><strong>Multiple Keys</strong>: Different rate limits per key (user, IP, API key, etc.)
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Basic usage
 * RateLimiter rateLimiter = new InMemoryRateLimiter(
 *     RateLimitConfig.perMinute(100));
 *
 * RateLimitResult result = rateLimiter.tryConsume("user123");
 * if (result.isAllowed()) {
 *     // Process request
 * } else {
 *     // Reject with 429 Too Many Requests
 * }
 *
 * // Multiple tokens
 * RateLimitResult result = rateLimiter.tryConsume("api-key-456", 5);
 *
 * // Check capacity without consuming
 * RateLimitResult result = rateLimiter.probe("user789");
 * }</pre>
 */
public interface RateLimiter {

  /**
   * Attempts to consume a single token from the bucket identified by the key.
   *
   * <p>This method tries to consume one token from the rate limiting bucket. If successful, the
   * request is allowed and the token is consumed. If the bucket is empty, the request is rejected
   * and no tokens are consumed.
   *
   * @param key unique identifier for the rate limiting bucket (e.g., user ID, IP address)
   * @return rate limit result with consumption details
   */
  default RateLimitResult tryConsume(String key) {
    return tryConsume(key, 1);
  }

  /**
   * Attempts to consume the specified number of tokens from the bucket identified by the key.
   *
   * <p>This method tries to consume the requested number of tokens from the rate limiting bucket.
   * All tokens must be available for the operation to succeed. If insufficient tokens are
   * available, no tokens are consumed and the request is rejected.
   *
   * @param key unique identifier for the rate limiting bucket
   * @param tokens number of tokens to consume (must be > 0)
   * @return rate limit result with consumption details
   * @throws IllegalArgumentException if tokens <= 0
   */
  RateLimitResult tryConsume(String key, long tokens);

  /**
   * Probes the bucket to get current capacity without consuming any tokens.
   *
   * <p>This method allows checking the current state of a rate limiting bucket without affecting
   * it. Useful for providing rate limit information to clients or for monitoring purposes.
   *
   * @param key unique identifier for the rate limiting bucket
   * @return rate limit result with current capacity information
   */
  RateLimitResult probe(String key);

  /**
   * Asynchronously attempts to consume a single token from the bucket.
   *
   * <p>This method provides non-blocking rate limiting for reactive applications. The returned
   * CompletableFuture will complete with the rate limit result.
   *
   * @param key unique identifier for the rate limiting bucket
   * @return future that completes with rate limit result
   */
  default CompletableFuture<RateLimitResult> tryConsumeAsync(String key) {
    return tryConsumeAsync(key, 1);
  }

  /**
   * Asynchronously attempts to consume the specified number of tokens from the bucket.
   *
   * @param key unique identifier for the rate limiting bucket
   * @param tokens number of tokens to consume
   * @return future that completes with rate limit result
   */
  default CompletableFuture<RateLimitResult> tryConsumeAsync(String key, long tokens) {
    return CompletableFuture.supplyAsync(() -> tryConsume(key, tokens));
  }

  /**
   * Asynchronously probes the bucket to get current capacity.
   *
   * @param key unique identifier for the rate limiting bucket
   * @return future that completes with rate limit result
   */
  default CompletableFuture<RateLimitResult> probeAsync(String key) {
    return CompletableFuture.supplyAsync(() -> probe(key));
  }

  /**
   * Gets the configuration used by this rate limiter.
   *
   * @return rate limit configuration
   */
  RateLimitConfig getConfig();

  /**
   * Resets the bucket for the specified key.
   *
   * <p>This method clears the rate limiting state for the given key, effectively giving the key a
   * fresh start with a full bucket. Use with caution as this bypasses rate limiting protections.
   *
   * @param key unique identifier for the rate limiting bucket to reset
   */
  default void reset(String key) {
    // Default implementation does nothing
    // Implementations should override if they support reset
  }

  /**
   * Gets statistics about rate limiter usage.
   *
   * <p>This method returns usage statistics if supported by the implementation. Default
   * implementation returns empty statistics.
   *
   * @return rate limiter statistics
   */
  default RateLimiterStats getStats() {
    return RateLimiterStats.empty();
  }
}