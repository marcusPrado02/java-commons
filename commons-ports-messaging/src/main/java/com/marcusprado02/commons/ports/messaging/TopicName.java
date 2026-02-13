package com.marcusprado02.commons.ports.messaging;

import java.util.Objects;

public final class TopicName {

  private final String value;

  private TopicName(String value) {
    this.value = Objects.requireNonNull(value, "value must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException("topic name must not be blank");
    }
  }

  public static TopicName of(String value) {
    return new TopicName(value);
  }

  public String value() {
    return value;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof TopicName other)) {
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
