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

  List<DomainEvent> domainEvents();

  default void recordEvent(DomainEvent event) {
    domainEvents().add(event);
  }

  default List<DomainEvent> pullDomainEvents() {
    if (domainEvents().isEmpty()) return List.of();
    List<DomainEvent> copy = List.copyOf(domainEvents());
    domainEvents().clear();
    return copy;
  }

  static List<DomainEvent> newBuffer() {
    return new ArrayList<>();
  }
}
