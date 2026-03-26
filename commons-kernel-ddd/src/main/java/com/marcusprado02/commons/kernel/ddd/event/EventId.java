package com.marcusprado02.commons.kernel.ddd.event;

import java.util.Objects;
import java.util.UUID;

/** Strongly-typed identifier for a domain event. */
public record EventId(UUID value) {
  public EventId {
    Objects.requireNonNull(value, "value");
  }

  /** Creates a new EventId with a randomly generated UUID. */
  public static EventId newId() {
    return new EventId(UUID.randomUUID());
  }
}
