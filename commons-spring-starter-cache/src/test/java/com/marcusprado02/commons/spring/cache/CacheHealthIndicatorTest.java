package com.marcusprado02.commons.spring.cache;

import static org.assertj.core.api.Assertions.assertThat;

import com.marcusprado02.commons.ports.cache.CachePort;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

class CacheHealthIndicatorTest {

  @Test
  void shouldReturnUpWhenCacheIsHealthy() {
    // Arrange
    CachePort<String, Object> cachePort = new InMemoryCachePort();
    CacheHealthIndicator indicator = new CacheHealthIndicator(cachePort);

    // Act
    Health health = indicator.health();

    // Assert
    assertThat(health.getStatus()).isEqualTo(Status.UP);
    assertThat(health.getDetails()).containsKey("type");
    assertThat(health.getDetails()).containsKey("status");
  }

  @Test
  void shouldReturnDownWhenCacheThrowsException() {
    // Arrange
    CachePort<String, Object> cachePort = new FailingCachePort();
    CacheHealthIndicator indicator = new CacheHealthIndicator(cachePort);

    // Act
    Health health = indicator.health();

    // Assert
    assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    assertThat(health.getDetails()).containsKey("error");
  }

  private static class InMemoryCachePort implements CachePort<String, Object> {
    private final java.util.Map<String, Object> cache =
        new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    public java.util.Optional<Object> get(String key) {
      return java.util.Optional.ofNullable(cache.get(key));
    }

    @Override
    public void put(String key, Object value) {
      cache.put(key, value);
    }

    @Override
    public void put(String key, Object value, java.time.Duration ttl) {
      cache.put(key, value);
    }

    @Override
    public void remove(String key) {
      cache.remove(key);
    }

    @Override
    public void clear() {
      cache.clear();
    }

    @Override
    public boolean contains(String key) {
      return cache.containsKey(key);
    }

    @Override
    public java.util.Set<String> keys() {
      return cache.keySet();
    }

    @Override
    public long size() {
      return cache.size();
    }
  }

  private static class FailingCachePort implements CachePort<String, Object> {
    @Override
    public java.util.Optional<Object> get(String key) {
      throw new RuntimeException("Cache connection failed");
    }

    @Override
    public void put(String key, Object value) {
      throw new RuntimeException("Cache connection failed");
    }

    @Override
    public void put(String key, Object value, java.time.Duration ttl) {
      throw new RuntimeException("Cache connection failed");
    }

    @Override
    public void remove(String key) {
      throw new RuntimeException("Cache connection failed");
    }

    @Override
    public void clear() {
      throw new RuntimeException("Cache connection failed");
    }

    @Override
    public boolean contains(String key) {
      throw new RuntimeException("Cache connection failed");
    }

    @Override
    public java.util.Set<String> keys() {
      throw new RuntimeException("Cache connection failed");
    }

    @Override
    public long size() {
      throw new RuntimeException("Cache connection failed");
    }
  }
}
