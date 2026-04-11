package com.marcusprado02.commons.starter.resilience.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Applies in-memory caching with resilience semantics to the annotated method. */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheableResilience {
  /** Maximum cache size; -1 uses the configured default. */
  int maxSize() default -1;

  /** ISO-8601 duration, e.g. PT5M */
  String ttl() default "";
}
