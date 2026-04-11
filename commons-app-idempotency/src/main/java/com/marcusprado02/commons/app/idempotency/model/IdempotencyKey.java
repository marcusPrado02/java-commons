package com.marcusprado02.commons.app.idempotency.model;

import java.util.Objects;

/** Value object representing a unique key for an idempotent request. */
public record IdempotencyKey(String value) {

  /** Validates and normalizes the idempotency key value. */
  public IdempotencyKey {
    Objects.requireNonNull(value, "value must not be null");
    value = value.trim();
    if (value.isBlank()) {
      throw new IllegalArgumentException("value must not be blank");
    }
    if (value.length() > 160) {
      throw new IllegalArgumentException("value must be <= 160 chars");
    }
  }
}
