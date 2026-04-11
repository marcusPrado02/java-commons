package com.marcusprado02.commons.app.ratelimiting;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class RateLimiterStatsTest {

  @Test
  void shouldCreateEmptyStats() {
    var stats = RateLimiterStats.empty();

    assertThat(stats.totalRequests()).isEqualTo(0);
    assertThat(stats.allowedRequests()).isEqualTo(0);
    assertThat(stats.rejectedRequests()).isEqualTo(0);
    assertThat(stats.activeBuckets()).isEqualTo(0);
    assertThat(stats.getRejectionRate()).isEqualTo(0.0);
    assertThat(stats.getAllowRate()).isEqualTo(1.0);
    assertThat(stats.hasActivity()).isFalse();
  }

  @Test
  void shouldBuildStatsWithRecordedRequests() {
    var builder = RateLimiterStats.builder();
    builder.recordAllowed();
    builder.recordAllowed();
    builder.recordRejected();

    var stats = builder.build();

    assertThat(stats.totalRequests()).isEqualTo(3);
    assertThat(stats.allowedRequests()).isEqualTo(2);
    assertThat(stats.rejectedRequests()).isEqualTo(1);
    assertThat(stats.getRejectionRate()).isCloseTo(1.0 / 3.0, within(0.001));
    assertThat(stats.hasActivity()).isTrue();
  }

  @Test
  void shouldTrackActiveBuckets() {
    var builder = RateLimiterStats.builder();
    builder.activeBuckets(5);

    var stats = builder.build();

    assertThat(stats.activeBuckets()).isEqualTo(5);
  }

  @Test
  void shouldUpdateResponseTime() {
    var builder = RateLimiterStats.builder();
    builder.updateResponseTime(10.0);
    builder.updateResponseTime(20.0);

    var stats = builder.build();

    assertThat(stats.averageResponseTimeMs()).isGreaterThan(0.0);
  }

  @Test
  void shouldResetBuilder() {
    var builder = RateLimiterStats.builder();
    builder.recordAllowed();
    builder.recordRejected();
    builder.activeBuckets(3);
    builder.reset();

    var stats = builder.build();

    assertThat(stats.totalRequests()).isEqualTo(0);
    assertThat(stats.activeBuckets()).isEqualTo(0);
  }

  @Test
  void shouldExposeGettersOnBuilder() {
    var builder = RateLimiterStats.builder();
    builder.recordAllowed();
    builder.recordAllowed();
    builder.recordRejected();
    builder.activeBuckets(2);

    assertThat(builder.getTotalRequests()).isEqualTo(3);
    assertThat(builder.getAllowedRequests()).isEqualTo(2);
    assertThat(builder.getRejectedRequests()).isEqualTo(1);
    assertThat(builder.getActiveBuckets()).isEqualTo(2);
  }
}
