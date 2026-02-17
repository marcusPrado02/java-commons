package com.marcusprado02.commons.adapters.serialization.protobuf;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import com.marcusprado02.commons.kernel.errors.Problem;
import com.marcusprado02.commons.kernel.errors.ErrorCode;
import com.marcusprado02.commons.kernel.errors.ErrorCategory;
import com.marcusprado02.commons.kernel.errors.Severity;
import com.marcusprado02.commons.kernel.result.Result;
import com.marcusprado02.commons.ports.serialization.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Protocol Buffers implementation of SerializationPort.
 */
public class ProtobufSerializationAdapter<T extends Message> implements SerializationPort<T> {

  private static final Logger log = LoggerFactory.getLogger(ProtobufSerializationAdapter.class);

  private final Class<T> messageClass;
  private final SchemaRegistry schemaRegistry;
  private final SerializationOptions defaultOptions;

  /**
   * Creates a new Protocol Buffers serialization adapter.
   *
   * @param messageClass the Protocol Buffers message class
   */
  public ProtobufSerializationAdapter(Class<T> messageClass) {
    this(messageClass, null, SerializationOptions.defaults());
  }

  /**
   * Creates a new Protocol Buffers serialization adapter with schema registry.
   *
   * @param messageClass   the Protocol Buffers message class
   * @param schemaRegistry the schema registry for validation
   */
  public ProtobufSerializationAdapter(Class<T> messageClass, SchemaRegistry schemaRegistry) {
    this(messageClass, schemaRegistry, SerializationOptions.defaults());
  }

  /**
   * Creates a new Protocol Buffers serialization adapter with options.
   *
   * @param messageClass   the Protocol Buffers message class
   * @param schemaRegistry the schema registry for validation
   * @param defaultOptions the default serialization options
   */
  public ProtobufSerializationAdapter(Class<T> messageClass, SchemaRegistry schemaRegistry,
                                     SerializationOptions defaultOptions) {
    this.messageClass = Objects.requireNonNull(messageClass, "Message class cannot be null");
    this.schemaRegistry = schemaRegistry;
    this.defaultOptions = Objects.requireNonNull(defaultOptions, "Default options cannot be null");
  }

  @Override
  public Result<byte[]> serialize(T object) {
    return serialize(object, defaultOptions);
  }

  @Override
  public Result<byte[]> serialize(T object, SerializationOptions options) {
    try {
      Objects.requireNonNull(object, "Object to serialize cannot be null");
      Objects.requireNonNull(options, "Serialization options cannot be null");

      // Validate schema if required
      if (options.isValidateSchema() && schemaRegistry != null) {
        Result<ValidationResult> validationResult = validateWithRegistry(object, options);
        if (!validationResult.isOk() || !validationResult.getOrNull().isValid()) {
          return Result.fail(Problem.of(ErrorCode.of("UNKNOWN"), ErrorCategory.BUSINESS, Severity.ERROR, "Error: Object does not conform to schema"));
        }
      }

      // Serialize based on format
      SerializationFormat format = options.getFormat();
      if (format == null) {
        format = SerializationFormat.PROTOBUF; // Default to binary
      }

      return switch (format) {
        case PROTOBUF -> Result.ok(object.toByteArray());
        case PROTOBUF_JSON -> serializeToJson(object);
        case PROTOBUF_TEXT -> serializeToText(object);
        default -> Result.fail(Problem.of(
            ErrorCode.of("UNSUPPORTED_FORMAT"),
            ErrorCategory.TECHNICAL,
            Severity.ERROR,
            "Format " + format + " is not supported by Protocol Buffers adapter"));
      };

    } catch (Exception e) {
      log.error("Failed to serialize object", e);
      return Result.fail(Problem.of(
          ErrorCode.of("SERIALIZATION_ERROR"),
          ErrorCategory.TECHNICAL,
          Severity.ERROR,
          "Failed to serialize object: " + e.getMessage()));
    }
  }

  @Override
  public Result<Void> serialize(T object, OutputStream outputStream) {
    return serialize(object, outputStream, defaultOptions);
  }

  @Override
  public Result<Void> serialize(T object, OutputStream outputStream, SerializationOptions options) {
    try {
      Objects.requireNonNull(object, "Object to serialize cannot be null");
      Objects.requireNonNull(outputStream, "Output stream cannot be null");

      Result<byte[]> serializeResult = serialize(object, options);
      if (!serializeResult.isOk()) {
        return Result.fail(serializeResult.problemOrNull());
      }

      outputStream.write(serializeResult.getOrNull());
      return Result.ok(null);

    } catch (IOException e) {
      log.error("Failed to write serialized data to stream", e);
      return Result.fail(Problem.of(ErrorCode.of("IO_ERROR"), ErrorCategory.BUSINESS, Severity.ERROR, "Failed to write to stream"));
    }
  }

