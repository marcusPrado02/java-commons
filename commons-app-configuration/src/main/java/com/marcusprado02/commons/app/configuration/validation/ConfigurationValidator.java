package com.marcusprado02.commons.app.configuration.validation;

/**
 * Validates configuration properties.
 *
 * <p>Validators check that configuration properties meet certain requirements and return validation
 * results that can be used for fail-fast startup behavior.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * ConfigurationValidator validator = ConfigurationValidator.builder()
 *     .required("database.url")
 *     .required("database.username")
 *     .pattern("server.port", "\\d+")
 *     .range("pool.size", 1, 100)
 *     .build();
 *
 * ValidationResult result = validator.validate(configProvider);
 *
 * if (result.hasErrors()) {
 *     throw new ConfigurationValidationException(result.getErrors());
 * }
 * }</pre>
 *
 * @see ValidationRule
 * @see ValidationResult
 */
public interface ConfigurationValidator {

  /**
   * Validates the configuration.
   *
   * @param provider the configuration provider
   * @return validation result
   */
  ValidationResult validate(
      com.marcusprado02.commons.app.configuration.ConfigurationProvider provider);

  /**
   * Creates a new builder for constructing validators.
   *
   * @return validator builder
   */
  static Builder builder() {
    return new DefaultConfigurationValidator.BuilderImpl();
  }

  /** Builder for creating configuration validators. */
  interface Builder {

    /**
     * Adds a required property validation.
     *
     * @param key the property key
     * @return this builder
     */
    Builder required(String key);

    /**
     * Adds a required property validation with custom message.
     *
     * @param key the property key
     * @param message the custom error message
     * @return this builder
     */
    Builder required(String key, String message);

    /**
     * Adds a pattern validation for a property.
     *
     * @param key the property key
     * @param pattern the regex pattern
     * @return this builder
     */
    Builder pattern(String key, String pattern);

    /**
     * Adds a pattern validation with custom message.
     *
     * @param key the property key
     * @param pattern the regex pattern
     * @param message the custom error message
     * @return this builder
     */
    Builder pattern(String key, String pattern, String message);

    /**
     * Adds a numeric range validation.
     *
     * @param key the property key
     * @param min minimum value (inclusive)
     * @param max maximum value (inclusive)
     * @return this builder
     */
    Builder range(String key, long min, long max);

    /**
     * Adds a custom validation rule.
     *
     * @param rule the validation rule
     * @return this builder
     */
    Builder rule(ValidationRule rule);

    /**
     * Builds the validator.
     *
     * @return the configuration validator
     */
    ConfigurationValidator build();
  }
}
