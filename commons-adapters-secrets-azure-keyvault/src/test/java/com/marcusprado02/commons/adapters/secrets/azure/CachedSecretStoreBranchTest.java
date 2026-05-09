package com.marcusprado02.commons.adapters.secrets.azure;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marcusprado02.commons.ports.secrets.SecretKey;
import com.marcusprado02.commons.ports.secrets.SecretStorePort;
import com.marcusprado02.commons.ports.secrets.SecretValue;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CachedSecretStoreBranchTest {

  private static final SecretKey KEY = SecretKey.of("my-secret");
  private static final SecretValue VALUE = SecretValue.of("secret-data", "v1");

  // ── Constructor: refreshInterval non-null → starts scheduler ─────────────

  @Test
  void constructor_withRefreshInterval_startsScheduler() throws Exception {
    SecretStorePort delegate = mock(SecretStorePort.class);
    when(delegate.get(any(SecretKey.class))).thenReturn(Optional.of(VALUE));

    try (CachedSecretStorePort cache =
        new CachedSecretStorePort(
            delegate, Duration.ofMinutes(5), Clock.systemUTC(), Duration.ofSeconds(60))) {
      // Just verify construction doesn't throw
      assertNotNull(cache);
    }
  }

  @Test
  void constructor_withNullRefreshInterval_noScheduler() {
    SecretStorePort delegate = mock(SecretStorePort.class);
    CachedSecretStorePort cache =
        new CachedSecretStorePort(delegate, Duration.ofMinutes(5), Clock.systemUTC(), null);
    cache.close();
  }

  @Test
  void constructor_withZeroRefreshInterval_noScheduler() {
    SecretStorePort delegate = mock(SecretStorePort.class);
    CachedSecretStorePort cache =
        new CachedSecretStorePort(
            delegate, Duration.ofMinutes(5), Clock.systemUTC(), Duration.ZERO);
    cache.close();
  }

  @Test
  void constructor_withNegativeRefreshInterval_noScheduler() {
    SecretStorePort delegate = mock(SecretStorePort.class);
    CachedSecretStorePort cache =
        new CachedSecretStorePort(
            delegate, Duration.ofMinutes(5), Clock.systemUTC(), Duration.ofSeconds(-1));
    cache.close();
  }

  // ── get(): cache hit (valid entry) ────────────────────────────────────────

  @Test
  void get_cacheHit_returnsCachedValue() {
    SecretStorePort delegate = mock(SecretStorePort.class);
    when(delegate.get(KEY)).thenReturn(Optional.of(VALUE));

    CachedSecretStorePort cache =
        new CachedSecretStorePort(delegate, Duration.ofMinutes(5), Clock.systemUTC(), null);

    cache.get(KEY); // Load into cache
    cache.get(KEY); // Should hit cache

    verify(delegate, times(1)).get(KEY); // Only loaded once
    cache.close();
  }

  // ── get(): cache miss → load from delegate ────────────────────────────────

  @Test
  void get_cacheMiss_loadsFromDelegate() {
    SecretStorePort delegate = mock(SecretStorePort.class);
    when(delegate.get(KEY)).thenReturn(Optional.empty());

    CachedSecretStorePort cache =
        new CachedSecretStorePort(delegate, Duration.ofMinutes(5), Clock.systemUTC(), null);

    Optional<SecretValue> result = cache.get(KEY);
    assertTrue(result.isEmpty());
    cache.close();
  }

  // ── get(): TTL expiry → reload from delegate ──────────────────────────────

  @Test
  void get_ttlExpired_reloadsFromDelegate() {
    Instant startTime = Instant.parse("2024-01-01T00:00:00Z");
    Instant afterTtl = startTime.plusSeconds(10); // 10s later = after 5s TTL

    // Clock that returns startTime first, then afterTtl
    Clock[] clocks = {
      Clock.fixed(startTime, ZoneOffset.UTC), Clock.fixed(afterTtl, ZoneOffset.UTC)
    };
    int[] callCount = {0};
    Clock tickingClock =
        new Clock() {
          @Override
          public ZoneOffset getZone() {
            return ZoneOffset.UTC;
          }

          @Override
          public Clock withZone(java.time.ZoneId zone) {
            return this;
          }

          @Override
          public Instant instant() {
            return clocks[Math.min(callCount[0]++, 1)].instant();
          }
        };

    SecretStorePort delegate = mock(SecretStorePort.class);
    when(delegate.get(KEY)).thenReturn(Optional.of(VALUE));

    CachedSecretStorePort cache =
        new CachedSecretStorePort(delegate, Duration.ofSeconds(5), tickingClock, null);

    cache.get(KEY); // Load at startTime
    cache.get(KEY); // Re-fetch at afterTtl (expired)

    verify(delegate, times(2)).get(KEY);
    cache.close();
  }

  // ── get(): secret with expiresAt already past ─────────────────────────────

  @Test
  void get_secretWithPastExpiresAt_reloadsFromDelegate() {
    Instant past = Instant.parse("2023-01-01T00:00:00Z");
    SecretValue expiredSecret =
        SecretValue.of("data".getBytes(), "v1", Instant.parse("2023-01-01T00:00:00Z"), past);

    SecretStorePort delegate = mock(SecretStorePort.class);
    when(delegate.get(KEY)).thenReturn(Optional.of(expiredSecret));

    Clock fixedClock = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC);
    CachedSecretStorePort cache =
        new CachedSecretStorePort(delegate, Duration.ofMinutes(60), fixedClock, null);

    cache.get(KEY); // Load into cache (expiresAt is in past)
    cache.get(KEY); // CacheEntry.isValid returns false because expiresAt passed → reload

    verify(delegate, times(2)).get(KEY);
    cache.close();
  }

  // ── get(): SecretValue with null createdAt ────────────────────────────────

  @Test
  void get_secretWithNullCreatedAt_normalizedToNow() {
    SecretValue noCreatedAt = SecretValue.of("bytes".getBytes(), null, null, null);

    SecretStorePort delegate = mock(SecretStorePort.class);
    when(delegate.get(KEY)).thenReturn(Optional.of(noCreatedAt));

    CachedSecretStorePort cache =
        new CachedSecretStorePort(delegate, Duration.ofMinutes(5), Clock.systemUTC(), null);

    Optional<SecretValue> result = cache.get(KEY);
    assertTrue(result.isPresent());
    cache.close();
  }

  // ── exists(): entry valid in cache ────────────────────────────────────────

  @Test
  void exists_entryInCache_returnsTrue() {
    SecretStorePort delegate = mock(SecretStorePort.class);
    when(delegate.get(KEY)).thenReturn(Optional.of(VALUE));
    when(delegate.exists(KEY)).thenReturn(true);

    CachedSecretStorePort cache =
        new CachedSecretStorePort(delegate, Duration.ofMinutes(5), Clock.systemUTC(), null);

    cache.get(KEY); // Put into cache
    boolean exists = cache.exists(KEY); // Should hit cache

    assertTrue(exists);
    verify(delegate, times(0)).exists(KEY); // Should NOT call delegate.exists
    cache.close();
  }

  @Test
  void exists_notInCache_delegatesToDelegate() {
    SecretStorePort delegate = mock(SecretStorePort.class);
    when(delegate.exists(KEY)).thenReturn(false);

    CachedSecretStorePort cache =
        new CachedSecretStorePort(delegate, Duration.ofMinutes(5), Clock.systemUTC(), null);

    boolean exists = cache.exists(KEY);
    assertFalse(exists);
    verify(delegate, times(1)).exists(KEY);
    cache.close();
  }

  // ── put(key, map) ─────────────────────────────────────────────────────────

  @Test
  void put_map_invalidatesCache() {
    SecretStorePort delegate = mock(SecretStorePort.class);
    when(delegate.get(KEY)).thenReturn(Optional.of(VALUE));
    when(delegate.put(any(), any(java.util.Map.class))).thenReturn("v2");

    CachedSecretStorePort cache =
        new CachedSecretStorePort(delegate, Duration.ofMinutes(5), Clock.systemUTC(), null);

    cache.get(KEY);
    cache.put(KEY, java.util.Map.of("k", "v"));
    cache.get(KEY); // Should reload from delegate

    verify(delegate, times(2)).get(KEY);
    cache.close();
  }

  // ── delete() ─────────────────────────────────────────────────────────────

  @Test
  void delete_invalidatesCache() {
    SecretStorePort delegate = mock(SecretStorePort.class);
    when(delegate.get(KEY)).thenReturn(Optional.of(VALUE));
    when(delegate.delete(KEY)).thenReturn(true);

    CachedSecretStorePort cache =
        new CachedSecretStorePort(delegate, Duration.ofMinutes(5), Clock.systemUTC(), null);

    cache.get(KEY);
    boolean deleted = cache.delete(KEY);

    assertTrue(deleted);
    cache.close();
  }

  // ── list() and get(key, version) ─────────────────────────────────────────

  @Test
  void list_delegatesToDelegate() {
    SecretStorePort delegate = mock(SecretStorePort.class);
    when(delegate.list("prefix")).thenReturn(List.of(KEY));

    CachedSecretStorePort cache =
        new CachedSecretStorePort(delegate, Duration.ofMinutes(5), Clock.systemUTC(), null);

    List<SecretKey> keys = cache.list("prefix");
    assertTrue(keys.contains(KEY));
    cache.close();
  }

  @Test
  void getVersioned_bypassesCache() {
    SecretStorePort delegate = mock(SecretStorePort.class);
    when(delegate.get(KEY, "v1")).thenReturn(Optional.of(VALUE));

    CachedSecretStorePort cache =
        new CachedSecretStorePort(delegate, Duration.ofMinutes(5), Clock.systemUTC(), null);

    Optional<SecretValue> result = cache.get(KEY, "v1");
    assertTrue(result.isPresent());
    verify(delegate).get(KEY, "v1");
    cache.close();
  }
}
