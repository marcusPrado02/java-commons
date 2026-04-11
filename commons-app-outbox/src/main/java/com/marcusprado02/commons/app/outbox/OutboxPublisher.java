package com.marcusprado02.commons.app.outbox;

import com.marcusprado02.commons.kernel.ddd.entity.AggregateRoot;

/** OutboxPublisher contract. */
public interface OutboxPublisher {

  void publishFrom(AggregateRoot<?> aggregate);
}
