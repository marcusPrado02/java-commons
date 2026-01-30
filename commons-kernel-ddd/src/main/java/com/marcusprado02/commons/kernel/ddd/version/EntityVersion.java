package com.marcusprado02.commons.kernel.ddd.version;

public record EntityVersion(long value) {

  public EntityVersion {
    if (value < 0) throw new IllegalArgumentException("EntityVersion cannot be negative");
  }

  public static EntityVersion initial() {
    return new EntityVersion(0);
  }

  public EntityVersion next() {
    return new EntityVersion(value + 1);
  }
}
