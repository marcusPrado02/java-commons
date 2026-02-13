package com.marcusprado02.commons.starter.idempotency.aop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marcusprado02.commons.app.idempotency.port.IdempotencyStorePort;
import com.marcusprado02.commons.app.idempotency.store.InMemoryIdempotencyStore;
import com.marcusprado02.commons.starter.idempotency.IdempotencyAutoConfiguration;
import com.marcusprado02.commons.starter.idempotency.annotation.Idempotent;
import com.marcusprado02.commons.starter.idempotency.exception.DuplicateIdempotencyKeyException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class IdempotencyAspectTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withPropertyValues("commons.idempotency.aop.enabled=true")
          .withConfiguration(
              AutoConfigurations.of(
                  AopAutoConfiguration.class,
                  IdempotencyAutoConfiguration.class,
                  IdempotencyAopAutoConfiguration.class))
          .withUserConfiguration(TestConfig.class);

  @Test
  void shouldApplyIdempotencyViaAnnotation() {
    contextRunner.run(
        ctx -> {
          TestService service = ctx.getBean(TestService.class);
          assertThat(AopUtils.isAopProxy(service)).isTrue();

          TestService.CALLS.set(0);
          String first = service.create("k1");
          assertThat(first).isEqualTo("r:k1");
          assertThat(TestService.CALLS.get()).isEqualTo(1);

          assertThatThrownBy(() -> service.create("k1"))
              .isInstanceOf(DuplicateIdempotencyKeyException.class);
          assertThat(TestService.CALLS.get()).isEqualTo(1);
        });
  }

  @Configuration
  static class TestConfig {
    @Bean
    IdempotencyStorePort idempotencyStorePort() {
      return new InMemoryIdempotencyStore();
    }

    @Bean
    TestService testService() {
      return new TestService();
    }
  }

  static class TestService {
    static final AtomicInteger CALLS = new AtomicInteger();

    @Idempotent(key = "#p0", resultRef = "#result")
    public String create(String key) {
      CALLS.incrementAndGet();
      return "r:" + key;
    }
  }
}
