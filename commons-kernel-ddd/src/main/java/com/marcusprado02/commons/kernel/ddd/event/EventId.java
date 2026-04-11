package com.marcusprado02.commons.kernel.ddd.event;

import com.marcusprado02.commons.kernel.ddd.id.Identifier;
import java.io.Serial;
import java.util.Objects;
import java.util.UUID;

/** Strongly-typed identifier for a domain event. */
public record EventId(UUID value) implements Identifier<UUID> {
  @Serial private static final long serialVersionUID = 1L;

  public EventId {
    Objects.requireNonNull(value, "value");
  }

  /** Creates a new EventId with a randomly generated UUID. */
  public static EventId newId() {
    return new EventId(UUID.randomUUID());
  }
}
