package com.marcusprado02.commons.testkit.contracts;

import static org.assertj.core.api.Assertions.assertThat;

import com.marcusprado02.commons.ports.cache.CachePort;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Base contract test for {@link CachePort} implementations.
 *
 * <p>Extend this class to verify that your cache implementation correctly follows the CachePort
 * contract.
 *
 * <p>Example:
 *
 * <pre>{@code
 * class RedisCacheContractTest extends CachePortContract<String, String> {
 *   @Override
 *   protected CachePort<String, String> createCache() {
 *     return new RedisCacheAdapter(redisTemplate);
 *   }
 *
 *   @Override
 *   protected String createTestKey() {
 *     return "test-key-" + UUID.randomUUID();
 *   }
 *
 *   @Override
 *   protected String createTestValue() {
 *     return "test-value-" + System.currentTimeMillis();
 *   }
 * }
 * }</pre>
 *
 * @param <K> Key type
 * @param <V> Value type
 */
public abstract class CachePortContract<K, V> {

  protected CachePort<K, V> cache;

  /**
   * Create the cache instance to be tested.
   *
   * @return cache implementation
   */
  protected abstract CachePort<K, V> createCache();

  /**
   * Create a test key (should be unique per test).
   *
   * @return test key
   */
  protected abstract K createTestKey();

  /**
   * Create another test key (different from createTestKey()).
   *
   * @return another test key
   */
  protected abstract K createAnotherTestKey();

  /**
   * Create a test value.
   *
   * @return test value
   */
  protected abstract V createTestValue();

  /**
   * Create another test value (different from createTestValue()).
   *
   * @return another test value
   */
  protected abstract V createAnotherTestValue();

  @BeforeEach
  void setUp() {
    cache = createCache();
    cache.clear();
  }

  @Test
  @DisplayName("Should put and get value")
  void shouldPutAndGet() {
    // Given
    K key = createTestKey();
    V value = createTestValue();

    // When
    cache.put(key, value);
    Optional<V> retrieved = cache.get(key);

    // Then
    assertThat(retrieved).isPresent();
    assertThat(retrieved.get()).isEqualTo(value);
  }

  @Test
  @DisplayName("Should return empty when key not found")
  void shouldReturnEmptyWhenNotFound() {
    // Given
    K key = createTestKey();

    // When
    Optional<V> retrieved = cache.get(key);

    // Then
    assertThat(retrieved).isEmpty();
  }

  @Test
  @DisplayName("Should remove value from cache")
  void shouldRemoveValue() {
    // Given
    K key = createTestKey();
    V value = createTestValue();
    cache.put(key, value);

    // When
    cache.remove(key);
    Optional<V> retrieved = cache.get(key);

    // Then
    assertThat(retrieved).isEmpty();
  }

  @Test
  @DisplayName("Should clear all entries")
  void shouldClearAll() {
    // Given
    K key1 = createTestKey();
    K key2 = createAnotherTestKey();
    V value1 = createTestValue();
    V value2 = createAnotherTestValue();
    cache.put(key1, value1);
    cache.put(key2, value2);

    // When
    cache.clear();

    // Then
    assertThat(cache.get(key1)).isEmpty();
    assertThat(cache.get(key2)).isEmpty();
    assertThat(cache.size()).isZero();
  }

  @Test
  @DisplayName("Should check if key exists")
  void shouldCheckIfKeyExists() {
    // Given
    K key = createTestKey();
    V value = createTestValue();

    // When
    cache.put(key, value);

    // Then
    assertThat(cache.contains(key)).isTrue();
    assertThat(cache.contains(createAnotherTestKey())).isFalse();
  }

  @Test
  @DisplayName("Should return all keys")
  void shouldReturnAllKeys() {
    // Given
    K key1 = createTestKey();
    K key2 = createAnotherTestKey();
    V value1 = createTestValue();
    V value2 = createAnotherTestValue();
    cache.put(key1, value1);
    cache.put(key2, value2);

    // When
    Set<K> keys = cache.keys();

    // Then
    assertThat(keys).hasSize(2);
    assertThat(keys).contains(key1, key2);
  }

  @Test
  @DisplayName("Should return correct size")
  void shouldReturnCorrectSize() {
    // Given
    K key1 = createTestKey();
    K key2 = createAnotherTestKey();
    V value1 = createTestValue();
    V value2 = createAnotherTestValue();

    // When/Then
    assertThat(cache.size()).isZero();

    cache.put(key1, value1);
    assertThat(cache.size()).isEqualTo(1);

    cache.put(key2, value2);
    assertThat(cache.size()).isEqualTo(2);

    cache.remove(key1);
    assertThat(cache.size()).isEqualTo(1);
  }

  @Test
  @DisplayName("Should update existing value")
  void shouldUpdateExistingValue() {
    // Given
    K key = createTestKey();
    V oldValue = createTestValue();
    V newValue = createAnotherTestValue();

    // When
    cache.put(key, oldValue);
    cache.put(key, newValue);
    Optional<V> retrieved = cache.get(key);

    // Then
    assertThat(retrieved).isPresent();
    assertThat(retrieved.get()).isEqualTo(newValue);
    assertThat(cache.size()).isEqualTo(1);
  }

  @Test
  @DisplayName("Should put value with TTL")
  void shouldPutWithTtl() {
    // Given
    K key = createTestKey();
    V value = createTestValue();
    Duration ttl = Duration.ofMillis(100);

    // When
    cache.put(key, value, ttl);

    // Then - value should be present immediately
    assertThat(cache.get(key)).isPresent();

    // Note: Testing actual expiration is implementation-specific
    // Some implementations may require waiting, others may not support TTL
  }
}
