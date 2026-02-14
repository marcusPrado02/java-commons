package com.marcusprado02.commons.spring.cache;

import static org.assertj.core.api.Assertions.assertThat;

import com.marcusprado02.commons.adapters.cache.memcached.MemcachedCacheAdapter;
import com.marcusprado02.commons.ports.cache.CachePort;
import net.spy.memcached.MemcachedClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class MemcachedCacheAutoConfigurationTest {

  @Container
  private static final GenericContainer<?> memcached =
      new GenericContainer<>(DockerImageName.parse("memcached:1.6-alpine")).withExposedPorts(11211);

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(MemcachedCacheAutoConfiguration.class));

  @Test
  void shouldAutoConfigureMemcachedCache() {
    contextRunner
        .withPropertyValues(
            "commons.cache.type=memcached",
            "commons.cache.memcached.host=" + memcached.getHost(),
            "commons.cache.memcached.port=" + memcached.getFirstMappedPort())
        .run(
            context -> {
              assertThat(context).hasSingleBean(MemcachedClient.class);
              assertThat(context).hasSingleBean(CachePort.class);

              CachePort<String, Object> cachePort = context.getBean(CachePort.class);
              assertThat(cachePort).isInstanceOf(MemcachedCacheAdapter.class);

              // Test cache operations
              cachePort.put("test-key", "test-value");
              Thread.sleep(100); // Memcached async
              assertThat(cachePort.get("test-key")).hasValue("test-value");
            });
  }

  @Test
  void shouldRespectKeyPrefix() {
    contextRunner
        .withPropertyValues(
            "commons.cache.type=memcached",
            "commons.cache.memcached.host=" + memcached.getHost(),
            "commons.cache.memcached.port=" + memcached.getFirstMappedPort(),
            "commons.cache.memcached.key-prefix=myapp:")
        .run(
            context -> {
              CachePort<String, Object> cachePort = context.getBean(CachePort.class);

              cachePort.put("key1", "value1");
              Thread.sleep(100);
              assertThat(cachePort.get("key1")).hasValue("value1");
            });
  }

  @Test
  void shouldNotAutoConfigureWhenDisabled() {
    contextRunner
        .withPropertyValues(
            "commons.cache.memcached.enabled=false",
            "commons.cache.memcached.host=" + memcached.getHost(),
            "commons.cache.memcached.port=" + memcached.getFirstMappedPort())
        .run(context -> assertThat(context).doesNotHaveBean(CachePort.class));
  }

  @Test
  void shouldNotAutoConfigureWhenTypeIsNotMemcached() {
    contextRunner
        .withPropertyValues(
            "commons.cache.type=redis",
            "commons.cache.memcached.host=" + memcached.getHost(),
            "commons.cache.memcached.port=" + memcached.getFirstMappedPort())
        .run(context -> assertThat(context).doesNotHaveBean(CachePort.class));
  }
}
