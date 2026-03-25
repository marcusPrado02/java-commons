package com.example.events;

import com.example.events.adapters.messaging.WelcomeEmailHandler;
import com.marcusprado02.commons.app.domainevents.DomainEventBus;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Event-Driven Service — Kafka + Domain Events + Transactional Outbox.
 *
 * <p>Key concepts demonstrated:
 * <ul>
 *   <li>{@code DomainEventBus} — in-process event dispatch; handlers react synchronously
 *   <li>{@code KafkaOutboxEventBridge} — appends events to Outbox within the write transaction
 *   <li>{@code WelcomeEmailHandler} — reacts to {@code UserRegisteredEvent} in-process
 *   <li>{@code commons-spring-starter-outbox} — polls Outbox and forwards to Kafka
 * </ul>
 *
 * <p>Topology:
 * <pre>
 *  RegisterUserUseCase
 *       │
 *       ├─── DomainEventBus.publish() ──→ WelcomeEmailHandler (in-process, sync)
 *       │
 *       └─── OutboxEventBridge.publish() ──→ outbox_messages table
 *                                                  │
 *                                           OutboxProcessor (scheduled)
 *                                                  │
 *                                           Kafka topic: users.registered
 * </pre>
 *
 * <p>Run: {@code mvn spring-boot:run}
 * <p>API: {@code POST /api/users  {email, name}}
 */
@SpringBootApplication
public class EventDrivenServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(EventDrivenServiceApplication.class, args);
  }

  /** Register all in-process domain event handlers with the bus. */
  @Bean
  public DomainEventBus domainEventBus(WelcomeEmailHandler welcomeHandler) {
    DomainEventBus bus = new DomainEventBus();
    bus.register(welcomeHandler);
    return bus;
  }
}
