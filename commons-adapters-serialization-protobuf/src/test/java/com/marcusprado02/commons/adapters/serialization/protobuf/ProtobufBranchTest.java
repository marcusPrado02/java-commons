package com.marcusprado02.commons.adapters.serialization.protobuf;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.StringValue;
import com.marcusprado02.commons.ports.serialization.Schema;
import com.marcusprado02.commons.ports.serialization.SerializationFormat;
import com.marcusprado02.commons.ports.serialization.SerializationOptions;
import com.marcusprado02.commons.ports.serialization.ValidationResult;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProtobufBranchTest {

  // ── InMemoryProtobufSchemaRegistry: validateSchema branch coverage ────────

  @Test
  void validateSchema_withSyntaxAndPackage_returnsValid() {
    InMemoryProtobufSchemaRegistry registry = new InMemoryProtobufSchemaRegistry();
    Schema schema =
        new Schema(
            "TestMsg",
            "1.0.0",
            "syntax = \"proto3\";\npackage test;\nmessage TestMsg {}",
            SerializationFormat.PROTOBUF);
    ValidationResult result = registry.validateSchema(schema);
    assertThat(result.isValid()).isTrue();
    assertThat(result.getWarnings()).isEmpty();
  }

  @Test
  void validateSchema_missingSyntaxAndPackage_returnsWarnings() {
    InMemoryProtobufSchemaRegistry registry = new InMemoryProtobufSchemaRegistry();
    Schema schema =
        new Schema(
            "TestMsg",
            "1.0.0",
            "message TestMsg { string value = 1; }",
            SerializationFormat.PROTOBUF);
    ValidationResult result = registry.validateSchema(schema);
    assertThat(result.isValid()).isTrue();
    assertThat(result.getWarnings()).isNotEmpty();
  }

  @Test
  void validateSchema_emptyDefinition_returnsInvalid() {
    InMemoryProtobufSchemaRegistry registry = new InMemoryProtobufSchemaRegistry();
    Schema schema = new Schema("TestMsg", "1.0.0", "   ", SerializationFormat.PROTOBUF);
    ValidationResult result = registry.validateSchema(schema);
    assertThat(result.isValid()).isFalse();
  }

  // ── InMemoryProtobufSchemaRegistry: isCompatible branches ────────────────

  @Test
  void isCompatible_olderTargetVersion_returnsWarning() {
    InMemoryProtobufSchemaRegistry registry = new InMemoryProtobufSchemaRegistry();
    Schema source =
        new Schema(
            "Msg", "2.0.0", "syntax = \"proto3\";\npackage p;", SerializationFormat.PROTOBUF);
    Schema target =
        new Schema(
            "Msg", "1.0.0", "syntax = \"proto3\";\npackage p;", SerializationFormat.PROTOBUF);
    ValidationResult result = registry.isCompatible(source, target);
    assertThat(result.isValid()).isTrue();
    assertThat(result.getWarnings()).isNotEmpty();
  }

  @Test
  void isCompatible_sameVersion_returnsValid() {
    InMemoryProtobufSchemaRegistry registry = new InMemoryProtobufSchemaRegistry();
    Schema source =
        new Schema(
            "Msg", "1.0.0", "syntax = \"proto3\";\npackage p;", SerializationFormat.PROTOBUF);
    Schema target =
        new Schema(
            "Msg", "1.0.0", "syntax = \"proto3\";\npackage p;", SerializationFormat.PROTOBUF);
    ValidationResult result = registry.isCompatible(source, target);
    assertThat(result.isValid()).isTrue();
    assertThat(result.getWarnings()).isEmpty();
  }

  @Test
  void isCompatible_differentNames_returnsInvalid() {
    InMemoryProtobufSchemaRegistry registry = new InMemoryProtobufSchemaRegistry();
    Schema source =
        new Schema("Msg1", "1.0.0", "syntax = \"proto3\";", SerializationFormat.PROTOBUF);
    Schema target =
        new Schema("Msg2", "1.0.0", "syntax = \"proto3\";", SerializationFormat.PROTOBUF);
    ValidationResult result = registry.isCompatible(source, target);
    assertThat(result.isValid()).isFalse();
  }

  @Test
  void isCompatible_differentFormats_returnsInvalid() {
    InMemoryProtobufSchemaRegistry registry = new InMemoryProtobufSchemaRegistry();
    Schema source =
        new Schema("Msg", "1.0.0", "syntax = \"proto3\";", SerializationFormat.PROTOBUF);
    Schema target =
        new Schema("Msg", "1.0.0", "syntax = \"proto3\";", SerializationFormat.PROTOBUF_JSON);
    ValidationResult result = registry.isCompatible(source, target);
    assertThat(result.isValid()).isFalse();
  }

  // ── InMemoryProtobufSchemaRegistry: Version comparison branches ──────────

  @Test
  void isCompatible_majorVersionDifference_coveredByVersionCompareTo() {
    InMemoryProtobufSchemaRegistry registry = new InMemoryProtobufSchemaRegistry();
    Schema source =
        new Schema(
            "Msg", "2.1.0", "syntax = \"proto3\";\npackage p;", SerializationFormat.PROTOBUF);
    Schema target =
        new Schema(
            "Msg", "1.2.3", "syntax = \"proto3\";\npackage p;", SerializationFormat.PROTOBUF);
    ValidationResult result = registry.isCompatible(source, target);
    assertThat(result.isValid()).isTrue(); // valid with warning
  }

  @Test
  void isCompatible_minorVersionDifference_coveredByVersionCompareTo() {
    InMemoryProtobufSchemaRegistry registry = new InMemoryProtobufSchemaRegistry();
    Schema source =
        new Schema(
            "Msg", "1.2.0", "syntax = \"proto3\";\npackage p;", SerializationFormat.PROTOBUF);
    Schema target =
        new Schema(
            "Msg", "1.1.5", "syntax = \"proto3\";\npackage p;", SerializationFormat.PROTOBUF);
    ValidationResult result = registry.isCompatible(source, target);
    assertThat(result.isValid()).isTrue();
  }

  @Test
  void parseVersion_nonNumericParts_fallsBackToZero() {
    InMemoryProtobufSchemaRegistry registry = new InMemoryProtobufSchemaRegistry();
    // "alpha" version won't parse as numeric → fallback to 0.0.0
    Schema source =
        new Schema(
            "Msg", "alpha", "syntax = \"proto3\";\npackage p;", SerializationFormat.PROTOBUF);
    Schema target =
        new Schema(
            "Msg", "1.0.0", "syntax = \"proto3\";\npackage p;", SerializationFormat.PROTOBUF);
    // Won't throw, just use fallback version
    ValidationResult result = registry.isCompatible(source, target);
    assertThat(result).isNotNull();
  }

  // ── InMemoryProtobufSchemaRegistry: getEvolutionPath ────────────────────

  @Test
  void getEvolutionPath_unknownSchema_returnsEmpty() {
    InMemoryProtobufSchemaRegistry registry = new InMemoryProtobufSchemaRegistry();
    List<Schema> path = registry.getEvolutionPath("Unknown", "1.0", "2.0");
    assertThat(path).isEmpty();
  }

  @Test
  void getEvolutionPath_missingSourceOrTarget_returnsEmpty() {
    InMemoryProtobufSchemaRegistry registry = new InMemoryProtobufSchemaRegistry();
    Schema s =
        new Schema(
            "Msg",
            "1.0.0",
            "syntax = \"proto3\";\npackage p;\nmessage Msg {}",
            SerializationFormat.PROTOBUF);
    registry.register(s);
    // Target version doesn't exist
    List<Schema> path = registry.getEvolutionPath("Msg", "1.0.0", "2.0.0");
    assertThat(path).isEmpty();
  }

  // ── InMemoryProtobufSchemaRegistry: deleteSchema cleanup ─────────────────

  @Test
  void deleteSchema_lastVersion_removesSchemaEntry() {
    InMemoryProtobufSchemaRegistry registry = new InMemoryProtobufSchemaRegistry();
    Schema s =
        new Schema(
            "ToDelete",
            "1.0.0",
            "syntax = \"proto3\";\npackage p;\nmessage ToDelete {}",
            SerializationFormat.PROTOBUF);
    registry.register(s);
    var result = registry.deleteSchema("ToDelete", "1.0.0");
    assertThat(result.isOk()).isTrue();
    assertThat(registry.getAllVersions("ToDelete")).isEmpty();
  }

  @Test
  void deleteSchema_unknownSchema_returnsFail() {
    InMemoryProtobufSchemaRegistry registry = new InMemoryProtobufSchemaRegistry();
    var result = registry.deleteSchema("NonExistent", "1.0");
    assertThat(result.isFail()).isTrue();
  }

  @Test
  void deleteSchema_unknownVersion_returnsFail() {
    InMemoryProtobufSchemaRegistry registry = new InMemoryProtobufSchemaRegistry();
    Schema s =
        new Schema(
            "Msg",
            "1.0.0",
            "syntax = \"proto3\";\npackage p;\nmessage Msg {}",
            SerializationFormat.PROTOBUF);
    registry.register(s);
    var result = registry.deleteSchema("Msg", "9.9.9");
    assertThat(result.isFail()).isTrue();
  }

  // ── ProtobufSerializationAdapter: validateWithRegistry branches ──────────

  @Test
  void serialize_withValidateSchemaAndVersionPresent_schemaNotFound_returnsFail() {
    InMemoryProtobufSchemaRegistry registry = new InMemoryProtobufSchemaRegistry();
    var adapter =
        new ProtobufSerializationAdapter<>(
            StringValue.class, registry, SerializationOptions.defaults());

    SerializationOptions opts =
        SerializationOptions.builder().validateSchema(true).schemaVersion("1.0.0").build();

    var result = adapter.serialize(StringValue.of("hello"), opts);
    assertThat(result.isFail()).isTrue();
  }

  @Test
  void serialize_withValidateSchemaNoVersion_usesLatestSchema_notFound_returnsFail() {
    InMemoryProtobufSchemaRegistry registry = new InMemoryProtobufSchemaRegistry();
    var adapter =
        new ProtobufSerializationAdapter<>(
            StringValue.class, registry, SerializationOptions.defaults());

    SerializationOptions opts =
        SerializationOptions.builder()
            .validateSchema(true)
            // no schemaVersion → uses getLatestSchema
            .build();

    var result = adapter.serialize(StringValue.of("hello"), opts);
    assertThat(result.isFail()).isTrue();
  }

  @Test
  void serialize_withValidateSchemaAndRegisteredSchema_returnsOk() {
    InMemoryProtobufSchemaRegistry registry = new InMemoryProtobufSchemaRegistry();
    Schema schema =
        new Schema(
            "StringValue",
            "1.0.0",
            "syntax = \"proto3\";\npackage google.protobuf;\nmessage StringValue { string value = 1; }",
            SerializationFormat.PROTOBUF);
    registry.register(schema);

    var adapter =
        new ProtobufSerializationAdapter<>(
            StringValue.class, registry, SerializationOptions.defaults());

    SerializationOptions opts =
        SerializationOptions.builder().validateSchema(true).schemaVersion("1.0.0").build();

    var result = adapter.serialize(StringValue.of("hello"), opts);
    assertThat(result.isOk()).isTrue();
  }

  // ── ProtobufSerializationAdapter: validate() with registry ───────────────

  @Test
  void validate_withSchemaRegistry_usesRegistryValidation() {
    InMemoryProtobufSchemaRegistry registry = new InMemoryProtobufSchemaRegistry();
    Schema schema =
        new Schema(
            "StringValue",
            "1.0.0",
            "syntax = \"proto3\";\npackage p;\nmessage StringValue {}",
            SerializationFormat.PROTOBUF);
    var adapter = new ProtobufSerializationAdapter<>(StringValue.class, registry);
    var result = adapter.validate(StringValue.of("x"), schema);
    assertThat(result.isOk()).isTrue();
  }

  // ── ProtobufSerializationAdapter: serialize with null format ─────────────

  @Test
  void serialize_nullFormat_defaultsToProtobuf() {
    var adapter = new ProtobufSerializationAdapter<>(StringValue.class);
    SerializationOptions opts = SerializationOptions.builder().format(null).build();
    var result = adapter.serialize(StringValue.of("test"), opts);
    assertThat(result.isOk()).isTrue();
  }
}
