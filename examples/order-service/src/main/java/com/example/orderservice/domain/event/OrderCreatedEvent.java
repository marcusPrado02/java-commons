package com.example.orderservice.domain.event;

import com.example.orderservice.domain.OrderId;
import java.math.BigDecimal;
import java.time.Instant;

/** Published when a new Order is successfully created. */
public record OrderCreatedEvent(
    OrderId orderId,
    String customerId,
    BigDecimal totalAmount,
    Instant occurredAt) {}
