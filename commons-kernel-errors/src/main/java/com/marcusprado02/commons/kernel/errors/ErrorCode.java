package com.marcusprado02.commons.kernel.errors;

import java.util.Objects;

/**
 * Error code is a stable identifier (machine-readable) for clients, logs and metrics. Example:
 * "PROJECT.VALIDATION.INVALID_DATE_RANGE"
 */
public record ErrorCode(String value) {
  /** Validates that the error code value is non-null and non-blank. */
  public ErrorCode {
    Objects.requireNonNull(value, "value");
    if (value.isBlank()) {
      throw new IllegalArgumentException("ErrorCode cannot be blank");
    }
  }

  public static ErrorCode of(String value) {
    return new ErrorCode(value);
  }
}
