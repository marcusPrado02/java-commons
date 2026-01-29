package com.marcusprado02.commons.adapters.persistence.jpa.outbox;

import com.marcusprado02.commons.app.outbox.model.OutboxMessage;
import com.marcusprado02.commons.app.outbox.model.OutboxMessageId;
import com.marcusprado02.commons.app.outbox.model.OutboxPayload;
import java.util.Map;

public final class OutboxJpaMapper {

  private OutboxJpaMapper() {}

  public static OutboxMessageEntity toEntity(OutboxMessage m) {
    OutboxMessageEntity e = new OutboxMessageEntity();
    e.setId(m.id().value());
    e.setAggregateType(m.aggregateType());
    e.setAggregateId(m.aggregateId());
    e.setEventType(m.eventType());
    e.setTopic(m.topic());
    e.setContentType(m.payload().contentType());
    e.setPayload(m.payload().body());
    e.setHeadersJson(headersToJson(m.headers()));
    e.setOccurredAt(m.occurredAt());
    e.setStatus(m.status());
    e.setAttempts(m.attempts());
    return e;
  }

  public static OutboxMessage toModel(OutboxMessageEntity e) {
    return new OutboxMessage(
        new OutboxMessageId(e.getId()),
        e.getAggregateType(),
        e.getAggregateId(),
        e.getEventType(),
        e.getTopic(),
        new OutboxPayload(e.getContentType(), e.getPayload()),
        jsonToHeaders(e.getHeadersJson()),
        e.getOccurredAt(),
        e.getStatus(),
        e.getAttempts());
  }

  private static String headersToJson(Map<String, String> headers) {
    // placeholder simples (sem Jackson). Você pode trocar por JSON real depois.
    return headers == null ? null : headers.toString();
  }

  private static Map<String, String> jsonToHeaders(String headersJson) {
    // placeholder simples (sem Jackson)
    return Map.of();
  }
}
