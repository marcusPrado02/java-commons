package com.marcusprado02.commons.app.domainevents;

import com.marcusprado02.commons.kernel.ddd.event.DomainEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registry for domain event handlers.
 *
 * <p>Maintains a mapping of event types to their handlers and provides methods to register and
 * retrieve handlers.
 *
 * <p><strong>Thread Safety:</strong> This registry is thread-safe and can be used in concurrent
 * environments.
 *
 * <p><strong>Usage:</strong>
 *
 * <pre>{@code
 * DomainEventHandlerRegistry registry = new DomainEventHandlerRegistry();
 *
 * // Register handlers
 * registry.register(new OrderCreatedHandler());
 * registry.register(new OrderCreatedNotificationHandler());
 *
 * // Get handlers for an event type
 * List<DomainEventHandler<?>> handlers = registry.getHandlers(OrderCreatedEvent.class);
 * }</pre>
 */
public final class DomainEventHandlerRegistry {

  private static final Logger log = LoggerFactory.getLogger(DomainEventHandlerRegistry.class);

  private final Map<Class<? extends DomainEvent>, List<DomainEventHandler<?>>> handlers =
      new ConcurrentHashMap<>();

  /**
   * Registers a domain event handler.
   *
   * @param handler the handler to register
   * @param <T> the type of event the handler processes
   */
  public <T extends DomainEvent> void register(DomainEventHandler<T> handler) {
    Class<T> eventType = handler.eventType();
    handlers.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(handler);
    log.debug(
        "Registered handler {} for event type {}",
        handler.getClass().getSimpleName(),
        eventType.getSimpleName());
  }

  /**
   * Unregisters a domain event handler.
   *
   * @param handler the handler to unregister
   * @param <T> the type of event the handler processes
   */
  public <T extends DomainEvent> void unregister(DomainEventHandler<T> handler) {
    Class<T> eventType = handler.eventType();
    List<DomainEventHandler<?>> eventHandlers = handlers.get(eventType);
    if (eventHandlers != null) {
      eventHandlers.remove(handler);
      log.debug(
          "Unregistered handler {} for event type {}",
          handler.getClass().getSimpleName(),
          eventType.getSimpleName());
    }
  }

  /**
   * Gets all handlers for a specific event type.
   *
   * @param eventType the event type
   * @return list of handlers (empty if none registered)
   */
  public List<DomainEventHandler<?>> getHandlers(Class<? extends DomainEvent> eventType) {
    return new ArrayList<>(handlers.getOrDefault(eventType, List.of()));
  }

  /**
   * Checks if there are any handlers registered for the given event type.
   *
   * @param eventType the event type
   * @return true if at least one handler is registered
   */
  public boolean hasHandlers(Class<? extends DomainEvent> eventType) {
    List<DomainEventHandler<?>> eventHandlers = handlers.get(eventType);
    return eventHandlers != null && !eventHandlers.isEmpty();
  }

  /**
   * Clears all registered handlers.
   */
  public void clear() {
    handlers.clear();
    log.debug("Cleared all event handlers");
  }

  /**
   * Returns the total number of registered handlers across all event types.
   *
   * @return total handler count
   */
  public int size() {
    return handlers.values().stream().mapToInt(List::size).sum();
  }
}
