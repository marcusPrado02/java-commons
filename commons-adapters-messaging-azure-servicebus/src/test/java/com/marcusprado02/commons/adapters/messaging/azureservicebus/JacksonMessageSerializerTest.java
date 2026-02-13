package com.marcusprado02.commons.adapters.messaging.azureservicebus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class JacksonMessageSerializerTest {

  private final JacksonMessageSerializer<TestMessage> serializer = new JacksonMessageSerializer<>();

  @Test
  void shouldSerializeAndDeserialize() {
    TestMessage message = new TestMessage("test-id", "test content", Instant.now());

    byte[] serialized = serializer.serialize(message);
    TestMessage deserialized = serializer.deserialize(serialized, TestMessage.class);

    assertThat(deserialized.id()).isEqualTo(message.id());
    assertThat(deserialized.content()).isEqualTo(message.content());
    assertThat(deserialized.timestamp()).isEqualTo(message.timestamp());
  }

  @Test
  void shouldHandleInstant() {
    Instant now = Instant.parse("2024-01-01T12:00:00Z");
    TestMessage message = new TestMessage("1", "test", now);

    byte[] serialized = serializer.serialize(message);
    String json = new String(serialized, StandardCharsets.UTF_8);

    assertThat(json).contains("2024-01-01T12:00:00Z");

    TestMessage deserialized = serializer.deserialize(serialized, TestMessage.class);
    assertThat(deserialized.timestamp()).isEqualTo(now);
  }

  @Test
  void shouldFailOnNullMessage() {
    assertThatThrownBy(() -> serializer.serialize(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("message must not be null");
  }

  @Test
  void shouldFailOnNullData() {
    assertThatThrownBy(() -> serializer.deserialize(null, TestMessage.class))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("data must not be null");
  }

  @Test
  void shouldFailOnInvalidJson() {
    byte[] invalidJson = "invalid json".getBytes(StandardCharsets.UTF_8);
    assertThatThrownBy(() -> serializer.deserialize(invalidJson, TestMessage.class))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Failed to deserialize message");
  }

  @Test
  void shouldUseCustomObjectMapper() {
    ObjectMapper customMapper = new ObjectMapper();
    customMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
    JacksonMessageSerializer<TestMessage> customSerializer =
        new JacksonMessageSerializer<>(customMapper);

    TestMessage message = new TestMessage("1", "test", Instant.now());
    byte[] serialized = customSerializer.serialize(message);

    assertThat(serialized).isNotNull();
  }

  record TestMessage(String id, String content, Instant timestamp) {}
}
