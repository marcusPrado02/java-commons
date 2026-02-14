package com.marcusprado02.commons.starter.resilience;

import com.marcusprado02.commons.adapters.resilience4j.Resilience4jExecutor;
import com.marcusprado02.commons.app.observability.MetricsFacade;
import com.marcusprado02.commons.app.resilience.NoopResilienceExecutor;
import com.marcusprado02.commons.app.resilience.ResilienceExecutor;
import com.marcusprado02.commons.starter.resilience.aop.ResilienceAspect;
import com.marcusprado02.commons.starter.resilience.props.ResilienceProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(ResilienceProperties.class)
public class ResilienceAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean(ResilienceExecutor.class)
  @ConditionalOnClass(name = "io.github.resilience4j.retry.Retry")
  public ResilienceExecutor resilienceExecutor(ObjectProvider<MetricsFacade> metricsFacade) {
    MetricsFacade metrics = metricsFacade.getIfAvailable(MetricsFacade::noop);
    return new Resilience4jExecutor(metrics);
  }

  @Bean
  @ConditionalOnMissingBean(ResilienceExecutor.class)
  public ResilienceExecutor noopResilienceExecutor() {
    return new NoopResilienceExecutor();
  }

  @Bean
  @ConditionalOnBean(ResilienceExecutor.class)
  @ConditionalOnClass(name = "org.aspectj.lang.ProceedingJoinPoint")
  @ConditionalOnProperty(
      prefix = "commons.resilience",
      name = "aop.enabled",
      havingValue = "true",
      matchIfMissing = true)
  public ResilienceAspect resilienceAspect(
      ResilienceExecutor resilienceExecutor, ResilienceProperties resilienceProperties) {
    return new ResilienceAspect(resilienceExecutor, resilienceProperties);
  }
}
