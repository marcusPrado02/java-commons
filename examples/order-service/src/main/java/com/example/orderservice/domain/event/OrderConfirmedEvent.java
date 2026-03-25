package com.example.orderservice.domain.event;

import com.example.orderservice.domain.OrderId;
import java.time.Instant;

/** Published when an Order transitions from PENDING to CONFIRMED. */
public record OrderConfirmedEvent(OrderId orderId, Instant confirmedAt) {}
