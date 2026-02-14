package com.marcusprado02.commons.starter.resilience.aop;

import com.marcusprado02.commons.app.resilience.BulkheadPolicy;
import com.marcusprado02.commons.app.resilience.CachePolicy;
import com.marcusprado02.commons.app.resilience.CircuitBreakerPolicy;
import com.marcusprado02.commons.app.resilience.FallbackStrategy;
import com.marcusprado02.commons.app.resilience.RateLimiterPolicy;
import com.marcusprado02.commons.app.resilience.ResilienceExecutor;
import com.marcusprado02.commons.app.resilience.ResiliencePolicySet;
import com.marcusprado02.commons.app.resilience.RetryPolicy;
import com.marcusprado02.commons.app.resilience.TimeoutPolicy;
import com.marcusprado02.commons.starter.resilience.annotation.CacheableResilience;
import com.marcusprado02.commons.starter.resilience.annotation.Resilient;
import com.marcusprado02.commons.starter.resilience.props.ResilienceProperties;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

@Aspect
public final class ResilienceAspect {

  private final ResilienceExecutor resilienceExecutor;
  private final ResilienceProperties resilienceProperties;
  private final ConcurrentMap<String, Method> fallbackMethodCache = new ConcurrentHashMap<>();

  public ResilienceAspect(
      ResilienceExecutor resilienceExecutor, ResilienceProperties resilienceProperties) {
    this.resilienceExecutor =
        Objects.requireNonNull(resilienceExecutor, "resilienceExecutor must not be null");
    this.resilienceProperties =
        Objects.requireNonNull(resilienceProperties, "resilienceProperties must not be null");
  }

  @Around("@annotation(resilient)")
  public Object aroundResilient(ProceedingJoinPoint joinPoint, Resilient resilient) {
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    Method method = signature.getMethod();

    String opName =
        (resilient.name() == null || resilient.name().isBlank())
            ? method.getDeclaringClass().getSimpleName() + "." + method.getName()
            : resilient.name().trim();

    ResiliencePolicySet policies = buildPolicies(method);
    FallbackStrategy<Object> fallback =
        buildFallback(
            joinPoint.getTarget(), method, resilient.fallbackMethod(), joinPoint.getArgs());

    Supplier<Object> action =
        () -> {
          try {
            return joinPoint.proceed();
          } catch (RuntimeException e) {
            throw e;
          } catch (Error e) {
            throw e;
          } catch (Throwable t) {
            throw new RuntimeException(t);
          }
        };

    CacheableResilience cacheable = method.getAnnotation(CacheableResilience.class);
    if (cacheable != null) {
      CachePolicy cachePolicy = toCachePolicy(cacheable);
      ResiliencePolicySet withCache =
          new ResiliencePolicySet(
              policies.retry(),
              policies.timeout(),
              policies.circuitBreaker(),
              policies.bulkhead(),
              policies.rateLimiter(),
              cachePolicy);
      Object key = Arrays.asList(opName, Arrays.deepToString(joinPoint.getArgs()));
      return resilienceExecutor.supplyCached(opName, withCache, key, action, fallback);
    }

    return resilienceExecutor.supply(opName, policies, action, fallback);
  }

