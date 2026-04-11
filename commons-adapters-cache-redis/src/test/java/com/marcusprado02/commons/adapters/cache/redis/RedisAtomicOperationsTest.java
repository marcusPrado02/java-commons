package com.marcusprado02.commons.adapters.cache.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class RedisAtomicOperationsTest {

  @Container
  static GenericContainer<?> redis =
      new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

  private static LettuceConnectionFactory connectionFactory;
  private static RedisTemplate<String, Long> redisTemplate;
  private RedisAtomicOperations<String> atomicOps;

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
    redisTemplate.setValueSerializer(new GenericToStringSerializer<>(Long.class));
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
    atomicOps = new RedisAtomicOperations<>(redisTemplate);
  }

  @Test
  void shouldIncrementByOne() {
    long first = atomicOps.increment("counter");
    long second = atomicOps.increment("counter");

    assertThat(first).isEqualTo(1L);
    assertThat(second).isEqualTo(2L);
  }

  @Test
  void shouldIncrementByDelta() {
    long result = atomicOps.incrementBy("counter", 5L);

    assertThat(result).isEqualTo(5L);
  }

  @Test
  void shouldDecrementByOne() {
    atomicOps.set("counter", 10L);

    long result = atomicOps.decrement("counter");

    assertThat(result).isEqualTo(9L);
  }

  @Test
  void shouldDecrementByDelta() {
    atomicOps.set("counter", 10L);

    long result = atomicOps.decrementBy("counter", 3L);

    assertThat(result).isEqualTo(7L);
  }

  @Test
  void shouldGetCurrentValue() {
    atomicOps.set("counter", 42L);

    long value = atomicOps.get("counter");

    assertThat(value).isEqualTo(42L);
  }

  @Test
  void shouldReturnZeroForNonExistentKey() {
    long value = atomicOps.get("nonexistent");

    assertThat(value).isZero();
  }

  @Test
  void shouldResetCounterToZero() {
    atomicOps.set("counter", 99L);

    atomicOps.reset("counter");

    assertThat(atomicOps.get("counter")).isZero();
  }

  @Test
  void shouldSetIfAbsentWhenKeyDoesNotExist() {
    boolean acquired = atomicOps.setIfAbsent("lock", 1L, Duration.ofSeconds(10));

    assertThat(acquired).isTrue();
    assertThat(atomicOps.get("lock")).isEqualTo(1L);
  }

  @Test
  void shouldNotSetIfAbsentWhenKeyAlreadyExists() {
    atomicOps.setIfAbsent("lock", 1L, Duration.ofSeconds(10));

    boolean acquired = atomicOps.setIfAbsent("lock", 2L, Duration.ofSeconds(10));

    assertThat(acquired).isFalse();
    assertThat(atomicOps.get("lock")).isEqualTo(1L);
  }

  @Test
  void shouldApplyKeyPrefixOnAllOperations() {
    RedisAtomicOperations<String> prefixedOps =
        new RedisAtomicOperations<>(redisTemplate, "myapp:");

    prefixedOps.set("counter", 5L);
    long value = prefixedOps.get("counter");

    assertThat(value).isEqualTo(5L);
  }

  @Test
  void shouldThrowOnNullKeyForIncrement() {
    assertThatThrownBy(() -> atomicOps.increment(null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  void shouldThrowOnNullKeyForGet() {
    assertThatThrownBy(() -> atomicOps.get(null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  void shouldThrowOnNullValueForSetIfAbsent() {
    assertThatThrownBy(() -> atomicOps.setIfAbsent("key", null, Duration.ofSeconds(5)))
        .isInstanceOf(NullPointerException.class);
  }
}
