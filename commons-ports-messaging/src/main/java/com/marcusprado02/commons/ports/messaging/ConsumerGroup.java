package com.marcusprado02.commons.ports.messaging;

import java.util.Objects;

public final class ConsumerGroup {

  private final String value;

  private ConsumerGroup(String value) {
    this.value = Objects.requireNonNull(value, "value must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException("consumer group must not be blank");
    }
  }

  public static ConsumerGroup of(String value) {
    return new ConsumerGroup(value);
  }

  public String value() {
    return value;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof ConsumerGroup other)) {
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
