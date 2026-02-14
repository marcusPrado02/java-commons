package com.marcusprado02.commons.kernel.errors;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Fluent builder for creating Problem instances with rich metadata and details.
 *
 * <p>Provides a convenient way to construct Problems with optional fields like details, metadata,
 * and custom timestamps.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * Problem problem = ProblemBuilder.of(StandardErrorCodes.VALIDATION_FAILED)
 *     .category(ErrorCategory.VALIDATION)
 *     .severity(Severity.ERROR)
 *     .message("Validation failed for user registration")
 *     .detail("email", "Email format is invalid")
 *     .detail("age", "Age must be 18 or older")
 *     .meta("requestId", "123-456")
 *     .meta("timestamp", Instant.now())
 *     .build();
 * }</pre>
 */
public final class ProblemBuilder {

  private ErrorCode code;
  private ErrorCategory category;
  private Severity severity;
  private String message;
  private final List<ProblemDetail> details = new ArrayList<>();
  private final Map<String, Object> meta = new HashMap<>();
  private Instant timestamp;

  private ProblemBuilder(ErrorCode code) {
    this.code = Objects.requireNonNull(code, "code");
  }

  /**
   * Creates a new ProblemBuilder with the specified error code.
   *
   * @param code the error code
   * @return a new ProblemBuilder
   */
  public static ProblemBuilder of(ErrorCode code) {
    return new ProblemBuilder(code);
  }

  /**
   * Creates a new ProblemBuilder with the specified error code string.
   *
   * @param code the error code string
   * @return a new ProblemBuilder
   */
  public static ProblemBuilder of(String code) {
    return new ProblemBuilder(ErrorCode.of(code));
  }

  /**
   * Creates a Problem Builder for validation errors.
   *
   * @param code the error code
   * @param message the error message
   * @return a new ProblemBuilder configured for validation
   */
  public static ProblemBuilder validation(ErrorCode code, String message) {
    return of(code).category(ErrorCategory.VALIDATION).severity(Severity.ERROR).message(message);
  }

  /**
   * Creates a ProblemBuilder for validation errors with string code.
   *
   * @param code the error code string
   * @param message the error message
   * @return a new ProblemBuilder configured for validation
   */
  public static ProblemBuilder validation(String code, String message) {
    return validation(ErrorCode.of(code), message);
  }

  /**
   * Creates a ProblemBuilder for business errors.
   *
   * @param code the error code
   * @param message the error message
   * @return a new ProblemBuilder configured for business errors
   */
  public static ProblemBuilder business(ErrorCode code, String message) {
    return of(code).category(ErrorCategory.BUSINESS).severity(Severity.ERROR).message(message);
  }

  /**
   * Creates a ProblemBuilder for business errors with string code.
   *
   * @param code the error code string
   * @param message the error message
   * @return a new ProblemBuilder configured for business errors
   */
  public static ProblemBuilder business(String code, String message) {
    return business(ErrorCode.of(code), message);
  }

  /**
   * Creates a ProblemBuilder for not found errors.
   *
   * @param code the error code
   * @param message the error message
   * @return a new ProblemBuilder configured for not found errors
   */
  public static ProblemBuilder notFound(ErrorCode code, String message) {
    return of(code).category(ErrorCategory.NOT_FOUND).severity(Severity.ERROR).message(message);
  }

  /**
   * Creates a ProblemBuilder for not found errors with string code.
   *
   * @param code the error code string
   * @param message the error message
   * @return a new ProblemBuilder configured for not found errors
   */
  public static ProblemBuilder notFound(String code, String message) {
    return notFound(ErrorCode.of(code), message);
  }

  /**
   * Creates a ProblemBuilder for technical errors.
   *
   * @param code the error code
   * @param message the error message
   * @return a new ProblemBuilder configured for technical errors
   */
  public static ProblemBuilder technical(ErrorCode code, String message) {
    return of(code).category(ErrorCategory.TECHNICAL).severity(Severity.CRITICAL).message(message);
  }

  /**
   * Creates a ProblemBuilder for technical errors with string code.
   *
   * @param code the error code string
   * @param message the error message
   * @return a new ProblemBuilder configured for technical errors
   */
  public static ProblemBuilder technical(String code, String message) {
    return technical(ErrorCode.of(code), message);
  }

