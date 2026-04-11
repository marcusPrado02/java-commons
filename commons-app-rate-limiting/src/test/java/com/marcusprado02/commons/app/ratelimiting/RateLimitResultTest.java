package com.marcusprado02.commons.app.ratelimiting;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class RateLimitResultTest {

  @Test
  void shouldCreateAllowedResult() {
    var result = RateLimitResult.allowed(1, 9, 10, 0L);

    assertThat(result.isAllowed()).isTrue();
    assertThat(result.consumedTokens()).isEqualTo(1);
    assertThat(result.remainingTokens()).isEqualTo(9);
    assertThat(result.totalCapacity()).isEqualTo(10);
  }

  @Test
  void shouldCreateRejectedResult() {
    var result = RateLimitResult.rejected(0, 10, 0L);

    assertThat(result.isAllowed()).isFalse();
    assertThat(result.consumedTokens()).isEqualTo(0);
    assertThat(result.remainingTokens()).isEqualTo(0);
    assertThat(result.totalCapacity()).isEqualTo(10);
  }

  @Test
  void shouldCalculateUtilizationWhenPartiallyConsumed() {
    var result = RateLimitResult.allowed(5, 5, 10, 0L);

    assertThat(result.getUtilization()).isEqualTo(0.5);
  }

  @Test
  void shouldReturnZeroUtilizationWhenCapacityIsZero() {
    var result = new RateLimitResult(true, 0, 0, 0, 0L);

    assertThat(result.getUtilization()).isEqualTo(0.0);
  }

  @Test
  void shouldReturnEmptyRetryAfterWhenRefillTimeIsInPast() {
    var result = new RateLimitResult(false, 0, 0, 10, System.nanoTime() - 1_000_000_000L);

    assertThat(result.getRetryAfter()).isEmpty();
  }

  @Test
  void shouldReturnRetryAfterWhenRefillTimeIsInFuture() {
    var result = new RateLimitResult(false, 0, 0, 10, System.nanoTime() + 5_000_000_000L);

    assertThat(result.getRetryAfter()).isPresent();
  }

  @Test
  void shouldReturnRefillTime() {
    var result = new RateLimitResult(false, 0, 0, 10, System.nanoTime() + 1_000_000_000L);

    assertThat(result.getRefillTime()).isNotNull();
  }

  @Test
  void shouldReturnTrueForIsEmpty() {
    var result = RateLimitResult.rejected(0, 10, 0L);

    assertThat(result.isEmpty()).isTrue();
    assertThat(result.isFull()).isFalse();
  }

  @Test
  void shouldReturnTrueForIsFull() {
    var result = RateLimitResult.allowed(0, 10, 10, 0L);

    assertThat(result.isFull()).isTrue();
    assertThat(result.isEmpty()).isFalse();
  }
}
