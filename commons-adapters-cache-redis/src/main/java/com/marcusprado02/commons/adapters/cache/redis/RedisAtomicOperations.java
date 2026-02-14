package com.marcusprado02.commons.adapters.cache.redis;

import java.time.Duration;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * Redis atomic operations support.
 *
 * <p>Provides thread-safe atomic operations like increment/decrement, useful for counters, rate
 * limiting, and distributed locking scenarios.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * RedisAtomicOperations<String> atomicOps = new RedisAtomicOperations<>(redisTemplate);
 *
 * // Increment counter
 * long newValue = atomicOps.increment("page:views");
 *
 * // Increment by specific delta
 * long visits = atomicOps.incrementBy("user:123:visits", 5);
 *
 * // Decrement counter
 * long remaining = atomicOps.decrement("stock:product:456");
 *
 * // Set if absent (distributed lock)
 * boolean acquired = atomicOps.setIfAbsent("lock:resource", "owner-id", Duration.ofSeconds(30));
 * }</pre>
 *
 * @param <K> Key type
 */
public class RedisAtomicOperations<K> {

  private static final Logger logger = LoggerFactory.getLogger(RedisAtomicOperations.class);

  private final RedisTemplate<K, Long> redisTemplate;
  private final String keyPreflix;

  public RedisAtomicOperations(RedisTemplate<K, Long> redisTemplate) {
    this(redisTemplate, "");
  }

  public RedisAtomicOperations(RedisTemplate<K, Long> redisTemplate, String keyPrefix) {
    this.redisTemplate = Objects.requireNonNull(redisTemplate, "redisTemplate must not be null");
    this.keyPreflix = keyPrefix != null ? keyPrefix : "";
  }

  /**
   * Atomically increment value by 1.
   *
   * @param key Counter key
   * @return New value after increment, or 0 on error
   */
  public long increment(K key) {
    return incrementBy(key, 1L);
  }

  /**
   * Atomically increment value by delta.
   *
   * @param key Counter key
   * @param delta Amount to increment by
   * @return New value after increment, or 0 on error
   */
  public long incrementBy(K key, long delta) {
    Objects.requireNonNull(key, "key must not be null");
    try {
      Long result = redisTemplate.opsForValue().increment(prefixKey(key), delta);
      long newValue = result != null ? result : 0L;
      logger.debug("Incremented key {} by {}, new value: {}", key, delta, newValue);
      return newValue;
    } catch (Exception e) {
      logger.error("Error incrementing key: {}", key, e);
      return 0L;
    }
  }

  /**
   * Atomically decrement value by 1.
   *
   * @param key Counter key
   * @return New value after decrement, or 0 on error
   */
  public long decrement(K key) {
    return decrementBy(key, 1L);
  }

  /**
   * Atomically decrement value by delta.
   *
   * @param key Counter key
   * @param delta Amount to decrement by
   * @return New value after decrement, or 0 on error
   */
  public long decrementBy(K key, long delta) {
    Objects.requireNonNull(key, "key must not be null");
    try {
      Long result = redisTemplate.opsForValue().decrement(prefixKey(key), delta);
      long newValue = result != null ? result : 0L;
      logger.debug("Decremented key {} by {}, new value: {}", key, delta, newValue);
      return newValue;
    } catch (Exception e) {
      logger.error("Error decrementing key: {}", key, e);
      return 0L;
    }
  }

  /**
   * Set key to hold value if key does not exist (SETNX).
   *
   * <p>Useful for distributed locking patterns.
   *
   * @param key Lock key
   * @param value Lock value (e.g., owner ID)
   * @param ttl Time to live for the lock
   * @return true if key was set, false if key already exists
   */
  public boolean setIfAbsent(K key, Long value, Duration ttl) {
    Objects.requireNonNull(key, "key must not be null");
    Objects.requireNonNull(value, "value must not be null");
    Objects.requireNonNull(ttl, "ttl must not be null");
    try {
      Boolean result = redisTemplate.opsForValue().setIfAbsent(prefixKey(key), value, ttl);
      boolean success = Boolean.TRUE.equals(result);
      logger.debug("SetIfAbsent key {} with TTL {}: {}", key, ttl, success);
      return success;
    } catch (Exception e) {
      logger.error("Error setting key if absent: {}", key, e);
      return false;
    }
  }

  /**
   * Get current counter value.
   *
   * @param key Counter key
   * @return Current value, or 0 if not exists or on error
   */
  public long get(K key) {
    Objects.requireNonNull(key, "key must not be null");
    try {
      Long value = redisTemplate.opsForValue().get(prefixKey(key));
      return value != null ? value : 0L;
    } catch (Exception e) {
      logger.error("Error getting counter value: {}", key, e);
      return 0L;
    }
  }

  /**
   * Set counter value.
   *
   * @param key Counter key
   * @param value Value to set
   */
  public void set(K key, long value) {
    Objects.requireNonNull(key, "key must not be null");
    try {
      redisTemplate.opsForValue().set(prefixKey(key), value);
      logger.debug("Set counter {} to {}", key, value);
    } catch (Exception e) {
      logger.error("Error setting counter value: {}", key, e);
    }
  }

  /**
   * Reset counter to 0.
   *
   * @param key Counter key
   */
  public void reset(K key) {
    set(key, 0L);
  }

  @SuppressWarnings("unchecked")
  private K prefixKey(K key) {
    if (keyPreflix.isEmpty()) {
      return key;
    }
    return (K) (keyPreflix + key.toString());
  }
}
