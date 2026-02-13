package com.marcusprado02.commons.starter.resilience.actuator;

import com.marcusprado02.commons.app.resilience.ResilienceExecutor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(name = "org.springframework.boot.actuate.endpoint.annotation.Endpoint")
public class ResilienceActuatorAutoConfiguration {

  @Bean
  @ConditionalOnBean(ResilienceExecutor.class)
  public CircuitBreakersEndpoint circuitBreakersEndpoint(ResilienceExecutor resilienceExecutor) {
    return new CircuitBreakersEndpoint(resilienceExecutor);
  }
}
