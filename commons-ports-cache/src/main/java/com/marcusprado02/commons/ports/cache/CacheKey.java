package com.marcusprado02.commons.ports.cache;

import java.util.Objects;

/**
 * Represents a cache key with namespace support.
 *
 * @param namespace Key namespace (e.g., "user", "product")
 * @param key Actual key value
 */
public record CacheKey(String namespace, String key) {

  public CacheKey {
    Objects.requireNonNull(namespace, "namespace must not be null");
    Objects.requireNonNull(key, "key must not be null");
  }

  /**
   * Create a cache key with namespace.
   *
   * @param namespace Namespace
   * @param key Key
   * @return CacheKey instance
   */
  public static CacheKey of(String namespace, String key) {
    return new CacheKey(namespace, key);
  }

  /**
   * Create a cache key without namespace.
   *
   * @param key Key
   * @return CacheKey instance
   */
  public static CacheKey of(String key) {
    return new CacheKey("default", key);
  }

  /**
   * Get full key with namespace.
   *
   * @return Formatted key
   */
  public String toFullKey() {
    return namespace + ":" + key;
  }

  @Override
  public String toString() {
    return toFullKey();
  }
}
