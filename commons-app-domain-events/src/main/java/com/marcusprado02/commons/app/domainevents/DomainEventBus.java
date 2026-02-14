package com.marcusprado02.commons.app.domainevents;

import com.marcusprado02.commons.kernel.ddd.event.DomainEvent;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * In-memory event bus for domain events.
 *
 * <p>Combines event handler registry and dispatcher into a simple facade for publishing and
 * handling domain events.
 *
 * <p><strong>Thread Safety:</strong> This event bus is thread-safe and can be used in concurrent
 * environments.
 *
 * <p><strong>Usage:</strong>
 *
 * <pre>{@code
 * // Create event bus
 * DomainEventBus eventBus = new DomainEventBus();
 *
 * // Register handlers
 * eventBus.register(new OrderCreatedHandler());
 * eventBus.register(new OrderCreatedNotificationHandler());
 *
 * // Publish events
 * eventBus.publish(orderCreatedEvent);
 *
 * // Or publish from aggregate
 * Order order = ...; // aggregate root
 * eventBus.publishAll(order.pullDomainEvents());
 * }</pre>
 */
public final class DomainEventBus {

  private static final Logger log = LoggerFactory.getLogger(DomainEventBus.class);

  private final DomainEventHandlerRegistry registry;
  private final DomainEventDispatcher dispatcher;
  private final List<DomainEventInterceptor> interceptors;

  public DomainEventBus() {
    this.registry = new DomainEventHandlerRegistry();
    this.dispatcher = new DomainEventDispatcher(registry);
    this.interceptors = new CopyOnWriteArrayList<>();
  }

  /**
   * Registers a domain event handler.
   *
   * @param handler the handler to register
   * @param <T> the type of event the handler processes
   */
  public <T extends DomainEvent> void register(DomainEventHandler<T> handler) {
    registry.register(handler);
  }

  /**
   * Unregisters a domain event handler.
   *
   * @param handler the handler to unregister
   * @param <T> the type of event the handler processes
   */
  public <T extends DomainEvent> void unregister(DomainEventHandler<T> handler) {
    registry.unregister(handler);
  }

  /**
   * Registers an interceptor for all events.
   *
   * @param interceptor the interceptor to register
   */
  public void registerInterceptor(DomainEventInterceptor interceptor) {
    Objects.requireNonNull(interceptor, "interceptor");
    interceptors.add(interceptor);
    log.debug("Registered interceptor {}", interceptor.getClass().getSimpleName());
  }

  /**
   * Publishes a single domain event.
   *
   * @param event the event to publish
   */
  public void publish(DomainEvent event) {
    Objects.requireNonNull(event, "event");

    // Pre-publish interceptors
    for (DomainEventInterceptor interceptor : interceptors) {
      interceptor.beforePublish(event);
    }

    // Dispatch to handlers
    dispatcher.dispatch(event);

    // Post-publish interceptors
    for (DomainEventInterceptor interceptor : interceptors) {
      interceptor.afterPublish(event);
    }
  }

  /**
   * Publishes multiple domain events.
   *
   * @param events the events to publish
   */
  public void publishAll(List<DomainEvent> events) {
    Objects.requireNonNull(events, "events");
    for (DomainEvent event : events) {
      publish(event);
    }
  }

  /**
   * Returns the number of registered handlers.
   *
   * @return handler count
   */
  public int handlerCount() {
    return registry.size();
  }

  /**
   * Clears all registered handlers and interceptors.
   */
  public void clear() {
    registry.clear();
    interceptors.clear();
  }
}
