package com.marcusprado02.commons.ports.cache;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

/**
 * Port for cache operations.
 *
 * @param <K> Key type
 * @param <V> Value type
 */
public interface CachePort<K, V> {

  /**
   * Get value from cache.
   *
   * @param key Cache key
   * @return Optional value
   */
  Optional<V> get(K key);

  /**
   * Put value in cache without expiration.
   *
   * @param key Cache key
   * @param value Cache value
   */
  void put(K key, V value);

  /**
   * Put value in cache with TTL.
   *
   * @param key Cache key
   * @param value Cache value
   * @param ttl Time to live
   */
  void put(K key, V value, Duration ttl);

  /**
   * Remove value from cache.
   *
   * @param key Cache key
   */
  void remove(K key);

  /** Clear all cache entries. */
  void clear();

  /**
   * Check if key exists in cache.
   *
   * @param key Cache key
   * @return true if exists
   */
  boolean contains(K key);

  /**
   * Get all keys in cache.
   *
   * @return Set of keys
   */
  Set<K> keys();

  /**
   * Get cache size.
   *
   * @return Number of entries
   */
  long size();
}
