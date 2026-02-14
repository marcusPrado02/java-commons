package com.marcusprado02.commons.spring.cache;

import com.marcusprado02.commons.adapters.cache.memcached.MemcachedCacheAdapter;
import com.marcusprado02.commons.ports.cache.CachePort;
import java.net.InetSocketAddress;
import net.spy.memcached.MemcachedClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for Memcached cache adapter.
 *
 * <p>Activated when:
 *
 * <ul>
 *   <li>{@code commons.cache.type=memcached}
 *   <li>{@code MemcachedCacheAdapter} is on classpath
 *   <li>{@code commons.cache.memcached.enabled=true} (default)
 * </ul>
 */
@AutoConfiguration
@ConditionalOnClass(MemcachedCacheAdapter.class)
@ConditionalOnProperty(
    prefix = "commons.cache.memcached",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
@EnableConfigurationProperties(CacheProperties.class)
public class MemcachedCacheAutoConfiguration {

  private static final Logger log = LoggerFactory.getLogger(MemcachedCacheAutoConfiguration.class);

  @Bean(destroyMethod = "shutdown")
  @ConditionalOnMissingBean
  public MemcachedClient memcachedClient(CacheProperties properties) throws Exception {
    CacheProperties.Memcached memcached = properties.getMemcached();

    InetSocketAddress address = new InetSocketAddress(memcached.getHost(), memcached.getPort());
    MemcachedClient client = new MemcachedClient(address);

    log.info(
        "Configured Memcached connection: {}:{} (prefix: {})",
        memcached.getHost(),
        memcached.getPort(),
        memcached.getKeyPrefix().isBlank() ? "<none>" : memcached.getKeyPrefix());

    return client;
  }

  @Bean
  @ConditionalOnMissingBean(CachePort.class)
  @ConditionalOnProperty(prefix = "commons.cache", name = "type", havingValue = "memcached")
  public CachePort<String, Object> cachePort(
      MemcachedClient memcachedClient, CacheProperties properties) {
    String keyPrefix = properties.getMemcached().getKeyPrefix();

    MemcachedCacheAdapter<String, Object> adapter =
        new MemcachedCacheAdapter<>(memcachedClient, Object.class, keyPrefix);

    log.info("Created Memcached cache adapter with key prefix: '{}'", keyPrefix);

    return adapter;
  }
}
