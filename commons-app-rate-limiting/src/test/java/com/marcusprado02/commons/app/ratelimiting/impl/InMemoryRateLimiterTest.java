package com.marcusprado02.commons.app.ratelimiting.impl;

import com.marcusprado02.commons.app.ratelimiting.RateLimitConfig;
import com.marcusprado02.commons.app.ratelimiting.RateLimitResult;
import com.marcusprado02.commons.app.ratelimiting.RateLimiterStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryRateLimiterTest {

  private InMemoryRateLimiter rateLimiter;
  private RateLimitConfig config;

  @BeforeEach
  void setUp() {
    config = RateLimitConfig.builder()
        .capacity(10)
        .refillRate(5)
        .refillPeriod(Duration.ofSeconds(1))
        .build();
    rateLimiter = new InMemoryRateLimiter(config);
  }

  @Test
  void testBasicRateLimiting() {
    String key = "test-key";

    // Should allow initial requests up to capacity
    for (int i = 0; i < 10; i++) {
      RateLimitResult result = rateLimiter.tryConsume(key);
      assertTrue(result.isAllowed(), "Request " + (i + 1) + " should be allowed");
      assertEquals(9 - i, result.getRemainingTokens(), "Remaining tokens should decrease");
    }

    // Next request should be rejected
    RateLimitResult result = rateLimiter.tryConsume(key);
    assertFalse(result.isAllowed(), "Request should be rejected when limit exceeded");
    assertEquals(0, result.getRemainingTokens(), "No tokens should remain");
  }

  @Test
  void testMultipleTokenConsumption() {
    String key = "multi-token-key";

    // Consume multiple tokens at once
    RateLimitResult result = rateLimiter.tryConsume(key, 5);
    assertTrue(result.isAllowed(), "Multi-token consumption should be allowed");
    assertEquals(5, result.getConsumedTokens(), "Should consume 5 tokens");
    assertEquals(5, result.getRemainingTokens(), "Should have 5 tokens remaining");

    // Try to consume more than available
    result = rateLimiter.tryConsume(key, 10);
    assertFalse(result.isAllowed(), "Should reject when trying to consume more than available");
  }

  @Test
  void testProbe() {
    String key = "probe-key";

    // Probe should not consume tokens
    RateLimitResult probeResult = rateLimiter.probe(key);
    assertTrue(probeResult.isAllowed(), "Probe should be allowed");
    assertEquals(0, probeResult.getConsumedTokens(), "Probe should not consume tokens");
    assertEquals(10, probeResult.getRemainingTokens(), "All tokens should remain");

    // Consume some tokens
    rateLimiter.tryConsume(key, 3);

    // Probe again
    probeResult = rateLimiter.probe(key);
    assertEquals(7, probeResult.getRemainingTokens(), "Probe should show remaining tokens");
  }

  @Test
  void testReset() {
    String key = "reset-key";

    // Consume all tokens
    rateLimiter.tryConsume(key, 10);
    RateLimitResult result = rateLimiter.probe(key);
    assertEquals(0, result.getRemainingTokens(), "No tokens should remain");

    // Reset the bucket
    rateLimiter.reset(key);

    // Should be able to consume again
    result = rateLimiter.tryConsume(key);
    assertTrue(result.isAllowed(), "Should be allowed after reset");
  }

  @Test
  void testDifferentKeys() {
    String key1 = "key1";
    String key2 = "key2";

    // Consume all tokens for key1
    for (int i = 0; i < 10; i++) {
      rateLimiter.tryConsume(key1);
    }

    // key1 should be rate limited
    RateLimitResult result1 = rateLimiter.tryConsume(key1);
    assertFalse(result1.isAllowed(), "key1 should be rate limited");

    // key2 should still be available
    RateLimitResult result2 = rateLimiter.tryConsume(key2);
    assertTrue(result2.isAllowed(), "key2 should not be affected");
  }

  @Test
  void testConcurrency() throws InterruptedException {
    String key = "concurrent-key";
    int threadCount = 20;
    int requestsPerThread = 5;

    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch latch = new CountDownLatch(threadCount);
    AtomicInteger allowedCount = new AtomicInteger(0);
    AtomicInteger rejectedCount = new AtomicInteger(0);

    for (int i = 0; i < threadCount; i++) {
      executor.submit(() -> {
        try {
          for (int j = 0; j < requestsPerThread; j++) {
            RateLimitResult result = rateLimiter.tryConsume(key);
            if (result.isAllowed()) {
              allowedCount.incrementAndGet();
            } else {
              rejectedCount.incrementAndGet();
            }
          }
        } finally {
          latch.countDown();
        }
      });
    }

    latch.await();
    executor.shutdown();

    int totalRequests = threadCount * requestsPerThread;
    assertEquals(totalRequests, allowedCount.get() + rejectedCount.get(),
        "All requests should be processed");

    // Should allow up to capacity (10), reject the rest
    assertTrue(allowedCount.get() <= 10, "Should not allow more than capacity");
    assertEquals(totalRequests - allowedCount.get(), rejectedCount.get(),
        "Remaining should be rejected");
  }

  @Test
  void testAsyncOperations() {
    String key = "async-key";

    // Test async tryConsume
    CompletableFuture<RateLimitResult> future = rateLimiter.tryConsumeAsync(key);
    RateLimitResult result = future.join();

    assertTrue(result.isAllowed(), "Async consume should be allowed");
    assertEquals(1, result.getConsumedTokens(), "Should consume 1 token");
    assertEquals(9, result.getRemainingTokens(), "Should have 9 tokens remaining");

    // Test async probe
    CompletableFuture<RateLimitResult> probeFuture = rateLimiter.probeAsync(key);
    RateLimitResult probeResult = probeFuture.join();

    assertEquals(0, probeResult.getConsumedTokens(), "Probe should not consume tokens");
    assertEquals(9, probeResult.getRemainingTokens(), "Should show current remaining tokens");
  }

  @Test
  void testStatistics() {
    String key = "stats-key";

    // Perform some operations
    rateLimiter.tryConsume(key); // allowed
    rateLimiter.tryConsume(key); // allowed

    // Consume all remaining tokens
    rateLimiter.tryConsume(key, 8); // allowed
    rateLimiter.tryConsume(key); // rejected
    rateLimiter.tryConsume(key); // rejected

    RateLimiterStats stats = rateLimiter.getStats();

    assertTrue(stats.getAllowedRequests() >= 3, "Should have at least 3 allowed requests");
    assertTrue(stats.getRejectedRequests() >= 2, "Should have at least 2 rejected requests");
    assertTrue(stats.getTotalRequests() >= 5, "Should have at least 5 total requests");
    assertTrue(stats.getAllowRate() > 0, "Allow rate should be positive");
    assertTrue(stats.getRejectRate() > 0, "Reject rate should be positive");
  }

  @Test
  void testConfiguration() {
    assertEquals(config, rateLimiter.getConfig(), "Should return the same configuration");
  }

  @Test
  void testBucketCount() {
    assertEquals(0, rateLimiter.getBucketCount(), "Should start with no buckets");

    rateLimiter.tryConsume("key1");
    assertEquals(1, rateLimiter.getBucketCount(), "Should have 1 bucket after first key");

    rateLimiter.tryConsume("key2");
    assertEquals(2, rateLimiter.getBucketCount(), "Should have 2 buckets after second key");
  }

  @Test
  void testClearAll() {
    rateLimiter.tryConsume("key1");
    rateLimiter.tryConsume("key2");
    assertEquals(2, rateLimiter.getBucketCount(), "Should have 2 buckets");

    rateLimiter.clearAll();
    assertEquals(0, rateLimiter.getBucketCount(), "Should have no buckets after clear");
  }

  @Test
  void testInvalidInput() {
    assertThrows(IllegalArgumentException.class, () ->
        rateLimiter.tryConsume(null), "Should reject null key");

    assertThrows(IllegalArgumentException.class, () ->
        rateLimiter.tryConsume(""), "Should reject empty key");

    assertThrows(IllegalArgumentException.class, () ->
        rateLimiter.tryConsume("key", 0), "Should reject zero tokens");

    assertThrows(IllegalArgumentException.class, () ->
        rateLimiter.tryConsume("key", -1), "Should reject negative tokens");
  }
}
