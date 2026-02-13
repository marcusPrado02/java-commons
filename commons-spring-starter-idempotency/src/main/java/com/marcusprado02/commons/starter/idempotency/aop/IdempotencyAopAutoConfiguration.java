package com.marcusprado02.commons.starter.idempotency.aop;

import com.marcusprado02.commons.app.idempotency.service.IdempotencyService;
import com.marcusprado02.commons.starter.idempotency.IdempotencyProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(name = "org.aspectj.lang.ProceedingJoinPoint")
@ConditionalOnProperty(prefix = "commons.idempotency.aop", name = "enabled", havingValue = "true")
public class IdempotencyAopAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public IdempotencyAspect idempotencyAspect(
      IdempotencyService idempotencyService, IdempotencyProperties properties) {
    return new IdempotencyAspect(idempotencyService, properties);
  }
}
