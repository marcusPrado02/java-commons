package com.marcusprado02.commons.app.outbox.model;

import java.time.Instant;
import java.util.Map;

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
    int attempts) {}
