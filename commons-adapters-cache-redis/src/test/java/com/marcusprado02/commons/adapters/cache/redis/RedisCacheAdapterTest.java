package com.marcusprado02.commons.adapters.cache.redis;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class RedisCacheAdapterTest {

  @Container
  static GenericContainer<?> redis =
      new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

  private static LettuceConnectionFactory connectionFactory;
  private static RedisTemplate<String, String> redisTemplate;
  private RedisCacheAdapter<String, String> cache;

  @BeforeAll
  static void setupRedis() {
    redis.start();

    RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
    config.setHostName(redis.getHost());
    config.setPort(redis.getFirstMappedPort());

    connectionFactory = new LettuceConnectionFactory(config);
    connectionFactory.afterPropertiesSet();

    redisTemplate = new RedisTemplate<>();
    redisTemplate.setConnectionFactory(connectionFactory);
    redisTemplate.setKeySerializer(new StringRedisSerializer());
    redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
    redisTemplate.setHashKeySerializer(new StringRedisSerializer());
    redisTemplate.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
    redisTemplate.afterPropertiesSet();
  }

  @AfterAll
  static void cleanup() {
    if (connectionFactory != null) {
      connectionFactory.destroy();
    }
    redis.stop();
  }

  @BeforeEach
  void setUp() {
    redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    cache = new RedisCacheAdapter<>(redisTemplate);
  }

  @Test
  void shouldPutAndGetValue() {
    cache.put("key1", "value1");

    var result = cache.get("key1");

    assertThat(result).isPresent().contains("value1");
  }

  @Test
  void shouldReturnEmptyForNonExistentKey() {
    var result = cache.get("nonexistent");

    assertThat(result).isEmpty();
  }

  @Test
  void shouldPutValueWithTTL() throws InterruptedException {
    cache.put("key2", "value2", Duration.ofSeconds(1));

    assertThat(cache.get("key2")).isPresent().contains("value2");

    Thread.sleep(1100);

    assertThat(cache.get("key2")).isEmpty();
  }

  @Test
  void shouldRemoveValue() {
    cache.put("key3", "value3");
    assertThat(cache.get("key3")).isPresent();

    cache.remove("key3");

    assertThat(cache.get("key3")).isEmpty();
  }

  @Test
  void shouldCheckIfKeyExists() {
    cache.put("key4", "value4");

    assertThat(cache.contains("key4")).isTrue();
    assertThat(cache.contains("nonexistent")).isFalse();
  }

  @Test
  void shouldGetAllKeys() {
    cache.put("key5", "value5");
    cache.put("key6", "value6");
    cache.put("key7", "value7");

    Set<String> keys = cache.keys();

    assertThat(keys).containsExactlyInAnyOrder("key5", "key6", "key7");
  }

  @Test
  void shouldGetCacheSize() {
    cache.put("key8", "value8");
    cache.put("key9", "value9");

    assertThat(cache.size()).isEqualTo(2);
  }

  @Test
  void shouldClearCache() {
    cache.put("key10", "value10");
    cache.put("key11", "value11");
    assertThat(cache.size()).isEqualTo(2);

    cache.clear();

    assertThat(cache.size()).isZero();
    assertThat(cache.get("key10")).isEmpty();
    assertThat(cache.get("key11")).isEmpty();
  }

  @Test
  void shouldHandleComplexObjects() {
    TestObject obj = new TestObject("test", 42);

    // Create separate RedisTemplate for TestObject
    RedisTemplate<String, TestObject> objectTemplate = new RedisTemplate<>();
    objectTemplate.setConnectionFactory(connectionFactory);
    objectTemplate.setKeySerializer(new StringRedisSerializer());
    objectTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
    objectTemplate.afterPropertiesSet();

    RedisCacheAdapter<String, TestObject> objectCache = new RedisCacheAdapter<>(objectTemplate);

    objectCache.put("obj1", obj);

    var result = objectCache.get("obj1");

    assertThat(result).isPresent();
    assertThat(result.get().name()).isEqualTo("test");
    assertThat(result.get().value()).isEqualTo(42);
  }

  @Test
  void shouldHandleKeyPrefix() {
    RedisCacheAdapter<String, String> prefixedCache =
        new RedisCacheAdapter<>(redisTemplate, "prefix:");

    prefixedCache.put("key12", "value12");

    assertThat(prefixedCache.get("key12")).isPresent().contains("value12");
    assertThat(prefixedCache.contains("key12")).isTrue();
    assertThat(prefixedCache.keys()).contains("key12");
  }

  @Test
  void shouldUpdateExistingValue() {
    cache.put("key13", "oldValue");
    assertThat(cache.get("key13")).contains("oldValue");

    cache.put("key13", "newValue");

    assertThat(cache.get("key13")).contains("newValue");
  }

  @Test
  void shouldHandleNullKey() {
    assertThat(cache.get(null)).isEmpty();
  }

  record TestObject(String name, int value) {}
}
