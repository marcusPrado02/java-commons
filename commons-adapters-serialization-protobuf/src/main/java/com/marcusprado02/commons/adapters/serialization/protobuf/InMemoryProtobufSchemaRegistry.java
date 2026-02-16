package com.marcusprado02.commons.adapters.serialization.protobuf;

import com.marcusprado02.commons.kernel.errors.Problem;
import com.marcusprado02.commons.kernel.errors.ErrorCode;
import com.marcusprado02.commons.kernel.errors.ErrorCategory;
import com.marcusprado02.commons.kernel.errors.Severity;
import com.marcusprado02.commons.kernel.result.Result;
import com.marcusprado02.commons.ports.serialization.Schema;
import com.marcusprado02.commons.ports.serialization.SchemaRegistry;
import com.marcusprado02.commons.ports.serialization.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of SchemaRegistry for Protocol Buffers.
 */
public class InMemoryProtobufSchemaRegistry implements SchemaRegistry {

  private static final Logger log = LoggerFactory.getLogger(InMemoryProtobufSchemaRegistry.class);

  private final Map<String, Map<String, Schema>> schemas = new ConcurrentHashMap<>();

  @Override
  public Result<Void> register(Schema schema) {
    try {
      Objects.requireNonNull(schema, "Schema cannot be null");

      // Validate schema before registration
      ValidationResult validation = validateSchema(schema);
      if (!validation.isValid()) {
        return Result.fail(Problem.builder()
            .type("INVALID_SCHEMA")
            .title("Invalid schema")
            .detail("Schema validation failed: " + String.join(", ", validation.getErrors()))
            .build());
      }

      schemas.computeIfAbsent(schema.getName(), k -> new ConcurrentHashMap<>())
              .put(schema.getVersion(), schema);

      log.info("Registered schema: {} version {}", schema.getName(), schema.getVersion());
      return Result.ok(null);

    } catch (Exception e) {
      log.error("Failed to register schema", e);
      return Result.fail(Problem.builder()
          .type("REGISTRATION_ERROR")
          .title("Schema registration failed")
          .detail("Failed to register schema: " + e.getMessage())
          .build());
    }
  }

  @Override
  public Optional<Schema> getSchema(String name, String version) {
    Objects.requireNonNull(name, "Schema name cannot be null");
    Objects.requireNonNull(version, "Schema version cannot be null");

    return Optional.ofNullable(schemas.get(name))
                   .map(versions -> versions.get(version));
  }

  @Override
  public Optional<Schema> getLatestSchema(String name) {
    Objects.requireNonNull(name, "Schema name cannot be null");

    Map<String, Schema> versions = schemas.get(name);
    if (versions == null || versions.isEmpty()) {
      return Optional.empty();
    }

    // Find the latest version (assuming semantic versioning)
    return versions.values().stream()
                   .max(Comparator.comparing(s -> parseVersion(s.getVersion())));
  }

  @Override
  public List<Schema> getAllVersions(String name) {
    Objects.requireNonNull(name, "Schema name cannot be null");

    Map<String, Schema> versions = schemas.get(name);
    if (versions == null) {
      return Collections.emptyList();
    }

    return new ArrayList<>(versions.values());
  }

  @Override
  public List<Schema> getAllSchemas() {
    return schemas.values().stream()
                  .flatMap(versions -> versions.values().stream())
                  .toList();
  }

  @Override
  public ValidationResult isCompatible(Schema sourceSchema, Schema targetSchema) {
    Objects.requireNonNull(sourceSchema, "Source schema cannot be null");
    Objects.requireNonNull(targetSchema, "Target schema cannot be null");

    // Basic compatibility check for Protocol Buffers
    if (!sourceSchema.getName().equals(targetSchema.getName())) {
      return ValidationResult.invalid(List.of("Schema names do not match"));
    }

    if (!sourceSchema.getFormat().equals(targetSchema.getFormat())) {
      return ValidationResult.invalid(List.of("Schema formats do not match"));
    }

    // For Protocol Buffers, we would need to parse .proto definitions
    // and check field compatibility (field numbers, types, etc.)
    // This is a simplified implementation

    List<String> warnings = new ArrayList<>();

    // Version comparison
    Version sourceVersion = parseVersion(sourceSchema.getVersion());
    Version targetVersion = parseVersion(targetSchema.getVersion());

    if (targetVersion.compareTo(sourceVersion) < 0) {
      warnings.add("Target schema version is older than source version");
    }

    return warnings.isEmpty()
        ? ValidationResult.valid()
        : ValidationResult.validWithWarnings(warnings);
  }

