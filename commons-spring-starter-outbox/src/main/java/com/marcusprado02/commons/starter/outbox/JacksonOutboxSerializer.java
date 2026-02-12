package com.marcusprado02.commons.starter.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marcusprado02.commons.app.outbox.OutboxSerializer;
import java.util.Objects;

public final class JacksonOutboxSerializer implements OutboxSerializer {

  private final ObjectMapper mapper;

  public JacksonOutboxSerializer(ObjectMapper mapper) {
    this.mapper = Objects.requireNonNull(mapper, "mapper");
  }

  @Override
  public String serialize(Object event) {
    try {
      return mapper.writeValueAsString(event);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(
          "Failed to serialize outbox event: " + event.getClass().getName(), e);
    }
  }
}
