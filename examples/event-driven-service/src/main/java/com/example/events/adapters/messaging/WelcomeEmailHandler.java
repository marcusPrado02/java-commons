package com.example.events.adapters.messaging;

import com.example.events.domain.UserRegisteredEvent;
import com.marcusprado02.commons.app.domainevents.DomainEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * In-process domain event handler: sends a welcome e-mail stub when a user registers.
 *
 * <p>Registered with the {@code DomainEventBus} via Spring's component scan.
 * No Kafka dependency — reacts to the in-process event synchronously within the same request.
 *
 * <p>This is ideal for fast, non-critical side effects (logging, metrics, small notifications).
 * For durable delivery (e-mail that must arrive even if the service restarts), use the Outbox path.
 */
@Component
public class WelcomeEmailHandler implements DomainEventHandler<UserRegisteredEvent> {

  private static final Logger log = LoggerFactory.getLogger(WelcomeEmailHandler.class);

  @Override
  public void handle(UserRegisteredEvent event) {
    // In a real service, inject EmailPort and call sendWelcome(event.email(), event.name())
    log.info("[WelcomeEmailHandler] Sending welcome e-mail to {} (userId={})",
        event.email(), event.userId());
  }

  @Override
  public Class<UserRegisteredEvent> eventType() {
    return UserRegisteredEvent.class;
  }
}
