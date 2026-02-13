package com.marcusprado02.commons.starter.observability;

import com.marcusprado02.commons.app.observability.TracerFacade;
import io.micrometer.tracing.Tracer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Bridges Micrometer Tracing to the commons {@link TracerFacade} abstraction.
 *
 * <p>This auto-config does not create a Micrometer {@link Tracer}; it only adapts an existing one
 * (provided by Spring Boot observability auto-config or by the application).
 */
@AutoConfiguration
@ConditionalOnClass(Tracer.class)
public class MicrometerTracingAutoConfiguration {

  @Bean
  @ConditionalOnBean(Tracer.class)
  @ConditionalOnMissingBean(TracerFacade.class)
  public TracerFacade micrometerTracerFacade(Tracer tracer) {
    return new MicrometerTracerFacade(tracer);
  }
}
