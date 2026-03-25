package com.example.orderservice.domain.event;

import com.example.orderservice.domain.OrderId;
import java.time.Instant;

/** Published when an Order is cancelled. */
public record OrderCancelledEvent(OrderId orderId, String reason, Instant cancelledAt) {}
