package com.marcusprado02.commons.starter.resilience;

import static org.assertj.core.api.Assertions.assertThat;

import com.marcusprado02.commons.app.resilience.ResilienceExecutor;
import com.marcusprado02.commons.starter.resilience.actuator.CircuitBreakersEndpoint;
import com.marcusprado02.commons.starter.resilience.actuator.ResilienceActuatorAutoConfiguration;
import com.marcusprado02.commons.starter.resilience.annotation.Resilient;
import com.marcusprado02.commons.starter.resilience.annotation.Retry;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class ResilienceAutoConfigurationTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withConfiguration(
              AutoConfigurations.of(AopAutoConfiguration.class, ResilienceAutoConfiguration.class));

  private final ApplicationContextRunner contextRunnerWithActuator =
      new ApplicationContextRunner()
          .withConfiguration(
              AutoConfigurations.of(
                  AopAutoConfiguration.class,
                  ResilienceAutoConfiguration.class,
                  ResilienceActuatorAutoConfiguration.class));

  @Test
  void shouldAutoConfigureResilienceExecutor() {
    contextRunner.run(
        ctx -> {
          assertThat(ctx).hasSingleBean(ResilienceExecutor.class);
        });
  }

  @Test
  void shouldApplyRetryAndFallbackViaAnnotations() {
    contextRunner
        .withUserConfiguration(TestServiceConfig.class)
        .run(
            ctx -> {
              TestService service = ctx.getBean(TestService.class);
              TestService.CALLS.set(0);
              String value = service.flaky("x");

              assertThat(value).isEqualTo("ok:x");
              assertThat(TestService.CALLS.get()).isEqualTo(2);
            });
  }

  @Test
  void shouldRegisterActuatorEndpointWhenActuatorIsPresent() {
    // Actuator is an optional dependency, but present in this module's classpath.
    contextRunnerWithActuator.run(
        ctx -> {
          assertThat(ctx).hasSingleBean(CircuitBreakersEndpoint.class);
        });
  }

  @Configuration
  static class TestServiceConfig {

    @Bean
    TestService testService() {
      return new TestService();
    }
  }

  static class TestService {

    static final AtomicInteger CALLS = new AtomicInteger();

    @Resilient(name = "demo", fallbackMethod = "fallback")
    @Retry(maxAttempts = 2, initialBackoff = "PT0S")
    public String flaky(String input) {
      if (CALLS.incrementAndGet() == 1) {
        throw new IllegalStateException("boom");
      }
      return "ok:" + input;
    }

    public String fallback(String input, Throwable cause) {
      return "fallback:" + input;
    }
  }
}
