package com.marcusprado02.commons.app.ratelimiting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.marcusprado02.commons.app.ratelimiting.impl.InMemoryRateLimiter;
import com.marcusprado02.commons.app.ratelimiting.impl.RedisRateLimiter;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.JedisPool;

class RateLimiterBranchTest {

  // ── RateLimitConfig: equals() branches ───────────────────────────────────

  @Test
  void equals_sameObject_returnsTrue() {
    RateLimitConfig cfg = RateLimitConfig.perSecond(10);
    assertThat(cfg.equals(cfg)).isTrue();
  }

  @Test
  void equals_null_returnsFalse() {
    RateLimitConfig cfg = RateLimitConfig.perSecond(10);
    assertThat(cfg.equals(null)).isFalse();
  }

  @Test
  void equals_differentClass_returnsFalse() {
    RateLimitConfig cfg = RateLimitConfig.perSecond(10);
    assertThat(cfg.equals("notAConfig")).isFalse();
  }

  @Test
  void equals_sameValues_returnsTrue() {
    RateLimitConfig a = RateLimitConfig.perSecond(10);
    RateLimitConfig b = RateLimitConfig.perSecond(10);
    assertThat(a.equals(b)).isTrue();
    assertThat(a.hashCode()).isEqualTo(b.hashCode());
  }

  @Test
  void equals_differentValues_returnsFalse() {
    RateLimitConfig a = RateLimitConfig.perSecond(10);
    RateLimitConfig b = RateLimitConfig.perSecond(20);
    assertThat(a.equals(b)).isFalse();
  }

  // ── RateLimitConfig static factories ─────────────────────────────────────

  @Test
  void perHour_buildsConfig() {
    RateLimitConfig cfg = RateLimitConfig.perHour(3600);
    assertThat(cfg.getCapacity()).isEqualTo(3600);
    assertThat(cfg.getRefillPeriod()).isEqualTo(Duration.ofHours(1));
  }

  @Test
  void withBurst_alternativeOrder_sameResult() {
    RateLimitConfig a = RateLimitConfig.withBurst(100, Duration.ofSeconds(1), 200);
    RateLimitConfig b = RateLimitConfig.withBurst(100, 200, Duration.ofSeconds(1));
    assertThat(a.getCapacity()).isEqualTo(b.getCapacity());
    assertThat(a.getRefillRate()).isEqualTo(b.getRefillRate());
  }

  // ── RateLimiterFactory.RedisBuilder: null config ──────────────────────────

  @Test
  void redisBuilder_nullConfig_throwsIllegalState() {
    JedisPool mockPool = mock(JedisPool.class);
    assertThatThrownBy(() -> RateLimiterFactory.redis(mockPool).build())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Configuration is required");
  }

  // ── RedisRateLimiter: validation branches (before Redis call) ────────────

  private static RedisRateLimiter createRedisRateLimiter() {
    JedisPool mockPool = mock(JedisPool.class);
    RateLimitConfig config = RateLimitConfig.perSecond(10);
    return new RedisRateLimiter(mockPool, config);
  }

  @Test
  void tryConsume_nullKey_throwsIllegalArgument() {
    RedisRateLimiter limiter = createRedisRateLimiter();
    assertThatThrownBy(() -> limiter.tryConsume(null, 1))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void tryConsume_emptyKey_throwsIllegalArgument() {
    RedisRateLimiter limiter = createRedisRateLimiter();
    assertThatThrownBy(() -> limiter.tryConsume("", 1))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void tryConsume_zeroTokens_throwsIllegalArgument() {
    RedisRateLimiter limiter = createRedisRateLimiter();
    assertThatThrownBy(() -> limiter.tryConsume("key", 0))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void tryConsume_negativeTokens_throwsIllegalArgument() {
    RedisRateLimiter limiter = createRedisRateLimiter();
    assertThatThrownBy(() -> limiter.tryConsume("key", -1))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void probe_nullKey_throwsIllegalArgument() {
    RedisRateLimiter limiter = createRedisRateLimiter();
    assertThatThrownBy(() -> limiter.probe(null)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void probe_emptyKey_throwsIllegalArgument() {
    RedisRateLimiter limiter = createRedisRateLimiter();
    assertThatThrownBy(() -> limiter.probe("")).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void reset_nullKey_throwsIllegalArgument() {
    RedisRateLimiter limiter = createRedisRateLimiter();
    assertThatThrownBy(() -> limiter.reset(null)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void reset_emptyKey_throwsIllegalArgument() {
    RedisRateLimiter limiter = createRedisRateLimiter();
    assertThatThrownBy(() -> limiter.reset("")).isInstanceOf(IllegalArgumentException.class);
  }

  // ── RedisRateLimiter: getters ─────────────────────────────────────────────

  @Test
  void getConfig_returnsConfig() {
    JedisPool mockPool = mock(JedisPool.class);
    RateLimitConfig config = RateLimitConfig.perMinute(60);
    RedisRateLimiter limiter = new RedisRateLimiter(mockPool, config);
    assertThat(limiter.getConfig()).isSameAs(config);
  }

  @Test
  void getKeyPrefix_defaultPrefix_returnsRateLimiter() {
    JedisPool mockPool = mock(JedisPool.class);
    RedisRateLimiter limiter = new RedisRateLimiter(mockPool, RateLimitConfig.perSecond(5));
    assertThat(limiter.getKeyPrefix()).isEqualTo("rate_limiter");
  }

  @Test
  void getStats_returnsStats() {
    JedisPool mockPool = mock(JedisPool.class);
    RedisRateLimiter limiter = new RedisRateLimiter(mockPool, RateLimitConfig.perSecond(5));
    RateLimiterStats stats = limiter.getStats();
    assertThat(stats).isNotNull();
  }

  // ── InMemoryRateLimiter: tryConsume validation ────────────────────────────

  @Test
  void inMemory_tryConsume_nullKey_throwsIllegalArgument() {
    InMemoryRateLimiter limiter = new InMemoryRateLimiter(RateLimitConfig.perSecond(10));
    assertThatThrownBy(() -> limiter.tryConsume(null, 1))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void inMemory_tryConsume_emptyKey_throwsIllegalArgument() {
    InMemoryRateLimiter limiter = new InMemoryRateLimiter(RateLimitConfig.perSecond(10));
    assertThatThrownBy(() -> limiter.tryConsume("", 1))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void inMemory_tryConsume_zeroTokens_throwsIllegalArgument() {
    InMemoryRateLimiter limiter = new InMemoryRateLimiter(RateLimitConfig.perSecond(10));
    assertThatThrownBy(() -> limiter.tryConsume("k", 0))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
