package com.example.orderservice.adapters.persistence;

import com.example.orderservice.domain.Order;
import com.example.orderservice.domain.OrderId;
import com.example.orderservice.domain.OrderRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

/**
 * In-memory OrderRepository.
 *
 * <p>Suitable for development and testing. In production, replace with a JPA
 * adapter backed by PostgreSQL and use {@code commons-adapters-persistence-jpa}.
 */
@Repository
public class InMemoryOrderRepository implements OrderRepository {

  private final Map<OrderId, Order> store = new ConcurrentHashMap<>();

  @Override
  public Order save(Order order) {
    store.put(order.id(), order);
    return order;
  }

  @Override
  public Optional<Order> findById(OrderId id) {
    return Optional.ofNullable(store.get(id));
  }

  @Override
  public List<Order> findByCustomerId(String customerId) {
    return store.values().stream()
        .filter(o -> o.customerId().equals(customerId))
        .toList();
  }

  @Override
  public List<Order> findByStatus(Order.Status status) {
    return store.values().stream()
        .filter(o -> o.status() == status)
        .toList();
  }
}
