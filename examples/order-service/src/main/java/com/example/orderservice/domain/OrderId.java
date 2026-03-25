package com.example.orderservice.domain;

import java.util.Objects;
import java.util.UUID;

/** Value object representing the identity of an Order. */
public record OrderId(UUID value) {

  public OrderId {
    Objects.requireNonNull(value, "OrderId value must not be null");
  }

  public static OrderId generate() {
    return new OrderId(UUID.randomUUID());
  }

  public static OrderId of(String value) {
    return new OrderId(UUID.fromString(value));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
