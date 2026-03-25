package com.example.orderservice.domain.event;

import com.example.orderservice.domain.OrderId;
import java.time.Instant;

/** Published when an Order is dispatched for shipping. */
public record OrderShippedEvent(OrderId orderId, String trackingNumber, Instant shippedAt) {}
