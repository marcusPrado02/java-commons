package com.marcusprado02.commons.starter.resilience.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Bulkhead {
  int maxConcurrentCalls() default -1;

  /** ISO-8601 duration, e.g. PT0S */
  String maxWaitDuration() default "";
}
