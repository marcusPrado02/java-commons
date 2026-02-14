package com.marcusprado02.commons.app.domainevents;

import static org.junit.jupiter.api.Assertions.*;

import com.marcusprado02.commons.kernel.ddd.entity.AggregateSnapshot;
import com.marcusprado02.commons.kernel.ddd.event.DomainEvent;
import com.marcusprado02.commons.kernel.ddd.id.UuidIdentifier;
import com.marcusprado02.commons.kernel.ddd.tenant.TenantId;
import com.marcusprado02.commons.kernel.ddd.version.EntityVersion;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DomainEventHandlerRegistryTest {

  private DomainEventHandlerRegistry registry;

  @BeforeEach
  void setUp() {
    registry = new DomainEventHandlerRegistry();
  }

  @Test
  void shouldRegisterHandler() {
    TestEventHandler handler = new TestEventHandler();

    registry.register(handler);

    assertTrue(registry.hasHandlers(TestEvent.class));
    assertEquals(1, registry.getHandlers(TestEvent.class).size());
  }

  @Test
  void shouldRegisterMultipleHandlers() {
    TestEventHandler handler1 = new TestEventHandler();
    TestEventHandler handler2 = new TestEventHandler();

    registry.register(handler1);
    registry.register(handler2);

    assertEquals(2, registry.getHandlers(TestEvent.class).size());
    assertEquals(2, registry.size());
  }

  @Test
  void shouldUnregisterHandler() {
    TestEventHandler handler = new TestEventHandler();
    registry.register(handler);

    registry.unregister(handler);

    assertFalse(registry.hasHandlers(TestEvent.class));
    assertEquals(0, registry.getHandlers(TestEvent.class).size());
  }

  @Test
  void shouldReturnEmptyListForUnknownEventType() {
    assertTrue(registry.getHandlers(TestEvent.class).isEmpty());
  }

  @Test
  void shouldClearAllHandlers() {
    registry.register(new TestEventHandler());
    registry.register(new AnotherTestEventHandler());

    registry.clear();

    assertEquals(0, registry.size());
    assertFalse(registry.hasHandlers(TestEvent.class));
    assertFalse(registry.hasHandlers(AnotherTestEvent.class));
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

  record AnotherTestEvent(AggregateSnapshot<TestId> aggregate) implements DomainEvent {
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

  // Test handlers
  static class TestEventHandler implements DomainEventHandler<TestEvent> {
    @Override
    public void handle(TestEvent event) {
      // Do nothing
    }

    @Override
    public Class<TestEvent> eventType() {
      return TestEvent.class;
    }
  }

  static class AnotherTestEventHandler implements DomainEventHandler<AnotherTestEvent> {
    @Override
    public void handle(AnotherTestEvent event) {
      // Do nothing
    }

    @Override
    public Class<AnotherTestEvent> eventType() {
      return AnotherTestEvent.class;
    }
  }
}
