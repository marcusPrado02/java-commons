package com.example.orderservice.application;

import com.example.orderservice.domain.Order;
import com.example.orderservice.domain.OrderItem;
import com.example.orderservice.domain.OrderRepository;
import com.marcusprado02.commons.kernel.result.Result;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Places a new order and publishes an OrderCreated event via the Outbox.
 *
 * <p>Flow:
 * 1. Validate inputs (delegated to the Order aggregate)
 * 2. Persist the Order
 * 3. Pass domain events to the OutboxEventPublisher (inside the same transaction)
 * 4. Return the created Order
 */
@Service
public class PlaceOrderUseCase {

  private final OrderRepository orderRepository;
  private final OutboxEventPublisher outboxPublisher;

  public PlaceOrderUseCase(OrderRepository orderRepository, OutboxEventPublisher outboxPublisher) {
    this.orderRepository = orderRepository;
    this.outboxPublisher = outboxPublisher;
  }

  public Result<Order> execute(String customerId, List<OrderItem> items) {
    return Order.create(customerId, items)
        .map(order -> {
          Order saved = orderRepository.save(order);
          // Flush domain events → Outbox (same transaction)
          saved.flushEvents().forEach(event -> outboxPublisher.publish("orders", event));
          return saved;
        });
  }
}
