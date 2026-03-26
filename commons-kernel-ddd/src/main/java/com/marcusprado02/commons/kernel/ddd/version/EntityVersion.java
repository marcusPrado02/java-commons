package com.marcusprado02.commons.kernel.ddd.version;

/** Monotonically-increasing version of a domain entity, used for optimistic locking. */
public record EntityVersion(long value) {

  /** Validates that the version is non-negative. */
  public EntityVersion {
    if (value < 0) {
      throw new IllegalArgumentException("EntityVersion cannot be negative");
    }
  }

  /** Returns the initial version (zero). */
  public static EntityVersion initial() {
    return new EntityVersion(0);
  }

  /** Returns the next version (current + 1). */
  public EntityVersion next() {
    return new EntityVersion(value + 1);
  }
}