  @Override
  public ValidationResult validateSchema(Schema schema) {
    Objects.requireNonNull(schema, "Schema cannot be null");

    List<String> errors = new ArrayList<>();
    List<String> warnings = new ArrayList<>();

    // Basic validation
    if (schema.getName().trim().isEmpty()) {
      errors.add("Schema name cannot be empty");
    }

    if (schema.getVersion().trim().isEmpty()) {
      errors.add("Schema version cannot be empty");
    }

    if (schema.getDefinition().trim().isEmpty()) {
      errors.add("Schema definition cannot be empty");
    }

    // Protocol Buffers specific validation
    String definition = schema.getDefinition();
    if (!definition.contains("syntax")) {
      warnings.add("Schema definition should specify syntax version");
    }

    if (!definition.contains("package")) {
      warnings.add("Schema definition should specify package");
    }

    return errors.isEmpty()
        ? (warnings.isEmpty() ? ValidationResult.valid() : ValidationResult.validWithWarnings(warnings))
        : ValidationResult.invalid(errors, warnings);
  }

  @Override
  public Result<Void> deleteSchema(String name, String version) {
    Objects.requireNonNull(name, "Schema name cannot be null");
    Objects.requireNonNull(version, "Schema version cannot be null");

    try {
      Map<String, Schema> versions = schemas.get(name);
      if (versions == null) {
        return Result.fail(Problem.builder()
            .type("SCHEMA_NOT_FOUND")
            .title("Schema not found")
            .detail("No schema found with name: " + name)
            .build());
      }

      Schema removed = versions.remove(version);
      if (removed == null) {
        return Result.fail(Problem.builder()
            .type("VERSION_NOT_FOUND")
            .title("Schema version not found")
            .detail("No schema found with name: " + name + " version: " + version)
            .build());
      }

      // Remove schema name entry if no versions left
      if (versions.isEmpty()) {
        schemas.remove(name);
      }

      log.info("Deleted schema: {} version {}", name, version);
      return Result.ok(null);

    } catch (Exception e) {
      log.error("Failed to delete schema", e);
      return Result.fail(Problem.builder()
          .type("DELETION_ERROR")
          .title("Schema deletion failed")
          .detail("Failed to delete schema: " + e.getMessage())
          .build());
    }
  }

  @Override
  public boolean exists(String name, String version) {
    return getSchema(name, version).isPresent();
  }

  @Override
  public List<Schema> getEvolutionPath(String name, String sourceVersion, String targetVersion) {
    Objects.requireNonNull(name, "Schema name cannot be null");
    Objects.requireNonNull(sourceVersion, "Source version cannot be null");
    Objects.requireNonNull(targetVersion, "Target version cannot be null");

    Map<String, Schema> versions = schemas.get(name);
    if (versions == null) {
      return Collections.emptyList();
    }

    Schema source = versions.get(sourceVersion);
    Schema target = versions.get(targetVersion);

    if (source == null || target == null) {
      return Collections.emptyList();
    }

    // For simplicity, return direct path
    // In a real implementation, this would compute the evolution path
    // through intermediate compatible versions
    return Arrays.asList(source, target);
  }

  /**
   * Parse version string to Version object for comparison.
   */
  private Version parseVersion(String versionString) {
    try {
      String[] parts = versionString.split("\\.");
      int major = parts.length > 0 ? Integer.parseInt(parts[0]) : 0;
      int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
      int patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
      return new Version(major, minor, patch);
    } catch (NumberFormatException e) {
      // Fallback for non-semantic versions
      return new Version(0, 0, 0);
    }
  }

  /**
   * Simple version representation for comparison.
   */
  private record Version(int major, int minor, int patch) implements Comparable<Version> {
    @Override
    public int compareTo(Version other) {
      int result = Integer.compare(major, other.major);
      if (result != 0) return result;

      result = Integer.compare(minor, other.minor);
      if (result != 0) return result;

      return Integer.compare(patch, other.patch);
    }
  }
}
