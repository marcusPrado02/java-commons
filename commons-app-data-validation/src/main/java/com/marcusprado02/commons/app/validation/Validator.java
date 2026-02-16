package com.marcusprado02.commons.app.validation;

/**
 * Validator for a specific type.
 *
 * <p>Implementations define validation logic for domain objects.
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * public class UserValidator implements Validator<User> {
 *     @Override
 *     public ValidationResult validate(User user) {
 *         if (user.getEmail() == null || user.getEmail().isBlank()) {
 *             return ValidationResult.invalid(
 *                 ValidationViolation.builder()
 *                     .field("email")
 *                     .message("Email is required")
 *                     .build()
 *             );
 *         }
 *         return ValidationResult.valid();
 *     }
 * }
 * }</pre>
 *
 * @param <T> the type being validated
 */
@FunctionalInterface
public interface Validator<T> {

  /**
   * Validates the given object.
   *
   * @param value the value to validate
   * @return validation result
   */
  ValidationResult validate(T value);

  /**
   * Combines this validator with another.
   *
   * @param other the other validator
   * @return combined validator
   */
  default Validator<T> and(Validator<T> other) {
    return value -> {
      ValidationResult first = this.validate(value);
      ValidationResult second = other.validate(value);
      return first.merge(second);
    };
  }
}
