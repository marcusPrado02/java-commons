package com.marcusprado02.commons.starter.idempotency;

import com.marcusprado02.commons.app.idempotency.port.IdempotencyStorePort;
import com.marcusprado02.commons.app.idempotency.service.DefaultIdempotencyService;
import com.marcusprado02.commons.app.idempotency.service.DefaultIdempotentExecutor;
import com.marcusprado02.commons.app.idempotency.service.IdempotencyService;
import com.marcusprado02.commons.app.idempotency.service.IdempotentExecutor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(IdempotencyProperties.class)
public class IdempotencyAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean(IdempotencyService.class)
  public IdempotencyService idempotencyService(
      IdempotencyStorePort storePort, IdempotencyProperties properties) {
    return new DefaultIdempotencyService(storePort, properties.defaultTtl());
  }

  @Bean
  @ConditionalOnMissingBean(IdempotentExecutor.class)
  public IdempotentExecutor idempotentExecutor(
      IdempotencyStorePort storePort, IdempotencyProperties properties) {
    return new DefaultIdempotentExecutor(storePort, properties.defaultTtl());
  }
}
