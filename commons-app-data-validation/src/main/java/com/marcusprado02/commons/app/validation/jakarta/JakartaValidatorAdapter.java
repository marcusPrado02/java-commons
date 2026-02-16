package com.marcusprado02.commons.app.validation.jakarta;

import com.marcusprado02.commons.app.validation.ValidationResult;
import com.marcusprado02.commons.app.validation.ValidationViolation;
import com.marcusprado02.commons.app.validation.Validator;
import com.marcusprado02.commons.app.validation.ViolationSeverity;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Adapter for Jakarta Bean Validation.
 *
 * <p>Allows using Jakarta validation annotations with the commons validation API.
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * public class User {
 *     @NotBlank
 *     @Email
 *     private String email;
 *
 *     @NotBlank
 *     @Size(min = 3, max = 50)
 *     private String name;
 * }
 *
 * Validator<User> validator = JakartaValidatorAdapter.create();
 * ValidationResult result = validator.validate(user);
 * }</pre>
 */
public final class JakartaValidatorAdapter<T> implements Validator<T> {

  private final jakarta.validation.Validator validator;

  private JakartaValidatorAdapter(jakarta.validation.Validator validator) {
    this.validator = validator;
  }

  /**
   * Creates an adapter using default validator factory.
   *
   * @return adapter
   */
  public static <T> JakartaValidatorAdapter<T> create() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    return new JakartaValidatorAdapter<>(factory.getValidator());
  }

  /**
   * Creates an adapter using provided validator.
   *
   * @param validator Jakarta validator
   * @return adapter
   */
  public static <T> JakartaValidatorAdapter<T> of(jakarta.validation.Validator validator) {
    return new JakartaValidatorAdapter<>(validator);
  }

  @Override
  public ValidationResult validate(T value) {
    if (value == null) {
      return ValidationResult.valid();
    }

    Set<ConstraintViolation<T>> violations = validator.validate(value);

    if (violations.isEmpty()) {
      return ValidationResult.valid();
    }

    List<ValidationViolation> converted =
        violations.stream().map(this::toValidationViolation).collect(Collectors.toList());

    return ValidationResult.invalid(converted);
  }

  /**
   * Validates specific property.
   *
   * @param object the object
   * @param propertyName the property name
   * @return validation result
   */
  public ValidationResult validateProperty(T object, String propertyName) {
    Set<ConstraintViolation<T>> violations = validator.validateProperty(object, propertyName);

    if (violations.isEmpty()) {
      return ValidationResult.valid();
    }

    List<ValidationViolation> converted =
        violations.stream().map(this::toValidationViolation).collect(Collectors.toList());

    return ValidationResult.invalid(converted);
  }

  private ValidationViolation toValidationViolation(ConstraintViolation<T> violation) {
    return ValidationViolation.builder()
        .field(violation.getPropertyPath().toString())
        .value(violation.getInvalidValue())
        .message(violation.getMessage())
        .code(violation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName())
        .severity(ViolationSeverity.ERROR)
        .build();
  }
}
