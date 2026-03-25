package com.example.orderservice.domain;

import java.math.BigDecimal;
import java.util.Objects;

/** Value object representing one line item in an Order. */
public record OrderItem(String productId, int quantity, BigDecimal unitPrice) {

  public OrderItem {
    Objects.requireNonNull(productId, "productId");
    Objects.requireNonNull(unitPrice, "unitPrice");
    if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive");
    if (unitPrice.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("Unit price must be >= 0");
  }

  public BigDecimal subtotal() {
    return unitPrice.multiply(BigDecimal.valueOf(quantity));
  }
}
