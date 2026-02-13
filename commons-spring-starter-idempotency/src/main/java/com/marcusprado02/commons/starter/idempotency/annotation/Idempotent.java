package com.marcusprado02.commons.starter.idempotency.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

  /**
   * SpEL expression that resolves an idempotency key.
   *
   * <p>Examples: {@code "#p0"}, {@code "#request.id"}, {@code "'order:' + #p0"}
   */
  String key();

  /**
   * TTL to use for this key. If empty, uses {@code commons.idempotency.default-ttl}.
   *
   * <p>Format: {@link java.time.Duration#parse(String)} (e.g., {@code "PT5M"}).
   */
  String ttl() default "";

  /**
   * SpEL expression that resolves a result reference from the return value.
   *
   * <p>Example: {@code "#result"}.
   */
  String resultRef() default "";
}
