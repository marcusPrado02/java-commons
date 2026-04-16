package com.marcusprado02.commons.app.configuration.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Result of configuration validation.
 *
 * <p>Contains all validation errors found during validation.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * ValidationResult result =validator.validate(configProvider);
 *
 * if (result.hasErrors()) {
 *     result.getErrors().forEach(error -> logger.error(error));
 *     throw new ConfigurationValidationException(result);
 * }
 * }</pre>
 *
 * @see ConfigurationValidator
 */
public final class ValidationResult {

  private final List<String> errors;

  private ValidationResult(List<String> errors) {
    this.errors = Collections.unmodifiableList(new ArrayList<>(errors));
  }

  /**
   * Creates a validation result with no errors.
   *
   * @return valid result
   */
  public static ValidationResult valid() {
    return new ValidationResult(List.of());
  }

  /**
   * Creates a validation result with errors.
   *
   * @param errors the validation errors
   * @return invalid result
   */
  public static ValidationResult invalid(List<String> errors) {
    Objects.requireNonNull(errors, "errors");
    if (errors.isEmpty()) {
      throw new IllegalArgumentException("At least one error is required");
    }
    return new ValidationResult(errors);
  }

  /**
   * Creates a validation result with a single error.
   *
   * @param error the validation error
   * @return invalid result
   */
  public static ValidationResult invalid(String error) {
    Objects.requireNonNull(error, "error");
    return new ValidationResult(List.of(error));
  }

  /**
   * Checks if there are validation errors.
   *
   * @return true if there are errors
   */
  public boolean hasErrors() {
    return !errors.isEmpty();
  }

  /**
   * Checks if validation passed.
   *
   * @return true if no errors
   */
  public boolean isValid() {
    return errors.isEmpty();
  }

  /**
   * Returns all validation errors.
   *
   * @return unmodifiable list of errors
   */
  public List<String> getErrors() {
    return errors;
  }

  /**
   * Returns the number of errors.
   *
   * @return error count
   */
  public int getErrorCount() {
    return errors.size();
  }

  /**
   * Formats errors as a single string.
   *
   * @return formatted error message
   */
  public String formatErrors() {
    if (errors.isEmpty()) {
      return "No errors";
    }

    if (errors.size() == 1) {
      return errors.get(0);
    }

    StringBuilder sb = new StringBuilder();
    sb.append(errors.size()).append(" configuration error(s):\n");
    for (int i = 0; i < errors.size(); i++) {
      sb.append("  ").append(i + 1).append(". ").append(errors.get(i)).append("\n");
    }
    return sb.toString();
  }

  @Override
  public String toString() {
    return formatErrors();
  }
}
