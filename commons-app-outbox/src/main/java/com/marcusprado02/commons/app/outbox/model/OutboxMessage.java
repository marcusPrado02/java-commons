package com.marcusprado02.commons.app.outbox.model;

import java.time.Instant;
import java.util.Map;

/** OutboxMessage data. */
public record OutboxMessage(
    OutboxMessageId id,
    String aggregateType,
    String aggregateId,
    String eventType,
    String topic,
    OutboxPayload payload,
    Map<String, String> headers,
    Instant occurredAt,
    OutboxStatus status,
    int attempts,
    int priority) {

  /** Creates a new OutboxMessage instance. */
  public OutboxMessage(
      OutboxMessageId id,
      String aggregateType,
      String aggregateId,
      String eventType,
      String topic,
      OutboxPayload payload,
      Map<String, String> headers,
      Instant occurredAt,
      OutboxStatus status,
      int attempts) {
    this(
        id,
        aggregateType,
        aggregateId,
        eventType,
        topic,
        payload,
        headers,
        occurredAt,
        status,
        attempts,
        0);
  }
}