  /**
   * Sets the error category.
   *
   * @param category the error category
   * @return this builder
   */
  public ProblemBuilder category(ErrorCategory category) {
    this.category = category;
    return this;
  }

  /**
   * Sets the severity.
   *
   * @param severity the severity
   * @return this builder
   */
  public ProblemBuilder severity(Severity severity) {
    this.severity = severity;
    return this;
  }

  /**
   * Sets the error message.
   *
   * @param message the error message
   * @return this builder
   */
  public ProblemBuilder message(String message) {
    this.message = message;
    return this;
  }

  /**
   * Adds a detail entry.
   *
   * @param field the field name
   * @param message the detail message
   * @return this builder
   */
  public ProblemBuilder detail(String field, String message) {
    this.details.add(ProblemDetail.of(field, message));
    return this;
  }

  /**
   * Adds a detail entry with metadata.
   *
   * @param field the field name
   * @param message the detail message
   * @param meta the metadata
   * @return this builder
   */
  public ProblemBuilder detail(String field, String message, Map<String, Object> meta) {
    this.details.add(new ProblemDetail(field, message, meta));
    return this;
  }

  /**
   * Adds a ProblemDetail.
   *
   * @param detail the problem detail
   * @return this builder
   */
  public ProblemBuilder detail(ProblemDetail detail) {
    this.details.add(detail);
    return this;
  }

  /**
   * Adds multiple ProblemDetails.
   *
   * @param details the problem details
   * @return this builder
   */
  public ProblemBuilder details(List<ProblemDetail> details) {
    this.details.addAll(details);
    return this;
  }

  /**
   * Adds a metadata entry.
   *
   * @param key the metadata key
   * @param value the metadata value
   * @return this builder
   */
  public ProblemBuilder meta(String key, Object value) {
    this.meta.put(key, value);
    return this;
  }

  /**
   * Adds multiple metadata entries.
   *
   * @param meta the metadata map
   * @return this builder
   */
  public ProblemBuilder meta(Map<String, Object> meta) {
    this.meta.putAll(meta);
    return this;
  }

  /**
   * Sets a custom timestamp.
   *
   * @param timestamp the timestamp
   * @return this builder
   */
  public ProblemBuilder timestamp(Instant timestamp) {
    this.timestamp = timestamp;
    return this;
  }

  /**
   * Adds a correlation ID to metadata.
   *
   * @param correlationId the correlation ID
   * @return this builder
   */
  public ProblemBuilder correlationId(String correlationId) {
    return meta("correlationId", correlationId);
  }

  /**
   * Adds a trace ID to metadata.
   *
   * @param traceId the trace ID
   * @return this builder
   */
  public ProblemBuilder traceId(String traceId) {
    return meta("traceId", traceId);
  }

  /**
   * Adds userId to metadata.
   *
   * @param userId the user ID
   * @return this builder
   */
  public ProblemBuilder userId(String userId) {
    return meta("userId", userId);
  }

  /**
   * Adds tenantId to metadata.
   *
   * @param tenantId the tenant ID
   * @return this builder
   */
  public ProblemBuilder tenantId(String tenantId) {
    return meta("tenantId", tenantId);
  }

  /**
   * Adds a help URL to metadata.
   *
   * @param helpUrl the help URL
   * @return this builder
   */
  public ProblemBuilder helpUrl(String helpUrl) {
    return meta("helpUrl", helpUrl);
  }

  /**
   * Builds the Problem instance.
   *
   * @return a new Problem
   * @throws NullPointerException if required fields are null
   */
  public Problem build() {
    Objects.requireNonNull(code, "code is required");
    Objects.requireNonNull(category, "category is required");
    Objects.requireNonNull(severity, "severity is required");
    Objects.requireNonNull(message, "message is required");

    return new Problem(code, category, severity, message, details, meta, timestamp);
  }

  /**
   * Builds and throws a DomainException with the constructed Problem.
   *
   * @throws DomainException always
   */
  public void throwIt() {
    throw new DomainException(build());
  }

  /**
   * Builds and throws a specific exception type with the constructed Problem.
   *
   * @param exceptionFactory factory to create the exception
   * @param <E> exception type
   * @throws E always
   */
  public <E extends DomainException> void throwAs(
      java.util.function.Function<Problem, E> exceptionFactory) {
    throw exceptionFactory.apply(build());
  }
}
