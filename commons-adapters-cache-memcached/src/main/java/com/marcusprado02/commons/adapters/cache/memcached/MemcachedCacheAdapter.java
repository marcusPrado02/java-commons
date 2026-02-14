package com.marcusprado02.commons.adapters.cache.memcached;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.marcusprado02.commons.ports.cache.CachePort;
import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import net.spy.memcached.MemcachedClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Memcached implementation of CachePort using Spymemcached client.
 *
 * <p>This adapter provides a distributed caching solution with JSON serialization support.
 *
 * <p>Features:
 *
 * <ul>
 *   <li>Type-safe operations with generics
 *   <li>JSON serialization via Jackson
 *   <li>TTL support
 *   <li>Key prefix for multi-tenancy
 *   <li>Graceful error handling
 *   <li>Local key tracking (Memcached limitation)
 * </ul>
 *
 * <p>Note: Memcached doesn't provide a native way to list all keys. This implementation maintains a
 * local concurrent map to track keys for {@link #keys()}, {@link #clear()}, and {@link #size()}
 * operations.
 *
 * @param <K> Key type (must be serializable to String)
 * @param <V> Value type (must be JSON serializable)
 */
public class MemcachedCacheAdapter<K, V> implements CachePort<K, V> {

  private static final Logger logger = LoggerFactory.getLogger(MemcachedCacheAdapter.class);
  private static final int DEFAULT_EXPIRATION = 0; // No expiration

  private final MemcachedClient client;
  private final ObjectMapper objectMapper;
  private final String keyPrefix;
  private final Class<V> valueType;
  private final Map<String, Long> keyTracker; // Track keys locally

  /**
   * Create adapter without key prefix.
   *
   * @param client Memcached client
   * @param valueType Value class type for deserialization
   */
  public MemcachedCacheAdapter(MemcachedClient client, Class<V> valueType) {
    this(client, valueType, "");
  }

  /**
   * Create adapter with key prefix for multi-tenancy.
   *
   * @param client Memcached client
   * @param valueType Value class type for deserialization
   * @param keyPrefix Prefix for all keys (e.g., "tenant1:")
   */
  public MemcachedCacheAdapter(MemcachedClient client, Class<V> valueType, String keyPrefix) {
    this.client = Objects.requireNonNull(client, "client must not be null");
    this.valueType = Objects.requireNonNull(valueType, "valueType must not be null");
    this.keyPrefix = keyPrefix != null ? keyPrefix : "";
    this.keyTracker = new ConcurrentHashMap<>();
    this.objectMapper = createObjectMapper();
  }

  @Override
  public Optional<V> get(K key) {
    if (key == null) {
      logger.warn("Attempted to get null key");
      return Optional.empty();
    }

    String prefixedKey = prefixKey(key);
    try {
      Future<Object> future = client.asyncGet(prefixedKey);
      Object result = future.get();

      if (result == null) {
        logger.debug("Cache miss for key: {}", key);
        return Optional.empty();
      }

      V value = deserialize((String) result, valueType);
      logger.debug("Cache hit for key: {}", key);
      return Optional.ofNullable(value);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      logger.error("Thread interrupted while getting key: {}", key, e);
      return Optional.empty();
    } catch (ExecutionException e) {
      logger.error("Error getting key from Memcached: {}", key, e);
      return Optional.empty();
    }
  }

  @Override
  public void put(K key, V value) {
    put(key, value, Duration.ZERO);
  }

  @Override
  public void put(K key, V value, Duration ttl) {
    Objects.requireNonNull(key, "key must not be null");
    Objects.requireNonNull(value, "value must not be null");

    String prefixedKey = prefixKey(key);
    int expiration = ttl != null && !ttl.isZero() ? (int) ttl.toSeconds() : DEFAULT_EXPIRATION;

    try {
      String serialized = serialize(value);
      Future<Boolean> future = client.set(prefixedKey, expiration, serialized);
      Boolean success = future.get();

      if (Boolean.TRUE.equals(success)) {
        keyTracker.put(prefixedKey, System.currentTimeMillis() + (expiration * 1000L));
        logger.debug("Cached key: {} with TTL: {}", key, ttl);
      } else {
        logger.warn("Failed to cache key: {}", key);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      logger.error("Thread interrupted while putting key: {}", key, e);
    } catch (ExecutionException e) {
      logger.error("Error putting key in Memcached: {}", key, e);
    }
  }

  @Override
  public void remove(K key) {
    Objects.requireNonNull(key, "key must not be null");

    String prefixedKey = prefixKey(key);
    try {
      Future<Boolean> future = client.delete(prefixedKey);
      Boolean success = future.get();

      if (Boolean.TRUE.equals(success)) {
        keyTracker.remove(prefixedKey);
        logger.debug("Removed key: {}", key);
      } else {
        logger.debug("Key not found for removal: {}", key);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      logger.error("Thread interrupted while removing key: {}", key, e);
    } catch (ExecutionException e) {
      logger.error("Error removing key from Memcached: {}", key, e);
    }
  }

  @Override
  public void clear() {
    try {
      // Memcached flush_all command
      client.flush();
      keyTracker.clear();
      logger.debug("Cleared all cache entries");
    } catch (Exception e) {
      logger.error("Error clearing cache", e);
    }
  }

  @Override
  public boolean contains(K key) {
    Objects.requireNonNull(key, "key must not be null");

    String prefixedKey = prefixKey(key);
    try {
      Future<Object> future = client.asyncGet(prefixedKey);
      Object result = future.get();
      return result != null;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      logger.error("Thread interrupted while checking key existence: {}", key, e);
      return false;
    } catch (ExecutionException e) {
      logger.error("Error checking key existence: {}", key, e);
      return false;
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public Set<K> keys() {
    try {
      // Remove expired keys from tracker
      long now = System.currentTimeMillis();
      keyTracker.entrySet().removeIf(entry -> entry.getValue() > 0 && entry.getValue() < now);

      Set<K> result = new HashSet<>();
      for (String prefixedKey : keyTracker.keySet()) {
        String unprefixedKey =
            keyPrefix.isEmpty() ? prefixedKey : prefixedKey.substring(keyPrefix.length());
        result.add((K) unprefixedKey);
      }
      return result;
    } catch (Exception e) {
      logger.error("Error getting keys", e);
      return Collections.emptySet();
    }
  }

  @Override
  public long size() {
    try {
      return keys().size();
    } catch (Exception e) {
      logger.error("Error getting cache size", e);
      return 0;
    }
  }

  /**
   * Close the Memcached client connection. Should be called when the adapter is no longer needed.
   */
  public void shutdown() {
    try {
      client.shutdown();
      logger.info("Memcached client shutdown completed");
    } catch (Exception e) {
      logger.error("Error shutting down Memcached client", e);
    }
  }

  private String prefixKey(K key) {
    return keyPrefix + key.toString();
  }

  private String serialize(V value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (Exception e) {
      logger.error("Error serializing value", e);
      throw new RuntimeException("Failed to serialize value", e);
    }
  }

  private V deserialize(String json, Class<V> type) {
    try {
      return objectMapper.readValue(json, type);
    } catch (Exception e) {
      logger.error("Error deserializing value", e);
      throw new RuntimeException("Failed to deserialize value", e);
    }
  }

  private ObjectMapper createObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    return mapper;
  }
}
