package com.marcusprado02.commons.adapters.serialization.protobuf;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.StringValue;
import com.marcusprado02.commons.kernel.result.Result;
import com.marcusprado02.commons.ports.serialization.SerializationFormat;
import com.marcusprado02.commons.ports.serialization.SerializationOptions;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ProtobufSerializationAdapter} using {@code StringValue} as a convenient
 * well-known Protobuf message type that is already compiled and available in protobuf-java.
 */
class ProtobufSerializationAdapterTest {

  private final ProtobufSerializationAdapter<StringValue> adapter =
      new ProtobufSerializationAdapter<>(StringValue.class);

  private static StringValue sv(String value) {
    return StringValue.newBuilder().setValue(value).build();
  }

  // ── serialize: PROTOBUF binary format ────────────────────────────────────

  @Test
  void serializeShouldProduceBinaryBytes() {
    Result<byte[]> result = adapter.serialize(sv("hello"));

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).hasSizeGreaterThan(0);
  }

  @Test
  void serializeWithProtobufFormatShouldWork() {
    var options = SerializationOptions.builder().format(SerializationFormat.PROTOBUF).build();

    Result<byte[]> result = adapter.serialize(sv("world"), options);

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull()).hasSizeGreaterThan(0);
  }

  // ── serialize: PROTOBUF_JSON format ──────────────────────────────────────

  @Test
  void serializeToJsonShouldProduceJsonBytes() {
    var options = SerializationOptions.builder().format(SerializationFormat.PROTOBUF_JSON).build();

    Result<byte[]> result = adapter.serialize(sv("json-test"), options);

    assertThat(result.isOk()).isTrue();
    String json = new String(result.getOrNull());
    assertThat(json).contains("json-test");
  }

  // ── serialize: PROTOBUF_TEXT format ──────────────────────────────────────

  @Test
  void serializeToTextShouldProduceReadableBytes() {
    var options = SerializationOptions.builder().format(SerializationFormat.PROTOBUF_TEXT).build();

    Result<byte[]> result = adapter.serialize(sv("text-test"), options);

    assertThat(result.isOk()).isTrue();
    assertThat(new String(result.getOrNull())).contains("text-test");
  }

  // ── serialize: unsupported format ────────────────────────────────────────

  @Test
  void serializeWithUnsupportedFormatShouldReturnFail() {
    var options = SerializationOptions.builder().format(SerializationFormat.AVRO).build();

    Result<byte[]> result = adapter.serialize(sv("x"), options);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("UNSUPPORTED_FORMAT");
  }

  // ── serialize to OutputStream ─────────────────────────────────────────────

  @Test
  void serializeToOutputStreamShouldWriteBytes() throws Exception {
    var baos = new ByteArrayOutputStream();
    Result<Void> result = adapter.serialize(sv("stream-test"), baos);

    assertThat(result.isOk()).isTrue();
    assertThat(baos.toByteArray()).hasSizeGreaterThan(0);
  }

  @Test
  void serializeToOutputStreamWithOptionsShouldWork() throws Exception {
    var options = SerializationOptions.builder().format(SerializationFormat.PROTOBUF_JSON).build();
    var baos = new ByteArrayOutputStream();

    Result<Void> result = adapter.serialize(sv("opts-stream"), baos, options);

    assertThat(result.isOk()).isTrue();
    assertThat(new String(baos.toByteArray())).contains("opts-stream");
  }

  // ── deserialize from bytes ────────────────────────────────────────────────

  @Test
  void deserializeFromBytesShouldRecoverOriginalValue() {
    StringValue original = sv("round-trip");
    byte[] bytes = original.toByteArray();

    Result<StringValue> result = adapter.deserialize(bytes, StringValue.class);

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull().getValue()).isEqualTo("round-trip");
  }

  @Test
  void deserializeWithClassMismatchShouldReturnFail() {
    byte[] bytes = sv("x").toByteArray();

    // Use wrong class (com.google.protobuf.Int32Value instead of StringValue)
    @SuppressWarnings("unchecked")
    var wrongAdapter = new ProtobufSerializationAdapter(com.google.protobuf.Int32Value.class);
    Result<?> result = wrongAdapter.deserialize(bytes, StringValue.class);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("CLASS_MISMATCH");
  }

  // ── deserialize from InputStream ─────────────────────────────────────────

  @Test
  void deserializeFromInputStreamShouldWork() {
    StringValue original = sv("from-stream");
    InputStream in = new ByteArrayInputStream(original.toByteArray());

    Result<StringValue> result = adapter.deserialize(in, StringValue.class);

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull().getValue()).isEqualTo("from-stream");
  }

  @Test
  void deserializeFromInputStreamWithOptionsShouldWork() {
    StringValue original = sv("opts-stream-deser");
    InputStream in = new ByteArrayInputStream(original.toByteArray());

    Result<StringValue> result =
        adapter.deserialize(in, StringValue.class, SerializationOptions.defaults());

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull().getValue()).isEqualTo("opts-stream-deser");
  }

  // ── getContentType / getFormat ────────────────────────────────────────────

  @Test
  void getContentTypeShouldReturnProtobufMimeType() {
    assertThat(adapter.getContentType()).isEqualTo("application/x-protobuf");
  }

  @Test
  void getFormatShouldReturnProtobuf() {
    assertThat(adapter.getFormat()).isEqualTo(SerializationFormat.PROTOBUF);
  }

  // ── validate ──────────────────────────────────────────────────────────────

  @Test
  void validateWithoutSchemaRegistryShouldReturnValidResult() {
    // adapter was created without a schema registry
    var schema =
        new com.marcusprado02.commons.ports.serialization.Schema(
            "StringValue", "1.0", "{}", SerializationFormat.PROTOBUF);

    Result<com.marcusprado02.commons.ports.serialization.ValidationResult> result =
        adapter.validate(sv("v"), schema);

    assertThat(result.isOk()).isTrue();
    assertThat(result.getOrNull().isValid()).isTrue();
  }

  // ── ProtobufSerialization factory ─────────────────────────────────────────

  @Test
  void forMessageShouldReturnWorkingAdapter() {
    var port = ProtobufSerialization.forMessage(StringValue.class);

    Result<byte[]> result = port.serialize(sv("factory"));
    assertThat(result.isOk()).isTrue();
  }

  // ── InMemoryProtobufSchemaRegistry ───────────────────────────────────────

  @Test
  void schemaRegistryShouldRegisterAndRetrieveSchema() {
    var registry = new InMemoryProtobufSchemaRegistry();
    var schema =
        new com.marcusprado02.commons.ports.serialization.Schema(
            "TestMsg",
            "1.0",
            "syntax=\"proto3\"; message TestMsg { string value = 1; }",
            SerializationFormat.PROTOBUF);

    Result<Void> registerResult = registry.register(schema);
    assertThat(registerResult.isOk()).isTrue();

    assertThat(registry.getSchema("TestMsg", "1.0")).isPresent();
    assertThat(registry.getLatestSchema("TestMsg")).isPresent();
  }

  @Test
  void schemaRegistryShouldReturnEmptyForUnknownSchema() {
    var registry = new InMemoryProtobufSchemaRegistry();
    assertThat(registry.getSchema("Unknown", "1.0")).isEmpty();
    assertThat(registry.getLatestSchema("Unknown")).isEmpty();
  }

  // ── InMemoryProtobufSchemaRegistry: extended coverage ────────────────────

  private static com.marcusprado02.commons.ports.serialization.Schema protoSchema(
      String name, String version) {
    return new com.marcusprado02.commons.ports.serialization.Schema(
        name,
        version,
        "syntax=\"proto3\"; package test; message M {}",
        SerializationFormat.PROTOBUF);
  }

  @Test
  void schemaRegistryGetAllVersionsShouldReturnAll() {
    var registry = new InMemoryProtobufSchemaRegistry();
    registry.register(protoSchema("Msg", "1.0"));
    registry.register(protoSchema("Msg", "2.0"));

    assertThat(registry.getAllVersions("Msg")).hasSize(2);
    assertThat(registry.getAllVersions("UnknownMsg")).isEmpty();
  }

  @Test
  void schemaRegistryGetAllSchemasShouldReturnAll() {
    var registry = new InMemoryProtobufSchemaRegistry();
    registry.register(protoSchema("A", "1.0"));
    registry.register(protoSchema("B", "1.0"));

    assertThat(registry.getAllSchemas()).hasSize(2);
  }

  @Test
  void schemaRegistryExistsShouldReturnCorrectly() {
    var registry = new InMemoryProtobufSchemaRegistry();
    registry.register(protoSchema("Msg", "1.0"));

    assertThat(registry.exists("Msg", "1.0")).isTrue();
    assertThat(registry.exists("Msg", "9.9")).isFalse();
    assertThat(registry.exists("Other", "1.0")).isFalse();
  }

  @Test
  void schemaRegistryDeleteShouldRemoveSchema() {
    var registry = new InMemoryProtobufSchemaRegistry();
    registry.register(protoSchema("Msg", "1.0"));

    Result<Void> deleteResult = registry.deleteSchema("Msg", "1.0");
    assertThat(deleteResult.isOk()).isTrue();
    assertThat(registry.getSchema("Msg", "1.0")).isEmpty();
  }

  @Test
  void schemaRegistryDeleteNonExistentShouldFail() {
    var registry = new InMemoryProtobufSchemaRegistry();

    Result<Void> result = registry.deleteSchema("NoSuch", "1.0");
    assertThat(result.isFail()).isTrue();
  }

  @Test
  void schemaRegistryDeleteNonExistentVersionShouldFail() {
    var registry = new InMemoryProtobufSchemaRegistry();
    registry.register(protoSchema("Msg", "1.0"));

    Result<Void> result = registry.deleteSchema("Msg", "9.9");
    assertThat(result.isFail()).isTrue();
  }

  @Test
  void schemaRegistryIsCompatibleSameSchemaShouldReturnValid() {
    var registry = new InMemoryProtobufSchemaRegistry();
    var v1 = protoSchema("Msg", "1.0");
    var v2 = protoSchema("Msg", "2.0");

    var result = registry.isCompatible(v1, v2);
    assertThat(result.isValid()).isTrue();
  }

  @Test
  void schemaRegistryIsCompatibleDifferentNamesShouldReturnInvalid() {
    var registry = new InMemoryProtobufSchemaRegistry();
    var a = protoSchema("A", "1.0");
    var b = protoSchema("B", "1.0");

    var result = registry.isCompatible(a, b);
    assertThat(result.isValid()).isFalse();
  }

  @Test
  void schemaRegistryIsCompatibleOlderTargetShouldWarn() {
    var registry = new InMemoryProtobufSchemaRegistry();
    var v2 = protoSchema("Msg", "2.0");
    var v1 = protoSchema("Msg", "1.0");

    // Target is older than source → warning
    var result = registry.isCompatible(v2, v1);
    assertThat(result.isValid()).isTrue();
    assertThat(result.getWarnings()).isNotEmpty();
  }

  @Test
  void schemaRegistryGetEvolutionPathShouldReturnPath() {
    var registry = new InMemoryProtobufSchemaRegistry();
    registry.register(protoSchema("Msg", "1.0"));
    registry.register(protoSchema("Msg", "2.0"));

    var path = registry.getEvolutionPath("Msg", "1.0", "2.0");
    assertThat(path).hasSize(2);
  }

  @Test
  void schemaRegistryGetEvolutionPathForUnknownShouldReturnEmpty() {
    var registry = new InMemoryProtobufSchemaRegistry();

    var path = registry.getEvolutionPath("NoSuch", "1.0", "2.0");
    assertThat(path).isEmpty();
  }

  @Test
  void schemaRegistryValidateSchemaShouldReturnWarningsForMissingKeys() {
    var registry = new InMemoryProtobufSchemaRegistry();
    var schema =
        new com.marcusprado02.commons.ports.serialization.Schema(
            "Msg", "1.0", "message M {}", SerializationFormat.PROTOBUF);

    var result = registry.validateSchema(schema);
    assertThat(result.isValid()).isTrue();
    // "message M {}" lacks "syntax" and "package" — should have warnings
    assertThat(result.getWarnings()).isNotEmpty();
  }

  @Test
  void schemaRegistryGetLatestShouldPreferHigherVersion() {
    var registry = new InMemoryProtobufSchemaRegistry();
    registry.register(protoSchema("Msg", "1.0"));
    registry.register(protoSchema("Msg", "2.0"));
    registry.register(protoSchema("Msg", "1.5"));

    var latest = registry.getLatestSchema("Msg");
    assertThat(latest).isPresent();
    assertThat(latest.get().getVersion()).isEqualTo("2.0");
  }

  // ── ProtobufSerialization.Builder ─────────────────────────────────────────

  @Test
  void builderShouldBuildAdapterWithRegistry() {
    var registry = new InMemoryProtobufSchemaRegistry();
    var port =
        ProtobufSerialization.builder(StringValue.class)
            .withSchemaRegistry(registry)
            .validateSchema(false)
            .build();

    Result<byte[]> result = port.serialize(sv("builder"));
    assertThat(result.isOk()).isTrue();
  }

  @Test
  void builderWithInMemoryRegistryShouldWork() {
    var port =
        ProtobufSerialization.builder(StringValue.class)
            .withInMemoryRegistry()
            .validateSchema(false)
            .build();

    Result<byte[]> result = port.serialize(sv("in-memory"));
    assertThat(result.isOk()).isTrue();
  }

  @Test
  void builderWithOptionsShouldWork() {
    var options = SerializationOptions.builder().format(SerializationFormat.PROTOBUF).build();
    var port = ProtobufSerialization.builder(StringValue.class).withOptions(options).build();

    Result<byte[]> result = port.serialize(sv("with-opts"));
    assertThat(result.isOk()).isTrue();
  }

  @Test
  void builderValidateSchemaShouldWork() {
    var port = ProtobufSerialization.builder(StringValue.class).validateSchema(true).build();

    Result<byte[]> result = port.serialize(sv("validate"));
    assertThat(result.isOk()).isTrue();
  }

  @Test
  void builderUseSchemaRegistryShouldWork() {
    var port = ProtobufSerialization.builder(StringValue.class).useSchemaRegistry(false).build();

    Result<byte[]> result = port.serialize(sv("no-reg"));
    assertThat(result.isOk()).isTrue();
  }

  @Test
  void forMessageWithRegistryShouldReturnNonNullPort() {
    // 2-arg factory: just verify construction — serializing would require schema validation
    var registry = new InMemoryProtobufSchemaRegistry();
    var port = ProtobufSerialization.forMessage(StringValue.class, registry);
    assertThat(port).isNotNull();
  }

  @Test
  void forMessageWithOptionsAndRegistryShouldWork() {
    // 3-arg factory: explicit options disable validation so serialize succeeds
    var registry = new InMemoryProtobufSchemaRegistry();
    var options = SerializationOptions.builder().validateSchema(false).build();
    var port = ProtobufSerialization.forMessage(StringValue.class, registry, options);

    Result<byte[]> result = port.serialize(sv("with-reg"));
    assertThat(result.isOk()).isTrue();
  }

  @Test
  void createInMemoryRegistryShouldReturnRegistry() {
    var registry = ProtobufSerialization.createInMemoryRegistry();
    assertThat(registry).isNotNull().isInstanceOf(InMemoryProtobufSchemaRegistry.class);
  }
}
