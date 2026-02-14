package com.marcusprado02.commons.spring.cache;

import static org.assertj.core.api.Assertions.assertThat;

import com.marcusprado02.commons.adapters.cache.redis.RedisCacheAdapter;
import com.marcusprado02.commons.ports.cache.CachePort;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class RedisCacheAutoConfigurationTest {

  @Container
  private static final GenericContainer<?> redis =
      new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(RedisCacheAutoConfiguration.class));

  @Test
  void shouldAutoConfigureRedisCache() {
    contextRunner
        .withPropertyValues(
            "commons.cache.type=redis",
            "commons.cache.redis.host=" + redis.getHost(),
            "commons.cache.redis.port=" + redis.getFirstMappedPort())
        .run(
            context -> {
              assertThat(context).hasSingleBean(LettuceConnectionFactory.class);
              assertThat(context).hasSingleBean(RedisTemplate.class);
              assertThat(context).hasSingleBean(CachePort.class);

              CachePort<String, Object> cachePort = context.getBean(CachePort.class);
              assertThat(cachePort).isInstanceOf(RedisCacheAdapter.class);

              // Test cache operations
              cachePort.put("test-key", "test-value");
              assertThat(cachePort.get("test-key")).hasValue("test-value");
            });
  }

  @Test
  void shouldRespectKeyPrefix() {
    contextRunner
        .withPropertyValues(
            "commons.cache.type=redis",
            "commons.cache.redis.host=" + redis.getHost(),
            "commons.cache.redis.port=" + redis.getFirstMappedPort(),
            "commons.cache.redis.key-prefix=myapp:")
        .run(
            context -> {
              CachePort<String, Object> cachePort = context.getBean(CachePort.class);

              cachePort.put("key1", "value1");
              assertThat(cachePort.get("key1")).hasValue("value1");
            });
  }

  @Test
  void shouldNotAutoConfigureWhenDisabled() {
    contextRunner
        .withPropertyValues(
            "commons.cache.redis.enabled=false",
            "commons.cache.redis.host=" + redis.getHost(),
            "commons.cache.redis.port=" + redis.getFirstMappedPort())
        .run(context -> assertThat(context).doesNotHaveBean(CachePort.class));
  }

  @Test
  void shouldNotAutoConfigureWhenTypeIsNotRedis() {
    contextRunner
        .withPropertyValues(
            "commons.cache.type=memcached",
            "commons.cache.redis.host=" + redis.getHost(),
            "commons.cache.redis.port=" + redis.getFirstMappedPort())
        .run(context -> assertThat(context).doesNotHaveBean(CachePort.class));
  }

  @Test
  void shouldUseCustomRedisTemplate() {
    contextRunner
        .withPropertyValues(
            "commons.cache.type=redis",
            "commons.cache.redis.host=" + redis.getHost(),
            "commons.cache.redis.port=" + redis.getFirstMappedPort())
        .withUserConfiguration(CustomRedisTemplateConfiguration.class)
        .run(
            context -> {
              assertThat(context).hasSingleBean(RedisTemplate.class);
              assertThat(context).getBean("customRedisTemplate").isNotNull();
            });
  }

  static class CustomRedisTemplateConfiguration {
    // Custom configuration if needed for testing
  }
}
