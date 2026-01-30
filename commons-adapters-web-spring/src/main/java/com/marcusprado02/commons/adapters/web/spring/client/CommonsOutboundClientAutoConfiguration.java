package com.marcusprado02.commons.adapters.web.spring.client;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class CommonsOutboundClientAutoConfiguration {

  @Bean
  RestTemplateContextInterceptor restTemplateContextInterceptor() {
    return new RestTemplateContextInterceptor();
  }

  @Bean
  @ConditionalOnClass(RestTemplate.class)
  RestTemplateCustomizer commonsRestTemplateCustomizer(RestTemplateContextInterceptor interceptor) {
    return restTemplate -> restTemplate.getInterceptors().add(interceptor);
  }
}
