package com.marcusprado02.commons.kernel.ddd.id;

import java.util.Objects;
import java.util.UUID;

/** Helper base class for identifiers backed by UUID. */
public abstract class UuidIdentifier implements Identifier<UUID> {

  private final UUID value;

  protected UuidIdentifier(UUID value) {
    this.value = Objects.requireNonNull(value, "value");
  }

  protected UuidIdentifier(String value) {
    this(UUID.fromString(value));
  }

  @Override
  public final UUID value() {
    return value;
  }

  @Override
  public final String asString() {
    return value.toString();
  }

  @Override
  public final boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    UuidIdentifier that = (UuidIdentifier) o;
    return value.equals(that.value);
  }

  @Override
  public final int hashCode() {
    return value.hashCode();
  }
}
