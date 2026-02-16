package com.marcusprado02.commons.ports.serialization;

import java.util.Objects;

/**
 * Configuration options for serialization operations.
 */
public class SerializationOptions {

  private final boolean validateSchema;
  private final boolean useSchemaRegistry;
  private final boolean includeMetadata;
  private final String schemaVersion;
  private final SerializationFormat format;

  private SerializationOptions(Builder builder) {
    this.validateSchema = builder.validateSchema;
    this.useSchemaRegistry = builder.useSchemaRegistry;
    this.includeMetadata = builder.includeMetadata;
    this.schemaVersion = builder.schemaVersion;
    this.format = builder.format;
  }

  /**
   * Creates a new builder for serialization options.
   *
   * @return a new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates default serialization options.
   *
   * @return default options
   */
  public static SerializationOptions defaults() {
    return builder().build();
  }

  /**
   * Checks if schema validation is enabled.
   *
   * @return true if schema validation is enabled
   */
  public boolean isValidateSchema() {
    return validateSchema;
  }

  /**
   * Checks if schema registry usage is enabled.
   *
   * @return true if schema registry is used
   */
  public boolean isUseSchemaRegistry() {
    return useSchemaRegistry;
  }

  /**
   * Checks if metadata should be included in serialization.
   *
   * @return true if metadata is included
   */
  public boolean isIncludeMetadata() {
    return includeMetadata;
  }

  /**
   * Gets the specific schema version to use.
   *
   * @return the schema version, or null for latest
   */
  public String getSchemaVersion() {
    return schemaVersion;
  }

  /**
   * Gets the preferred serialization format.
   *
   * @return the serialization format, or null for adapter default
   */
  public SerializationFormat getFormat() {
    return format;
  }

  /**
   * Builder class for SerializationOptions.
   */
  public static class Builder {
    private boolean validateSchema = true;
    private boolean useSchemaRegistry = false;
    private boolean includeMetadata = false;
    private String schemaVersion;
    private SerializationFormat format;

    /**
     * Sets whether to validate schema during serialization.
     *
     * @param validateSchema true to enable validation
     * @return this builder
     */
    public Builder validateSchema(boolean validateSchema) {
      this.validateSchema = validateSchema;
      return this;
    }

    /**
     * Sets whether to use schema registry.
     *
     * @param useSchemaRegistry true to use registry
     * @return this builder
     */
    public Builder useSchemaRegistry(boolean useSchemaRegistry) {
      this.useSchemaRegistry = useSchemaRegistry;
      return this;
    }

    /**
     * Sets whether to include metadata in serialization.
     *
     * @param includeMetadata true to include metadata
     * @return this builder
     */
    public Builder includeMetadata(boolean includeMetadata) {
      this.includeMetadata = includeMetadata;
      return this;
    }

    /**
     * Sets the specific schema version to use.
     *
     * @param schemaVersion the schema version
     * @return this builder
     */
    public Builder schemaVersion(String schemaVersion) {
      this.schemaVersion = schemaVersion;
      return this;
    }

    /**
     * Sets the preferred serialization format.
     *
     * @param format the serialization format
     * @return this builder
     */
    public Builder format(SerializationFormat format) {
      this.format = format;
      return this;
    }

    /**
     * Builds the serialization options.
     *
     * @return the configured options
     */
    public SerializationOptions build() {
      return new SerializationOptions(this);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SerializationOptions that = (SerializationOptions) o;
    return validateSchema == that.validateSchema &&
           useSchemaRegistry == that.useSchemaRegistry &&
           includeMetadata == that.includeMetadata &&
           Objects.equals(schemaVersion, that.schemaVersion) &&
           format == that.format;
  }

  @Override
  public int hashCode() {
    return Objects.hash(validateSchema, useSchemaRegistry, includeMetadata, schemaVersion, format);
  }

  @Override
  public String toString() {
    return "SerializationOptions{" +
           "validateSchema=" + validateSchema +
           ", useSchemaRegistry=" + useSchemaRegistry +
           ", includeMetadata=" + includeMetadata +
           ", schemaVersion='" + schemaVersion + '\'' +
           ", format=" + format +
           '}';
  }
}
