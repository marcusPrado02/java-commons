package com.example.events.application;

import com.example.events.domain.UserRegisteredEvent;
import com.marcusprado02.commons.app.domainevents.DomainEventBus;
import com.marcusprado02.commons.kernel.errors.Problems;
import com.marcusprado02.commons.kernel.result.Result;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Use case: Register a new user.
 *
 * <p>Demonstrates dual publishing:
 * <ol>
 *   <li>The {@code UserRegisteredEvent} is published to the in-process {@code DomainEventBus}
 *       so that handlers in the same JVM react immediately (e.g. sending a welcome e-mail stub).
 *   <li>The same event is also appended to the Outbox (via {@code OutboxEventBridge}) so it
 *       will be reliably forwarded to Kafka even if the broker is temporarily unavailable.
 * </ol>
 */
@Service
public class RegisterUserUseCase {

  private final DomainEventBus eventBus;
  private final OutboxEventBridge outboxBridge;

  // Simple in-memory user "store" for this example
  private final java.util.concurrent.ConcurrentHashMap<String, String> users =
      new java.util.concurrent.ConcurrentHashMap<>();

  public RegisterUserUseCase(DomainEventBus eventBus, OutboxEventBridge outboxBridge) {
    this.eventBus = eventBus;
    this.outboxBridge = outboxBridge;
  }

  public Result<String> execute(String email, String name) {
    if (email == null || !email.contains("@")) {
      return Result.fail(Problems.validation("USER.INVALID_EMAIL", "Invalid e-mail: " + email));
    }
    if (name == null || name.isBlank()) {
      return Result.fail(Problems.validation("USER.INVALID_NAME", "Name is required"));
    }
    if (users.containsValue(email)) {
      return Result.fail(Problems.conflict("USER.EMAIL_CONFLICT", "E-mail already registered: " + email));
    }

    String userId = UUID.randomUUID().toString();
    users.put(userId, email);

    var event = new UserRegisteredEvent(userId, email, name, Instant.now());

    // 1. In-process dispatch (synchronous handlers receive it immediately)
    eventBus.publish(event);

    // 2. Outbox → Kafka (transactional, reliable)
    outboxBridge.publish("users.registered", event);

    return Result.ok(userId);
  }
}
