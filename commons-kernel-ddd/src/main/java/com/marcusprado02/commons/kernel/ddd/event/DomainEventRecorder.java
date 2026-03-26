package com.marcusprado02.commons.kernel.ddd.event;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides an in-memory buffer for domain events.
 *
 * <p>This enables aggregates to record events and later "pull" them for publication (e.g.,
 * Transactional Outbox).
 */
public interface DomainEventRecorder {

  /** Returns the live event buffer. */
  List<DomainEvent> domainEvents();

  /**
   * Adds a single event to the buffer.
   *
   * @param event the event to record
   */
  default void recordEvent(DomainEvent event) {
    domainEvents().add(event);
  }

  /**
   * Pulls all pending events from the buffer and clears it.
   *
   * @return immutable copy of the pending events
   */
  default List<DomainEvent> pullDomainEvents() {
    if (domainEvents().isEmpty()) {
      return List.of();
    }
    List<DomainEvent> copy = List.copyOf(domainEvents());
    domainEvents().clear();
    return copy;
  }

  /** Creates a new empty mutable event buffer. */
  static List<DomainEvent> newBuffer() {
    return new ArrayList<>();
  }
}
