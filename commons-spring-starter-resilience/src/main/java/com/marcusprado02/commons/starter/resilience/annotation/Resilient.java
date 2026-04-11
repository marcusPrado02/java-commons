package com.marcusprado02.commons.starter.resilience.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method for composite resilience (retry, circuit breaker, bulkhead, timeout, rate
 * limiting, and caching) via the resilience AOP aspect.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Resilient {

  /** Optional name for the resilience operation; defaults to ClassName.methodName. */
  String name() default "";

  /**
   * Name of a fallback method on the same bean.
   *
   * <p>Signature options:
   *
   * <ul>
   *   <li>same parameters as the annotated method
   *   <li>same parameters + (Throwable)
   * </ul>
   */
  String fallbackMethod() default "";
}
