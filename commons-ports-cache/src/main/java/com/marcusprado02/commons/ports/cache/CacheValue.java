package com.marcusprado02.commons.ports.cache;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents a cached value with metadata.
 *
 * @param <T> Value type
 * @param value Cached value
 * @param cachedAt Timestamp when cached
 * @param expiresAt Optional expiration timestamp
 */
public record CacheValue<T>(T value, Instant cachedAt, Optional<Instant> expiresAt) {

  public CacheValue {
    Objects.requireNonNull(value, "value must not be null");
    Objects.requireNonNull(cachedAt, "cachedAt must not be null");
    Objects.requireNonNull(expiresAt, "expiresAt must not be null");
  }

  /**
   * Create a cache value without expiration.
   *
   * @param value Value
   * @param <T> Value type
   * @return CacheValue instance
   */
  public static <T> CacheValue<T> of(T value) {
    return new CacheValue<>(value, Instant.now(), Optional.empty());
  }

  /**
   * Create a cache value with expiration.
   *
   * @param value Value
   * @param expiresAt Expiration time
   * @param <T> Value type
   * @return CacheValue instance
   */
  public static <T> CacheValue<T> of(T value, Instant expiresAt) {
    return new CacheValue<>(value, Instant.now(), Optional.of(expiresAt));
  }

  /**
   * Check if value is expired.
   *
   * @return true if expired
   */
  public boolean isExpired() {
    return expiresAt.map(exp -> Instant.now().isAfter(exp)).orElse(false);
  }
}
