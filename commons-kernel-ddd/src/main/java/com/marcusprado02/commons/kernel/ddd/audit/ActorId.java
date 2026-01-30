package com.marcusprado02.commons.kernel.ddd.audit;

import java.util.Objects;

/**
 * Identifier for an actor performing an action within the system. An actor can be a user, a system
 * process, or any entity that can initiate actions. This class ensures that the actor ID is
 * non-null and non-blank. It provides factory methods for creating ActorId instances. The special
 * ActorId "system" can be used to represent system-initiated actions.
 *
 * @param value the identifier value
 */
public record ActorId(String value) {
  public ActorId {
    Objects.requireNonNull(value, "value");
    if (value.isBlank()) throw new IllegalArgumentException("ActorId cannot be blank");
  }

  public static ActorId of(String value) {
    return new ActorId(value);
  }

  public static ActorId system() {
    return new ActorId("system");
  }
}
