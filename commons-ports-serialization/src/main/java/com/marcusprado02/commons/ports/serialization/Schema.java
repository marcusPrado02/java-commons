package com.marcusprado02.commons.ports.serialization;

import java.util.Objects;

/**
 * Represents a serialization schema with version information.
 */
public class Schema {

  private final String name;
  private final String version;
  private final String definition;
  private final SerializationFormat format;

  /**
   * Creates a new schema.
   *
   * @param name       the schema name
   * @param version    the schema version
   * @param definition the schema definition content
   * @param format     the serialization format
   */
  public Schema(String name, String version, String definition, SerializationFormat format) {
    this.name = Objects.requireNonNull(name, "Schema name cannot be null");
    this.version = Objects.requireNonNull(version, "Schema version cannot be null");
    this.definition = Objects.requireNonNull(definition, "Schema definition cannot be null");
    this.format = Objects.requireNonNull(format, "Schema format cannot be null");
  }

  /**
   * Gets the schema name.
   *
   * @return the schema name
   */
  public String getName() {
    return name;
  }

  /**
   * Gets the schema version.
   *
   * @return the schema version
   */
  public String getVersion() {
    return version;
  }

  /**
   * Gets the schema definition content.
   *
   * @return the schema definition
   */
  public String getDefinition() {
    return definition;
  }

  /**
   * Gets the serialization format for this schema.
   *
   * @return the format
   */
  public SerializationFormat getFormat() {
    return format;
  }

  /**
   * Gets the full schema identifier (name:version).
   *
   * @return the schema identifier
   */
  public String getIdentifier() {
    return name + ":" + version;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Schema schema = (Schema) o;
    return Objects.equals(name, schema.name) &&
           Objects.equals(version, schema.version) &&
           Objects.equals(definition, schema.definition) &&
           format == schema.format;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, version, definition, format);
  }

  @Override
  public String toString() {
    return "Schema{" +
           "name='" + name + '\'' +
           ", version='" + version + '\'' +
           ", format=" + format +
           '}';
  }
}
