package com.marcusprado02.commons.kernel.errors;

/** High-level category of a {@link Problem} used for routing and HTTP status mapping. */
public enum ErrorCategory {
  VALIDATION,
  BUSINESS,
  NOT_FOUND,
  CONFLICT,
  UNAUTHORIZED,
  FORBIDDEN,
  TECHNICAL
}
