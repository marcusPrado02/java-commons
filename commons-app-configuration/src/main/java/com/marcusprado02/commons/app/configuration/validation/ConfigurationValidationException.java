package com.marcusprado02.commons.app.configuration.validation;

import java.util.List;

/**
 * Exception thrown when configuration validation fails.
 *
 * <p>Contains all validation errors found during validation.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * ValidationResult result = validator.validate(configProvider);
 *
 * if (result.hasErrors()) {
 *     throw new ConfigurationValidationException(result);
 * }
 * }</pre>
 *
 * @see ValidationResult
 * @see ConfigurationValidator
 */
public class ConfigurationValidationException extends RuntimeException {

  private final ValidationResult validationResult;

  /**
   * Creates an exception from a validation result.
   *
   * @param validationResult the validation result
   */
  public ConfigurationValidationException(ValidationResult validationResult) {
    super(validationResult.formatErrors());
    this.validationResult = validationResult;
  }

  /**
   * Creates an exception from a list of errors.
   *
   * @param errors the validation errors
   */
  public ConfigurationValidationException(List<String> errors) {
    this(ValidationResult.invalid(errors));
  }

  /**
   * Creates an exception from a single error.
   *
   * @param error the validation error
   */
  public ConfigurationValidationException(String error) {
    this(ValidationResult.invalid(error));
  }

  /**
   * Returns the validation result.
   *
   * @return validation result
   */
  public ValidationResult getValidationResult() {
    return validationResult;
  }

  /**
   * Returns all validation errors.
   *
   * @return unmodifiable list of errors
   */
  public List<String> getErrors() {
    return validationResult.getErrors();
  }
}
