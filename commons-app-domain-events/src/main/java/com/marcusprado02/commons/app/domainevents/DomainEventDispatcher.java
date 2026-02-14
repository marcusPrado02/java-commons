package com.marcusprado02.commons.app.domainevents;

import com.marcusprado02.commons.kernel.ddd.event.DomainEvent;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dispatches domain events to registered handlers.
 *
 * <p>The dispatcher retrieves handlers from the registry and invokes them for each event.
 *
 * <p><strong>Error Handling:</strong> If a handler throws an exception, it is logged and the
 * dispatcher continues processing other handlers. This ensures that one failing handler doesn't
 * prevent others from executing.
 *
 * <p><strong>Usage:</strong>
 *
 * <pre>{@code
 * DomainEventHandlerRegistry registry = new DomainEventHandlerRegistry();
 * registry.register(new OrderCreatedHandler());
 *
 * DomainEventDispatcher dispatcher = new DomainEventDispatcher(registry);
 *
 * // Dispatch single event
 * dispatcher.dispatch(orderCreatedEvent);
 *
 * // Dispatch multiple events
 * dispatcher.dispatchAll(order.pullDomainEvents());
 * }</pre>
 */
public final class DomainEventDispatcher {

  private static final Logger log = LoggerFactory.getLogger(DomainEventDispatcher.class);

  private final DomainEventHandlerRegistry registry;

  public DomainEventDispatcher(DomainEventHandlerRegistry registry) {
    this.registry = Objects.requireNonNull(registry, "registry");
  }

  /**
   * Dispatches a single domain event to all registered handlers.
   *
   * @param event the event to dispatch
   */
  public void dispatch(DomainEvent event) {
    Objects.requireNonNull(event, "event");

    Class<? extends DomainEvent> eventType = event.getClass();
    List<DomainEventHandler<?>> handlers = registry.getHandlers(eventType);

    log.debug(
        "Dispatching event {} to {} handler(s)",
        eventType.getSimpleName(),
        handlers.size());

    for (DomainEventHandler<?> handler : handlers) {
      try {
        @SuppressWarnings("unchecked")
        DomainEventHandler<DomainEvent> typedHandler = (DomainEventHandler<DomainEvent>) handler;
        typedHandler.handle(event);
        log.trace(
            "Handler {} processed event {}",
            handler.getClass().getSimpleName(),
            eventType.getSimpleName());
      } catch (Exception e) {
        log.error(
            "Handler {} failed to process event {}: {}",
            handler.getClass().getSimpleName(),
            eventType.getSimpleName(),
            e.getMessage(),
            e);
        // Continue processing other handlers
      }
    }
  }

  /**
   * Dispatches multiple domain events to all registered handlers.
   *
   * @param events the events to dispatch
   */
  public void dispatchAll(List<DomainEvent> events) {
    Objects.requireNonNull(events, "events");
    for (DomainEvent event : events) {
      dispatch(event);
    }
  }
}