  private ResiliencePolicySet buildPolicies(Method method) {
    ResiliencePolicySet base = resilienceProperties.toPolicySet();

    com.marcusprado02.commons.starter.resilience.annotation.Retry retryAnn =
        method.getAnnotation(com.marcusprado02.commons.starter.resilience.annotation.Retry.class);
    com.marcusprado02.commons.starter.resilience.annotation.Timeout timeoutAnn =
        method.getAnnotation(com.marcusprado02.commons.starter.resilience.annotation.Timeout.class);
    com.marcusprado02.commons.starter.resilience.annotation.CircuitBreaker cbAnn =
        method.getAnnotation(
            com.marcusprado02.commons.starter.resilience.annotation.CircuitBreaker.class);
    com.marcusprado02.commons.starter.resilience.annotation.Bulkhead bulkheadAnn =
        method.getAnnotation(
            com.marcusprado02.commons.starter.resilience.annotation.Bulkhead.class);
    com.marcusprado02.commons.starter.resilience.annotation.RateLimiter rlAnn =
        method.getAnnotation(
            com.marcusprado02.commons.starter.resilience.annotation.RateLimiter.class);

    RetryPolicy retry = base.retry();
    if (retryAnn != null) {
      RetryPolicy seed = (retry == null) ? defaultRetry() : retry;
      int maxAttempts = (retryAnn.maxAttempts() <= 0) ? seed.maxAttempts() : retryAnn.maxAttempts();
      Duration initialBackoff =
          (retryAnn.initialBackoff() == null || retryAnn.initialBackoff().isBlank())
              ? seed.initialBackoff()
              : Duration.parse(retryAnn.initialBackoff());
      retry = new RetryPolicy(maxAttempts, initialBackoff, seed.maxBackoff());
    }

    TimeoutPolicy timeout = base.timeout();
    if (timeoutAnn != null) {
      timeout = new TimeoutPolicy(Duration.parse(timeoutAnn.value()));
    }

    CircuitBreakerPolicy circuitBreaker = base.circuitBreaker();
    if (cbAnn != null) {
      CircuitBreakerPolicy seed =
          (circuitBreaker == null) ? defaultCircuitBreaker() : circuitBreaker;
      float threshold =
          (cbAnn.failureRateThreshold() <= 0)
              ? seed.failureRateThreshold()
              : cbAnn.failureRateThreshold();
      int window =
          (cbAnn.slidingWindowSize() <= 0) ? seed.slidingWindowSize() : cbAnn.slidingWindowSize();
      circuitBreaker = new CircuitBreakerPolicy(threshold, window);
    }

    BulkheadPolicy bulkhead = base.bulkhead();
    if (bulkheadAnn != null) {
      BulkheadPolicy seed = (bulkhead == null) ? defaultBulkhead() : bulkhead;
      int max =
          (bulkheadAnn.maxConcurrentCalls() <= 0)
              ? seed.maxConcurrentCalls()
              : bulkheadAnn.maxConcurrentCalls();
      Duration wait =
          (bulkheadAnn.maxWaitDuration() == null || bulkheadAnn.maxWaitDuration().isBlank())
              ? seed.maxWaitDuration()
              : Duration.parse(bulkheadAnn.maxWaitDuration());
      bulkhead = new BulkheadPolicy(max, wait);
    }

    RateLimiterPolicy rateLimiter = base.rateLimiter();
    if (rlAnn != null) {
      RateLimiterPolicy seed = (rateLimiter == null) ? defaultRateLimiter() : rateLimiter;
      int limit = (rlAnn.limitForPeriod() <= 0) ? seed.limitForPeriod() : rlAnn.limitForPeriod();
      Duration refresh =
          (rlAnn.refreshPeriod() == null || rlAnn.refreshPeriod().isBlank())
              ? seed.refreshPeriod()
              : Duration.parse(rlAnn.refreshPeriod());
      Duration timeoutDuration =
          (rlAnn.timeout() == null || rlAnn.timeout().isBlank())
              ? seed.timeout()
              : Duration.parse(rlAnn.timeout());
      rateLimiter = new RateLimiterPolicy(limit, refresh, timeoutDuration);
    }

    return new ResiliencePolicySet(
        retry, timeout, circuitBreaker, bulkhead, rateLimiter, base.cache());
  }

  private CachePolicy toCachePolicy(CacheableResilience cacheable) {
    CachePolicy base = resilienceProperties.toPolicySet().cache();
    CachePolicy seed = (base == null) ? defaultCache() : base;
    int maxSize = (cacheable.maxSize() <= 0) ? seed.maxSize() : cacheable.maxSize();
    Duration ttl =
        (cacheable.ttl() == null || cacheable.ttl().isBlank())
            ? seed.ttl()
            : Duration.parse(cacheable.ttl());
    return new CachePolicy(maxSize, ttl);
  }

  private RetryPolicy defaultRetry() {
    return new RetryPolicy(3, Duration.ofMillis(100), Duration.ofMillis(100));
  }

  private CircuitBreakerPolicy defaultCircuitBreaker() {
    return new CircuitBreakerPolicy(50.0f, 20);
  }

  private BulkheadPolicy defaultBulkhead() {
    return BulkheadPolicy.of(10);
  }

  private RateLimiterPolicy defaultRateLimiter() {
    return new RateLimiterPolicy(100, Duration.ofSeconds(1), Duration.ZERO);
  }

  private CachePolicy defaultCache() {
    return new CachePolicy(1_000, Duration.ofMinutes(5));
  }

  private FallbackStrategy<Object> buildFallback(
      Object target, Method original, String fallbackMethodName, Object[] args) {
    if (fallbackMethodName == null || fallbackMethodName.isBlank()) {
      return FallbackStrategy.none();
    }

    String key = target.getClass().getName() + "#" + original.getName() + "->" + fallbackMethodName;
    Method fallback =
        fallbackMethodCache.computeIfAbsent(
            key,
            ignored ->
                resolveFallbackMethod(
                    target.getClass(), fallbackMethodName.trim(), original.getParameterTypes()));

    return cause -> {
      try {
        Object[] params;
        if (fallback.getParameterCount() == args.length + 1
            && Throwable.class.isAssignableFrom(
                fallback.getParameterTypes()[fallback.getParameterCount() - 1])) {
          params = Arrays.copyOf(args, args.length + 1);
          params[params.length - 1] = cause;
        } else {
          params = args;
        }

        fallback.setAccessible(true);
        return fallback.invoke(target, params);
      } catch (RuntimeException e) {
        throw e;
      } catch (Throwable t) {
        throw new RuntimeException(t);
      }
    };
  }

  private Method resolveFallbackMethod(Class<?> targetClass, String name, Class<?>[] paramTypes) {
    try {
      return targetClass.getMethod(name, paramTypes);
    } catch (NoSuchMethodException ignored) {
      // Try with Throwable as last parameter
      Class<?>[] withThrowable = Arrays.copyOf(paramTypes, paramTypes.length + 1);
      withThrowable[withThrowable.length - 1] = Throwable.class;
      try {
        return targetClass.getMethod(name, withThrowable);
      } catch (NoSuchMethodException e) {
        throw new IllegalStateException(
            "Fallback method '" + name + "' not found on " + targetClass.getName(), e);
      }
    }
  }
}
