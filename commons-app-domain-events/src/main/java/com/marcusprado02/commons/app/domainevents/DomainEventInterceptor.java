package com.marcusprado02.commons.app.domainevents;

import com.marcusprado02.commons.kernel.ddd.event.DomainEvent;

/**
 * Interceptor for domain events.
 *
 * <p>Allows cross-cutting concerns to be applied to all events (e.g., logging, metrics, tracing).
 *
 * <p><strong>Usage:</strong>
 *
 * <pre>{@code
 * public class LoggingInterceptor implements DomainEventInterceptor {
 *     @Override
 *     public void beforePublish(DomainEvent event) {
 *         log.info("Publishing event: {}", event.eventType());
 *     }
 *
 *     @Override
 *     public void afterPublish(DomainEvent event) {
 *         log.info("Published event: {}", event.eventType());
 *     }
 * }
 *
 * eventBus.registerInterceptor(new LoggingInterceptor());
 * }</pre>
 */
public interface DomainEventInterceptor {

  /**
   * Called before an event is published to handlers.
   *
   * @param event the event about to be published
   */
  default void beforePublish(DomainEvent event) {
    // Default: no-op
  }

  /**
   * Called after an event has been published to all handlers.
   *
   * @param event the event that was published
   */
  default void afterPublish(DomainEvent event) {
    // Default: no-op
  }
}
