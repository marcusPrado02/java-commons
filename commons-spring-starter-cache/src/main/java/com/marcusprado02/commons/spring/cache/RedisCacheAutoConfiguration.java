package com.marcusprado02.commons.spring.cache;

import com.marcusprado02.commons.adapters.cache.redis.RedisCacheAdapter;
import com.marcusprado02.commons.ports.cache.CachePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Auto-configuration for Redis cache adapter.
 *
 * <p>Activated when:
 *
 * <ul>
 *   <li>{@code commons.cache.type=redis} (default)
 *   <li>{@code RedisCacheAdapter} is on classpath
 *   <li>{@code commons.cache.redis.enabled=true} (default)
 * </ul>
 */
@AutoConfiguration
@ConditionalOnClass(RedisCacheAdapter.class)
@ConditionalOnProperty(
    prefix = "commons.cache.redis",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
@EnableConfigurationProperties(CacheProperties.class)
public class RedisCacheAutoConfiguration {

  private static final Logger log = LoggerFactory.getLogger(RedisCacheAutoConfiguration.class);

  @Bean
  @ConditionalOnMissingBean
  public LettuceConnectionFactory redisConnectionFactory(CacheProperties properties) {
    CacheProperties.Redis redis = properties.getRedis();

    RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
    config.setHostName(redis.getHost());
    config.setPort(redis.getPort());

    if (redis.getPassword() != null && !redis.getPassword().isBlank()) {
      config.setPassword(redis.getPassword());
    }

    LettuceConnectionFactory factory = new LettuceConnectionFactory(config);
    log.info(
        "Configured Redis connection: {}:{} (prefix: {})",
        redis.getHost(),
        redis.getPort(),
        redis.getKeyPrefix().isBlank() ? "<none>" : redis.getKeyPrefix());

    return factory;
  }

  @Bean
  @ConditionalOnMissingBean(name = "cacheRedisTemplate")
  public RedisTemplate<String, Object> cacheRedisTemplate(
      LettuceConnectionFactory connectionFactory) {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);

    // Use String serializer for keys
    StringRedisSerializer stringSerializer = new StringRedisSerializer();
    template.setKeySerializer(stringSerializer);
    template.setHashKeySerializer(stringSerializer);

    // Use JSON serializer for values
    GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();
    template.setValueSerializer(jsonSerializer);
    template.setHashValueSerializer(jsonSerializer);

    template.afterPropertiesSet();

    return template;
  }

  @Bean
  @ConditionalOnMissingBean(CachePort.class)
  @ConditionalOnProperty(
      prefix = "commons.cache",
      name = "type",
      havingValue = "redis",
      matchIfMissing = true)
  public CachePort<String, Object> cachePort(
      RedisTemplate<String, Object> redisTemplate, CacheProperties properties) {
    String keyPrefix = properties.getRedis().getKeyPrefix();

    RedisCacheAdapter<String, Object> adapter = new RedisCacheAdapter<>(redisTemplate, keyPrefix);

    log.info("Created Redis cache adapter with key prefix: '{}'", keyPrefix);

    return adapter;
  }
}
