package com.marcusprado02.commons.starter.resilience.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CircuitBreaker {
  float failureRateThreshold() default -1;

  int slidingWindowSize() default -1;
}
