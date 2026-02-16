package com.marcusprado02.commons.ports.serialization;

import com.marcusprado02.commons.kernel.result.Result;

import java.util.List;
import java.util.Optional;

/**
 * Registry for managing serialization schemas.
 */
public interface SchemaRegistry {

  /**
   * Registers a new schema in the registry.
   *
   * @param schema the schema to register
   * @return success result or failure with problem details
   */
  Result<Void> register(Schema schema);

  /**
   * Gets a schema by name and version.
   *
   * @param name    the schema name
   * @param version the schema version
   * @return the schema if found, empty otherwise
   */
  Optional<Schema> getSchema(String name, String version);

  /**
   * Gets the latest version of a schema by name.
   *
   * @param name the schema name
   * @return the latest schema version if found, empty otherwise
   */
  Optional<Schema> getLatestSchema(String name);

  /**
   * Gets all versions of a schema by name.
   *
   * @param name the schema name
   * @return list of all schema versions
   */
  List<Schema> getAllVersions(String name);

  /**
   * Gets all registered schemas.
   *
   * @return list of all schemas in the registry
   */
  List<Schema> getAllSchemas();

  /**
   * Checks if a schema is compatible with another schema.
   *
   * @param sourceSchema the source schema
   * @param targetSchema the target schema
   * @return validation result indicating compatibility
   */
  ValidationResult isCompatible(Schema sourceSchema, Schema targetSchema);

  /**
   * Validates a schema definition.
   *
   * @param schema the schema to validate
   * @return validation result
   */
  ValidationResult validateSchema(Schema schema);

  /**
   * Deletes a schema from the registry.
   *
   * @param name    the schema name
   * @param version the schema version
   * @return success result or failure with problem details
   */
  Result<Void> deleteSchema(String name, String version);

  /**
   * Checks if a schema exists in the registry.
   *
   * @param name    the schema name
   * @param version the schema version
   * @return true if the schema exists
   */
  boolean exists(String name, String version);

  /**
   * Gets schema evolution path from source to target version.
   *
   * @param name          the schema name
   * @param sourceVersion the source version
   * @param targetVersion the target version
   * @return list of schemas in evolution order, or empty if no path exists
   */
  List<Schema> getEvolutionPath(String name, String sourceVersion, String targetVersion);
}
