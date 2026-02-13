package com.marcusprado02.commons.starter.idempotency.web;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.ControllerAdvice;

@AutoConfiguration
@ConditionalOnClass(ControllerAdvice.class)
@ConditionalOnProperty(prefix = "commons.idempotency.web", name = "enabled", havingValue = "true")
public class IdempotencyWebExceptionHandlingAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public IdempotencyWebExceptionHandler idempotencyWebExceptionHandler() {
    return new IdempotencyWebExceptionHandler();
  }
}
