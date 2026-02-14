package com.marcusprado02.commons.starter.idempotency.aop;

import com.marcusprado02.commons.app.idempotency.model.IdempotencyKey;
import com.marcusprado02.commons.app.idempotency.service.IdempotencyResult;
import com.marcusprado02.commons.app.idempotency.service.IdempotencyService;
import com.marcusprado02.commons.starter.idempotency.IdempotencyProperties;
import com.marcusprado02.commons.starter.idempotency.annotation.Idempotent;
import com.marcusprado02.commons.starter.idempotency.exception.DuplicateIdempotencyKeyException;
import com.marcusprado02.commons.starter.idempotency.exception.IdempotencyInProgressException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Objects;
import java.util.function.Supplier;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.StringUtils;

@Aspect
public class IdempotencyAspect {

  private final IdempotencyService idempotencyService;
  private final IdempotencyProperties properties;

  private final ExpressionParser spel = new SpelExpressionParser();
  private final ParameterNameDiscoverer parameterNameDiscoverer =
      new DefaultParameterNameDiscoverer();

  public IdempotencyAspect(
      IdempotencyService idempotencyService, IdempotencyProperties properties) {
    this.idempotencyService =
        Objects.requireNonNull(idempotencyService, "idempotencyService must not be null");
    this.properties = Objects.requireNonNull(properties, "properties must not be null");
  }

  @Around("@annotation(idempotent)")
  public Object around(ProceedingJoinPoint pjp, Idempotent idempotent) throws Throwable {
    MethodSignature signature = (MethodSignature) pjp.getSignature();
    Method method = signature.getMethod();

    String keyExpression = idempotent.key();
    if (!StringUtils.hasText(keyExpression)) {
      return pjp.proceed();
    }

    StandardEvaluationContext baseContext = new StandardEvaluationContext();
    baseContext.setTypeLocator(
        new org.springframework.expression.spel.support.StandardTypeLocator(
            method.getDeclaringClass().getClassLoader()));

    Object[] args = pjp.getArgs();
    String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);
    if (parameterNames != null) {
      for (int i = 0; i < parameterNames.length; i++) {
        baseContext.setVariable(parameterNames[i], args[i]);
      }
    }
    for (int i = 0; i < args.length; i++) {
      baseContext.setVariable("p" + i, args[i]);
    }

    Expression keyExpr = spel.parseExpression(keyExpression);
    String rawKey = keyExpr.getValue(baseContext, String.class);
    IdempotencyKey key = new IdempotencyKey(rawKey);

    Duration ttl = parseTtl(idempotent.ttl());

    Supplier<Object> action =
        () -> {
          try {
            return pjp.proceed();
          } catch (Throwable t) {
            if (t instanceof Error err) {
              throw err;
            }
            if (t instanceof RuntimeException ex) {
              throw ex;
            }
            throw new RuntimeException(t);
          }
        };

    IdempotencyResult<Object> result =
        idempotencyService.execute(
            key,
            ttl,
            action,
            returned -> resolveResultRef(idempotent.resultRef(), baseContext, returned));

    if (result.executed()) {
      return result.value();
    }

    if (StringUtils.hasText(result.existingResultRef())) {
      throw new DuplicateIdempotencyKeyException(key.value(), result.existingResultRef());
    }
    throw new IdempotencyInProgressException(key.value());
  }

  private Duration parseTtl(String rawTtl) {
    if (!StringUtils.hasText(rawTtl)) {
      return properties.defaultTtl();
    }
    try {
      return Duration.parse(rawTtl);
    } catch (RuntimeException ignored) {
      return properties.defaultTtl();
    }
  }

  private String resolveResultRef(
      String expr, StandardEvaluationContext baseContext, Object result) {
    if (!StringUtils.hasText(expr)) {
      return null;
    }

    baseContext.setVariable("result", result);
    Expression resultExpr = spel.parseExpression(expr);
    return resultExpr.getValue(baseContext, String.class);
  }
}
