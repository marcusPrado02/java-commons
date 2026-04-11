package com.marcusprado02.commons.starter.resilience.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Applies circuit breaker resilience to the annotated method. */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CircuitBreaker {
  /** Failure rate threshold percentage; -1 uses the configured default. */
  float failureRateThreshold() default -1;

  /** Sliding window size for failure rate calculation; -1 uses the configured default. */
  int slidingWindowSize() default -1;
}