  @Override
  public Result<T> deserialize(byte[] data, Class<T> targetClass) {
    return deserialize(data, targetClass, defaultOptions);
  }

  @Override
  public Result<T> deserialize(byte[] data, Class<T> targetClass, SerializationOptions options) {
    try {
      Objects.requireNonNull(data, "Data to deserialize cannot be null");
      Objects.requireNonNull(targetClass, "Target class cannot be null");

      if (!messageClass.equals(targetClass)) {
        return Result.fail(Problem.of(ErrorCode.of("CLASS_MISMATCH"), ErrorCategory.BUSINESS, Severity.ERROR, "Target class mismatch"));
      }

      // Get parser method from the message class
      Method parseFromMethod = messageClass.getMethod("parseFrom", byte[].class);
      @SuppressWarnings("unchecked")
      T message = (T) parseFromMethod.invoke(null, data);

      return Result.ok(message);

    } catch (Exception e) {
      log.error("Failed to deserialize object", e);
      return Result.fail(Problem.of(ErrorCode.of("DESERIALIZATION_ERROR"), ErrorCategory.BUSINESS, Severity.ERROR, "Deserialization failed"));
    }
  }

  @Override
  public Result<T> deserialize(InputStream inputStream, Class<T> targetClass) {
    return deserialize(inputStream, targetClass, defaultOptions);
  }

  @Override
  public Result<T> deserialize(InputStream inputStream, Class<T> targetClass, SerializationOptions options) {
    try {
      Objects.requireNonNull(inputStream, "Input stream cannot be null");

      byte[] data = inputStream.readAllBytes();
      return deserialize(data, targetClass, options);

    } catch (IOException e) {
      log.error("Failed to read data from stream", e);
      return Result.fail(Problem.of(ErrorCode.of("IO_ERROR"), ErrorCategory.BUSINESS, Severity.ERROR, "Failed to read from stream"));
    }
  }

  @Override
  public Result<ValidationResult> validate(T object, Schema schema) {
    try {
      Objects.requireNonNull(object, "Object to validate cannot be null");
      Objects.requireNonNull(schema, "Schema cannot be null");

      if (schemaRegistry == null) {
        return Result.ok(ValidationResult.valid());
      }

      return Result.ok(schemaRegistry.validateSchema(schema));

    } catch (Exception e) {
      log.error("Failed to validate object", e);
      return Result.fail(Problem.of(ErrorCode.of("VALIDATION_ERROR"), ErrorCategory.BUSINESS, Severity.ERROR, "Validation failed"));
    }
  }

  @Override
  public String getContentType() {
    return SerializationFormat.PROTOBUF.getContentType();
  }

  @Override
  public SerializationFormat getFormat() {
    return SerializationFormat.PROTOBUF;
  }

  private Result<byte[]> serializeToJson(T object) {
    try {
      String json = JsonFormat.printer()
          .includingDefaultValueFields()
          .print(object);
      return Result.ok(json.getBytes());
    } catch (Exception e) {
      log.error("Failed to serialize to JSON", e);
      return Result.fail(Problem.of(ErrorCode.of("JSON_SERIALIZATION_ERROR"), ErrorCategory.BUSINESS, Severity.ERROR, "JSON serialization failed"));
    }
  }

  private Result<byte[]> serializeToText(T object) {
    try {
      String text = object.toString();
      return Result.ok(text.getBytes());
    } catch (Exception e) {
      log.error("Failed to serialize to text", e);
      return Result.fail(Problem.of(ErrorCode.of("TEXT_SERIALIZATION_ERROR"), ErrorCategory.BUSINESS, Severity.ERROR, "Text serialization failed"));
    }
  }

  private Result<ValidationResult> validateWithRegistry(T object, SerializationOptions options) {
    try {
      String schemaName = messageClass.getSimpleName();
      String version = options.getSchemaVersion();

      Schema schema = (version != null)
          ? schemaRegistry.getSchema(schemaName, version).orElse(null)
          : schemaRegistry.getLatestSchema(schemaName).orElse(null);

      if (schema == null) {
        return Result.fail(Problem.of(ErrorCode.of("SCHEMA_NOT_FOUND"), ErrorCategory.BUSINESS, Severity.ERROR, "Schema not found"));
      }

      return validate(object, schema);

    } catch (Exception e) {
      log.error("Failed to validate with registry", e);
      return Result.fail(Problem.of(ErrorCode.of("REGISTRY_VALIDATION_ERROR"), ErrorCategory.BUSINESS, Severity.ERROR, "Registry validation failed"));
    }
  }
}
