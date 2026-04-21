package com.marcusprado02.commons.adapters.cache.redis;

import com.marcusprado02.commons.ports.cache.CachePort;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * Redis implementation of {@link CachePort} backed by Spring Data Redis.
 *
 * <p>Keys are always {@link String} — Redis internally stores keys as byte arrays and all pattern
 * operations (KEYS, SCAN) work on String patterns. The value type {@code V} is generic and
 * serialized via the {@link RedisTemplate}'s configured value serializer.
 *
 * @param <V> Value type
 */
public class RedisCacheAdapter<V> implements CachePort<String, V> {

  private static final Logger logger = LoggerFactory.getLogger(RedisCacheAdapter.class);

  private final RedisTemplate<String, V> redisTemplate;
  private final String keyPrefix;

  public RedisCacheAdapter(RedisTemplate<String, V> redisTemplate) {
    this(redisTemplate, "");
  }

  public RedisCacheAdapter(RedisTemplate<String, V> redisTemplate, String keyPrefix) {
    this.redisTemplate = Objects.requireNonNull(redisTemplate, "redisTemplate must not be null");
    this.keyPrefix = keyPrefix != null ? keyPrefix : "";
  }

  @Override
  public Optional<V> get(String key) {
    if (key == null) {
      logger.warn("Attempted to get null key");
      return Optional.empty();
    }
    try {
      V value = redisTemplate.opsForValue().get(prefixKey(key));
      return Optional.ofNullable(value);
    } catch (DataAccessException e) {
      logger.error("Error getting key from Redis: {}", key, e);
      return Optional.empty();
    }
  }

  @Override
  public void put(String key, V value) {
    Objects.requireNonNull(key, "key must not be null");
    Objects.requireNonNull(value, "value must not be null");
    try {
      redisTemplate.opsForValue().set(prefixKey(key), value);
      logger.debug("Cached key: {}", key);
    } catch (DataAccessException e) {
      logger.error("Error putting key in Redis: {}", key, e);
    }
  }

  @Override
  public void put(String key, V value, Duration ttl) {
    Objects.requireNonNull(key, "key must not be null");
    Objects.requireNonNull(value, "value must not be null");
    Objects.requireNonNull(ttl, "ttl must not be null");
    try {
      redisTemplate.opsForValue().set(prefixKey(key), value, ttl);
      logger.debug("Cached key: {} with TTL: {}", key, ttl);
    } catch (DataAccessException e) {
      logger.error("Error putting key in Redis with TTL: {}", key, e);
    }
  }

  @Override
  public void remove(String key) {
    Objects.requireNonNull(key, "key must not be null");
    try {
      redisTemplate.delete(prefixKey(key));
      logger.debug("Removed key: {}", key);
    } catch (DataAccessException e) {
      logger.error("Error removing key from Redis: {}", key, e);
    }
  }

  @Override
  public void clear() {
    try {
      Set<String> allKeys = keys();
      if (!allKeys.isEmpty()) {
        redisTemplate.delete(allKeys);
        logger.debug("Cleared {} keys", allKeys.size());
      }
    } catch (DataAccessException e) {
      logger.error("Error clearing cache", e);
    }
  }

  @Override
  public boolean contains(String key) {
    Objects.requireNonNull(key, "key must not be null");
    try {
      Boolean exists = redisTemplate.hasKey(prefixKey(key));
      return Boolean.TRUE.equals(exists);
    } catch (DataAccessException e) {
      logger.error("Error checking key existence: {}", key, e);
      return false;
    }
  }

  @Override
  public Set<String> keys() {
    try {
      String pattern = keyPrefix.isEmpty() ? "*" : keyPrefix + "*";
      Set<String> keys = redisTemplate.keys(pattern);
      if (keys == null) {
        return Set.of();
      }
      if (keyPrefix.isEmpty()) {
        return keys;
      }
      return keys.stream()
          .map(k -> k.substring(keyPrefix.length()))
          .collect(Collectors.toSet());
    } catch (DataAccessException e) {
      logger.error("Error getting keys", e);
      return Set.of();
    }
  }

  @Override
  public long size() {
    try {
      return keys().size();
    } catch (DataAccessException e) {
      logger.error("Error getting cache size", e);
      return 0;
    }
  }

  private String prefixKey(String key) {
    return keyPrefix.isEmpty() ? key : keyPrefix + key;
  }
}
