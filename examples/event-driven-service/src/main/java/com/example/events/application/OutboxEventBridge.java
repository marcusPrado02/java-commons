package com.example.events.application;

import com.marcusprado02.commons.kernel.ddd.event.DomainEvent;

/**
 * Port: bridges a domain event into the Outbox for reliable out-of-process delivery.
 *
 * <p>The adapter ({@code KafkaOutboxEventBridge}) serializes the event to JSON and
 * appends it to the outbox table within the current transaction.
 */
public interface OutboxEventBridge {
  void publish(String topic, DomainEvent event);
}
