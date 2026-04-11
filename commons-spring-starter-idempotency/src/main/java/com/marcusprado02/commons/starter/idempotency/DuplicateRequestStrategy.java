package com.marcusprado02.commons.starter.idempotency;

/** Strategy for handling duplicate idempotency key requests. */
public enum DuplicateRequestStrategy {
  /** Reject duplicate or in-progress keys with a 409 Conflict. */
  CONFLICT,

  /** Let the request proceed even if the key exists (not idempotent). */
  ALLOW
}
