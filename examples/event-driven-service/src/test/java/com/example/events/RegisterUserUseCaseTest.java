package com.example.events;

import static org.junit.jupiter.api.Assertions.*;

import com.example.events.application.OutboxEventBridge;
import com.example.events.application.RegisterUserUseCase;
import com.example.events.domain.UserRegisteredEvent;
import com.marcusprado02.commons.app.domainevents.DomainEventBus;
import com.marcusprado02.commons.app.domainevents.DomainEventHandler;
import com.marcusprado02.commons.kernel.ddd.event.DomainEvent;
import com.marcusprado02.commons.kernel.result.Result;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RegisterUserUseCaseTest {

  private DomainEventBus eventBus;
  private CapturingOutboxBridge outboxBridge;
  private CapturingEventHandler eventHandler;
  private RegisterUserUseCase useCase;

  @BeforeEach
  void setUp() {
    eventBus = new DomainEventBus();
    eventHandler = new CapturingEventHandler();
    eventBus.register(eventHandler);

    outboxBridge = new CapturingOutboxBridge();
    useCase = new RegisterUserUseCase(eventBus, outboxBridge);
  }

  @Test
  void should_register_user_and_publish_event_in_process_and_to_outbox() {
    Result<String> result = useCase.execute("alice@example.com", "Alice");

    assertTrue(result.isOk());
    assertNotNull(result.getOrNull()); // userId

    // In-process handler received the event
    assertEquals(1, eventHandler.events.size());
    assertInstanceOf(UserRegisteredEvent.class, eventHandler.events.get(0));
    assertEquals("alice@example.com", ((UserRegisteredEvent) eventHandler.events.get(0)).email());

    // Outbox bridge also received the event
    assertEquals(1, outboxBridge.published.size());
    assertEquals("users.registered", outboxBridge.published.get(0).topic());
  }

  @Test
  void should_fail_with_invalid_email() {
    Result<String> result = useCase.execute("not-an-email", "Bob");
    assertTrue(result.isFail());
    assertEquals("USER.INVALID_EMAIL", result.problemOrNull().code().value());
    assertTrue(eventHandler.events.isEmpty());
    assertTrue(outboxBridge.published.isEmpty());
  }

  @Test
  void should_fail_with_blank_name() {
    Result<String> result = useCase.execute("bob@example.com", "   ");
    assertTrue(result.isFail());
    assertEquals("USER.INVALID_NAME", result.problemOrNull().code().value());
  }

  @Test
  void should_fail_on_duplicate_email() {
    useCase.execute("dup@example.com", "First");
    Result<String> second = useCase.execute("dup@example.com", "Second");
    assertTrue(second.isFail());
    assertEquals("USER.EMAIL_CONFLICT", second.problemOrNull().code().value());
  }

  // ---- Test doubles ----

  static class CapturingEventHandler implements DomainEventHandler<UserRegisteredEvent> {
    final List<UserRegisteredEvent> events = new ArrayList<>();

    @Override
    public void handle(UserRegisteredEvent event) { events.add(event); }

    @Override
    public Class<UserRegisteredEvent> eventType() { return UserRegisteredEvent.class; }
  }

  static class CapturingOutboxBridge implements OutboxEventBridge {
    record Entry(String topic, DomainEvent event) {}
    final List<Entry> published = new ArrayList<>();

    @Override
    public void publish(String topic, DomainEvent event) {
      published.add(new Entry(topic, event));
    }
  }
}
