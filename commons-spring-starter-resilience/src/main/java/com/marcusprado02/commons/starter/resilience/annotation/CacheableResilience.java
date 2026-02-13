package com.marcusprado02.commons.starter.resilience.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheableResilience {
  int maxSize() default -1;

  /** ISO-8601 duration, e.g. PT5M */
  String ttl() default "";
}
