package com.marcusprado02.commons.starter.observability;

import static org.assertj.core.api.Assertions.assertThat;

import com.marcusprado02.commons.app.observability.TracerFacade;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class MicrometerTracingAutoConfigurationTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(MicrometerTracingAutoConfiguration.class));

  @Test
  void shouldCreateTracerFacadeWhenMicrometerTracerBeanExists() {
    contextRunner
        .withUserConfiguration(TestTracerConfiguration.class)
        .run(
            ctx -> {
              assertThat(ctx).hasSingleBean(Tracer.class);
              assertThat(ctx).hasSingleBean(TracerFacade.class);
            });
  }

  @Test
  void shouldNotCreateTracerFacadeWhenMicrometerTracerIsMissing() {
    contextRunner.run(
        ctx -> {
          assertThat(ctx).doesNotHaveBean(Tracer.class);
          assertThat(ctx).doesNotHaveBean(TracerFacade.class);
        });
  }

  @Configuration
  static class TestTracerConfiguration {

    @Bean
    Tracer micrometerTracer() {
      return Mockito.mock(Tracer.class);
    }
  }
}
