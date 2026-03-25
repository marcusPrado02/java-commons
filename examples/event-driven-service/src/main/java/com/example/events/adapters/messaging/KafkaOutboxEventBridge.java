package com.example.events.adapters.messaging;

import com.example.events.application.OutboxEventBridge;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marcusprado02.commons.app.outbox.model.OutboxMessage;
import com.marcusprado02.commons.app.outbox.model.OutboxMessageId;
import com.marcusprado02.commons.app.outbox.model.OutboxPayload;
import com.marcusprado02.commons.app.outbox.model.OutboxStatus;
import com.marcusprado02.commons.app.outbox.port.OutboxRepositoryPort;
import com.marcusprado02.commons.kernel.ddd.event.DomainEvent;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Adapter: serializes a DomainEvent and appends it to the Outbox table.
 *
 * <p>The Outbox Processor (running in a background scheduler, configured by
 * {@code commons-spring-starter-outbox}) will later dequeue these messages and
 * publish them to Kafka. This decouples the write path from broker availability.
 *
 * <p>Architecture note: the outbox append and the aggregate save MUST share the
 * same transaction. Spring's {@code @Transactional} on the use case ensures this.
 */
@Component
public class KafkaOutboxEventBridge implements OutboxEventBridge {

  private final OutboxRepositoryPort outboxRepository;
  private final ObjectMapper objectMapper;

  public KafkaOutboxEventBridge(
      OutboxRepositoryPort outboxRepository, ObjectMapper objectMapper) {
    this.outboxRepository = outboxRepository;
    this.objectMapper = objectMapper;
  }

  @Override
  public void publish(String topic, DomainEvent event) {
    try {
      byte[] body = objectMapper.writeValueAsBytes(event);
      String eventType = event.getClass().getSimpleName();

      outboxRepository.append(new OutboxMessage(
          new OutboxMessageId(UUID.randomUUID().toString()),
          event.aggregateType(),
          event.aggregateId(),
          eventType,
          topic,
          new OutboxPayload("application/json", body),
          Map.of(
              "event-type", eventType,
              "aggregate-version", String.valueOf(event.aggregateVersion())),
          Instant.now(),
          OutboxStatus.PENDING,
          0));
    } catch (Exception e) {
      throw new RuntimeException(
          "Failed to append event to outbox: " + event.getClass().getSimpleName(), e);
    }
  }
}
