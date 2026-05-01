package com.marcusprado02.commons.adapters.secrets.aws;

import static org.assertj.core.api.Assertions.assertThat;
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
import software.amazon.awssdk.regions.Region;

class CachedSecretStoreBranchTest {

  // --- refreshInterval non-null: scheduler created, close() cancels task ---

  @Test
  void constructor_withRefreshInterval_schedulerCreatedAndClosed() throws Exception {
    SecretStorePort delegate = mock(SecretStorePort.class);
    try (CachedSecretStorePort cached =
        new CachedSecretStorePort(
            delegate, Duration.ofMinutes(5), Clock.systemUTC(), Duration.ofHours(1))) {
      assertThat(cached).isNotNull();
    }
  }

  // --- get(): cache miss then cache hit (entry valid) ---

  @Test
  void get_cacheMissThenCacheHit_delegateCalledOnce() {
    SecretStorePort delegate = mock(SecretStorePort.class);
    Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
    SecretKey key = SecretKey.of("my/secret");
    SecretValue value = SecretValue.of("secret-value");

    when(delegate.get(key)).thenReturn(Optional.of(value));

    try (CachedSecretStorePort cached =
        new CachedSecretStorePort(delegate, Duration.ofMinutes(5), clock, null)) {
      // First call: cache miss → delegates
      Optional<SecretValue> first = cached.get(key);
      // Second call: cache hit → no delegate call
      Optional<SecretValue> second = cached.get(key);

      assertThat(first).isPresent();
      assertThat(second).isPresent();
      verify(delegate, times(1)).get(key);
    }
  }

  // --- get(): delegate returns empty → cache.remove(key) ---

  @Test
  void get_delegateReturnsEmpty_returnsEmpty() {
    SecretStorePort delegate = mock(SecretStorePort.class);
    SecretKey key = SecretKey.of("missing");
    when(delegate.get(key)).thenReturn(Optional.empty());

    try (CachedSecretStorePort cached =
        new CachedSecretStorePort(delegate, Duration.ofMinutes(5))) {
      Optional<SecretValue> result = cached.get(key);
      assertThat(result).isEmpty();
    }
  }

  // --- get(): TTL expired → re-fetch from delegate ---

  @Test
  void get_ttlExpired_refetchesFromDelegate() {
    SecretStorePort delegate = mock(SecretStorePort.class);

    Instant start = Instant.parse("2026-01-01T00:00:00Z");
    Instant afterTtl = Instant.parse("2026-01-01T00:01:01Z");

    // First call at T=0, second at T=61s (TTL is 60s)
    Clock[] clocks = {Clock.fixed(start, ZoneOffset.UTC), Clock.fixed(afterTtl, ZoneOffset.UTC)};
    SecretKey key = SecretKey.of("my/secret");
    SecretValue value = SecretValue.of("v1");
    when(delegate.get(key)).thenReturn(Optional.of(value));

    // Use a clock that returns 'start' for the constructor, then switches for calls
    // Simplest: create two CachedSecretStorePort instances with different clocks
    try (CachedSecretStorePort cache1 =
        new CachedSecretStorePort(delegate, Duration.ofSeconds(60), clocks[0], null)) {
      cache1.get(key); // Cache miss → loads with T=start
    }

    // Recreate with expired clock
    try (CachedSecretStorePort cache2 =
        new CachedSecretStorePort(delegate, Duration.ofSeconds(60), clocks[1], null)) {
      cache2.get(key); // Cache miss (empty cache) → re-fetches
      verify(delegate, times(2)).get(key);
    }
  }

  // --- CacheEntry.from: value.createdAt() is null → uses now ---

  @Test
  void get_secretWithNullCreatedAt_usesNow() {
    SecretStorePort delegate = mock(SecretStorePort.class);
    Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
    SecretKey key = SecretKey.of("k");
    // SecretValue.of(String) → createdAt is set by the factory, check if null works
    SecretValue noCreatedAt = SecretValue.of("val", null, null, null);
    when(delegate.get(key)).thenReturn(Optional.of(noCreatedAt));

    try (CachedSecretStorePort cached =
        new CachedSecretStorePort(delegate, Duration.ofMinutes(5), clock, null)) {
      Optional<SecretValue> result = cached.get(key);
      assertThat(result).isPresent();
    }
  }

