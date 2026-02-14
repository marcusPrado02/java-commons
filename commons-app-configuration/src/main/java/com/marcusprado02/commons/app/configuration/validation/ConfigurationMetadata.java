package com.marcusprado02.commons.app.configuration.validation;

import java.util.*;

/**
 * Metadata for configuration properties.
 *
 * <p>Provides information about configuration properties for IDE support and documentation.
 *
 * <p>This can be used to generate Spring Boot configuration metadata
 * (spring-configuration-metadata.json) for IDE autocomplete and validation.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * ConfigurationMetadata metadata = ConfigurationMetadata.builder()
 *     .property("database.url")
 *         .type("java.lang.String")
 *         .description("JDBC URL for database connection")
 *         .required(true)
 *         .example("jdbc:postgresql://localhost:5432/mydb")
 *     .property("database.pool.max-size")
 *         .type("java.lang.Integer")
 *         .description("Maximum pool size")
 *         .defaultValue("10")
 *         .range(1, 100)
 *     .build();
 *
 * // Generate Spring Boot metadata JSON
 * String json = metadata.toSpringBootMetadataJson();
 * }</pre>
 *
 * @see PropertyMetadata
 */
public final class ConfigurationMetadata {

  private final Map<String, PropertyMetadata> properties;

  private ConfigurationMetadata(Map<String, PropertyMetadata> properties) {
    this.properties = Collections.unmodifiableMap(new LinkedHashMap<>(properties));
  }

  /**
   * Creates a new builder.
   *
   * @return metadata builder
   */
  public static Builder builder() {
    return new BuilderImpl();
  }

  /**
   * Returns all property metadata.
   *
   * @return unmodifiable map of property metadata
   */
  public Map<String, PropertyMetadata> getProperties() {
    return properties;
  }

  /**
   * Returns metadata for a specific property.
   *
   * @param key the property key
   * @return property metadata, or null if not found
   */
  public PropertyMetadata getProperty(String key) {
    return properties.get(key);
  }

  /**
   * Generates Spring Boot configuration metadata JSON.
   *
   * <p>This can be written to META-INF/spring-configuration-metadata.json for IDE support.
   *
   * @return JSON string
   */
  public String toSpringBootMetadataJson() {
    StringBuilder json = new StringBuilder();
    json.append("{\n");
    json.append("  \"properties\": [\n");

    Iterator<Map.Entry<String, PropertyMetadata>> iter = properties.entrySet().iterator();
    while (iter.hasNext()) {
      Map.Entry<String, PropertyMetadata> entry = iter.next();
      PropertyMetadata prop = entry.getValue();

      json.append("    {\n");
      json.append("      \"name\": \"").append(prop.getName()).append("\",\n");
      json.append("      \"type\": \"").append(prop.getType()).append("\"");

      if (prop.getDescription() != null) {
        json.append(",\n      \"description\": \"")
            .append(escapeJson(prop.getDescription()))
            .append("\"");
      }

      if (prop.getDefaultValue() != null) {
        json.append(",\n      \"defaultValue\": ");
        if (prop.getType().equals("java.lang.String")) {
          json.append("\"").append(escapeJson(prop.getDefaultValue())).append("\"");
        } else {
          json.append(prop.getDefaultValue());
        }
      }

      json.append("\n    }");
      if (iter.hasNext()) {
        json.append(",");
      }
      json.append("\n");
    }

    json.append("  ]\n");
    json.append("}\n");

    return json.toString();
  }

  private String escapeJson(String str) {
    return str.replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t");
  }

  /** Builder for ConfigurationMetadata. */
  public interface Builder {

    /**
     * Starts defining a property.
     *
     * @param key the property key
     * @return property builder
     */
    PropertyBuilder property(String key);

    /**
     * Builds the metadata.
     *
     * @return configuration metadata
     */
    ConfigurationMetadata build();
  }

  /** Builder for individual property metadata. */
  public interface PropertyBuilder {

    /**
     * Sets the property type.
     *
     * @param type the Java type (e.g., "java.lang.String", "java.lang.Integer")
     * @return this builder
     */
    PropertyBuilder type(String type);

    /**
     * Sets the property description.
     *
     * @param description the description
     * @return this builder
     */
    PropertyBuilder description(String description);

    /**
     * Sets the default value.
     *
     * @param defaultValue the default value
     * @return this builder
     */
    PropertyBuilder defaultValue(String defaultValue);

    /**
     * Marks the property as required.
     *
     * @param required true if required
     * @return this builder
     */
    PropertyBuilder required(boolean required);

    /**
     * Sets an example value.
     *
     * @param example the example value
     * @return this builder
     */
    PropertyBuilder example(String example);

    /**
     * Sets the valid value range (for numeric types).
     *
     * @param min minimum value
     * @param max maximum value
     * @return this builder
     */
    PropertyBuilder range(long min, long max);

    /**
     * Sets allowed values (for enum-like properties).
     *
     * @param values allowed values
     * @return this builder
     */
    PropertyBuilder allowedValues(String... values);

    /**
     * Continues to the next property or builds the metadata.
     *
     * @param key the next property key
     * @return property builder for next property
     */
    PropertyBuilder property(String key);

    /**
     * Builds the configuration metadata.
     *
     * @return configuration metadata
     */
    ConfigurationMetadata build();
  }

  private static class BuilderImpl implements Builder, PropertyBuilder {
    private final Map<String, PropertyMetadata> properties = new LinkedHashMap<>();
    private String currentKey;
    private PropertyMetadata.Builder currentPropertyBuilder;

    @Override
    public PropertyBuilder property(String key) {
      finishCurrentProperty();
      currentKey = key;
      currentPropertyBuilder = PropertyMetadata.builder(key);
      return this;
    }

    @Override
    public PropertyBuilder type(String type) {
      currentPropertyBuilder.type(type);
      return this;
    }

    @Override
    public PropertyBuilder description(String description) {
      currentPropertyBuilder.description(description);
      return this;
    }

    @Override
    public PropertyBuilder defaultValue(String defaultValue) {
      currentPropertyBuilder.defaultValue(defaultValue);
      return this;
    }

    @Override
    public PropertyBuilder required(boolean required) {
      currentPropertyBuilder.required(required);
      return this;
    }

    @Override
    public PropertyBuilder example(String example) {
      currentPropertyBuilder.example(example);
      return this;
    }

    @Override
    public PropertyBuilder range(long min, long max) {
      currentPropertyBuilder.range(min, max);
      return this;
    }

    @Override
    public PropertyBuilder allowedValues(String... values) {
      currentPropertyBuilder.allowedValues(values);
      return this;
    }

    @Override
    public ConfigurationMetadata build() {
      finishCurrentProperty();
      return new ConfigurationMetadata(properties);
    }

    private void finishCurrentProperty() {
      if (currentPropertyBuilder != null) {
        properties.put(currentKey, currentPropertyBuilder.build());
        currentPropertyBuilder = null;
        currentKey = null;
      }
    }
  }
}
