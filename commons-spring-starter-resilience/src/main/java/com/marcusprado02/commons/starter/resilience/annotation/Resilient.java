package com.marcusprado02.commons.starter.resilience.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Resilient {

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
