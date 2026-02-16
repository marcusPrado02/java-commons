package com.marcusprado02.commons.app.validation;

import java.util.Objects;

/**
 * Represents a validation violation.
 *
 * <p>Contains information about what failed, where it failed, and why.
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * ValidationViolation violation = ValidationViolation.builder()
 *     .field("email")
 *     .value("invalid-email")
 *     .message("Email must be valid")
 *     .code("INVALID_EMAIL")
 *     .build();
 * }</pre>
 */
public final class ValidationViolation {

  private final String field;
  private final Object value;
  private final String message;
  private final String code;
  private final ViolationSeverity severity;

  private ValidationViolation(Builder builder) {
    this.field = Objects.requireNonNull(builder.field, "field cannot be null");
    this.value = builder.value;
    this.message = Objects.requireNonNull(builder.message, "message cannot be null");
    this.code = builder.code;
    this.severity = builder.severity != null ? builder.severity : ViolationSeverity.ERROR;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getField() {
    return field;
  }

  public Object getValue() {
    return value;
  }

  public String getMessage() {
    return message;
  }

  public String getCode() {
    return code;
  }

  public ViolationSeverity getSeverity() {
    return severity;
  }

  @Override
  public String toString() {
    return String.format("%s [%s]: %s (value=%s, code=%s)", severity, field, message, value, code);
  }

  public static final class Builder {
    private String field;
    private Object value;
    private String message;
    private String code;
    private ViolationSeverity severity;

    private Builder() {}

    public Builder field(String field) {
      this.field = field;
      return this;
    }

    public Builder value(Object value) {
      this.value = value;
      return this;
    }

    public Builder message(String message) {
      this.message = message;
      return this;
    }

    public Builder code(String code) {
      this.code = code;
      return this;
    }

    public Builder severity(ViolationSeverity severity) {
      this.severity = severity;
      return this;
    }

    public ValidationViolation build() {
      return new ValidationViolation(this);
    }
  }
}
