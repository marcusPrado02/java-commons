package com.marcusprado02.commons.app.configuration.validation;

import com.marcusprado02.commons.app.configuration.ConfigurationProvider;
import java.util.Optional;

/**
 * Represents a validation rule for a configuration property.
 *
 * <p>Validation rules check a property value and return an error message if validation fails.
 *
 * <p>Example custom rule:
 *
 * <pre>{@code
 * ValidationRule rule = (provider) -> {
 *     Optional<String> url = provider.getString("database.url");
 *     if (url.isPresent() && !url.get().startsWith("jdbc:")) {
 *         return Optional.of("database.url must start with 'jdbc:'");
 *     }
 *     return Optional.empty();
 * };
 * }</pre>
 *
 * @see ConfigurationValidator
 */
@FunctionalInterface
public interface ValidationRule {

  /**
   * Validates the configuration.
   *
   * @param provider the configuration provider
   * @return error message if validation fails, empty if valid
   */
  Optional<String> validate(ConfigurationProvider provider);

  /**
   * Creates a required property rule.
   *
   * @param key the property key
   * @return validation rule
   */
  static ValidationRule required(String key) {
    return required(key, "Required property '" + key + "' is missing");
  }

  /**
   * Creates a required property rule with custom message.
   *
   * @param key the property key
   * @param message the error message
   * @return validation rule
   */
  static ValidationRule required(String key, String message) {
    return provider -> {
      if (!provider.containsKey(key)
          || provider.getString(key).filter(s -> !s.isBlank()).isEmpty()) {
        return Optional.of(message);
      }
      return Optional.empty();
    };
  }

  /**
   * Creates a pattern validation rule.
   *
   * @param key the property key
   * @param pattern the regex pattern
   * @return validation rule
   */
  static ValidationRule pattern(String key, String pattern) {
    return pattern(key, pattern, "Property '" + key + "' does not match pattern: " + pattern);
  }

  /**
   * Creates a pattern validation rule with custom message.
   *
   * @param key the property key
   * @param pattern the regex pattern
   * @param message the error message
   * @return validation rule
   */
  static ValidationRule pattern(String key, String pattern, String message) {
    return provider -> {
      Optional<String> value = provider.getString(key);
      if (value.isPresent() && !value.get().matches(pattern)) {
        return Optional.of(message);
      }
      return Optional.empty();
    };
  }

  /**
   * Creates a numeric range validation rule.
   *
   * @param key the property key
   * @param min minimum value (inclusive)
   * @param max maximum value (inclusive)
   * @return validation rule
   */
  static ValidationRule range(String key, long min, long max) {
    return provider -> {
      Optional<Long> value = provider.getLong(key);
      if (value.isEmpty()) {
        return Optional.empty(); // Not present, skip (use required() if needed)
      }

      long val = value.get();
      if (val < min || val > max) {
        return Optional.of(
            "Property '" + key + "' must be between " + min + " and " + max + ", but was: " + val);
      }
      return Optional.empty();
    };
  }

  /**
   * Creates an enum validation rule.
   *
   * @param key the property key
   * @param allowedValues allowed values
   * @return validation rule
   */
  static ValidationRule oneOf(String key, String... allowedValues) {
    return provider -> {
      Optional<String> value = provider.getString(key);
      if (value.isEmpty()) {
        return Optional.empty();
      }

      String val = value.get();
      for (String allowed : allowedValues) {
        if (allowed.equals(val)) {
          return Optional.empty();
        }
      }

      return Optional.of(
          "Property '"
              + key
              + "' must be one of ["
              + String.join(", ", allowedValues)
              + "], but was: "
              + val);
    };
  }
}
