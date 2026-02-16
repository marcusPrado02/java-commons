package com.marcusprado02.commons.ports.serialization;

/**
 * Supported serialization formats.
 */
public enum SerializationFormat {

  /**
   * Protocol Buffers binary format.
   */
  PROTOBUF("application/x-protobuf", "protobuf", true),

  /**
   * Protocol Buffers JSON format.
   */
  PROTOBUF_JSON("application/json", "protobuf-json", false),

  /**
   * Protocol Buffers text format.
   */
  PROTOBUF_TEXT("text/plain", "protobuf-text", false),

  /**
   * Apache Avro binary format.
   */
  AVRO("application/avro", "avro", true),

  /**
   * Apache Avro JSON format.
   */
  AVRO_JSON("application/json", "avro-json", false),

  /**
   * MessagePack binary format.
   */
  MESSAGEPACK("application/msgpack", "messagepack", true),

  /**
   * Standard JSON format.
   */
  JSON("application/json", "json", false),

  /**
   * XML format.
   */
  XML("application/xml", "xml", false);

  private final String contentType;
  private final String identifier;
  private final boolean binary;

  SerializationFormat(String contentType, String identifier, boolean binary) {
    this.contentType = contentType;
    this.identifier = identifier;
    this.binary = binary;
  }

  /**
   * Gets the MIME content type for this format.
   *
   * @return the content type
   */
  public String getContentType() {
    return contentType;
  }

  /**
   * Gets the unique identifier for this format.
   *
   * @return the identifier
   */
  public String getIdentifier() {
    return identifier;
  }

  /**
   * Checks if this format uses binary encoding.
   *
   * @return true if binary format, false if text format
   */
  public boolean isBinary() {
    return binary;
  }

  /**
   * Finds a format by its identifier.
   *
   * @param identifier the identifier to search for
   * @return the matching format, or null if not found
   */
  public static SerializationFormat fromIdentifier(String identifier) {
    for (SerializationFormat format : values()) {
      if (format.identifier.equals(identifier)) {
        return format;
      }
    }
    return null;
  }

  /**
   * Finds a format by its content type.
   *
   * @param contentType the content type to search for
   * @return the matching format, or null if not found
   */
  public static SerializationFormat fromContentType(String contentType) {
    for (SerializationFormat format : values()) {
      if (format.contentType.equals(contentType)) {
        return format;
      }
    }
    return null;
  }
}
