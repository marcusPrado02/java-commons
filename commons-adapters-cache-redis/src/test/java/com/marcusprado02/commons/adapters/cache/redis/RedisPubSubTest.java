package com.marcusprado02.commons.adapters.cache.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class RedisPubSubTest {

  @Container
  static GenericContainer<?> redis =
      new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

  private static LettuceConnectionFactory connectionFactory;
  private static RedisTemplate<String, String> redisTemplate;
  private RedisMessageListenerContainer listenerContainer;
  private RedisPubSub<String, String> pubSub;

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
    listenerContainer = new RedisMessageListenerContainer();
    listenerContainer.setConnectionFactory(connectionFactory);
    listenerContainer.afterPropertiesSet();
    listenerContainer.start();

    pubSub = new RedisPubSub<>(redisTemplate, listenerContainer, "test-channel");
  }

  @AfterEach
  void tearDown() {
    if (listenerContainer.isRunning()) {
      listenerContainer.stop();
    }
  }

  @Test
  void shouldReturnChannelName() {
    assertThat(pubSub.getChannelName()).isEqualTo("test-channel");
  }

  @Test
  void shouldPublishAndReceiveMessage() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<String> received = new AtomicReference<>();

    pubSub.subscribe(
        msg -> {
          received.set(msg);
          latch.countDown();
        });

    // Small delay to ensure subscription is registered before publishing
    Thread.sleep(100);

    pubSub.publish("hello-world");

    boolean delivered = latch.await(3, TimeUnit.SECONDS);
    assertThat(delivered).isTrue();
    assertThat(received.get()).isEqualTo("hello-world");
  }

  @Test
  void shouldUnsubscribeFromChannel() {
    pubSub.subscribe(msg -> {});

    pubSub.unsubscribe();

    assertThat(listenerContainer.isRunning()).isFalse();
  }

  @Test
  void shouldThrowOnNullRedisTemplate() {
    assertThatThrownBy(() -> new RedisPubSub<>(null, listenerContainer, "channel"))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void shouldThrowOnNullListenerContainer() {
    assertThatThrownBy(() -> new RedisPubSub<>(redisTemplate, null, "channel"))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void shouldThrowOnNullChannelName() {
    assertThatThrownBy(() -> new RedisPubSub<>(redisTemplate, listenerContainer, null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void shouldThrowOnNullMessageHandler() {
    assertThatThrownBy(() -> pubSub.subscribe(null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  void shouldThrowOnNullPublishedMessage() {
    assertThatThrownBy(() -> pubSub.publish(null)).isInstanceOf(NullPointerException.class);
  }
}
