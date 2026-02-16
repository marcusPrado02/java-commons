package com.marcusprado02.commons.app.validation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Composite validator that combines multiple validators.
 *
 * <p>Executes all validators and aggregates their results.
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * Validator<User> userValidator = CompositeValidator.of(
 *     FieldValidator.of("email", User::getEmail, Validators.isEmail(), "Invalid email"),
 *     FieldValidator.of("name", User::getName, Validators.notBlank(), "Name is required"),
 *     FieldValidator.of("age", User::getAge, Validators.isPositive(), "Age must be positive")
 * );
 *
 * ValidationResult result = userValidator.validate(user);
 * }</pre>
 *
 * @param <T> the type being validated
 */
public final class CompositeValidator<T> implements Validator<T> {

  private final List<Validator<T>> validators;

  private CompositeValidator(List<Validator<T>> validators) {
    this.validators = List.copyOf(validators);
  }

  @SafeVarargs
  public static <T> CompositeValidator<T> of(Validator<T>... validators) {
    return new CompositeValidator<>(Arrays.asList(validators));
  }

  public static <T> CompositeValidator<T> of(List<Validator<T>> validators) {
    return new CompositeValidator<>(validators);
  }

  @Override
  public ValidationResult validate(T value) {
    List<ValidationViolation> allViolations = new ArrayList<>();

    for (Validator<T> validator : validators) {
      ValidationResult result = validator.validate(value);
      if (result.hasViolations()) {
        allViolations.addAll(result.getViolations());
      }
    }

    if (allViolations.isEmpty()) {
      return ValidationResult.valid();
    }

    return ValidationResult.invalid(allViolations);
  }

  /**
   * Adds another validator to this composite.
   *
   * @param validator the validator to add
   * @return new composite with added validator
   */
  public CompositeValidator<T> with(Validator<T> validator) {
    List<Validator<T>> updated = new ArrayList<>(validators);
    updated.add(validator);
    return new CompositeValidator<>(updated);
  }
}
