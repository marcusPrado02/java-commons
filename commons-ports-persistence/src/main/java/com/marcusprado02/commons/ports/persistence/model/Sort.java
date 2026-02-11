package com.marcusprado02.commons.ports.persistence.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class Sort {

  private final List<Order> orders;

  private Sort(List<Order> orders) {
    this.orders = orders;
  }

  public List<Order> orders() {
    return orders;
  }

  public static Sort of(Order... orders) {
    return new Sort(Arrays.asList(orders));
  }

  public Sort and(Sort other) {
    var combined = new ArrayList<>(this.orders);
    combined.addAll(other.orders());
    return new Sort(combined);
  }
}
