package com.marcusprado02.commons.app.outbox.model;

public enum OutboxStatus {
  PENDING,
  PROCESSING,
  PUBLISHED,
  FAILED,
  DEAD
}
