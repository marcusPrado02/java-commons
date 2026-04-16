package com.marcusprado02.commons.adapters.graphql.subscription;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import reactor.test.StepVerifier;

class GraphQLSubscriptionManagerTest {

  private GraphQLSubscriptionManager<String> manager;

  @BeforeEach
  void setUp() {
    manager = new GraphQLSubscriptionManager<>();
  }

  @Test
  void shouldPublishAndReceiveEvent() {
    Publisher<String> subscription = manager.subscribe();

    manager.publish("event1");
    manager.publish("event2");

    StepVerifier.create(subscription)
        .expectNext("event1")
        .expectNext("event2")
        .thenCancel()
        .verify(Duration.ofSeconds(5));
  }

  @Test
  void shouldFilterEvents() {
    Publisher<String> subscription = manager.subscribe(event -> event.startsWith("test"));

    manager.publish("test1");
    manager.publish("other");
    manager.publish("test2");

    StepVerifier.create(subscription)
        .expectNext("test1")
        .expectNext("test2")
        .thenCancel()
        .verify(Duration.ofSeconds(5));
  }

  @Test
  void shouldPublishToSpecificSink() {
    Publisher<String> subscription1 = manager.subscribe("sink1");
    Publisher<String> subscription2 = manager.subscribe("sink2");

    manager.publish("sink1", "event1");
    manager.publish("sink2", "event2");

    StepVerifier.create(subscription1)
        .expectNext("event1")
        .thenCancel()
        .verify(Duration.ofSeconds(5));

    StepVerifier.create(subscription2)
        .expectNext("event2")
        .thenCancel()
        .verify(Duration.ofSeconds(5));
  }

  @Test
  void shouldCompleteSpecificSink() {
    Publisher<String> subscription = manager.subscribe("sink1");

    manager.publish("sink1", "event1");
    manager.complete("sink1");

    StepVerifier.create(subscription)
        .expectNext("event1")
        .expectComplete()
        .verify(Duration.ofSeconds(5));
  }

  @Test
  void shouldCompleteAllSinks() {
    Publisher<String> subscription1 = manager.subscribe("sink1");
    Publisher<String> subscription2 = manager.subscribe("sink2");

    manager.publish("event");
    manager.completeAll();

    StepVerifier.create(subscription1)
        .expectNext("event")
        .expectComplete()
        .verify(Duration.ofSeconds(5));

    StepVerifier.create(subscription2)
        .expectNext("event")
        .expectComplete()
        .verify(Duration.ofSeconds(5));
  }

  @Test
  void shouldTrackSinkCount() {
    assertEquals(0, manager.getSinkCount());

    manager.subscribe("sink1");
    assertEquals(1, manager.getSinkCount());

    manager.subscribe("sink2");
    assertEquals(2, manager.getSinkCount());

    manager.complete("sink1");
    assertEquals(1, manager.getSinkCount());

    manager.completeAll();
    assertEquals(0, manager.getSinkCount());
  }

  @Test
  void shouldHandleMultipleSubscribers() {
    // Multicast sink: each sequential subscriber receives events published after it subscribed.
    // Events consumed by subscriber-1's flux are not replayed to subscriber-2.
    Publisher<String> subscription1 = manager.subscribe();
    manager.publish("event1");
    StepVerifier.create(subscription1)
        .expectNext("event1")
        .thenCancel()
        .verify(Duration.ofSeconds(5));

    Publisher<String> subscription2 = manager.subscribe();
    manager.publish("event2");
    StepVerifier.create(subscription2)
        .expectNext("event2")
        .thenCancel()
        .verify(Duration.ofSeconds(5));
  }

  @Test
  void shouldIgnorePublishToNonExistentSink() {
    // Should not throw exception
    assertDoesNotThrow(() -> manager.publish("nonexistent", "event"));
  }

  @Test
  void shouldIgnoreCompleteNonExistentSink() {
    // Should not throw exception
    assertDoesNotThrow(() -> manager.complete("nonexistent"));
  }

  @Test
  void shouldHandleBackpressure() {
    Publisher<String> subscription = manager.subscribe();

    // Publish many events
    for (int i = 0; i < 100; i++) {
      manager.publish("event" + i);
    }

    // Should receive all events
    StepVerifier.create(subscription)
        .expectNextCount(100)
        .thenCancel()
        .verify(Duration.ofSeconds(10));
  }
}
