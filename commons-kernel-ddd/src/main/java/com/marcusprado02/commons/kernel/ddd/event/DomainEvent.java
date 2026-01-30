package com.marcusprado02.commons.kernel.ddd.event;

import java.time.Instant;

/**
 * Event representing a significant change or occurrence within the domain.
 *
 * <p>This interface defines the essential properties of a domain event, including its unique
 * identifier, occurrence timestamp, associated aggregate information, and metadata.
 *
 * <p>Implementing classes should provide concrete details for these properties.
 *
 * @see EventId
 * @see EventMetadata
 * @see Instant
 * @see Aggregate
 */
public interface DomainEvent {

  EventId eventId();

  Instant occurredAt();

  String aggregateType();

  String aggregateId();

  long aggregateVersion();

  EventMetadata metadata();
}
