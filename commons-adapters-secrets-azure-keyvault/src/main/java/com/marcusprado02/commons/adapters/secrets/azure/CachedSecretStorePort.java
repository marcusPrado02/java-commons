package com.marcusprado02.commons.adapters.secrets.azure;

import com.marcusprado02.commons.ports.secrets.SecretKey;
import com.marcusprado02.commons.ports.secrets.SecretStorePort;
import com.marcusprado02.commons.ports.secrets.SecretValue;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/**
 * Simple in-memory caching decorator for SecretStorePort.
 *
 * <p>Refresh strategy:
 *
 * <ul>
 *   <li>On access: cache entries expire after TTL and are reloaded.
 *   <li>Optional background refresh: periodically reload cached keys.
 * </ul>
 */
public final class CachedSecretStorePort implements SecretStorePort, AutoCloseable {

  private final SecretStorePort delegate;
  private final Duration ttl;
  private final Clock clock;

  private final ConcurrentHashMap<SecretKey, CacheEntry> cache = new ConcurrentHashMap<>();

  private final ScheduledExecutorService scheduler;
  private final ScheduledFuture<?> refreshTask;

  public CachedSecretStorePort(SecretStorePort delegate, Duration ttl) {
    this(delegate, ttl, Clock.systemUTC(), null);
  }

  public CachedSecretStorePort(
      SecretStorePort delegate, Duration ttl, Clock clock, Duration refreshInterval) {
    this.delegate = Objects.requireNonNull(delegate, "delegate cannot be null");
    this.ttl = Objects.requireNonNull(ttl, "ttl cannot be null");
    this.clock = Objects.requireNonNull(clock, "clock cannot be null");

    if (refreshInterval != null && !refreshInterval.isZero() && !refreshInterval.isNegative()) {
      this.scheduler = Executors.newSingleThreadScheduledExecutor(
          r -> {
            Thread t = new Thread(r, "secret-store-cache-refresh");
            t.setDaemon(true);
            return t;
          });
      this.refreshTask =
          this.scheduler.scheduleWithFixedDelay(
              this::refreshAllOnce,
              refreshInterval.toMillis(),
              refreshInterval.toMillis(),
              TimeUnit.MILLISECONDS);
    } else {
      this.scheduler = null;
      this.refreshTask = null;
    }
  }

  @Override
  public Optional<SecretValue> get(SecretKey key) {
    Objects.requireNonNull(key, "key cannot be null");

    Instant now = clock.instant();
    CacheEntry entry = cache.get(key);
    if (entry != null && entry.isValid(now, ttl)) {
      return Optional.of(entry.toSecretValue());
    }

    Optional<SecretValue> loaded = delegate.get(key);
    loaded.ifPresent(value -> cache.put(key, CacheEntry.from(now, value)));
    if (loaded.isEmpty()) {
      cache.remove(key);
    }
    return loaded.map(CacheEntry::copySecretValue);
  }

  @Override
  public Optional<SecretValue> get(SecretKey key, String version) {
    // Versioned reads are delegated (no caching to avoid key explosion).
    return delegate.get(key, version);
  }

  @Override
  public String put(SecretKey key, SecretValue value) {
    String version = delegate.put(key, value);
    // Invalidate cache; next get() fetches latest.
    cache.remove(key);
    return version;
  }

  @Override
  public String put(SecretKey key, Map<String, String> data) {
    String version = delegate.put(key, data);
    cache.remove(key);
    return version;
  }

  @Override
  public boolean delete(SecretKey key) {
    boolean deleted = delegate.delete(key);
    cache.remove(key);
    return deleted;
  }

  @Override
  public boolean exists(SecretKey key) {
    CacheEntry entry = cache.get(key);
    if (entry != null && entry.isValid(clock.instant(), ttl)) {
      return true;
    }
    return delegate.exists(key);
  }

  @Override
  public List<SecretKey> list(String prefix) {
    return delegate.list(prefix);
  }

  private void refreshAllOnce() {
    for (SecretKey key : cache.keySet()) {
      try {
        Optional<SecretValue> loaded = delegate.get(key);
        Instant now = clock.instant();
        loaded.ifPresent(value -> cache.put(key, CacheEntry.from(now, value)));
        if (loaded.isEmpty()) {
          cache.remove(key);
        }
      } catch (Exception ignored) {
        // Best-effort refresh.
      }
    }
  }

  @Override
  public void close() {
    if (refreshTask != null) {
      refreshTask.cancel(true);
    }
    if (scheduler != null) {
      scheduler.shutdownNow();
    }

    for (CacheEntry entry : cache.values()) {
      entry.zero();
    }
    cache.clear();
  }

  private static final class CacheEntry {

    private final byte[] data;
    private final String version;
    private final Instant createdAt;
    private final Instant expiresAt;
    private final Instant cachedAt;

    private CacheEntry(
        byte[] data, String version, Instant createdAt, Instant expiresAt, Instant cachedAt) {
      this.data = data;
      this.version = version;
      this.createdAt = createdAt;
      this.expiresAt = expiresAt;
      this.cachedAt = cachedAt;
    }

    static CacheEntry from(Instant now, SecretValue value) {
      byte[] bytes = value.asBytes();
      byte[] copy = Arrays.copyOf(bytes, bytes.length);
      Instant createdAt = value.createdAt() != null ? value.createdAt() : now;
      return new CacheEntry(
          copy,
          value.version().orElse(null),
          createdAt,
          value.expiresAt().orElse(null),
          now);
    }

    boolean isValid(Instant now, Duration ttl) {
      if (expiresAt != null && !now.isBefore(expiresAt)) {
        return false;
      }
      return !cachedAt.plus(ttl).isBefore(now);
    }

    SecretValue toSecretValue() {
      byte[] copy = Arrays.copyOf(data, data.length);
      return SecretValue.of(copy, version, createdAt, expiresAt);
    }

    void zero() {
      Arrays.fill(data, (byte) 0);
    }

    static SecretValue copySecretValue(SecretValue value) {
      byte[] bytes = value.asBytes();
      byte[] copy = Arrays.copyOf(bytes, bytes.length);
      Instant createdAt = value.createdAt() != null ? value.createdAt() : Instant.now();
      return SecretValue.of(
          copy, value.version().orElse(null), createdAt, value.expiresAt().orElse(null));
    }
  }
}
