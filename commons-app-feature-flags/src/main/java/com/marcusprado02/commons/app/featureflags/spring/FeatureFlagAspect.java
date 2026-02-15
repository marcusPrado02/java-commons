package com.marcusprado02.commons.app.featureflags.spring;

import com.marcusprado02.commons.app.featureflags.FeatureFlagContext;
import com.marcusprado02.commons.app.featureflags.FeatureFlagService;
import java.lang.reflect.Method;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * Aspect for feature flag-based method execution control.
 *
 * <p>Intercepts methods annotated with {@link FeatureFlag} and evaluates the feature flag before
 * allowing execution.
 */
@Aspect
public class FeatureFlagAspect {

  private final FeatureFlagService featureFlagService;
  private final ExpressionParser parser = new SpelExpressionParser();

  public FeatureFlagAspect(FeatureFlagService featureFlagService) {
    this.featureFlagService = featureFlagService;
  }

  @Around("@annotation(featureFlag)")
  public Object aroundFeatureFlaggedMethod(ProceedingJoinPoint joinPoint, FeatureFlag featureFlag)
      throws Throwable {

    String featureKey = featureFlag.key();

    // Build context
    FeatureFlagContext context = buildContext(joinPoint, featureFlag);

    // Check if feature is enabled
    boolean enabled = featureFlagService.isEnabled(featureKey, context);

    if (enabled) {
      return joinPoint.proceed();
    }

    // Feature is disabled - apply fallback strategy
    return handleFallback(joinPoint, featureFlag);
  }

  private FeatureFlagContext buildContext(ProceedingJoinPoint joinPoint, FeatureFlag featureFlag) {
    String userIdExpression = featureFlag.userId();

    if (userIdExpression.isEmpty()) {
      return FeatureFlagContext.anonymous();
    }

    // Evaluate SpEL expression
    String userId = evaluateExpression(userIdExpression, joinPoint);
    return FeatureFlagContext.forUser(userId);
  }

  private String evaluateExpression(String expression, ProceedingJoinPoint joinPoint) {
    try {
      Expression expr = parser.parseExpression(expression);
      EvaluationContext context = createEvaluationContext(joinPoint);
      Object value = expr.getValue(context);
      return value != null ? value.toString() : null;
    } catch (Exception e) {
      throw new RuntimeException("Failed to evaluate expression: " + expression, e);
    }
  }

  private EvaluationContext createEvaluationContext(ProceedingJoinPoint joinPoint) {
    StandardEvaluationContext context = new StandardEvaluationContext();

    // Add method parameters
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    String[] parameterNames = signature.getParameterNames();
    Object[] args = joinPoint.getArgs();

    for (int i = 0; i < parameterNames.length; i++) {
      context.setVariable(parameterNames[i], args[i]);
    }

    return context;
  }

  private Object handleFallback(ProceedingJoinPoint joinPoint, FeatureFlag featureFlag)
      throws Throwable {
    return switch (featureFlag.fallback()) {
      case THROW_EXCEPTION -> throw new FeatureFlagDisabledException(featureFlag.key());
      case RETURN_NULL -> null;
      case RETURN_DEFAULT -> getDefaultValue(joinPoint);
      case CALL_METHOD -> callFallbackMethod(joinPoint, featureFlag);
    };
  }

  private Object getDefaultValue(ProceedingJoinPoint joinPoint) {
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    Class<?> returnType = signature.getReturnType();

    if (returnType == void.class || returnType == Void.class) {
      return null;
    }
    if (returnType == boolean.class) {
      return false;
    }
    if (returnType == byte.class) {
      return (byte) 0;
    }
    if (returnType == short.class) {
      return (short) 0;
    }
    if (returnType == int.class) {
      return 0;
    }
    if (returnType == long.class) {
      return 0L;
    }
    if (returnType == float.class) {
      return 0.0f;
    }
    if (returnType == double.class) {
      return 0.0;
    }
    if (returnType == char.class) {
      return '\u0000';
    }
    return null;
  }

  private Object callFallbackMethod(ProceedingJoinPoint joinPoint, FeatureFlag featureFlag)
      throws Throwable {
    String fallbackMethodName = featureFlag.fallbackMethod();

    if (fallbackMethodName.isEmpty()) {
      throw new IllegalArgumentException(
          "fallbackMethod must be specified when using CALL_METHOD strategy");
    }

    Object target = joinPoint.getTarget();
    Object[] args = joinPoint.getArgs();

    try {
      Method fallbackMethod = findFallbackMethod(target.getClass(), fallbackMethodName, args);
      return fallbackMethod.invoke(target, args);
    } catch (Exception e) {
      throw new RuntimeException("Failed to call fallback method: " + fallbackMethodName, e);
    }
  }

  private Method findFallbackMethod(Class<?> targetClass, String methodName, Object[] args)
      throws NoSuchMethodException {
    Class<?>[] parameterTypes = new Class[args.length];
    for (int i = 0; i < args.length; i++) {
      parameterTypes[i] = args[i] != null ? args[i].getClass() : Object.class;
    }

    try {
      return targetClass.getDeclaredMethod(methodName, parameterTypes);
    } catch (NoSuchMethodException e) {
      // Try to find method with compatible parameter types
      for (Method method : targetClass.getDeclaredMethods()) {
        if (method.getName().equals(methodName)
            && method.getParameterCount() == args.length) {
          return method;
        }
      }
      throw e;
    }
  }
}
