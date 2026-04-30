package com.marcusprado02.commons.starter.resilience;

import static org.assertj.core.api.Assertions.assertThat;

import com.marcusprado02.commons.app.resilience.ResilienceExecutor;
import com.marcusprado02.commons.starter.resilience.actuator.CircuitBreakersEndpoint;
import com.marcusprado02.commons.starter.resilience.props.ResilienceProperties;
import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class ResiliencePropertiesBranchTest {

  // --- Aop ---

  @Test
  void aop_setEnabled_and_isEnabled() {
    ResilienceProperties.Aop aop = new ResilienceProperties.Aop();
    aop.setEnabled(false);
    assertThat(aop.isEnabled()).isFalse();
    aop.setEnabled(true);
    assertThat(aop.isEnabled()).isTrue();
  }

  // --- Retry toPolicy branches ---

  @Test
  void retry_toPolicy_all_null_returns_null() {
    ResilienceProperties.Retry retry = new ResilienceProperties.Retry();
    assertThat(retry.toPolicy()).isNull();
  }

  @Test
  void retry_toPolicy_only_maxAttempts_set_uses_defaults_for_backoffs() {
    ResilienceProperties.Retry retry = new ResilienceProperties.Retry();
    retry.setMaxAttempts(5);
    assertThat(retry.toPolicy()).isNotNull();
    assertThat(retry.getMaxAttempts()).isEqualTo(5);
  }

  @Test
  void retry_toPolicy_only_initialBackoff_set_uses_it_as_max() {
    ResilienceProperties.Retry retry = new ResilienceProperties.Retry();
    retry.setInitialBackoff(Duration.ofMillis(200));
    assertThat(retry.toPolicy()).isNotNull();
    assertThat(retry.getInitialBackoff()).isEqualTo(Duration.ofMillis(200));
  }

  @Test
  void retry_toPolicy_only_maxBackoff_set_uses_defaults() {
    ResilienceProperties.Retry retry = new ResilienceProperties.Retry();
    retry.setMaxBackoff(Duration.ofSeconds(1));
    assertThat(retry.toPolicy()).isNotNull();
    assertThat(retry.getMaxBackoff()).isEqualTo(Duration.ofSeconds(1));
  }

  @Test
  void retry_setters_and_getters() {
    ResilienceProperties.Retry retry = new ResilienceProperties.Retry();
    retry.setMaxAttempts(3);
    retry.setInitialBackoff(Duration.ofMillis(50));
    retry.setMaxBackoff(Duration.ofSeconds(2));
    assertThat(retry.getMaxAttempts()).isEqualTo(3);
    assertThat(retry.getInitialBackoff()).isEqualTo(Duration.ofMillis(50));
    assertThat(retry.getMaxBackoff()).isEqualTo(Duration.ofSeconds(2));
  }

  // --- Timeout toPolicy branches ---

  @Test
  void timeout_toPolicy_null_returns_null() {
    ResilienceProperties.Timeout timeout = new ResilienceProperties.Timeout();
    assertThat(timeout.toPolicy()).isNull();
  }

  @Test
  void timeout_toPolicy_set_returns_policy() {
    ResilienceProperties.Timeout timeout = new ResilienceProperties.Timeout();
    timeout.setTimeout(Duration.ofSeconds(5));
    assertThat(timeout.toPolicy()).isNotNull();
    assertThat(timeout.getTimeout()).isEqualTo(Duration.ofSeconds(5));
  }

  // --- CircuitBreaker toPolicy branches ---

  @Test
  void circuitBreaker_toPolicy_all_null_returns_null() {
    ResilienceProperties.CircuitBreaker cb = new ResilienceProperties.CircuitBreaker();
    assertThat(cb.toPolicy()).isNull();
  }

  @Test
  void circuitBreaker_toPolicy_only_threshold_set() {
    ResilienceProperties.CircuitBreaker cb = new ResilienceProperties.CircuitBreaker();
    cb.setFailureRateThreshold(60.0f);
    assertThat(cb.toPolicy()).isNotNull();
    assertThat(cb.getFailureRateThreshold()).isEqualTo(60.0f);
  }

  @Test
  void circuitBreaker_toPolicy_only_window_set() {
    ResilienceProperties.CircuitBreaker cb = new ResilienceProperties.CircuitBreaker();
    cb.setSlidingWindowSize(15);
    assertThat(cb.toPolicy()).isNotNull();
    assertThat(cb.getSlidingWindowSize()).isEqualTo(15);
  }

  // --- Bulkhead toPolicy branches ---

  @Test
  void bulkhead_toPolicy_all_null_returns_null() {
    ResilienceProperties.Bulkhead bulkhead = new ResilienceProperties.Bulkhead();
    assertThat(bulkhead.toPolicy()).isNull();
  }

  @Test
  void bulkhead_toPolicy_only_maxConcurrentCalls_set() {
    ResilienceProperties.Bulkhead bulkhead = new ResilienceProperties.Bulkhead();
    bulkhead.setMaxConcurrentCalls(8);
    assertThat(bulkhead.toPolicy()).isNotNull();
    assertThat(bulkhead.getMaxConcurrentCalls()).isEqualTo(8);
  }

  @Test
  void bulkhead_toPolicy_only_maxWaitDuration_set() {
    ResilienceProperties.Bulkhead bulkhead = new ResilienceProperties.Bulkhead();
    bulkhead.setMaxWaitDuration(Duration.ofMillis(500));
    assertThat(bulkhead.toPolicy()).isNotNull();
    assertThat(bulkhead.getMaxWaitDuration()).isEqualTo(Duration.ofMillis(500));
  }

  // --- RateLimiter toPolicy branches ---

  @Test
  void rateLimiter_toPolicy_all_null_returns_null() {
    ResilienceProperties.RateLimiter rl = new ResilienceProperties.RateLimiter();
    assertThat(rl.toPolicy()).isNull();
  }

  @Test
  void rateLimiter_toPolicy_only_limitForPeriod_set() {
    ResilienceProperties.RateLimiter rl = new ResilienceProperties.RateLimiter();
    rl.setLimitForPeriod(50);
    assertThat(rl.toPolicy()).isNotNull();
    assertThat(rl.getLimitForPeriod()).isEqualTo(50);
  }

  @Test
  void rateLimiter_toPolicy_only_refreshPeriod_set() {
    ResilienceProperties.RateLimiter rl = new ResilienceProperties.RateLimiter();
    rl.setRefreshPeriod(Duration.ofSeconds(2));
    assertThat(rl.toPolicy()).isNotNull();
    assertThat(rl.getRefreshPeriod()).isEqualTo(Duration.ofSeconds(2));
  }

  @Test
  void rateLimiter_toPolicy_only_timeout_set() {
    ResilienceProperties.RateLimiter rl = new ResilienceProperties.RateLimiter();
    rl.setTimeout(Duration.ofMillis(100));
    assertThat(rl.toPolicy()).isNotNull();
    assertThat(rl.getTimeout()).isEqualTo(Duration.ofMillis(100));
  }

  // --- Cache toPolicy branches ---

  @Test
  void cache_toPolicy_all_null_returns_null() {
    ResilienceProperties.Cache cache = new ResilienceProperties.Cache();
    assertThat(cache.toPolicy()).isNull();
  }

  @Test
  void cache_toPolicy_only_maxSize_set() {
    ResilienceProperties.Cache cache = new ResilienceProperties.Cache();
    cache.setMaxSize(500);
    assertThat(cache.toPolicy()).isNotNull();
    assertThat(cache.getMaxSize()).isEqualTo(500);
  }

  @Test
  void cache_toPolicy_only_ttl_set() {
    ResilienceProperties.Cache cache = new ResilienceProperties.Cache();
    cache.setTtl(Duration.ofMinutes(10));
    assertThat(cache.toPolicy()).isNotNull();
    assertThat(cache.getTtl()).isEqualTo(Duration.ofMinutes(10));
  }

  // --- Defaults getters ---

  @Test
  void defaults_getters_return_non_null_inner_objects() {
    ResilienceProperties props = new ResilienceProperties();
    assertThat(props.getDefaults()).isNotNull();
    assertThat(props.getAop()).isNotNull();
    assertThat(props.getDefaults().getRetry()).isNotNull();
    assertThat(props.getDefaults().getTimeout()).isNotNull();
    assertThat(props.getDefaults().getCircuitBreaker()).isNotNull();
    assertThat(props.getDefaults().getBulkhead()).isNotNull();
    assertThat(props.getDefaults().getRateLimiter()).isNotNull();
    assertThat(props.getDefaults().getCache()).isNotNull();
  }

  @Test
  void toPolicySet_all_defaults_null_returns_set_with_null_policies() {
    ResilienceProperties props = new ResilienceProperties();
    assertThat(props.toPolicySet()).isNotNull();
  }

  // --- CircuitBreakersEndpoint non-Resilience4j fallback ---

  @Test
  void circuitBreakersEndpoint_returns_empty_for_non_resilience4j_executor() {
    ResilienceExecutor mockExecutor =
        new ResilienceExecutor() {
          @Override
          public void execute(String name, Runnable action) {
            action.run();
          }

          @Override
          public <T> T execute(String name, Supplier<T> action) {
            return action.get();
          }
        };
    CircuitBreakersEndpoint endpoint = new CircuitBreakersEndpoint(mockExecutor);
    assertThat(endpoint.circuitBreakers()).isEqualTo(List.of());
  }
}
