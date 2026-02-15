package com.marcusprado02.commons.app.ratelimiting;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Rate limiting result containing consumption information and remaining capacity.
 *
 * <p>This record provides detailed information about a rate limiting operation, including whether
 * the request was allowed, how many tokens were consumed, and when the bucket will be refilled.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * RateLimitResult result = rateLimiter.tryConsume("user123", 1);
 * if (result.isAllowed()) {
 *     // Process request
 *     log.info("Remaining capacity: {}", result.getRemainingTokens());
 * } else {
 *     // Reject request
 *     Duration waitTime = result.getRetryAfter().orElse(Duration.ofMinutes(1));
 *     throw new RateLimitExceededException("Try again in " + waitTime);
 * }
 * }</pre>
 *
 * @param isAllowed whether the request was allowed (tokens were consumed)
 * @param consumedTokens number of tokens that were consumed (0 if not allowed)
 * @param remainingTokens number of tokens remaining in the bucket
 * @param totalCapacity total capacity of the bucket
 * @param refillTimeNanos when the bucket will be refilled (nanoseconds since epoch)
 */
public record RateLimitResult(
    boolean isAllowed,
    long consumedTokens,
    long remainingTokens,
    long totalCapacity,
    long refillTimeNanos) {

  /**
   * Creates a successful rate limit result.
   *
   * @param consumedTokens tokens consumed
   * @param remainingTokens tokens remaining
   * @param totalCapacity total bucket capacity
   * @param refillTimeNanos refill time in nanoseconds
   * @return successful result
   */
  public static RateLimitResult allowed(
      long consumedTokens, long remainingTokens, long totalCapacity, long refillTimeNanos) {
    return new RateLimitResult(true, consumedTokens, remainingTokens, totalCapacity, refillTimeNanos);
  }

  /**
   * Creates a rejected rate limit result.
   *
   * @param remainingTokens tokens remaining (usually 0)
   * @param totalCapacity total bucket capacity
   * @param refillTimeNanos when bucket will be refilled
   * @return rejected result
   */
  public static RateLimitResult rejected(
      long remainingTokens, long totalCapacity, long refillTimeNanos) {
    return new RateLimitResult(false, 0, remainingTokens, totalCapacity, refillTimeNanos);
  }

  /**
   * Gets the rate limit utilization as a percentage.
   *
   * @return utilization percentage (0.0 = empty, 1.0 = full)
   */
  public double getUtilization() {
    if (totalCapacity == 0) {
      return 0.0;
    }
    return (double) (totalCapacity - remainingTokens) / totalCapacity;
  }

  /**
   * Gets the time until the bucket will be refilled.
   *
   * @return duration until refill, or empty if already refilled
   */
  public Optional<Duration> getRetryAfter() {
    long currentNanos = System.nanoTime();
    if (refillTimeNanos <= currentNanos) {
      return Optional.empty();
    }
    return Optional.of(Duration.ofNanos(refillTimeNanos - currentNanos));
  }

  /**
   * Gets the refill time as an Instant.
   *
   * @return when the bucket will be refilled
   */
  public Instant getRefillTime() {
    // Convert from nanoTime to Instant (approximate)
    long currentMillis = System.currentTimeMillis();
    long currentNanos = System.nanoTime();
    long offsetNanos = refillTimeNanos - currentNanos;
    return Instant.ofEpochMilli(currentMillis).plusNanos(offsetNanos);
  }

  /**
   * Returns whether the bucket is completely empty.
   *
   * @return true if no tokens remaining
   */
  public boolean isEmpty() {
    return remainingTokens == 0;
  }

  /**
   * Returns whether the bucket is completely full.
   *
   * @return true if bucket is at full capacity
   */
  public boolean isFull() {
    return remainingTokens == totalCapacity;
  }
}