package com.marcusprado02.commons.kernel.ddd.event;

import java.util.Objects;
import java.util.UUID;

public record EventId(UUID value) {
  public EventId {
    Objects.requireNonNull(value, "value");
  }

  public static EventId newId() {
    return new EventId(UUID.randomUUID());
  }
}
