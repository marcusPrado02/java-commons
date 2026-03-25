package com.example.orderservice.domain;

import java.util.List;
import java.util.Optional;

/** Port: persistence contract for the Order aggregate. */
public interface OrderRepository {
  Order save(Order order);
  Optional<Order> findById(OrderId id);
  List<Order> findByCustomerId(String customerId);
  List<Order> findByStatus(Order.Status status);
}
