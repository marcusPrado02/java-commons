package com.example.orderservice.application;

/**
 * Port: publishes domain events to the Outbox within the current transaction.
 *
 * <p>The implementation serializes the event to JSON and delegates to
 * {@code OutboxRepositoryPort.append(OutboxMessage)}.  Because it runs inside
 * the same DB transaction as the aggregate save, the "dual write" problem is
 * eliminated: either both the aggregate and the outbox entry are committed, or
 * neither is.
 */
public interface OutboxEventPublisher {

  /**
   * Appends a domain event to the outbox for the given topic.
   *
   * @param topic   the messaging topic / routing key (e.g. "orders", "orders.created")
   * @param event   the domain event object (will be serialized to JSON)
   */
  void publish(String topic, Object event);
}
