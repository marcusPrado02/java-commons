package com.marcusprado02.commons.starter.otel;

import com.marcusprado02.commons.adapters.otel.OtelTracerFacade;
import com.marcusprado02.commons.app.observability.TracerFacade;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class OtelAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean(TracerFacade.class)
  public TracerFacade tracerFacade() {
    return new OtelTracerFacade("com.marcusprado02.commons");
  }
}
