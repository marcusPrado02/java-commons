package com.marcusprado02.commons.kernel.ddd.event;

import java.util.Collection;

/** Port interface for publishing domain events. */
public interface DomainEventPublisher {
  void publish(Collection<DomainEvent> events);
}
