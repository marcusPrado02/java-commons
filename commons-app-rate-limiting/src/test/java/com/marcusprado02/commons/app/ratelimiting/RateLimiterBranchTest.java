package com.marcusprado02.commons.app.ratelimiting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.marcusprado02.commons.app.ratelimiting.impl.InMemoryRateLimiter;
import com.marcusprado02.commons.app.ratelimiting.impl.RedisRateLimiter;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.RemoteBucketBuilder;
import io.github.bucket4j.redis.jedis.cas.JedisBasedProxyManager;
import java.time.Duration;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
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

  // ── Helpers ───────────────────────────────────────────────────────────────

  private record Holder(RedisRateLimiter limiter, JedisBasedProxyManager manager) {}

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static Holder buildWithMocks() {
    JedisPool mockPool = mock(JedisPool.class);
    RateLimitConfig config = RateLimitConfig.perSecond(10);
    JedisBasedProxyManager mockManager = mock(JedisBasedProxyManager.class);
    JedisBasedProxyManager.JedisBasedProxyManagerBuilder mockBuilder =
        mock(JedisBasedProxyManager.JedisBasedProxyManagerBuilder.class);
    when(mockBuilder.build()).thenReturn(mockManager);
    try (var mocked = mockStatic(JedisBasedProxyManager.class)) {
      mocked
          .when(() -> JedisBasedProxyManager.builderFor(any(JedisPool.class)))
          .thenReturn(mockBuilder);
      return new Holder(new RedisRateLimiter(mockPool, config), mockManager);
    }
  }

  // ── RedisRateLimiter: validation branches (before Redis call) ────────────

  @Test
  void tryConsume_nullKey_throwsIllegalArgument() {
    assertThatThrownBy(() -> buildWithMocks().limiter().tryConsume(null, 1))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void tryConsume_emptyKey_throwsIllegalArgument() {
    assertThatThrownBy(() -> buildWithMocks().limiter().tryConsume("", 1))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void tryConsume_zeroTokens_throwsIllegalArgument() {
    assertThatThrownBy(() -> buildWithMocks().limiter().tryConsume("key", 0))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void tryConsume_negativeTokens_throwsIllegalArgument() {
    assertThatThrownBy(() -> buildWithMocks().limiter().tryConsume("key", -1))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void probe_nullKey_throwsIllegalArgument() {
    assertThatThrownBy(() -> buildWithMocks().limiter().probe(null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void probe_emptyKey_throwsIllegalArgument() {
    assertThatThrownBy(() -> buildWithMocks().limiter().probe(""))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void reset_nullKey_throwsIllegalArgument() {
    assertThatThrownBy(() -> buildWithMocks().limiter().reset(null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void reset_emptyKey_throwsIllegalArgument() {
    assertThatThrownBy(() -> buildWithMocks().limiter().reset(""))
        .isInstanceOf(IllegalArgumentException.class);
  }

  // ── RedisRateLimiter: getters ─────────────────────────────────────────────

  @Test
  void getConfig_returnsConfig() {
    assertThat(buildWithMocks().limiter().getConfig()).isNotNull();
  }

  @Test
  void getKeyPrefix_defaultPrefix_returnsRateLimiter() {
    assertThat(buildWithMocks().limiter().getKeyPrefix()).isEqualTo("rate_limiter");
  }

  @Test
  void getStats_returnsStats() {
    assertThat(buildWithMocks().limiter().getStats()).isNotNull();
  }

  // ── RedisRateLimiter: operation paths via mocked proxyManager ────────────

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  void tryConsume_allowed_returnsAllowedResult() {
    Holder h = buildWithMocks();

    RemoteBucketBuilder mockBucketBuilder = mock(RemoteBucketBuilder.class);
    BucketProxy mockBucket = mock(BucketProxy.class);
    ConsumptionProbe probe = mock(ConsumptionProbe.class);

    when(h.manager().builder()).thenReturn(mockBucketBuilder);
    when(mockBucketBuilder.build(
            any(byte[].class), ArgumentMatchers.<Supplier<BucketConfiguration>>any()))
        .thenReturn(mockBucket);
    when(mockBucket.tryConsumeAndReturnRemaining(1L)).thenReturn(probe);
    when(probe.isConsumed()).thenReturn(true);
    when(probe.getRemainingTokens()).thenReturn(9L);
    when(probe.getNanosToWaitForRefill()).thenReturn(0L);

    RateLimitResult result = h.limiter().tryConsume("key", 1);
    assertThat(result.isAllowed()).isTrue();
    assertThat(result.remainingTokens()).isEqualTo(9L);
  }

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  void tryConsume_rejected_returnsRejectedResult() {
    Holder h = buildWithMocks();

    RemoteBucketBuilder mockBucketBuilder = mock(RemoteBucketBuilder.class);
    BucketProxy mockBucket = mock(BucketProxy.class);
    ConsumptionProbe probe = mock(ConsumptionProbe.class);

    when(h.manager().builder()).thenReturn(mockBucketBuilder);
    when(mockBucketBuilder.build(
            any(byte[].class), ArgumentMatchers.<Supplier<BucketConfiguration>>any()))
        .thenReturn(mockBucket);
    when(mockBucket.tryConsumeAndReturnRemaining(1L)).thenReturn(probe);
    when(probe.isConsumed()).thenReturn(false);
    when(probe.getRemainingTokens()).thenReturn(0L);
    when(probe.getNanosToWaitForRefill()).thenReturn(1_000_000_000L);

    RateLimitResult result = h.limiter().tryConsume("key", 1);
    assertThat(result.isAllowed()).isFalse();
  }

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  void tryConsume_redisException_throwsRuntimeException() {
    Holder h = buildWithMocks();

    RemoteBucketBuilder mockBucketBuilder = mock(RemoteBucketBuilder.class);
    when(h.manager().builder()).thenReturn(mockBucketBuilder);
    when(mockBucketBuilder.build(
            any(byte[].class), ArgumentMatchers.<Supplier<BucketConfiguration>>any()))
        .thenThrow(new RuntimeException("Redis down"));

    assertThatThrownBy(() -> h.limiter().tryConsume("key", 1))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Redis");
  }

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  void probe_success_returnsResult() {
    Holder h = buildWithMocks();

    RemoteBucketBuilder mockBucketBuilder = mock(RemoteBucketBuilder.class);
    BucketProxy mockBucket = mock(BucketProxy.class);

    when(h.manager().builder()).thenReturn(mockBucketBuilder);
    when(mockBucketBuilder.build(
            any(byte[].class), ArgumentMatchers.<Supplier<BucketConfiguration>>any()))
        .thenReturn(mockBucket);
    when(mockBucket.getAvailableTokens()).thenReturn(8L);

    RateLimitResult result = h.limiter().probe("key");
    assertThat(result.isAllowed()).isTrue();
    assertThat(result.remainingTokens()).isEqualTo(8L);
  }

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  void reset_withPositiveTokens_consumesAll() {
    Holder h = buildWithMocks();

    RemoteBucketBuilder mockBucketBuilder = mock(RemoteBucketBuilder.class);
    BucketProxy mockBucket = mock(BucketProxy.class);

    when(h.manager().builder()).thenReturn(mockBucketBuilder);
    when(mockBucketBuilder.build(
            any(byte[].class), ArgumentMatchers.<Supplier<BucketConfiguration>>any()))
        .thenReturn(mockBucket);
    when(mockBucket.getAvailableTokens()).thenReturn(5L);
    when(mockBucket.tryConsume(5L)).thenReturn(true);

    h.limiter().reset("key");
  }

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  void reset_withZeroTokens_skipsConsume() {
    Holder h = buildWithMocks();

    RemoteBucketBuilder mockBucketBuilder = mock(RemoteBucketBuilder.class);
    BucketProxy mockBucket = mock(BucketProxy.class);

    when(h.manager().builder()).thenReturn(mockBucketBuilder);
    when(mockBucketBuilder.build(
            any(byte[].class), ArgumentMatchers.<Supplier<BucketConfiguration>>any()))
        .thenReturn(mockBucket);
    when(mockBucket.getAvailableTokens()).thenReturn(0L);

    h.limiter().reset("key");
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
