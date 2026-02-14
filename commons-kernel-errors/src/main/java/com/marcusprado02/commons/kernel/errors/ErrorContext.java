package com.marcusprado02.commons.kernel.errors;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Helper class for building rich error context metadata.
 *
 * <p>Provides commonly used context fields for errors like correlation IDs, user information,
 * timestamps, and custom metadata.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * Map<String, Object> context = ErrorContext.builder()
 *     .correlationId("req-123-456")
 *     .userId("user-789")
 *     .tenantId("tenant-abc")
 *     .operation("createOrder")
 *     .resource("Order", "order-123")
 *     .put("ipAddress", "192.168.1.1")
 *     .build();
 * }</pre>
 */
public final class ErrorContext {

  private final Map<String, Object> context;

  private ErrorContext() {
    this.context = new HashMap<>();
  }

  /** Creates a new ErrorContext builder. */
  public static ErrorContext builder() {
    return new ErrorContext();
  }

  /** Creates an empty context map. */
  public static Map<String, Object> empty() {
    return Map.of();
  }

  /**
   * Adds a correlation ID.
   *
   * @param correlationId the correlation ID
   * @return this builder
   */
  public ErrorContext correlationId(String correlationId) {
    context.put("correlationId", correlationId);
    return this;
  }

  /**
   * Adds a trace ID (for distributed tracing).
   *
   * @param traceId the trace ID
   * @return this builder
   */
  public ErrorContext traceId(String traceId) {
    context.put("traceId", traceId);
    return this;
  }

  /**
   * Adds a span ID (for distributed tracing).
   *
   * @param spanId the span ID
   * @return this builder
   */
  public ErrorContext spanId(String spanId) {
    context.put("spanId", spanId);
    return this;
  }

  /**
   * Adds user ID.
   *
   * @param userId the user ID
   * @return this builder
   */
  public ErrorContext userId(String userId) {
    context.put("userId", userId);
    return this;
  }

  /**
   * Adds tenant ID (for multi-tenant applications).
   *
   * @param tenantId the tenant ID
   * @return this builder
   */
  public ErrorContext tenantId(String tenantId) {
    context.put("tenantId", tenantId);
    return this;
  }

  /**
   * Adds session ID.
   *
   * @param sessionId the session ID
   * @return this builder
   */
  public ErrorContext sessionId(String sessionId) {
    context.put("sessionId", sessionId);
    return this;
  }

  /**
   * Adds operation name.
   *
   * @param operation the operation being performed
   * @return this builder
   */
  public ErrorContext operation(String operation) {
    context.put("operation", operation);
    return this;
  }

  /**
   * Adds resource information.
   *
   * @param resourceType the type of resource
   * @param resourceId the resource ID
   * @return this builder
   */
  public ErrorContext resource(String resourceType, String resourceId) {
    context.put("resourceType", resourceType);
    context.put("resourceId", resourceId);
    return this;
  }

  /**
   * Adds timestamp.
   *
   * @param timestamp the timestamp
   * @return this builder
   */
  public ErrorContext timestamp(Instant timestamp) {
    context.put("timestamp", timestamp);
    return this;
  }

  /**
   * Adds current timestamp.
   *
   * @return this builder
   */
  public ErrorContext now() {
    return timestamp(Instant.now());
  }

  /**
   * Adds HTTP method.
   *
   * @param method the HTTP method
   * @return this builder
   */
  public ErrorContext httpMethod(String method) {
    context.put("httpMethod", method);
    return this;
  }

  /**
   * Adds HTTP path.
   *
   * @param path the HTTP path
   * @return this builder
   */
  public ErrorContext httpPath(String path) {
    context.put("httpPath", path);
    return this;
  }

  /**
   * Adds IP address.
   *
   * @param ipAddress the IP address
   * @return this builder
   */
  public ErrorContext ipAddress(String ipAddress) {
    context.put("ipAddress", ipAddress);
    return this;
  }

  /**
   * Adds user agent.
   *
   * @param userAgent the user agent
   * @return this builder
   */
  public ErrorContext userAgent(String userAgent) {
    context.put("userAgent", userAgent);
    return this;
  }

  /**
   * Adds environment information.
   *
   * @param environment the environment (e.g., "production", "staging")
   * @return this builder
   */
  public ErrorContext environment(String environment) {
    context.put("environment", environment);
    return this;
  }

  /**
   * Adds application version.
   *
   * @param version the application version
   * @return this builder
   */
  public ErrorContext appVersion(String version) {
    context.put("appVersion", version);
    return this;
  }

  /**
   * Adds a help/documentation URL.
   *
   * @param url the help URL
   * @return this builder
   */
  public ErrorContext helpUrl(String url) {
    context.put("helpUrl", url);
    return this;
  }

  /**
   * Adds exception class name.
   *
   * @param exceptionClass the exception class
   * @return this builder
   */
  public ErrorContext exceptionClass(Class<? extends Throwable> exceptionClass) {
    context.put("exceptionClass", exceptionClass.getName());
    return this;
  }

  /**
   * Adds exception message.
   *
   * @param message the exception message
   * @return this builder
   */
  public ErrorContext exceptionMessage(String message) {
    context.put("exceptionMessage", message);
    return this;
  }

  /**
   * Adds a custom key-value pair.
   *
   * @param key the key
   * @param value the value
   * @return this builder
   */
  public ErrorContext put(String key, Object value) {
    Objects.requireNonNull(key, "key");
    context.put(key, value);
    return this;
  }

  /**
   * Adds all entries from a map.
   *
   * @param map the map to add
   * @return this builder
   */
  public ErrorContext putAll(Map<String, Object> map) {
    Objects.requireNonNull(map, "map");
    context.putAll(map);
    return this;
  }

  /**
   * Builds and returns an immutable copy of the context map.
   *
   * @return immutable context map
   */
  public Map<String, Object> build() {
    return Map.copyOf(context);
  }

  /**
   * Builds and wraps the context in an ErrorEnvelope with the given Problem.
   *
   * @param problem the problem
   * @return error envelope
   */
  public ErrorEnvelope wrapProblem(Problem problem) {
    String correlationId = (String) context.get("correlationId");
    return new ErrorEnvelope(problem, correlationId, build());
  }
}
