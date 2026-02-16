package com.marcusprado02.commons.app.auditlog.aop;

import com.marcusprado02.commons.app.auditlog.ActorContext;
import com.marcusprado02.commons.app.auditlog.ActorProvider;
import com.marcusprado02.commons.app.auditlog.AuditEvent;
import com.marcusprado02.commons.app.auditlog.AuditService;
import com.marcusprado02.commons.app.auditlog.Audited;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

/**
 * AOP aspect for automatic audit logging.
 *
 * <p>Intercepts methods annotated with {@link Audited} and creates audit events.
 *
 * <h2>Spring Configuration</h2>
 *
 * <pre>{@code
 * @Configuration
 * @EnableAspectJAutoProxy
 * public class AuditConfig {
 *
 *     @Bean
 *     public AuditAspect auditAspect(AuditService auditService, ActorProvider actorProvider) {
 *         return new AuditAspect(auditService, actorProvider);
 *     }
 * }
 * }</pre>
 */
@Aspect
public class AuditAspect {

  private final AuditService auditService;
  private final ActorProvider actorProvider;

  public AuditAspect(AuditService auditService, ActorProvider actorProvider) {
    this.auditService = Objects.requireNonNull(auditService, "auditService cannot be null");
    this.actorProvider = Objects.requireNonNull(actorProvider, "actorProvider cannot be null");
  }

  @Around("@annotation(com.marcusprado02.commons.app.auditlog.Audited)")
  public Object auditMethod(ProceedingJoinPoint joinPoint) throws Throwable {
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    Method method = signature.getMethod();
    Audited audited = method.getAnnotation(Audited.class);

    Object result = null;
    Throwable error = null;

    try {
      result = joinPoint.proceed();
      return result;
    } catch (Throwable t) {
      error = t;
      throw t;
    } finally {
      if (error == null || audited.auditOnFailure()) {
        createAndRecordAuditEvent(audited, joinPoint, result, error);
      }
    }
  }

  private void createAndRecordAuditEvent(
      Audited audited, ProceedingJoinPoint joinPoint, Object result, Throwable error) {

    String actor = actorProvider.getCurrentActor();
    ActorContext actorContext = actorProvider.getActorContext();

    AuditEvent.Builder eventBuilder =
        AuditEvent.builder().eventType(audited.eventType()).actor(actor).action(audited.action());

    if (!audited.resourceType().isEmpty()) {
      eventBuilder.resourceType(audited.resourceType());
    }

    // Extract resource ID
    String resourceId = extractResourceId(audited, joinPoint, result);
    if (resourceId != null) {
      eventBuilder.resourceId(resourceId);
    }

    // Add actor context
    if (actorContext != null) {
      if (actorContext.getIpAddress() != null) {
        eventBuilder.ipAddress(actorContext.getIpAddress());
      }
      if (actorContext.getUserAgent() != null) {
        eventBuilder.userAgent(actorContext.getUserAgent());
      }
    }

    // Add metadata
    Map<String, Object> metadata = new HashMap<>();

    if (audited.includeParameters()) {
      addParameters(metadata, joinPoint);
    }

    if (audited.includeResult() && result != null) {
      metadata.put("result", result);
    }

    if (!metadata.isEmpty()) {
      eventBuilder.metadata(metadata);
    }

    // Set result status
    if (error != null) {
      eventBuilder.result("FAILURE").errorMessage(error.getMessage());
    } else {
      eventBuilder.result("SUCCESS");
    }

    AuditEvent event = eventBuilder.build();
    auditService.auditAsync(event);
  }

  private String extractResourceId(Audited audited, ProceedingJoinPoint joinPoint, Object result) {
    // Try parameter name
    if (!audited.resourceIdParam().isEmpty()) {
      return extractFromParameter(audited.resourceIdParam(), joinPoint);
    }

    // Try expression (simplified - could use SpEL in real implementation)
    if (!audited.resourceIdExpression().isEmpty()) {
      return evaluateExpression(audited.resourceIdExpression(), joinPoint, result);
    }

    return null;
  }

  private String extractFromParameter(String paramName, ProceedingJoinPoint joinPoint) {
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    Parameter[] parameters = signature.getMethod().getParameters();
    Object[] args = joinPoint.getArgs();

    for (int i = 0; i < parameters.length; i++) {
      if (parameters[i].getName().equals(paramName)) {
        return args[i] != null ? args[i].toString() : null;
      }
    }

    return null;
  }

  private String evaluateExpression(
      String expression, ProceedingJoinPoint joinPoint, Object result) {
    // Simplified implementation - real version would use SpEL
    if (expression.startsWith("#result.")) {
      String property = expression.substring("#result.".length());
      return extractProperty(result, property);
    }
    return null;
  }

  private String extractProperty(Object obj, String propertyName) {
    if (obj == null) {
      return null;
    }

    try {
      String methodName =
          "get" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
      Method method = obj.getClass().getMethod(methodName);
      Object value = method.invoke(obj);
      return value != null ? value.toString() : null;
    } catch (Exception e) {
      return null;
    }
  }

  private void addParameters(Map<String, Object> metadata, ProceedingJoinPoint joinPoint) {
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    String[] paramNames = signature.getParameterNames();
    Object[] args = joinPoint.getArgs();

    Map<String, Object> params = new HashMap<>();
    for (int i = 0; i < paramNames.length; i++) {
      params.put(paramNames[i], args[i]);
    }

    metadata.put("parameters", params);
  }
}
