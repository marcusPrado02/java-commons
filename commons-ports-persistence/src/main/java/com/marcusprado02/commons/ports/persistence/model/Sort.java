package com.marcusprado02.commons.ports.persistence.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Sorting criteria for query results. */
public final class Sort {

  private final List<Order> orders;

  private Sort(List<Order> orders) {
    this.orders = orders;
  }

  public List<Order> orders() {
    return orders;
  }

  /**
   * Creates a Sort from the given orders.
   *
   * @param orders sort orders
   * @return new Sort instance
   */
  public static Sort of(Order... orders) {
    return new Sort(Arrays.asList(orders));
  }

  /**
   * Combines this sort with another, appending the other's orders after this one's.
   *
   * @param other additional sort orders to append
   * @return a new {@link Sort} combining both
   */
  public Sort and(Sort other) {
    var combined = new ArrayList<>(this.orders);
    combined.addAll(other.orders());
    return new Sort(combined);
  }
}
