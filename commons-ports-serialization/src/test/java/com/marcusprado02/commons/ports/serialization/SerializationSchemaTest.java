package com.marcusprado02.commons.ports.serialization;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SerializationSchemaTest {

  // --- Schema ---

  @Test
  void schema_valid_construction() {
    Schema s = new Schema("user-events", "1.0", "{\"type\":\"object\"}", SerializationFormat.JSON);
    assertEquals("user-events", s.getName());
    assertEquals("1.0", s.getVersion());
    assertEquals("{\"type\":\"object\"}", s.getDefinition());
    assertEquals(SerializationFormat.JSON, s.getFormat());
    assertEquals("user-events:1.0", s.getIdentifier());
  }

  @Test
  void schema_null_name_throws() {
    assertThrows(
        NullPointerException.class, () -> new Schema(null, "1.0", "def", SerializationFormat.JSON));
  }

  @Test
  void schema_null_version_throws() {
    assertThrows(
        NullPointerException.class,
        () -> new Schema("name", null, "def", SerializationFormat.JSON));
  }

  @Test
  void schema_null_definition_throws() {
    assertThrows(
        NullPointerException.class,
        () -> new Schema("name", "1.0", null, SerializationFormat.JSON));
  }

  @Test
  void schema_null_format_throws() {
    assertThrows(NullPointerException.class, () -> new Schema("name", "1.0", "def", null));
  }

  @Test
  void schema_equals_same_instance() {
    Schema s = new Schema("n", "1", "d", SerializationFormat.AVRO);
    assertEquals(s, s);
  }

  @Test
  void schema_equals_null_returns_false() {
    Schema s = new Schema("n", "1", "d", SerializationFormat.AVRO);
    assertNotEquals(s, null);
  }

  @Test
  void schema_equals_different_class_returns_false() {
    Schema s = new Schema("n", "1", "d", SerializationFormat.AVRO);
    assertNotEquals(s, "string");
  }

  @Test
  void schema_equals_same_values() {
    Schema s1 = new Schema("n", "1", "d", SerializationFormat.JSON);
    Schema s2 = new Schema("n", "1", "d", SerializationFormat.JSON);
    assertEquals(s1, s2);
    assertEquals(s1.hashCode(), s2.hashCode());
  }

  @Test
  void schema_equals_different_values() {
    Schema s1 = new Schema("n", "1", "d", SerializationFormat.JSON);
    Schema s2 = new Schema("n", "2", "d", SerializationFormat.JSON);
    assertNotEquals(s1, s2);
  }

  @Test
  void schema_toString_contains_name() {
    Schema s = new Schema("my-schema", "1.0", "def", SerializationFormat.PROTOBUF);
    assertTrue(s.toString().contains("my-schema"));
  }

  // --- SerializationOptions ---

  @Test
  void serializationOptions_defaults() {
    SerializationOptions opts = SerializationOptions.defaults();
    assertTrue(opts.isValidateSchema());
    assertFalse(opts.isUseSchemaRegistry());
    assertFalse(opts.isIncludeMetadata());
    assertNull(opts.getSchemaVersion());
    assertNull(opts.getFormat());
  }

  @Test
  void serializationOptions_builder_all_fields() {
    SerializationOptions opts =
        SerializationOptions.builder()
            .validateSchema(false)
            .useSchemaRegistry(true)
            .includeMetadata(true)
            .schemaVersion("v2")
            .format(SerializationFormat.AVRO)
            .build();
    assertFalse(opts.isValidateSchema());
    assertTrue(opts.isUseSchemaRegistry());
    assertTrue(opts.isIncludeMetadata());
    assertEquals("v2", opts.getSchemaVersion());
    assertEquals(SerializationFormat.AVRO, opts.getFormat());
  }

  @Test
  void serializationOptions_equals_same_instance() {
    SerializationOptions opts = SerializationOptions.defaults();
    assertEquals(opts, opts);
  }

  @Test
  void serializationOptions_equals_null_returns_false() {
    assertNotEquals(SerializationOptions.defaults(), null);
  }

  @Test
  void serializationOptions_equals_different_class_returns_false() {
    assertNotEquals(SerializationOptions.defaults(), "string");
  }

  @Test
  void serializationOptions_equals_same_values() {
    SerializationOptions o1 = SerializationOptions.defaults();
    SerializationOptions o2 = SerializationOptions.defaults();
    assertEquals(o1, o2);
    assertEquals(o1.hashCode(), o2.hashCode());
  }

  @Test
  void serializationOptions_equals_different_values() {
    SerializationOptions o1 = SerializationOptions.defaults();
    SerializationOptions o2 = SerializationOptions.builder().validateSchema(false).build();
    assertNotEquals(o1, o2);
  }

  @Test
  void serializationOptions_toString_not_null() {
    assertNotNull(SerializationOptions.defaults().toString());
  }

  // --- SerializationFormat ---

  @Test
  void format_fromIdentifier_valid() {
    assertEquals(SerializationFormat.JSON, SerializationFormat.fromIdentifier("json"));
  }

  @Test
  void format_fromIdentifier_unknown_returns_null() {
    assertNull(SerializationFormat.fromIdentifier("unknown"));
  }

  @Test
  void format_fromContentType_valid() {
    assertEquals(SerializationFormat.AVRO, SerializationFormat.fromContentType("application/avro"));
  }

  @Test
  void format_fromContentType_unknown_returns_null() {
    assertNull(SerializationFormat.fromContentType("application/unknown"));
  }

  @Test
  void format_isBinary_protobuf_true() {
    assertTrue(SerializationFormat.PROTOBUF.isBinary());
  }

  @Test
  void format_isBinary_json_false() {
    assertFalse(SerializationFormat.JSON.isBinary());
  }
}
