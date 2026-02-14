package com.marcusprado02.commons.app.configuration.validation;

import com.marcusprado02.commons.app.configuration.ConfigurationProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Default implementation of ConfigurationValidator.
 *
 * @see ConfigurationValidator
 */
class DefaultConfigurationValidator implements ConfigurationValidator {

  private final List<ValidationRule> rules;

  DefaultConfigurationValidator(List<ValidationRule> rules) {
    this.rules = new ArrayList<>(rules);
  }

  @Override
  public ValidationResult validate(ConfigurationProvider provider) {
    List<String> errors = new ArrayList<>();

    for (ValidationRule rule : rules) {
      Optional<String> error = rule.validate(provider);
      error.ifPresent(errors::add);
    }

    return errors.isEmpty() ? ValidationResult.valid() : ValidationResult.invalid(errors);
  }

  static class BuilderImpl implements Builder {
    private final List<ValidationRule> rules = new ArrayList<>();

    @Override
    public Builder required(String key) {
      rules.add(ValidationRule.required(key));
      return this;
    }

    @Override
    public Builder required(String key, String message) {
      rules.add(ValidationRule.required(key, message));
      return this;
    }

    @Override
    public Builder pattern(String key, String pattern) {
      rules.add(ValidationRule.pattern(key, pattern));
      return this;
    }

    @Override
    public Builder pattern(String key, String pattern, String message) {
      rules.add(ValidationRule.pattern(key, pattern, message));
      return this;
    }

    @Override
    public Builder range(String key, long min, long max) {
      rules.add(ValidationRule.range(key, min, max));
      return this;
    }

    @Override
    public Builder rule(ValidationRule rule) {
      rules.add(rule);
      return this;
    }

    @Override
    public ConfigurationValidator build() {
      return new DefaultConfigurationValidator(rules);
    }
  }
}
