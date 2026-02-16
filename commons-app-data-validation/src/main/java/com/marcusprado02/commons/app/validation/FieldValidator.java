package com.marcusprado02.commons.app.validation;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Validator for a specific field of an object.
 *
 * <p>Allows extracting and validating individual fields.
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * FieldValidator<User, String> emailValidator = FieldValidator.of(
 *     "email",
 *     User::getEmail,
 *     email -> email != null && email.contains("@"),
 *     "Email must be valid"
 * );
 *
 * ValidationResult result = emailValidator.validate(user);
 * }</pre>
 *
 * @param <T> the type being validated
 * @param <F> the field type
 */
public final class FieldValidator<T, F> implements Validator<T> {

  private final String fieldName;
  private final Function<T, F> fieldExtractor;
  private final Predicate<F> predicate;
  private final String message;
  private final String code;
  private final ViolationSeverity severity;

  private FieldValidator(
      String fieldName,
      Function<T, F> fieldExtractor,
      Predicate<F> predicate,
      String message,
      String code,
      ViolationSeverity severity) {
    this.fieldName = fieldName;
    this.fieldExtractor = fieldExtractor;
    this.predicate = predicate;
    this.message = message;
    this.code = code;
    this.severity = severity != null ? severity : ViolationSeverity.ERROR;
  }

  /**
   * Creates a field validator.
   *
   * @param fieldName the field name
   * @param fieldExtractor function to extract the field value
   * @param predicate validation predicate (returns true if valid)
   * @param message violation message
   * @return field validator
   */
  public static <T, F> FieldValidator<T, F> of(
      String fieldName, Function<T, F> fieldExtractor, Predicate<F> predicate, String message) {
    return new FieldValidator<>(fieldName, fieldExtractor, predicate, message, null, null);
  }

  /**
   * Creates a field validator with code.
   *
   * @param fieldName the field name
   * @param fieldExtractor function to extract the field value
   * @param predicate validation predicate
   * @param message violation message
   * @param code violation code
   * @return field validator
   */
  public static <T, F> FieldValidator<T, F> of(
      String fieldName,
      Function<T, F> fieldExtractor,
      Predicate<F> predicate,
      String message,
      String code) {
    return new FieldValidator<>(fieldName, fieldExtractor, predicate, message, code, null);
  }

  /**
   * Creates a field validator with severity.
   *
   * @param fieldName the field name
   * @param fieldExtractor function to extract the field value
   * @param predicate validation predicate
   * @param message violation message
   * @param code violation code
   * @param severity violation severity
   * @return field validator
   */
  public static <T, F> FieldValidator<T, F> of(
      String fieldName,
      Function<T, F> fieldExtractor,
      Predicate<F> predicate,
      String message,
      String code,
      ViolationSeverity severity) {
    return new FieldValidator<>(fieldName, fieldExtractor, predicate, message, code, severity);
  }

  @Override
  public ValidationResult validate(T value) {
    if (value == null) {
      return ValidationResult.valid();
    }

    F fieldValue = fieldExtractor.apply(value);

    if (predicate.test(fieldValue)) {
      return ValidationResult.valid();
    }

    return ValidationResult.invalid(
        ValidationViolation.builder()
            .field(fieldName)
            .value(fieldValue)
            .message(message)
            .code(code)
            .severity(severity)
            .build());
  }
}
