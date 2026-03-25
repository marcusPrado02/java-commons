package com.example.orderservice.application;

import com.example.orderservice.domain.Order;
import com.example.orderservice.domain.OrderId;
import com.example.orderservice.domain.OrderRepository;
import com.marcusprado02.commons.kernel.errors.Problems;
import com.marcusprado02.commons.kernel.result.Result;
import org.springframework.stereotype.Service;

/**
 * Confirms a pending order and publishes an OrderConfirmed event via the Outbox.
 */
@Service
public class ConfirmOrderUseCase {

  private final OrderRepository orderRepository;
  private final OutboxEventPublisher outboxPublisher;

  public ConfirmOrderUseCase(
      OrderRepository orderRepository, OutboxEventPublisher outboxPublisher) {
    this.orderRepository = orderRepository;
    this.outboxPublisher = outboxPublisher;
  }

  public Result<Order> execute(String orderId) {
    return orderRepository
        .findById(OrderId.of(orderId))
        .map(order ->
            order.confirm().map(ignored -> {
              orderRepository.save(order);
              order.flushEvents().forEach(event -> outboxPublisher.publish("orders", event));
              return order;
            }))
        .orElse(
            Result.fail(Problems.notFound("ORDER.NOT_FOUND", "Order not found: " + orderId)));
  }
}
