package com.marcusprado02.commons.ports.messaging;

import java.util.Objects;
import java.util.UUID;

public final class MessageId {

  private final String value;

  private MessageId(String value) {
    this.value = Objects.requireNonNull(value, "value must not be null");
  }

  public static MessageId of(String value) {
    return new MessageId(value);
  }

  public static MessageId random() {
    return new MessageId(UUID.randomUUID().toString());
  }

  public String value() {
    return value;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof MessageId other)) {
      return false;
    }
    return value.equals(other.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public String toString() {
    return value;
  }
}
