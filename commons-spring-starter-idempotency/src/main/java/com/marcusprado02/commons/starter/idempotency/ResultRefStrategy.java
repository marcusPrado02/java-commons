package com.marcusprado02.commons.starter.idempotency;

/** Strategy for storing a result reference after a successful idempotent request. */
public enum ResultRefStrategy {
  /** Uses HTTP Location header as result reference when request completes successfully. */
  LOCATION_HEADER,

  /** Does not store any result reference at HTTP layer. */
  NONE
}
