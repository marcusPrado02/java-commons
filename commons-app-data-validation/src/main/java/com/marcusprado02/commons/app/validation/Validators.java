package com.marcusprado02.commons.app.validation;

import java.util.regex.Pattern;

/**
 * Common validation predicates and validators.
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * FieldValidator<User, String> emailValidator =
 *     FieldValidator.of("email", User::getEmail, Validators.isEmail(), "Invalid email");
 *
 * FieldValidator<User, String> nameValidator =
 *     FieldValidator.of("name", User::getName, Validators.notBlank(), "Name is required");
 * }</pre>
 */
public final class Validators {

  private Validators() {}

  private static final Pattern EMAIL_PATTERN =
      Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

  /**
   * Validates that a string is not null and not blank.
   *
   * @return predicate
   */
  public static java.util.function.Predicate<String> notBlank() {
    return value -> value != null && !value.isBlank();
  }

  /**
   * Validates that a string is not null and not empty.
   *
   * @return predicate
   */
  public static java.util.function.Predicate<String> notEmpty() {
    return value -> value != null && !value.isEmpty();
  }

  /**
   * Validates that a value is not null.
   *
   * @return predicate
   */
  public static <T> java.util.function.Predicate<T> notNull() {
    return value -> value != null;
  }

  /**
   * Validates that a string matches email format.
   *
   * @return predicate
   */
  public static java.util.function.Predicate<String> isEmail() {
    return value -> value != null && EMAIL_PATTERN.matcher(value).matches();
  }

  /**
   * Validates that a string has minimum length.
   *
   * @param min minimum length
   * @return predicate
   */
  public static java.util.function.Predicate<String> minLength(int min) {
    return value -> value != null && value.length() >= min;
  }

  /**
   * Validates that a string has maximum length.
   *
   * @param max maximum length
   * @return predicate
   */
  public static java.util.function.Predicate<String> maxLength(int max) {
    return value -> value != null && value.length() <= max;
  }

  /**
   * Validates that a string length is between min and max.
   *
   * @param min minimum length
   * @param max maximum length
   * @return predicate
   */
  public static java.util.function.Predicate<String> lengthBetween(int min, int max) {
    return value -> value != null && value.length() >= min && value.length() <= max;
  }

  /**
   * Validates that a number is positive.
   *
   * @return predicate
   */
  public static <T extends Number> java.util.function.Predicate<T> isPositive() {
    return value -> value != null && value.doubleValue() > 0;
  }

  /**
   * Validates that a number is non-negative.
   *
   * @return predicate
   */
  public static <T extends Number> java.util.function.Predicate<T> isNonNegative() {
    return value -> value != null && value.doubleValue() >= 0;
  }

  /**
   * Validates that a number is within range.
   *
   * @param min minimum value (inclusive)
   * @param max maximum value (inclusive)
   * @return predicate
   */
  public static <T extends Comparable<T>> java.util.function.Predicate<T> range(T min, T max) {
    return value -> value != null && value.compareTo(min) >= 0 && value.compareTo(max) <= 0;
  }

  /**
   * Validates that a string matches a regex pattern.
   *
   * @param pattern regex pattern
   * @return predicate
   */
  public static java.util.function.Predicate<String> matches(String pattern) {
    Pattern compiled = Pattern.compile(pattern);
    return value -> value != null && compiled.matcher(value).matches();
  }

  /**
   * Validates that a string matches a regex pattern.
   *
   * @param pattern compiled pattern
   * @return predicate
   */
  public static java.util.function.Predicate<String> matches(Pattern pattern) {
    return value -> value != null && pattern.matcher(value).matches();
  }
}
