package com.marcusprado02.commons.starter.observability;

import com.marcusprado02.commons.adapters.web.spring.filter.CorrelationIdFilter;
import com.marcusprado02.commons.adapters.web.spring.filter.RequestContextFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class ObservabilityAutoConfiguration {

  @Bean
  public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilter() {
    FilterRegistrationBean<CorrelationIdFilter> bean = new FilterRegistrationBean<>();
    bean.setFilter(new CorrelationIdFilter());
    bean.setOrder(0);
    return bean;
  }

  @Bean
  public FilterRegistrationBean<RequestContextFilter> requestContextFilter() {
    FilterRegistrationBean<RequestContextFilter> bean = new FilterRegistrationBean<>();
    bean.setFilter(new RequestContextFilter());
    bean.setOrder(1);
    return bean;
  }
}
