package com.marcusprado02.commons.adapters.serialization.protobuf;

import com.google.protobuf.Message;
import com.marcusprado02.commons.ports.serialization.SchemaRegistry;
import com.marcusprado02.commons.ports.serialization.SerializationOptions;
import com.marcusprado02.commons.ports.serialization.SerializationPort;

import java.util.Objects;

/**
 * Factory class for creating Protocol Buffers serialization components.
 */
public final class ProtobufSerialization {

  private ProtobufSerialization() {
    // Utility class
  }

  /**
   * Creates a Protocol Buffers serialization adapter for the given message type.
   *
   * @param <T>          the message type
   * @param messageClass the Protocol Buffers message class
   * @return a new serialization adapter
   */
  public static <T extends Message> SerializationPort<T> forMessage(Class<T> messageClass) {
    return new ProtobufSerializationAdapter<>(messageClass);
  }

  /**
   * Creates a Protocol Buffers serialization adapter with schema registry.
   *
   * @param <T>            the message type
   * @param messageClass   the Protocol Buffers message class
   * @param schemaRegistry the schema registry for validation
   * @return a new serialization adapter
   */
  public static <T extends Message> SerializationPort<T> forMessage(Class<T> messageClass, 
                                                                   SchemaRegistry schemaRegistry) {
    return new ProtobufSerializationAdapter<>(messageClass, schemaRegistry);
  }

  /**
   * Creates a Protocol Buffers serialization adapter with options.
   *
   * @param <T>            the message type
   * @param messageClass   the Protocol Buffers message class
   * @param schemaRegistry the schema registry for validation
   * @param options        the default serialization options
   * @return a new serialization adapter
   */
  public static <T extends Message> SerializationPort<T> forMessage(Class<T> messageClass, 
                                                                   SchemaRegistry schemaRegistry,
                                                                   SerializationOptions options) {
    return new ProtobufSerializationAdapter<>(messageClass, schemaRegistry, options);
  }

  /**
   * Creates an in-memory schema registry for Protocol Buffers.
   *
   * @return a new in-memory schema registry
   */
  public static SchemaRegistry createInMemoryRegistry() {
    return new InMemoryProtobufSchemaRegistry();
  }

  /**
   * Builder for creating configured Protocol Buffers serialization components.
   */
  public static class Builder<T extends Message> {
    
    private final Class<T> messageClass;
    private SchemaRegistry schemaRegistry;
    private SerializationOptions options;

    /**
     * Creates a new builder for the given message type.
     *
     * @param messageClass the Protocol Buffers message class
     */
    public Builder(Class<T> messageClass) {
      this.messageClass = Objects.requireNonNull(messageClass, "Message class cannot be null");
      this.options = SerializationOptions.defaults();
    }

    /**
     * Sets the schema registry.
     *
     * @param registry the schema registry
     * @return this builder
     */
    public Builder<T> withSchemaRegistry(SchemaRegistry registry) {
      this.schemaRegistry = registry;
      return this;
    }

    /**
     * Enables in-memory schema registry.
     *
     * @return this builder
     */
    public Builder<T> withInMemoryRegistry() {
      this.schemaRegistry = createInMemoryRegistry();
      return this;
    }

    /**
     * Sets the serialization options.
     *
     * @param options the serialization options
     * @return this builder
     */
    public Builder<T> withOptions(SerializationOptions options) {
      this.options = Objects.requireNonNull(options, "Options cannot be null");
      return this;
    }

    /**
     * Configures schema validation.
     *
     * @param validate true to enable validation
     * @return this builder
     */
    public Builder<T> validateSchema(boolean validate) {
      this.options = SerializationOptions.builder()
          .validateSchema(validate)
          .useSchemaRegistry(options.isUseSchemaRegistry())
          .includeMetadata(options.isIncludeMetadata())
          .schemaVersion(options.getSchemaVersion())
          .format(options.getFormat())
          ;
      return this;
    }

    /**
     * Configures schema registry usage.
     *
     * @param useRegistry true to use registry
     * @return this builder
     */
    public Builder<T> useSchemaRegistry(boolean useRegistry) {
      this.options = SerializationOptions.builder()
          .validateSchema(options.isValidateSchema())
          .useSchemaRegistry(useRegistry)
          .includeMetadata(options.isIncludeMetadata())
          .schemaVersion(options.getSchemaVersion())
          .format(options.getFormat())
          ;
      return this;
    }

    /**
     * Builds the serialization adapter.
     *
     * @return a configured serialization adapter
     */
    public SerializationPort<T> build() {
      return new ProtobufSerializationAdapter<>(messageClass, schemaRegistry, options);
    }
  }

  /**
   * Creates a builder for the given message type.
   *
   * @param <T>          the message type
   * @param messageClass the Protocol Buffers message class
   * @return a new builder
   */
  public static <T extends Message> Builder<T> builder(Class<T> messageClass) {
    return new Builder<>(messageClass);
  }
}