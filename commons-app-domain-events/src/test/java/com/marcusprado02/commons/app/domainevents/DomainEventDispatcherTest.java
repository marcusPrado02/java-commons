package com.marcusprado02.commons.app.domainevents;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.marcusprado02.commons.kernel.ddd.entity.AggregateSnapshot;
import com.marcusprado02.commons.kernel.ddd.event.DomainEvent;
import com.marcusprado02.commons.kernel.ddd.id.UuidIdentifier;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DomainEventDispatcherTest {

  private DomainEventHandlerRegistry registry;
  private DomainEventDispatcher dispatcher;

  @BeforeEach
  void setUp() {
    registry = new DomainEventHandlerRegistry();
    dispatcher = new DomainEventDispatcher(registry);
  }

  @Test
  void shouldDispatchEventToRegisteredHandlers() {
    AtomicInteger callCount = new AtomicInteger(0);

    DomainEventHandler<TestEvent> handler =
        new DomainEventHandler<>() {
          @Override
          public void handle(TestEvent event) {
            callCount.incrementAndGet();
          }

          @Override
          public Class<TestEvent> eventType() {
            return TestEvent.class;
          }
        };

    registry.register(handler);

    TestEvent event = createTestEvent("data");
    dispatcher.dispatch(event);

    assertEquals(1, callCount.get());
  }

  @Test
  void shouldDispatchEventToMultipleHandlers() {
    AtomicInteger callCount = new AtomicInteger(0);

    DomainEventHandler<TestEvent> handler1 =
        new DomainEventHandler<>() {
          @Override
          public void handle(TestEvent event) {
            callCount.incrementAndGet();
          }

          @Override
          public Class<TestEvent> eventType() {
            return TestEvent.class;
          }
        };

    DomainEventHandler<TestEvent> handler2 =
        new DomainEventHandler<>() {
          @Override
          public void handle(TestEvent event) {
            callCount.incrementAndGet();
          }

          @Override
          public Class<TestEvent> eventType() {
            return TestEvent.class;
          }
        };

    registry.register(handler1);
    registry.register(handler2);

    dispatcher.dispatch(createTestEvent("data"));

    assertEquals(2, callCount.get());
  }

  @Test
  void shouldContinueDispatchingWhenHandlerThrowsException() {
    AtomicInteger successCount = new AtomicInteger(0);

    DomainEventHandler<TestEvent> failingHandler =
        new DomainEventHandler<>() {
          @Override
          public void handle(TestEvent event) {
            throw new RuntimeException("Handler failed");
          }

          @Override
          public Class<TestEvent> eventType() {
            return TestEvent.class;
          }
        };

    DomainEventHandler<TestEvent> successHandler =
        new DomainEventHandler<>() {
          @Override
          public void handle(TestEvent event) {
            successCount.incrementAndGet();
          }

          @Override
          public Class<TestEvent> eventType() {
            return TestEvent.class;
          }
        };

    registry.register(failingHandler);
    registry.register(successHandler);

    dispatcher.dispatch(createTestEvent("data"));

    assertEquals(1, successCount.get());
  }

  @Test
  void shouldDispatchAllEvents() {
    AtomicInteger callCount = new AtomicInteger(0);

    DomainEventHandler<TestEvent> handler =
        new DomainEventHandler<>() {
          @Override
          public void handle(TestEvent event) {
            callCount.incrementAndGet();
          }

          @Override
          public Class<TestEvent> eventType() {
            return TestEvent.class;
          }
        };

    registry.register(handler);

    List<DomainEvent> events = List.of(createTestEvent("data1"), createTestEvent("data2"));

    dispatcher.dispatchAll(events);

    assertEquals(2, callCount.get());
  }

  private TestEvent createTestEvent(String data) {
    return new TestEvent(mockSnapshot(), data);
  }

  private AggregateSnapshot<TestId> mockSnapshot() {
    return mock(AggregateSnapshot.class);
  }

  // Test event
  record TestEvent(AggregateSnapshot<TestId> aggregate, String data) implements DomainEvent {
    @Override
    public com.marcusprado02.commons.kernel.ddd.event.EventId eventId() {
      return com.marcusprado02.commons.kernel.ddd.event.EventId.newId();
    }

    @Override
    public java.time.Instant occurredAt() {
      return java.time.Instant.now();
    }

    @Override
    public String aggregateType() {
      return "Test";
    }

    @Override
    public String aggregateId() {
      return aggregate.id().toString();
    }

    @Override
    public long aggregateVersion() {
      return 1L;
    }

    @Override
    public com.marcusprado02.commons.kernel.ddd.event.EventMetadata metadata() {
      return com.marcusprado02.commons.kernel.ddd.event.EventMetadata.empty();
    }
  }

  static class TestId extends UuidIdentifier {
    public TestId(String value) {
      super(value);
    }
  }
}
