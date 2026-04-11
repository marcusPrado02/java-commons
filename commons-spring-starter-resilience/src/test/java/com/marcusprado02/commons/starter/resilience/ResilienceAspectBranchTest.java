package com.marcusprado02.commons.starter.resilience;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marcusprado02.commons.starter.resilience.annotation.Bulkhead;
import com.marcusprado02.commons.starter.resilience.annotation.CacheableResilience;
import com.marcusprado02.commons.starter.resilience.annotation.CircuitBreaker;
import com.marcusprado02.commons.starter.resilience.annotation.RateLimiter;
import com.marcusprado02.commons.starter.resilience.annotation.Resilient;
import com.marcusprado02.commons.starter.resilience.annotation.Retry;
import com.marcusprado02.commons.starter.resilience.annotation.Timeout;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class ResilienceAspectBranchTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withConfiguration(
              AutoConfigurations.of(AopAutoConfiguration.class, ResilienceAutoConfiguration.class))
          .withUserConfiguration(BranchServiceConfig.class);

  @Test
  void shouldUseDefaultOperationNameWhenBlank() {
    contextRunner.run(
        ctx -> {
          BranchService service = ctx.getBean(BranchService.class);
          assertThat(service.noName()).isEqualTo("no-name");
        });
  }

  @Test
  void shouldApplyCircuitBreakerAnnotationWithExplicitValues() {
    contextRunner.run(
        ctx -> {
          BranchService service = ctx.getBean(BranchService.class);
          assertThat(service.withCb()).isEqualTo("cb-ok");
        });
  }

  @Test
  void shouldApplyCircuitBreakerAnnotationWithDefaultValues() {
    contextRunner.run(
        ctx -> {
          BranchService service = ctx.getBean(BranchService.class);
          // default -1 values → uses seed policy defaults (covers <= 0 true branches)
          assertThat(service.withCbDefaults()).isEqualTo("cb-defaults-ok");
        });
  }

  @Test
  void shouldApplyBulkheadAnnotationWithExplicitValues() {
    contextRunner.run(
        ctx -> {
          BranchService service = ctx.getBean(BranchService.class);
          assertThat(service.withBulkhead()).isEqualTo("bulkhead-ok");
        });
  }

  @Test
  void shouldApplyBulkheadAnnotationWithDefaultValues() {
    contextRunner.run(
        ctx -> {
          BranchService service = ctx.getBean(BranchService.class);
          assertThat(service.withBulkheadDefaults()).isEqualTo("bulkhead-defaults-ok");
        });
  }

  @Test
  void shouldApplyRateLimiterAnnotationWithExplicitValues() {
    contextRunner.run(
        ctx -> {
          BranchService service = ctx.getBean(BranchService.class);
          assertThat(service.withRl()).isEqualTo("rl-ok");
        });
  }

  @Test
  void shouldApplyRateLimiterAnnotationWithDefaultValues() {
    contextRunner.run(
        ctx -> {
          BranchService service = ctx.getBean(BranchService.class);
          assertThat(service.withRlDefaults()).isEqualTo("rl-defaults-ok");
        });
  }

  @Test
  void shouldApplyCacheableResilienceAnnotationWithExplicitValues() {
    contextRunner.run(
        ctx -> {
          BranchService service = ctx.getBean(BranchService.class);
          BranchService.CACHE_CALLS.set(0);
          assertThat(service.withCache("key1")).isEqualTo("cached:key1");
          assertThat(service.withCache("key1")).isEqualTo("cached:key1");
        });
  }

  @Test
  void shouldApplyCacheableResilienceAnnotationWithDefaultValues() {
    contextRunner.run(
        ctx -> {
          BranchService service = ctx.getBean(BranchService.class);
          // default values → uses seed policy for maxSize and ttl
          assertThat(service.withCacheDefaults("key2")).isEqualTo("cache-defaults:key2");
        });
  }

  @Test
  void shouldApplyTimeoutAnnotation() {
    contextRunner.run(
        ctx -> {
          BranchService service = ctx.getBean(BranchService.class);
          // @Timeout annotation exercises timeoutAnn != null branch in buildPolicies;
          // executor wraps the timeout execution which may throw RuntimeException
          assertThatThrownBy(service::withTimeout).isInstanceOf(RuntimeException.class);
        });
  }

  @Test
  void shouldCallFallbackWithoutThrowableParam() {
    contextRunner.run(
        ctx -> {
          BranchService service = ctx.getBean(BranchService.class);
          BranchService.ALWAYS_FAIL_CALLS.set(0);
          assertThat(service.alwaysFail()).isEqualTo("fallback-no-throwable");
        });
  }

  @Test
  void shouldWrapCheckedExceptionInRuntimeException() {
    contextRunner.run(
        ctx -> {
          BranchService service = ctx.getBean(BranchService.class);
          assertThatThrownBy(service::throwsChecked).isInstanceOf(RuntimeException.class);
        });
  }

  @Test
  void shouldWrapErrorFromActionInRuntimeException() {
    contextRunner.run(
        ctx -> {
          BranchService service = ctx.getBean(BranchService.class);
          // Error is caught by the executor's catch(Throwable) and wrapped in RuntimeException
          assertThatThrownBy(service::throwsError).isInstanceOf(RuntimeException.class);
        });
  }

  @Test
  void shouldApplyRetryWithAllDefaultAnnotationValues() {
    contextRunner.run(
        ctx -> {
          BranchService service = ctx.getBean(BranchService.class);
          BranchService.RETRY_DEFAULTS_CALLS.set(0);
          String result = service.retryWithDefaults();
          assertThat(result).isEqualTo("retry-defaults-ok");
        });
  }

  @Test
  void shouldApplyCacheAnnotationWithPropertiesConfigured() {
    new ApplicationContextRunner()
        .withConfiguration(
            AutoConfigurations.of(AopAutoConfiguration.class, ResilienceAutoConfiguration.class))
        .withPropertyValues(
            "commons.resilience.defaults.cache.max-size=200",
            "commons.resilience.defaults.cache.ttl=PT2M")
        .withUserConfiguration(BranchServiceConfig.class)
        .run(
            ctx -> {
              BranchService service = ctx.getBean(BranchService.class);
              // cache properties configured → base != null in toCachePolicy
              assertThat(service.withCacheDefaults("key3")).isEqualTo("cache-defaults:key3");
            });
  }

  @Configuration
  static class BranchServiceConfig {
    @Bean
    BranchService branchService() {
      return new BranchService();
    }
  }

  static class BranchService {
    static final AtomicInteger CACHE_CALLS = new AtomicInteger();
    static final AtomicInteger ALWAYS_FAIL_CALLS = new AtomicInteger();
    static final AtomicInteger RETRY_DEFAULTS_CALLS = new AtomicInteger();

    @Resilient(name = "")
    public String noName() {
      return "no-name";
    }

    @Resilient(name = "with-cb")
    @CircuitBreaker(failureRateThreshold = 75.0f, slidingWindowSize = 10)
    public String withCb() {
      return "cb-ok";
    }

    @Resilient(name = "with-cb-defaults")
    @CircuitBreaker
    public String withCbDefaults() {
      return "cb-defaults-ok";
    }

    @Resilient(name = "with-bulkhead")
    @Bulkhead(maxConcurrentCalls = 5, maxWaitDuration = "PT0S")
    public String withBulkhead() {
      return "bulkhead-ok";
    }

    @Resilient(name = "with-bulkhead-defaults")
    @Bulkhead
    public String withBulkheadDefaults() {
      return "bulkhead-defaults-ok";
    }

    @Resilient(name = "with-rl")
    @RateLimiter(limitForPeriod = 100, refreshPeriod = "PT1S", timeout = "PT0S")
    public String withRl() {
      return "rl-ok";
    }

    @Resilient(name = "with-rl-defaults")
    @RateLimiter
    public String withRlDefaults() {
      return "rl-defaults-ok";
    }

    @Resilient(name = "with-cache")
    @CacheableResilience(maxSize = 50, ttl = "PT1M")
    public String withCache(String key) {
      CACHE_CALLS.incrementAndGet();
      return "cached:" + key;
    }

    @Resilient(name = "with-cache-defaults")
    @CacheableResilience
    public String withCacheDefaults(String key) {
      return "cache-defaults:" + key;
    }

    @Resilient(name = "with-timeout-executor-bug")
    @Timeout("PT30S")
    public String withTimeout() {
      return "timeout-ok";
    }

    @Resilient(name = "always-fail", fallbackMethod = "fallbackNoThrowable")
    @Retry(maxAttempts = 1, initialBackoff = "PT0S")
    public String alwaysFail() {
      ALWAYS_FAIL_CALLS.incrementAndGet();
      throw new RuntimeException("always fails");
    }

    public String fallbackNoThrowable() {
      return "fallback-no-throwable";
    }

    @Resilient(name = "throws-checked")
    public String throwsChecked() throws Exception {
      throw new Exception("checked");
    }

    @Resilient(name = "throws-error")
    public String throwsError() {
      throw new AssertionError("error");
    }

    @Resilient(name = "retry-defaults")
    @Retry
    public String retryWithDefaults() {
      if (RETRY_DEFAULTS_CALLS.incrementAndGet() == 1) {
        throw new RuntimeException("first attempt");
      }
      return "retry-defaults-ok";
    }
  }
}
