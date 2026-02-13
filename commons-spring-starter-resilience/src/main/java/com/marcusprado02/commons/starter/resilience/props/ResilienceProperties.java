package com.marcusprado02.commons.starter.resilience.props;

import com.marcusprado02.commons.app.resilience.BulkheadPolicy;
import com.marcusprado02.commons.app.resilience.CachePolicy;
import com.marcusprado02.commons.app.resilience.CircuitBreakerPolicy;
import com.marcusprado02.commons.app.resilience.RateLimiterPolicy;
import com.marcusprado02.commons.app.resilience.ResiliencePolicySet;
import com.marcusprado02.commons.app.resilience.RetryPolicy;
import com.marcusprado02.commons.app.resilience.TimeoutPolicy;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "commons.resilience")
public class ResilienceProperties {

  private final Defaults defaults = new Defaults();
  private final Aop aop = new Aop();

  public Defaults getDefaults() {
    return defaults;
  }

  public Aop getAop() {
    return aop;
  }

  public ResiliencePolicySet toPolicySet() {
    return new ResiliencePolicySet(
        defaults.retry.toPolicy(),
        defaults.timeout.toPolicy(),
        defaults.circuitBreaker.toPolicy(),
        defaults.bulkhead.toPolicy(),
        defaults.rateLimiter.toPolicy(),
        defaults.cache.toPolicy());
  }

  public static final class Aop {
    private boolean enabled = true;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }
  }

  public static final class Defaults {
    private final Retry retry = new Retry();
    private final Timeout timeout = new Timeout();
    private final CircuitBreaker circuitBreaker = new CircuitBreaker();
    private final Bulkhead bulkhead = new Bulkhead();
    private final RateLimiter rateLimiter = new RateLimiter();
    private final Cache cache = new Cache();

    public Retry getRetry() {
      return retry;
    }

    public Timeout getTimeout() {
      return timeout;
    }

    public CircuitBreaker getCircuitBreaker() {
      return circuitBreaker;
    }

    public Bulkhead getBulkhead() {
      return bulkhead;
    }

    public RateLimiter getRateLimiter() {
      return rateLimiter;
    }

    public Cache getCache() {
      return cache;
    }
  }

  public static final class Retry {
    private Integer maxAttempts;
    private Duration initialBackoff;
    private Duration maxBackoff;

    public Integer getMaxAttempts() {
      return maxAttempts;
    }

    public void setMaxAttempts(Integer maxAttempts) {
      this.maxAttempts = maxAttempts;
    }

    public Duration getInitialBackoff() {
      return initialBackoff;
    }

    public void setInitialBackoff(Duration initialBackoff) {
      this.initialBackoff = initialBackoff;
    }

    public Duration getMaxBackoff() {
      return maxBackoff;
    }

    public void setMaxBackoff(Duration maxBackoff) {
      this.maxBackoff = maxBackoff;
    }

    RetryPolicy toPolicy() {
      if (maxAttempts == null && initialBackoff == null && maxBackoff == null) {
        return null;
      }
      int attempts = (maxAttempts == null) ? 3 : maxAttempts;
      Duration initial = (initialBackoff == null) ? Duration.ofMillis(100) : initialBackoff;
      Duration max = (maxBackoff == null) ? initial : maxBackoff;
      return new RetryPolicy(attempts, initial, max);
    }
  }

  public static final class Timeout {
    private Duration timeout;

    public Duration getTimeout() {
      return timeout;
    }

    public void setTimeout(Duration timeout) {
      this.timeout = timeout;
    }

    TimeoutPolicy toPolicy() {
      return (timeout == null) ? null : new TimeoutPolicy(timeout);
    }
  }

  public static final class CircuitBreaker {
    private Float failureRateThreshold;
    private Integer slidingWindowSize;

    public Float getFailureRateThreshold() {
      return failureRateThreshold;
    }

    public void setFailureRateThreshold(Float failureRateThreshold) {
      this.failureRateThreshold = failureRateThreshold;
    }

    public Integer getSlidingWindowSize() {
      return slidingWindowSize;
    }

    public void setSlidingWindowSize(Integer slidingWindowSize) {
      this.slidingWindowSize = slidingWindowSize;
    }

    CircuitBreakerPolicy toPolicy() {
      if (failureRateThreshold == null && slidingWindowSize == null) {
        return null;
      }
      float threshold = (failureRateThreshold == null) ? 50.0f : failureRateThreshold;
      int window = (slidingWindowSize == null) ? 20 : slidingWindowSize;
      return new CircuitBreakerPolicy(threshold, window);
    }
  }

  public static final class Bulkhead {
    private Integer maxConcurrentCalls;
    private Duration maxWaitDuration;

    public Integer getMaxConcurrentCalls() {
      return maxConcurrentCalls;
    }

    public void setMaxConcurrentCalls(Integer maxConcurrentCalls) {
      this.maxConcurrentCalls = maxConcurrentCalls;
    }

    public Duration getMaxWaitDuration() {
      return maxWaitDuration;
    }

    public void setMaxWaitDuration(Duration maxWaitDuration) {
      this.maxWaitDuration = maxWaitDuration;
    }

    BulkheadPolicy toPolicy() {
      if (maxConcurrentCalls == null && maxWaitDuration == null) {
        return null;
      }
      int maxCalls = (maxConcurrentCalls == null) ? 10 : maxConcurrentCalls;
      Duration wait = (maxWaitDuration == null) ? Duration.ZERO : maxWaitDuration;
      return new BulkheadPolicy(maxCalls, wait);
    }
  }

  public static final class RateLimiter {
    private Integer limitForPeriod;
    private Duration refreshPeriod;
    private Duration timeout;

    public Integer getLimitForPeriod() {
      return limitForPeriod;
    }

    public void setLimitForPeriod(Integer limitForPeriod) {
      this.limitForPeriod = limitForPeriod;
    }

    public Duration getRefreshPeriod() {
      return refreshPeriod;
    }

    public void setRefreshPeriod(Duration refreshPeriod) {
      this.refreshPeriod = refreshPeriod;
    }

    public Duration getTimeout() {
      return timeout;
    }

    public void setTimeout(Duration timeout) {
      this.timeout = timeout;
    }

    RateLimiterPolicy toPolicy() {
      if (limitForPeriod == null && refreshPeriod == null && timeout == null) {
        return null;
      }
      int limit = (limitForPeriod == null) ? 100 : limitForPeriod;
      Duration refresh = (refreshPeriod == null) ? Duration.ofSeconds(1) : refreshPeriod;
      Duration t = (timeout == null) ? Duration.ZERO : timeout;
      return new RateLimiterPolicy(limit, refresh, t);
    }
  }

  public static final class Cache {
    private Integer maxSize;
    private Duration ttl;

    public Integer getMaxSize() {
      return maxSize;
    }

    public void setMaxSize(Integer maxSize) {
      this.maxSize = maxSize;
    }

    public Duration getTtl() {
      return ttl;
    }

    public void setTtl(Duration ttl) {
      this.ttl = ttl;
    }

    CachePolicy toPolicy() {
      if (maxSize == null && ttl == null) {
        return null;
      }
      int size = (maxSize == null) ? 1_000 : maxSize;
      Duration duration = (ttl == null) ? Duration.ofMinutes(5) : ttl;
      return new CachePolicy(size, duration);
    }
  }
}
