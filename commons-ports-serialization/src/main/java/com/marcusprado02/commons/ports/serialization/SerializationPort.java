package com.marcusprado02.commons.ports.serialization;

import com.marcusprado02.commons.kernel.result.Result;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Port interface for serialization operations.
 *
 * @param <T> the type to serialize/deserialize
 */
public interface SerializationPort<T> {

  /**
   * Serializes an object to bytes.
   *
   * @param object the object to serialize
   * @return the serialized bytes or error details
   */
  Result<byte[]> serialize(T object);

  /**
   * Serializes an object to bytes with options.
   *
   * @param object  the object to serialize
   * @param options serialization options
   * @return the serialized bytes or error details
   */
  Result<byte[]> serialize(T object, SerializationOptions options);

  /**
   * Serializes an object to an output stream.
   *
   * @param object       the object to serialize
   * @param outputStream the output stream to write to
   * @return success or error details
   */
  Result<Void> serialize(T object, OutputStream outputStream);

  /**
   * Serializes an object to an output stream with options.
   *
   * @param object       the object to serialize
   * @param outputStream the output stream to write to
   * @param options      serialization options
   * @return success or error details
   */
  Result<Void> serialize(T object, OutputStream outputStream, SerializationOptions options);

  /**
   * Deserializes bytes to an object.
   *
   * @param data        the data to deserialize
   * @param targetClass the target class type
   * @return the deserialized object or error details
   */
  Result<T> deserialize(byte[] data, Class<T> targetClass);

  /**
   * Deserializes bytes to an object with options.
   *
   * @param data        the data to deserialize
   * @param targetClass the target class type
   * @param options     serialization options
   * @return the deserialized object or error details
   */
  Result<T> deserialize(byte[] data, Class<T> targetClass, SerializationOptions options);

  /**
   * Deserializes from an input stream to an object.
   *
   * @param inputStream the input stream to read from
   * @param targetClass the target class type
   * @return the deserialized object or error details
   */
  Result<T> deserialize(InputStream inputStream, Class<T> targetClass);

  /**
   * Deserializes from an input stream to an object with options.
   *
   * @param inputStream the input stream to read from
   * @param targetClass the target class type
   * @param options     serialization options
   * @return the deserialized object or error details
   */
  Result<T> deserialize(InputStream inputStream, Class<T> targetClass, SerializationOptions options);

  /**
   * Validates an object against a schema.
   *
   * @param object the object to validate
   * @param schema the schema to validate against
   * @return validation result or error details
   */
  Result<ValidationResult> validate(T object, Schema schema);

  /**
   * Gets the content type for this serializer.
   *
   * @return the MIME content type
   */
  String getContentType();

  /**
   * Gets the serialization format.
   *
   * @return the serialization format
   */
  SerializationFormat getFormat();
}
