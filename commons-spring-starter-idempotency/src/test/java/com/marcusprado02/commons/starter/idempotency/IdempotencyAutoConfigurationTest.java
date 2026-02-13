package com.marcusprado02.commons.starter.idempotency;

import static org.assertj.core.api.Assertions.assertThat;

import com.marcusprado02.commons.app.idempotency.port.IdempotencyStorePort;
import com.marcusprado02.commons.app.idempotency.service.IdempotencyService;
import com.marcusprado02.commons.app.idempotency.store.InMemoryIdempotencyStore;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class IdempotencyAutoConfigurationTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(IdempotencyAutoConfiguration.class))
          .withUserConfiguration(TestStoreConfig.class);

  @Test
  void shouldAutoConfigureIdempotencyService() {
    contextRunner.run(
        ctx -> {
          assertThat(ctx).hasSingleBean(IdempotencyService.class);
          assertThat(ctx).hasSingleBean(IdempotencyStorePort.class);
        });
  }

  @Configuration
  static class TestStoreConfig {
    @Bean
    IdempotencyStorePort idempotencyStorePort() {
      return new InMemoryIdempotencyStore();
    }
  }
}
