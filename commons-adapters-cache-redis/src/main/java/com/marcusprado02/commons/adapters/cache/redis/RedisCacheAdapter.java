package com.marcusprado02.commons.adapters.cache.redis;

import com.marcusprado02.commons.ports.cache.CachePort;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * Redis implementation of CachePort.
 *
 * @param <K> Key type
 * @param <V> Value type
 */
public class RedisCacheAdapter<K, V> implements CachePort<K, V> {

  private static final Logger logger = LoggerFactory.getLogger(RedisCacheAdapter.class);

  private final RedisTemplate<K, V> redisTemplate;
  private final String keyPrefix;

  public RedisCacheAdapter(RedisTemplate<K, V> redisTemplate) {
    this(redisTemplate, "");
  }

  public RedisCacheAdapter(RedisTemplate<K, V> redisTemplate, String keyPrefix) {
    this.redisTemplate = Objects.requireNonNull(redisTemplate, "redisTemplate must not be null");
    this.keyPrefix = keyPrefix != null ? keyPrefix : "";
  }

  @Override
  public Optional<V> get(K key) {
    if (key == null) {
      logger.warn("Attempted to get null key");
      return Optional.empty();
    }
    try {
      V value = redisTemplate.opsForValue().get(prefixKey(key));
      return Optional.ofNullable(value);
    } catch (Exception e) {
      logger.error("Error getting key from Redis: {}", key, e);
      return Optional.empty();
    }
  }

  @Override
  public void put(K key, V value) {
    Objects.requireNonNull(key, "key must not be null");
    Objects.requireNonNull(value, "value must not be null");
    try {
      redisTemplate.opsForValue().set(prefixKey(key), value);
      logger.debug("Cached key: {}", key);
    } catch (Exception e) {
      logger.error("Error putting key in Redis: {}", key, e);
    }
  }

  @Override
  public void put(K key, V value, Duration ttl) {
    Objects.requireNonNull(key, "key must not be null");
    Objects.requireNonNull(value, "value must not be null");
    Objects.requireNonNull(ttl, "ttl must not be null");
    try {
      redisTemplate.opsForValue().set(prefixKey(key), value, ttl);
      logger.debug("Cached key: {} with TTL: {}", key, ttl);
    } catch (Exception e) {
      logger.error("Error putting key in Redis with TTL: {}", key, e);
    }
  }

  @Override
  public void remove(K key) {
    Objects.requireNonNull(key, "key must not be null");
    try {
      redisTemplate.delete(prefixKey(key));
      logger.debug("Removed key: {}", key);
    } catch (Exception e) {
      logger.error("Error removing key from Redis: {}", key, e);
    }
  }

  @Override
  public void clear() {
    try {
      Set<K> allKeys = keys();
      if (!allKeys.isEmpty()) {
        redisTemplate.delete(allKeys);
        logger.debug("Cleared {} keys", allKeys.size());
      }
    } catch (Exception e) {
      logger.error("Error clearing cache", e);
    }
  }

  @Override
  public boolean contains(K key) {
    Objects.requireNonNull(key, "key must not be null");
    try {
      Boolean exists = redisTemplate.hasKey(prefixKey(key));
      return Boolean.TRUE.equals(exists);
    } catch (Exception e) {
      logger.error("Error checking key existence: {}", key, e);
      return false;
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public Set<K> keys() {
    try {
      String pattern = keyPrefix.isEmpty() ? "*" : keyPrefix + "*";
      Set<K> keys = redisTemplate.keys((K) pattern);
      if (keys == null) {
        return Set.of();
      }
      if (keyPrefix.isEmpty()) {
        return keys;
      }
      // Remove prefix from keys
      return keys.stream()
          .map(k -> (K) k.toString().substring(keyPrefix.length()))
          .collect(Collectors.toSet());
    } catch (Exception e) {
      logger.error("Error getting keys", e);
      return Set.of();
    }
  }

  @Override
  public long size() {
    try {
      Set<K> allKeys = keys();
      return allKeys.size();
    } catch (Exception e) {
      logger.error("Error getting cache size", e);
      return 0;
    }
  }

  @SuppressWarnings("unchecked")
  private K prefixKey(K key) {
    if (keyPrefix.isEmpty()) {
      return key;
    }
    return (K) (keyPrefix + key.toString());
  }
}
