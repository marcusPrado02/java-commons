package com.marcusprado02.commons.app.outbox.model;

/** OutboxStatus values. */
public enum OutboxStatus {
  PENDING,
  PROCESSING,
  PUBLISHED,
  FAILED,
  DEAD
}
