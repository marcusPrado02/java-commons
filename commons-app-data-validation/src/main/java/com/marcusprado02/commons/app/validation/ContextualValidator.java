package com.marcusprado02.commons.app.validation;

/**
 * Validator that accepts a validation context.
 *
 * <p>Allows validators to access additional metadata during validation.
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * public class UniqueEmailValidator implements ContextualValidator<User> {
 *     private final UserRepository userRepository;
 *
 *     @Override
 *     public ValidationResult validate(User user, ValidationContext context) {
 *         String userId = context.get("userId", String.class);
 *         boolean exists = userRepository.existsByEmailAndIdNot(user.getEmail(), userId);
 *         if (exists) {
 *             return ValidationResult.invalid(
 *                 ValidationViolation.builder()
 *                     .field("email")
 *                     .message("Email already exists")
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
public interface ContextualValidator<T> {

  /**
   * Validates the given object with context.
   *
   * @param value the value to validate
   * @param context validation context
   * @return validation result
   */
  ValidationResult validate(T value, ValidationContext context);

  /**
   * Converts this to a regular validator with empty context.
   *
   * @return validator
   */
  default Validator<T> asValidator() {
    return value -> validate(value, ValidationContext.empty());
  }

  /**
   * Converts this to a regular validator with fixed context.
   *
   * @param context the context to use
   * @return validator
   */
  default Validator<T> asValidator(ValidationContext context) {
    return value -> validate(value, context);
  }
}
