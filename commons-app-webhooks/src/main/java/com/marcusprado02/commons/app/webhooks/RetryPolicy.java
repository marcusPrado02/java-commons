package com.marcusprado02.commons.app.webhooks;

import java.time.Duration;

/**
 * Policy for retrying failed webhook deliveries.
 *
 * <p>Defines how many times to retry and the backoff strategy between attempts.
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * // Exponential backoff: 1s, 2s, 4s, 8s, 16s
 * RetryPolicy policy = RetryPolicy.exponentialBackoff(5, Duration.ofSeconds(1));
 *
 * // Fixed delay: 30s between each retry
 * RetryPolicy policy = RetryPolicy.fixedDelay(3, Duration.ofSeconds(30));
 * }</pre>
 */
public interface RetryPolicy {

  /**
   * Gets the maximum number of retry attempts.
   *
   * @return max retries
   */
  int getMaxRetries();

  /**
   * Calculates the delay before the next retry attempt.
   *
   * @param attemptNumber the attempt number (1-based)
   * @return the delay duration
   */
  Duration getRetryDelay(int attemptNumber);

  /**
   * Creates a policy with exponential backoff.
   *
   * @param maxRetries maximum number of retries
   * @param initialDelay initial delay before first retry
   * @return the retry policy
   */
  static RetryPolicy exponentialBackoff(int maxRetries, Duration initialDelay) {
    return new ExponentialBackoffRetryPolicy(maxRetries, initialDelay);
  }

  /**
   * Creates a policy with fixed delay between retries.
   *
   * @param maxRetries maximum number of retries
   * @param delay fixed delay between retries
   * @return the retry policy
   */
  static RetryPolicy fixedDelay(int maxRetries, Duration delay) {
    return new FixedDelayRetryPolicy(maxRetries, delay);
  }

  /**
   * Creates a no-retry policy.
   *
   * @return the retry policy
   */
  static RetryPolicy noRetry() {
    return new NoRetryPolicy();
  }
}
