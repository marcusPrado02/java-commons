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

class DomainEventBusTest {

  private DomainEventBus eventBus;

  @BeforeEach
  void setUp() {
    eventBus = new DomainEventBus();
  }

  @Test
  void shouldPublishEventToHandler() {
    AtomicInteger callCount = new AtomicInteger(0);

    DomainEventHandler<TestEvent> handler =
        new DomainEventHandler<>() {
          @Override
          public void handle(TestEvent event) {
            callCount.incrementAndGet();
            assertEquals("test data", event.data());
          }

          @Override
          public Class<TestEvent> eventType() {
            return TestEvent.class;
          }
        };

    eventBus.register(handler);
    eventBus.publish(createTestEvent("test data"));

    assertEquals(1, callCount.get());
  }

  @Test
  void shouldPublishAllEvents() {
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

    eventBus.register(handler);
    eventBus.publishAll(List.of(createTestEvent("data1"), createTestEvent("data2")));

    assertEquals(2, callCount.get());
  }

  @Test
  void shouldCallInterceptors() {
    AtomicInteger beforeCount = new AtomicInteger(0);
    AtomicInteger afterCount = new AtomicInteger(0);

    DomainEventInterceptor interceptor =
        new DomainEventInterceptor() {
          @Override
          public void beforePublish(DomainEvent event) {
            beforeCount.incrementAndGet();
          }

          @Override
          public void afterPublish(DomainEvent event) {
            afterCount.incrementAndGet();
          }
        };

    eventBus.registerInterceptor(interceptor);
    eventBus.publish(createTestEvent("data"));

    assertEquals(1, beforeCount.get());
    assertEquals(1, afterCount.get());
  }

  @Test
  void shouldUnregisterHandler() {
    DomainEventHandler<TestEvent> handler =
        new DomainEventHandler<>() {
          @Override
          public void handle(TestEvent event) {}

          @Override
          public Class<TestEvent> eventType() {
            return TestEvent.class;
          }
        };

    eventBus.register(handler);
    assertEquals(1, eventBus.handlerCount());

    eventBus.unregister(handler);
    assertEquals(0, eventBus.handlerCount());
  }

  @Test
  void shouldClearAllHandlersAndInterceptors() {
    DomainEventHandler<TestEvent> handler =
        new DomainEventHandler<>() {
          @Override
          public void handle(TestEvent event) {}

          @Override
          public Class<TestEvent> eventType() {
            return TestEvent.class;
          }
        };

    eventBus.register(handler);
    eventBus.registerInterceptor(new DomainEventInterceptor() {});

    eventBus.clear();

    assertEquals(0, eventBus.handlerCount());
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
