package com.marcusprado02.commons.starter.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marcusprado02.commons.app.outbox.OutboxSerializer;
import com.marcusprado02.commons.app.outbox.config.OutboxProcessorConfig;
import com.marcusprado02.commons.app.outbox.metrics.OutboxMetrics;
import com.marcusprado02.commons.app.outbox.port.OutboxRepositoryPort;
import com.marcusprado02.commons.kernel.ddd.context.ActorProvider;
import com.marcusprado02.commons.kernel.ddd.context.CorrelationProvider;
import com.marcusprado02.commons.kernel.ddd.context.TenantProvider;
import com.marcusprado02.commons.kernel.ddd.time.ClockProvider;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class OutboxAutoConfigurationTest {

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(OutboxAutoConfiguration.class))
          .withUserConfiguration(TestConfig.class);

  @Test
  void shouldCreateOutboxRepositoryPort() {
    runner.run(
        context -> {
          assertThat(context).hasSingleBean(OutboxRepositoryPort.class);
        });
  }

  @Test
  void shouldCreateOutboxSerializer() {
    runner.run(
        context -> {
          assertThat(context).hasSingleBean(OutboxSerializer.class);
        });
  }

  @Test
  void shouldCreateOutboxProcessorConfig() {
    runner
        .withPropertyValues(
            "commons.outbox.processing.batch-size=20",
            "commons.outbox.retry.max-attempts=3",
            "commons.outbox.retry.initial-backoff=200ms")
        .run(
            context -> {
              assertThat(context).hasSingleBean(OutboxProcessorConfig.class);
              var config = context.getBean(OutboxProcessorConfig.class);
              assertThat(config.batchSize()).isEqualTo(20);
              assertThat(config.maxAttempts()).isEqualTo(3);
              assertThat(config.initialBackoff().toMillis()).isEqualTo(200);
            });
  }

  @Test
  void shouldNotCreateHealthIndicatorWhenDisabled() {
    runner
        .withPropertyValues("commons.outbox.health.enabled=false")
        .run(
            context -> {
              assertThat(context).doesNotHaveBean(HealthIndicator.class);
            });
  }

  @Configuration
  static class TestConfig {

    @Bean
    EntityManager entityManager() {
      return org.mockito.Mockito.mock(EntityManager.class);
    }

    @Bean
    ObjectMapper objectMapper() {
      return new ObjectMapper();
    }

    @Bean
    TenantProvider tenantProvider() {
      return () -> null;
    }

    @Bean
    CorrelationProvider correlationProvider() {
      return () -> null;
    }

    @Bean
    ActorProvider actorProvider() {
      return () -> null;
    }

    @Bean
    ClockProvider clockProvider() {
      return ClockProvider.system();
    }

    @Bean
    OutboxMetrics outboxMetrics() {
      return com.marcusprado02.commons.app.outbox.metrics.NoOpOutboxMetrics.INSTANCE;
    }
  }
}
