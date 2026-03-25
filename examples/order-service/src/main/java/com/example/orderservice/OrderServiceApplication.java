package com.example.orderservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Order Service — demonstrates Transactional Outbox + simple Saga coordination.
 *
 * <p>Key concepts:
 * <ul>
 *   <li>{@code Order} aggregate captures domain events in-memory
 *   <li>{@code JsonOutboxEventPublisher} serializes events into the Outbox table (same tx)
 *   <li>{@code commons-spring-starter-outbox} polls the Outbox and publishes to the broker
 *   <li>{@code PlaceOrderUseCase} / {@code ConfirmOrderUseCase} orchestrate the Saga steps
 * </ul>
 *
 * <p>Run: {@code mvn spring-boot:run}
 * <p>API: {@code POST /api/orders}, {@code POST /api/orders/{id}/confirm}
 */
@SpringBootApplication
public class OrderServiceApplication {
  public static void main(String[] args) {
    SpringApplication.run(OrderServiceApplication.class, args);
  }
}
