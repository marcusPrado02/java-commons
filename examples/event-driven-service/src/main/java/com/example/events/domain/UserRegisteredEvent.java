package com.example.events.domain;

import com.marcusprado02.commons.kernel.ddd.event.DomainEvent;
import com.marcusprado02.commons.kernel.ddd.event.EventId;
import com.marcusprado02.commons.kernel.ddd.event.EventMetadata;
import java.time.Instant;

/**
 * Domain event fired when a new user registers in the system.
 *
 * <p>This event is published in two ways:
 * <ol>
 *   <li>In-process, via {@code DomainEventBus} (synchronous handlers in the same JVM)
 *   <li>Out-of-process, via the Outbox → Kafka pipeline (async, reliable delivery)
 * </ol>
 */
public record UserRegisteredEvent(
    String userId,
    String email,
    String name,
    Instant registeredAt
) implements DomainEvent {

  @Override
  public EventId eventId() {
    return EventId.newId();
  }

  @Override
  public Instant occurredAt() {
    return registeredAt;
  }

  @Override
  public String aggregateType() {
    return "User";
  }

  @Override
  public String aggregateId() {
    return userId;
  }

  @Override
  public long aggregateVersion() {
    return 1L;
  }

  @Override
  public EventMetadata metadata() {
    return EventMetadata.empty();
  }
}
