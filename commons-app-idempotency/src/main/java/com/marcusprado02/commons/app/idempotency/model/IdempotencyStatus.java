package com.marcusprado02.commons.app.idempotency.model;

/** Lifecycle status of an idempotent operation record. */
public enum IdempotencyStatus {
  IN_PROGRESS,
  COMPLETED,
  FAILED
}
