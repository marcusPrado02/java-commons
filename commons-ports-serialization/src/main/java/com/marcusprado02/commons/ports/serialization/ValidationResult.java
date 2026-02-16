package com.marcusprado02.commons.ports.serialization;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents the result of a schema validation operation.
 */
public class ValidationResult {

  private final boolean valid;
  private final List<String> errors;
  private final List<String> warnings;

  private ValidationResult(boolean valid, List<String> errors, List<String> warnings) {
    this.valid = valid;
    this.errors = Objects.requireNonNull(errors, "Errors list cannot be null");
    this.warnings = Objects.requireNonNull(warnings, "Warnings list cannot be null");
  }

  /**
   * Creates a successful validation result.
   *
   * @return a valid result with no errors
   */
  public static ValidationResult valid() {
    return new ValidationResult(true, Collections.emptyList(), Collections.emptyList());
  }

  /**
   * Creates a successful validation result with warnings.
   *
   * @param warnings the validation warnings
   * @return a valid result with warnings
   */
  public static ValidationResult validWithWarnings(List<String> warnings) {
    return new ValidationResult(true, Collections.emptyList(), warnings);
  }

  /**
   * Creates a failed validation result with errors.
   *
   * @param errors the validation errors
   * @return an invalid result with errors
   */
  public static ValidationResult invalid(List<String> errors) {
    return new ValidationResult(false, errors, Collections.emptyList());
  }

  /**
   * Creates a failed validation result with errors and warnings.
   *
   * @param errors   the validation errors
   * @param warnings the validation warnings
   * @return an invalid result with errors and warnings
   */
  public static ValidationResult invalid(List<String> errors, List<String> warnings) {
    return new ValidationResult(false, errors, warnings);
  }

  /**
   * Checks if the validation was successful.
   *
   * @return true if valid, false otherwise
   */
  public boolean isValid() {
    return valid;
  }

  /**
   * Gets the validation errors.
   *
   * @return the list of errors (empty if none)
   */
  public List<String> getErrors() {
    return Collections.unmodifiableList(errors);
  }

  /**
   * Gets the validation warnings.
   *
   * @return the list of warnings (empty if none)
   */
  public List<String> getWarnings() {
    return Collections.unmodifiableList(warnings);
  }

  /**
   * Checks if there are any warnings.
   *
   * @return true if there are warnings
   */
  public boolean hasWarnings() {
    return !warnings.isEmpty();
  }

  /**
   * Checks if there are any errors.
   *
   * @return true if there are errors
   */
  public boolean hasErrors() {
    return !errors.isEmpty();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ValidationResult that = (ValidationResult) o;
    return valid == that.valid &&
           Objects.equals(errors, that.errors) &&
           Objects.equals(warnings, that.warnings);
  }

  @Override
  public int hashCode() {
    return Objects.hash(valid, errors, warnings);
  }

  @Override
  public String toString() {
    return "ValidationResult{" +
           "valid=" + valid +
           ", errors=" + errors.size() +
           ", warnings=" + warnings.size() +
           '}';
  }
}
