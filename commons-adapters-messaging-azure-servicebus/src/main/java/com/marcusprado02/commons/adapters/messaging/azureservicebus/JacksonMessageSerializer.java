package com.marcusprado02.commons.adapters.messaging.azureservicebus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.marcusprado02.commons.ports.messaging.MessageSerializer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class JacksonMessageSerializer<T> implements MessageSerializer<T> {

  private final ObjectMapper objectMapper;

  public JacksonMessageSerializer() {
    this.objectMapper =
        new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  }

  public JacksonMessageSerializer(ObjectMapper objectMapper) {
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
  }

  @Override
  public byte[] serialize(T message) {
    Objects.requireNonNull(message, "message must not be null");
    try {
      return objectMapper.writeValueAsString(message).getBytes(StandardCharsets.UTF_8);
    } catch (Exception ex) {
      throw new RuntimeException("Failed to serialize message", ex);
    }
  }

  @Override
  public T deserialize(byte[] data, Class<T> type) {
    Objects.requireNonNull(data, "data must not be null");
    Objects.requireNonNull(type, "type must not be null");
    try {
      String json = new String(data, StandardCharsets.UTF_8);
      return objectMapper.readValue(json, type);
    } catch (Exception ex) {
      throw new RuntimeException("Failed to deserialize message", ex);
    }
  }
}
