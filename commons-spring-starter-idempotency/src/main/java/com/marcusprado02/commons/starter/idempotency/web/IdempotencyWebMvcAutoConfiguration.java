package com.marcusprado02.commons.starter.idempotency.web;

import com.marcusprado02.commons.app.idempotency.port.IdempotencyStorePort;
import com.marcusprado02.commons.starter.idempotency.IdempotencyProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@AutoConfiguration
@ConditionalOnClass(HandlerInterceptor.class)
@ConditionalOnProperty(prefix = "commons.idempotency.web", name = "enabled", havingValue = "true")
public class IdempotencyWebMvcAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public IdempotencyHandlerInterceptor idempotencyHandlerInterceptor(
      IdempotencyStorePort storePort, IdempotencyProperties properties) {
    return new IdempotencyHandlerInterceptor(storePort, properties);
  }

  @Bean
  @ConditionalOnMissingBean(name = "idempotencyWebMvcConfigurer")
  public WebMvcConfigurer idempotencyWebMvcConfigurer(IdempotencyHandlerInterceptor interceptor) {
    return new WebMvcConfigurer() {
      @Override
      public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor);
      }
    };
  }
}
