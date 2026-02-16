package com.marcusprado02.commons.app.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Result of a validation operation.
 *
 * <p>Contains all validation violations found during validation.
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * ValidationResult result = ValidationResult.valid();
 *
 * ValidationResult result = ValidationResult.invalid(
 *     ValidationViolation.builder()
 *         .field("email")
 *         .message("Email is required")
 *         .build()
 * );
 * }</pre>
 */
public final class ValidationResult {

  private final List<ValidationViolation> violations;

  private ValidationResult(List<ValidationViolation> violations) {
    this.violations = Collections.unmodifiableList(new ArrayList<>(violations));
  }

  public static ValidationResult valid() {
    return new ValidationResult(List.of());
  }

  public static ValidationResult invalid(ValidationViolation violation) {
    return new ValidationResult(List.of(violation));
  }

  public static ValidationResult invalid(List<ValidationViolation> violations) {
    Objects.requireNonNull(violations, "violations cannot be null");
    return new ValidationResult(violations);
  }

  public boolean isValid() {
    return violations.isEmpty();
  }

  public boolean hasViolations() {
    return !violations.isEmpty();
  }

  public List<ValidationViolation> getViolations() {
    return violations;
  }

  /**
   * Gets only error-level violations.
   *
   * @return list of errors
   */
  public List<ValidationViolation> getErrors() {
    return violations.stream()
        .filter(v -> v.getSeverity() == ViolationSeverity.ERROR)
        .collect(Collectors.toList());
  }

  /**
   * Gets only warning-level violations.
   *
   * @return list of warnings
   */
  public List<ValidationViolation> getWarnings() {
    return violations.stream()
        .filter(v -> v.getSeverity() == ViolationSeverity.WARNING)
        .collect(Collectors.toList());
  }

  /**
   * Checks if there are any errors.
   *
   * @return true if errors exist
   */
  public boolean hasErrors() {
    return violations.stream().anyMatch(v -> v.getSeverity() == ViolationSeverity.ERROR);
  }

  /**
   * Merges this result with another.
   *
   * @param other the other result
   * @return merged result
   */
  public ValidationResult merge(ValidationResult other) {
    if (other == null || other.isValid()) {
      return this;
    }
    if (this.isValid()) {
      return other;
    }
    List<ValidationViolation> merged = new ArrayList<>(this.violations);
    merged.addAll(other.violations);
    return new ValidationResult(merged);
  }

  @Override
  public String toString() {
    if (isValid()) {
      return "ValidationResult{valid}";
    }
    return "ValidationResult{violations=" + violations.size() + "}";
  }
}
