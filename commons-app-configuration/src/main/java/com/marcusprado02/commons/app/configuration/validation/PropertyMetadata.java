package com.marcusprado02.commons.app.configuration.validation;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Metadata for a single configuration property.
 *
 * <p>Contains information about the property's type, description, default value, validation rules,
 * etc.
 *
 * @see ConfigurationMetadata
 */
public final class PropertyMetadata {

  private final String name;
  private final String type;
  private final String description;
  private final String defaultValue;
  private final boolean required;
  private final String example;
  private final Long minValue;
  private final Long maxValue;
  private final List<String> allowedValues;

  private PropertyMetadata(
      String name,
      String type,
      String description,
      String defaultValue,
      boolean required,
      String example,
      Long minValue,
      Long maxValue,
      List<String> allowedValues) {
    this.name = Objects.requireNonNull(name, "name");
    this.type = type;
    this.description = description;
    this.defaultValue = defaultValue;
    this.required = required;
    this.example = example;
    this.minValue = minValue;
    this.maxValue = maxValue;
    this.allowedValues = allowedValues != null ? List.copyOf(allowedValues) : null;
  }

  /**
   * Creates a builder for property metadata.
   *
   * @param name the property name
   * @return property builder
   */
  public static Builder builder(String name) {
    return new Builder(name);
  }

  public String getName() {
    return name;
  }

  public String getType() {
    return type;
  }

  public String getDescription() {
    return description;
  }

  public String getDefaultValue() {
    return defaultValue;
  }

  public boolean isRequired() {
    return required;
  }

  public String getExample() {
    return example;
  }

  public Long getMinValue() {
    return minValue;
  }

  public Long getMaxValue() {
    return maxValue;
  }

  public List<String> getAllowedValues() {
    return allowedValues;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("PropertyMetadata{");
    sb.append("name='").append(name).append('\'');
    if (type != null) sb.append(", type='").append(type).append('\'');
    if (description != null) sb.append(", description='").append(description).append('\'');
    if (defaultValue != null) sb.append(", defaultValue='").append(defaultValue).append('\'');
    if (required) sb.append(", required=true");
    if (example != null) sb.append(", example='").append(example).append('\'');
    if (minValue != null) sb.append(", minValue=").append(minValue);
    if (maxValue != null) sb.append(", maxValue=").append(maxValue);
    if (allowedValues != null) sb.append(", allowedValues=").append(allowedValues);
    sb.append('}');
    return sb.toString();
  }

  /** Builder for PropertyMetadata. */
  public static class Builder {
    private final String name;
    private String type = "java.lang.String";
    private String description;
    private String defaultValue;
    private boolean required;
    private String example;
    private Long minValue;
    private Long maxValue;
    private List<String> allowedValues;

    private Builder(String name) {
      this.name = name;
    }

    public Builder type(String type) {
      this.type = type;
      return this;
    }

    public Builder description(String description) {
      this.description = description;
      return this;
    }

    public Builder defaultValue(String defaultValue) {
      this.defaultValue = defaultValue;
      return this;
    }

    public Builder required(boolean required) {
      this.required = required;
      return this;
    }

    public Builder example(String example) {
      this.example = example;
      return this;
    }

    public Builder range(long min, long max) {
      this.minValue = min;
      this.maxValue = max;
      return this;
    }

    public Builder allowedValues(String... values) {
      this.allowedValues = Arrays.asList(values);
      return this;
    }

    public PropertyMetadata build() {
      return new PropertyMetadata(
          name,
          type,
          description,
          defaultValue,
          required,
          example,
          minValue,
          maxValue,
          allowedValues);
    }
  }
}
