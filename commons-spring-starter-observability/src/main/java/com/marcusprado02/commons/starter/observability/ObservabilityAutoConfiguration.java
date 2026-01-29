package com.marcusprado02.commons.starter.observability;

import com.marcusprado02.commons.adapters.web.spring.filter.CorrelationIdFilter;
import com.marcusprado02.commons.adapters.web.spring.filter.RequestContextFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class ObservabilityAutoConfiguration {

  @Bean
  public CorrelationIdFilter correlationIdFilter() {
    return new CorrelationIdFilter();
  }

  @Bean
  public RequestContextFilter requestContextFilter() {
    return new RequestContextFilter();
  }
}
