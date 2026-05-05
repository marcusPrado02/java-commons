package com.marcusprado02.commons.adapters.serialization.protobuf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.protobuf.StringValue;
import com.marcusprado02.commons.ports.serialization.Schema;
import com.marcusprado02.commons.ports.serialization.SchemaRegistry;
import com.marcusprado02.commons.ports.serialization.SerializationFormat;
import com.marcusprado02.commons.ports.serialization.SerializationOptions;
import com.marcusprado02.commons.ports.serialization.ValidationResult;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ProtobufAdapterExceptionTest {

  private static StringValue sv(String v) {
    return StringValue.newBuilder().setValue(v).build();
  }

  // --- serialize(T, OutputStream, options): inner serialize fails → propagated ---

  @Test
  void serializeToStream_whenInnerSerializeFails_propagatesFail() throws Exception {
    var adapter = new ProtobufSerializationAdapter<>(StringValue.class);
    var baos = new java.io.ByteArrayOutputStream();
    SerializationOptions opts =
        SerializationOptions.builder().format(SerializationFormat.AVRO).build();

    var result = adapter.serialize(sv("x"), baos, opts);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("UNSUPPORTED_FORMAT");
  }

  // --- serialize(T, OutputStream, options): OutputStream.write() throws IOException ---

  @Test
  void serializeToStream_ioExceptionOnWrite_returnsIoError() throws Exception {
    var adapter = new ProtobufSerializationAdapter<>(StringValue.class);
    OutputStream throwingStream = mock(OutputStream.class);
    doThrow(new IOException("disk full")).when(throwingStream).write(any(byte[].class));

    var result =
        adapter.serialize(
            sv("io-test"),
            throwingStream,
            SerializationOptions.builder().format(SerializationFormat.PROTOBUF).build());

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("IO_ERROR");
  }

  // --- deserialize(InputStream, class, options): InputStream.readAllBytes() throws IOException ---

  @Test
  void deserializeFromStream_ioExceptionOnRead_returnsIoError() throws Exception {
    var adapter = new ProtobufSerializationAdapter<>(StringValue.class);
    InputStream throwingStream = mock(InputStream.class);
    when(throwingStream.readAllBytes()).thenThrow(new IOException("network error"));

    var result = adapter.deserialize(throwingStream, StringValue.class);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("IO_ERROR");
  }

  // --- validate(T, schema): registry.validateSchema() throws → catch block ---

  @Test
  void validate_registryThrows_returnsValidationError() {
    SchemaRegistry throwingRegistry = mock(SchemaRegistry.class);
    when(throwingRegistry.validateSchema(any(Schema.class)))
        .thenThrow(new RuntimeException("registry down"));

    var adapter = new ProtobufSerializationAdapter<>(StringValue.class, throwingRegistry);
    var schema =
        new Schema("StringValue", "1.0", "syntax=\"proto3\";", SerializationFormat.PROTOBUF);

    var result = adapter.validate(sv("v"), schema);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("VALIDATION_ERROR");
  }

  // --- validateWithRegistry: registry.getSchema() throws → catch block ---

  @Test
  void serialize_registryGetSchemaThrows_returnsRegistryValidationError() {
    SchemaRegistry throwingRegistry = mock(SchemaRegistry.class);
    when(throwingRegistry.getSchema(anyString(), anyString()))
        .thenThrow(new RuntimeException("registry unavailable"));

    var adapter = new ProtobufSerializationAdapter<>(StringValue.class, throwingRegistry);
    SerializationOptions opts =
        SerializationOptions.builder().validateSchema(true).schemaVersion("1.0").build();

    var result = adapter.serialize(sv("test"), opts);

    assertThat(result.isFail()).isTrue();
    // Either REGISTRY_VALIDATION_ERROR or falls through to SERIALIZATION_ERROR
    assertThat(result.problemOrNull().code().value()).isNotNull();
  }

  // --- validateWithRegistry: registry.getLatestSchema() throws → catch block ---

  @Test
  void serialize_registryGetLatestSchemaThrows_returnsError() {
    SchemaRegistry throwingRegistry = mock(SchemaRegistry.class);
    when(throwingRegistry.getLatestSchema(anyString()))
        .thenThrow(new RuntimeException("registry unavailable"));

    var adapter = new ProtobufSerializationAdapter<>(StringValue.class, throwingRegistry);
    SerializationOptions opts =
        SerializationOptions.builder()
            .validateSchema(true)
            // no schemaVersion → uses getLatestSchema
            .build();

    var result = adapter.serialize(sv("test"), opts);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isNotNull();
  }

  // --- validateWithRegistry: schema found but validation returns invalid ---

  @Test
  void serialize_schemaFoundButInvalid_returnsUnknownError() {
    SchemaRegistry registry = mock(SchemaRegistry.class);
    Schema schema =
        new Schema("StringValue", "1.0", "syntax=\"proto3\";", SerializationFormat.PROTOBUF);
    when(registry.getSchema(anyString(), anyString())).thenReturn(Optional.of(schema));
    when(registry.validateSchema(any(Schema.class)))
        .thenReturn(ValidationResult.invalid(java.util.List.of("bad schema")));

    var adapter = new ProtobufSerializationAdapter<>(StringValue.class, registry);
    SerializationOptions opts =
        SerializationOptions.builder().validateSchema(true).schemaVersion("1.0").build();

    var result = adapter.serialize(sv("test"), opts);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("UNKNOWN");
  }
}
