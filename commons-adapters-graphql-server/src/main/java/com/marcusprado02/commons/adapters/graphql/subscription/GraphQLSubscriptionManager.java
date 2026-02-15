package com.marcusprado02.commons.adapters.graphql.subscription;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Manager for GraphQL subscriptions.
 *
 * <p>Provides utilities for:
 *
 * <ul>
 *   <li>Creating and managing subscription publishers
 *   <li>Broadcasting events to subscribers
 *   <li>Filtering events by criteria
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * @Controller
 * public class UserSubscriptionController {
 *
 *     private final GraphQLSubscriptionManager<UserEvent> subscriptionManager =
 *         new GraphQLSubscriptionManager<>();
 *
 *     @SubscriptionMapping
 *     public Publisher<UserEvent> onUserCreated() {
 *         return subscriptionManager.subscribe(event -> event.type() == EventType.CREATED);
 *     }
 *
 *     public void publishUserEvent(UserEvent event) {
 *         subscriptionManager.publish(event);
 *     }
 * }
 * }</pre>
 *
 * @param <T> event type
 */
public class GraphQLSubscriptionManager<T> {

  private final ConcurrentHashMap<String, Sinks.Many<T>> sinks = new ConcurrentHashMap<>();
  private final Sinks.Many<T> broadcastSink;

  public GraphQLSubscriptionManager() {
    this.broadcastSink =
        Sinks.many().multicast().onBackpressureBuffer(1000, false);
  }

  /**
   * Subscribes to all events.
   *
   * @return publisher for all events
   */
  public Publisher<T> subscribe() {
    return broadcastSink.asFlux();
  }

  /**
   * Subscribes to events matching the filter.
   *
   * @param filter event filter
   * @return publisher for filtered events
   */
  public Publisher<T> subscribe(Predicate<T> filter) {
    return broadcastSink.asFlux().filter(filter);
  }

  /**
   * Subscribes to events with a custom sink ID.
   *
   * <p>Useful for managing individual subscriptions.
   *
   * @param sinkId unique sink ID
   * @return publisher for the sink
   */
  public Publisher<T> subscribe(String sinkId) {
    Sinks.Many<T> sink =
        sinks.computeIfAbsent(
            sinkId, k -> Sinks.many().multicast().onBackpressureBuffer(100, false));
    return sink.asFlux();
  }

  /**
   * Subscribes to events with periodic heartbeat.
   *
   * <p>Sends heartbeat events to keep connection alive.
   *
   * @param heartbeatInterval heartbeat interval
   * @param heartbeatEvent event to send as heartbeat
   * @return publisher with heartbeat
   */
  public Publisher<T> subscribeWithHeartbeat(Duration heartbeatInterval, T heartbeatEvent) {
    Flux<T> events = broadcastSink.asFlux();
    Flux<T> heartbeat = Flux.interval(heartbeatInterval).map(tick -> heartbeatEvent);
    return Flux.merge(events, heartbeat);
  }

  /**
   * Publishes an event to all subscribers.
   *
   * @param event event to publish
   */
  public void publish(T event) {
    broadcastSink.tryEmitNext(event);
    sinks.values().forEach(sink -> sink.tryEmitNext(event));
  }

  /**
   * Publishes an event to a specific sink.
   *
   * @param sinkId sink ID
   * @param event event to publish
   */
  public void publish(String sinkId, T event) {
    Sinks.Many<T> sink = sinks.get(sinkId);
    if (sink != null) {
      sink.tryEmitNext(event);
    }
  }

  /**
   * Completes a specific sink.
   *
   * @param sinkId sink ID
   */
  public void complete(String sinkId) {
    Sinks.Many<T> sink = sinks.remove(sinkId);
    if (sink != null) {
      sink.tryEmitComplete();
    }
  }

  /**
   * Completes all sinks.
   */
  public void completeAll() {
    broadcastSink.tryEmitComplete();
    sinks.values().forEach(Sinks.Many::tryEmitComplete);
    sinks.clear();
  }

  /**
   * Gets the number of active sinks.
   *
   * @return number of sinks
   */
  public int getSinkCount() {
    return sinks.size();
  }
}