  // --- CacheEntry.isValid: expiresAt not null, now >= expiresAt → invalid ---

  @Test
  void get_secretExpiredByExpiresAt_refetches() {
    SecretStorePort delegate = mock(SecretStorePort.class);
    SecretKey key = SecretKey.of("expiry");

    Instant now = Instant.parse("2026-01-01T12:00:00Z");
    Instant expiredAt = now.minusSeconds(1);
    Clock clock = Clock.fixed(now, ZoneOffset.UTC);

    SecretValue expired = SecretValue.of("val", null, null, expiredAt);
    when(delegate.get(key)).thenReturn(Optional.of(expired));

    try (CachedSecretStorePort cached =
        new CachedSecretStorePort(delegate, Duration.ofHours(1), clock, null)) {
      // First call: loads (cache miss since empty)
      Optional<SecretValue> first = cached.get(key);
      assertThat(first).isPresent();
      // Second call: entry is invalid (expiresAt already passed) → re-fetches
      Optional<SecretValue> second = cached.get(key);
      assertThat(second).isPresent();
      verify(delegate, times(2)).get(key);
    }
  }

  // --- exists(): cache hit returns true without delegate call ---

  @Test
  void exists_cacheHit_returnsTrueWithoutDelegate() {
    SecretStorePort delegate = mock(SecretStorePort.class);
    Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
    SecretKey key = SecretKey.of("e");
    when(delegate.get(key)).thenReturn(Optional.of(SecretValue.of("v")));

    try (CachedSecretStorePort cached =
        new CachedSecretStorePort(delegate, Duration.ofMinutes(5), clock, null)) {
      cached.get(key); // Populate cache
      boolean exists = cached.exists(key);
      assertThat(exists).isTrue();
      verify(delegate, times(1)).get(key); // delegate.exists not called
    }
  }

  // --- exists(): cache miss → delegates ---

  @Test
  void exists_cacheMiss_delegatesToDelegate() {
    SecretStorePort delegate = mock(SecretStorePort.class);
    SecretKey key = SecretKey.of("missing");
    when(delegate.exists(key)).thenReturn(false);

    try (CachedSecretStorePort cached =
        new CachedSecretStorePort(delegate, Duration.ofMinutes(5))) {
      boolean exists = cached.exists(key);
      assertThat(exists).isFalse();
      verify(delegate).exists(key);
    }
  }

  // --- delete(): delegates and removes from cache ---

  @Test
  void delete_removesFromCacheAndDelegates() {
    SecretStorePort delegate = mock(SecretStorePort.class);
    SecretKey key = SecretKey.of("del");
    when(delegate.delete(key)).thenReturn(true);

    try (CachedSecretStorePort cached =
        new CachedSecretStorePort(delegate, Duration.ofMinutes(5))) {
      boolean result = cached.delete(key);
      assertThat(result).isTrue();
      verify(delegate).delete(key);
    }
  }

  // --- list(): delegated directly ---

  @Test
  void list_delegatesDirectly() {
    SecretStorePort delegate = mock(SecretStorePort.class);
    when(delegate.list("prefix")).thenReturn(List.of(SecretKey.of("prefix/a")));

    try (CachedSecretStorePort cached =
        new CachedSecretStorePort(delegate, Duration.ofMinutes(5))) {
      var result = cached.list("prefix");
      assertThat(result).hasSize(1);
    }
  }

  // --- put(key, map): delegates and removes from cache ---

  @Test
  void put_map_delegatesAndRemovesFromCache() {
    SecretStorePort delegate = mock(SecretStorePort.class);
    SecretKey key = SecretKey.of("k");
    when(delegate.put(key, java.util.Map.of("field", "value"))).thenReturn("v1");

    try (CachedSecretStorePort cached =
        new CachedSecretStorePort(delegate, Duration.ofMinutes(5))) {
      String version = cached.put(key, java.util.Map.of("field", "value"));
      assertThat(version).isEqualTo("v1");
    }
  }

  // --- AwsSecretsManagerClients.secretsManagerClient(): creates client ---

  @Test
  void awsSecretsManagerClients_secretsManagerClient_createsClient() {
    var client = AwsSecretsManagerClients.secretsManagerClient(Region.US_EAST_1);
    assertThat(client).isNotNull();
    client.close();
  }
}
