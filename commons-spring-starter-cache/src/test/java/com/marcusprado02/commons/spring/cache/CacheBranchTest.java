package com.marcusprado02.commons.spring.cache;

import static org.assertj.core.api.Assertions.assertThat;

import com.marcusprado02.commons.ports.cache.CachePort;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

class CacheBranchTest {

  // --- CacheProperties getters/setters not yet exercised ---

  @Test
  void cacheProperties_getType_returnsDefault() {
    CacheProperties props = new CacheProperties();
    assertThat(props.getType()).isEqualTo(CacheProperties.CacheType.REDIS);
  }

  @Test
  void cacheProperties_redis_settersAndGetters() {
    CacheProperties.Redis redis = new CacheProperties().getRedis();
    redis.setPassword("secret");
    assertThat(redis.getPassword()).isEqualTo("secret");
    redis.setEnabled(false);
    assertThat(redis.isEnabled()).isFalse();
    redis.setEnabled(true);
    assertThat(redis.isEnabled()).isTrue();
  }

  @Test
  void cacheProperties_memcached_settersAndGetters() {
    CacheProperties.Memcached mem = new CacheProperties().getMemcached();
    mem.setEnabled(false);
    assertThat(mem.isEnabled()).isFalse();
    mem.setEnabled(true);
    assertThat(mem.isEnabled()).isTrue();
    mem.setTimeout(Duration.ofSeconds(10));
    assertThat(mem.getTimeout()).isEqualTo(Duration.ofSeconds(10));
  }

  // --- RedisCacheAutoConfiguration.redisConnectionFactory password branches ---

  @Test
  void redisConnectionFactory_withNonBlankPassword_setsPassword() {
    RedisCacheAutoConfiguration cfg = new RedisCacheAutoConfiguration();
    CacheProperties props = new CacheProperties();
    props.getRedis().setHost("localhost");
    props.getRedis().setPort(6379);
    props.getRedis().setPassword("secret");

    LettuceConnectionFactory factory = cfg.redisConnectionFactory(props);
    assertThat(factory).isNotNull();
  }

  @Test
  void redisConnectionFactory_withBlankPassword_skipsPassword() {
    RedisCacheAutoConfiguration cfg = new RedisCacheAutoConfiguration();
    CacheProperties props = new CacheProperties();
    props.getRedis().setPassword("   ");

    LettuceConnectionFactory factory = cfg.redisConnectionFactory(props);
    assertThat(factory).isNotNull();
  }

  // --- CacheHealthIndicator — wrong-value branch ---

  @Test
  void cacheHealthIndicator_downWhenGetReturnsWrongValue() {
    CachePort<String, Object> wrongValuePort = new WrongValueCachePort();
    CacheHealthIndicator indicator = new CacheHealthIndicator(wrongValuePort);

    Health health = indicator.health();

    assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    assertThat(health.getDetails()).containsKey("error");
  }

  private static class WrongValueCachePort implements CachePort<String, Object> {
    @Override
    public Optional<Object> get(String key) {
      return Optional.of("wrong-value");
    }

    @Override
    public void put(String key, Object value) {}

    @Override
    public void put(String key, Object value, Duration ttl) {}

    @Override
    public void remove(String key) {}

    @Override
    public void clear() {}

    @Override
    public boolean contains(String key) {
      return false;
    }

    @Override
    public Set<String> keys() {
      return Set.of();
    }

    @Override
    public long size() {
      return 0;
    }
  }
}
