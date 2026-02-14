package com.marcusprado02.commons.app.domainevents;

import com.marcusprado02.commons.kernel.ddd.event.DomainEvent;

/**
 * Handler for domain events.
 *
 * <p>Implementations define how to handle specific types of domain events.
 *
 * <p><strong>Usage:</strong>
 *
 * <pre>{@code
 * @Component
 * public class OrderCreatedHandler implements DomainEventHandler<OrderCreatedEvent> {
 *     @Override
 *     public void handle(OrderCreatedEvent event) {
 *         // Send notification email
 *         emailService.sendOrderConfirmation(event.customerId());
 *     }
 *
 *     @Override
 *     public Class<OrderCreatedEvent> eventType() {
 *         return OrderCreatedEvent.class;
 *     }
 * }
 * }</pre>
 *
 * @param <T> the type of event this handler processes
 */
public interface DomainEventHandler<T extends DomainEvent> {

  /**
   * Handles the domain event.
   *
   * @param event the event to handle
   */
  void handle(T event);

  /**
   * Returns the type of event this handler processes.
   *
   * @return the event class
   */
  Class<T> eventType();
}
