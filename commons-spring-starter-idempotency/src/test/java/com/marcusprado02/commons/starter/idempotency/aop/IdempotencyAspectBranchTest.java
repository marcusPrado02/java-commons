package com.marcusprado02.commons.starter.idempotency.aop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marcusprado02.commons.app.idempotency.port.IdempotencyStorePort;
import com.marcusprado02.commons.app.idempotency.store.InMemoryIdempotencyStore;
import com.marcusprado02.commons.starter.idempotency.IdempotencyAutoConfiguration;
import com.marcusprado02.commons.starter.idempotency.annotation.Idempotent;
import com.marcusprado02.commons.starter.idempotency.exception.IdempotencyInProgressException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class IdempotencyAspectBranchTest {

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
  void shouldProceedDirectlyWhenKeyExpressionIsEmpty() {
    contextRunner.run(
        ctx -> {
          BranchService service = ctx.getBean(BranchService.class);
          BranchService.CALLS.set(0);

          // noKey annotation has empty key → aspect calls pjp.proceed() directly
          String r1 = service.noKey("x");
          String r2 = service.noKey("x");
          assertThat(r1).isEqualTo("no-key:x");
          assertThat(r2).isEqualTo("no-key:x");
          assertThat(BranchService.CALLS.get()).isEqualTo(2); // both calls executed
        });
  }

  @Test
  void shouldRespectCustomTtlOnAnnotation() {
    contextRunner.run(
        ctx -> {
          BranchService service = ctx.getBean(BranchService.class);
          BranchService.CALLS.set(0);

          // explicit valid TTL on annotation
          String result = service.withTtl("t1");
          assertThat(result).isEqualTo("ttl:t1");
          assertThat(BranchService.CALLS.get()).isEqualTo(1);
        });
  }

  @Test
  void shouldFallBackToDefaultTtlWhenTtlIsInvalid() {
    contextRunner.run(
        ctx -> {
          BranchService service = ctx.getBean(BranchService.class);
          BranchService.CALLS.set(0);

          // invalid TTL on annotation → parseTtl catches RuntimeException, uses default
          String result = service.withBadTtl("b1");
          assertThat(result).isEqualTo("bad-ttl:b1");
          assertThat(BranchService.CALLS.get()).isEqualTo(1);
        });
  }

  @Test
  void shouldThrowIdempotencyInProgressExceptionWhenNoResultRef() {
    contextRunner.run(
        ctx -> {
          BranchService service = ctx.getBean(BranchService.class);
          // noResultRef has no resultRef expression
          service.noResultRef("np1"); // acquires lock

          // Second call: not executed, no resultRef → IdempotencyInProgressException
          assertThatThrownBy(() -> service.noResultRef("np1"))
              .isInstanceOf(IdempotencyInProgressException.class);
        });
  }

  @Test
  void shouldWrapCheckedExceptionsFromAction() {
    contextRunner.run(
        ctx -> {
          BranchService service = ctx.getBean(BranchService.class);
          assertThatThrownBy(() -> service.throwsChecked("ck1"))
              .isInstanceOf(RuntimeException.class);
        });
  }

  @Configuration
  static class TestConfig {
    @Bean
    IdempotencyStorePort idempotencyStorePort() {
      return new InMemoryIdempotencyStore();
    }

    @Bean
    BranchService branchService() {
      return new BranchService();
    }
  }

  static class BranchService {
    static final AtomicInteger CALLS = new AtomicInteger();

    @Idempotent(key = "")
    public String noKey(String input) {
      CALLS.incrementAndGet();
      return "no-key:" + input;
    }

    @Idempotent(key = "#p0", ttl = "PT10S", resultRef = "#result")
    public String withTtl(String input) {
      CALLS.incrementAndGet();
      return "ttl:" + input;
    }

    @Idempotent(key = "#p0", ttl = "NOT_A_DURATION", resultRef = "#result")
    public String withBadTtl(String input) {
      CALLS.incrementAndGet();
      return "bad-ttl:" + input;
    }

    @Idempotent(key = "#p0")
    public String noResultRef(String input) {
      return "no-ref:" + input;
    }

    @Idempotent(key = "#p0", resultRef = "#result")
    public String throwsChecked(String input) throws Exception {
      throw new Exception("checked");
    }
  }
}
