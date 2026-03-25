package com.example.orderservice.adapters.persistence;

import com.example.orderservice.application.OutboxEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marcusprado02.commons.app.outbox.model.OutboxMessage;
import com.marcusprado02.commons.app.outbox.model.OutboxMessageId;
import com.marcusprado02.commons.app.outbox.model.OutboxPayload;
import com.marcusprado02.commons.app.outbox.model.OutboxStatus;
import com.marcusprado02.commons.app.outbox.port.OutboxRepositoryPort;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Outbox adapter: serializes domain events to JSON and appends them to the Outbox table.
 *
 * <p>Runs within the same database transaction as the aggregate save, providing
 * at-least-once delivery semantics without a message broker in the write path.
 */
@Component
public class JsonOutboxEventPublisher implements OutboxEventPublisher {

  private final OutboxRepositoryPort outboxRepository;
  private final ObjectMapper objectMapper;

  public JsonOutboxEventPublisher(
      OutboxRepositoryPort outboxRepository, ObjectMapper objectMapper) {
    this.outboxRepository = outboxRepository;
    this.objectMapper = objectMapper;
  }

  @Override
  public void publish(String topic, Object event) {
    try {
      byte[] payload = objectMapper.writeValueAsBytes(event);
      String eventType = event.getClass().getSimpleName();

      OutboxMessage message = new OutboxMessage(
          new OutboxMessageId(UUID.randomUUID().toString()),
          "Order",
          extractAggregateId(event),
          eventType,
          topic + "." + camelToKebab(eventType),
          new OutboxPayload("application/json", payload),
          Map.of("event-type", eventType, "source", "order-service"),
          Instant.now(),
          OutboxStatus.PENDING,
          0);

      outboxRepository.append(message);
    } catch (Exception e) {
      throw new RuntimeException("Failed to serialize event " + event.getClass().getSimpleName(), e);
    }
  }

  private String extractAggregateId(Object event) {
    try {
      // Reflective access to orderId().value() for any OrderXxxEvent
      var method = event.getClass().getMethod("orderId");
      var orderId = method.invoke(event);
      var valueMethod = orderId.getClass().getMethod("value");
      return valueMethod.invoke(orderId).toString();
    } catch (Exception e) {
      return "unknown";
    }
  }

  private static String camelToKebab(String camel) {
    return camel
        .replaceAll("([a-z])([A-Z])", "$1-$2")
        .toLowerCase();
  }
}
