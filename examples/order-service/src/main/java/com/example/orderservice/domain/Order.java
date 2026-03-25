package com.example.orderservice.domain;

import com.example.orderservice.domain.event.OrderCancelledEvent;
import com.example.orderservice.domain.event.OrderConfirmedEvent;
import com.example.orderservice.domain.event.OrderCreatedEvent;
import com.example.orderservice.domain.event.OrderShippedEvent;
import com.marcusprado02.commons.kernel.errors.Problems;
import com.marcusprado02.commons.kernel.result.Result;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Order aggregate root.
 *
 * <p>Encapsulates the full lifecycle: PENDING → CONFIRMED → SHIPPED | CANCELLED.
 * Domain events are collected in-memory and published via the Outbox pattern.
 */
public class Order {

  public enum Status {
    PENDING, CONFIRMED, SHIPPED, CANCELLED
  }

  private final OrderId id;
  private final String customerId;
  private final List<OrderItem> items;
  private Status status;
  private Instant createdAt;
  private Instant updatedAt;

  // Collected domain events (flushed by the application layer)
  private final List<Object> domainEvents = new ArrayList<>();

  private Order(OrderId id, String customerId, List<OrderItem> items) {
    this.id = Objects.requireNonNull(id);
    this.customerId = Objects.requireNonNull(customerId);
    this.items = new ArrayList<>(items);
    this.status = Status.PENDING;
    this.createdAt = Instant.now();
    this.updatedAt = this.createdAt;

    domainEvents.add(new OrderCreatedEvent(id, customerId, totalAmount(), createdAt));
  }

  // ---- Factory ----

  public static Result<Order> create(String customerId, List<OrderItem> items) {
    if (customerId == null || customerId.isBlank()) {
      return Result.fail(Problems.validation("ORDER.INVALID_CUSTOMER", "Customer ID is required"));
    }
    if (items == null || items.isEmpty()) {
      return Result.fail(Problems.validation("ORDER.NO_ITEMS", "At least one item is required"));
    }
    return Result.ok(new Order(OrderId.generate(), customerId, items));
  }

  // ---- Commands ----

  public Result<Void> confirm() {
    if (status != Status.PENDING) {
      return Result.fail(
          Problems.business("ORDER.INVALID_STATE",
              "Cannot confirm order in state: " + status));
    }
    status = Status.CONFIRMED;
    updatedAt = Instant.now();
    domainEvents.add(new OrderConfirmedEvent(id, updatedAt));
    return Result.ok(null);
  }

  public Result<Void> ship(String trackingNumber) {
    if (status != Status.CONFIRMED) {
      return Result.fail(
          Problems.business("ORDER.INVALID_STATE",
              "Cannot ship order in state: " + status));
    }
    status = Status.SHIPPED;
    updatedAt = Instant.now();
    domainEvents.add(new OrderShippedEvent(id, trackingNumber, updatedAt));
    return Result.ok(null);
  }

  public Result<Void> cancel(String reason) {
    if (status == Status.SHIPPED || status == Status.CANCELLED) {
      return Result.fail(
          Problems.business("ORDER.CANNOT_CANCEL",
              "Cannot cancel order in state: " + status));
    }
    status = Status.CANCELLED;
    updatedAt = Instant.now();
    domainEvents.add(new OrderCancelledEvent(id, reason, updatedAt));
    return Result.ok(null);
  }

  // ---- Queries ----

  public BigDecimal totalAmount() {
    return items.stream()
        .map(OrderItem::subtotal)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  public List<Object> flushEvents() {
    List<Object> events = List.copyOf(domainEvents);
    domainEvents.clear();
    return events;
  }

  // ---- Accessors ----

  public OrderId id()          { return id; }
  public String customerId()   { return customerId; }
  public Status status()       { return status; }
  public Instant createdAt()   { return createdAt; }
  public Instant updatedAt()   { return updatedAt; }
  public List<OrderItem> items() { return Collections.unmodifiableList(items); }
}
