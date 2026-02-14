package com.marcusprado02.commons.kernel.ddd.specification;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SpecificationTest {

  @Test
  void shouldEvaluateSimpleSpecification() {
    Specification<Order> isPending = order -> order.status == Status.PENDING;

    assertTrue(isPending.isSatisfiedBy(new Order(Status.PENDING, 100)));
    assertFalse(isPending.isSatisfiedBy(new Order(Status.COMPLETED, 100)));
  }

  @Test
  void shouldCombineWithAnd() {
    Specification<Order> isPending = order -> order.status == Status.PENDING;
    Specification<Order> isExpensive = order -> order.amount > 1000;

    Specification<Order> combined = isPending.and(isExpensive);

    assertTrue(combined.isSatisfiedBy(new Order(Status.PENDING, 2000)));
    assertFalse(combined.isSatisfiedBy(new Order(Status.PENDING, 500)));
    assertFalse(combined.isSatisfiedBy(new Order(Status.COMPLETED, 2000)));
  }

  @Test
  void shouldCombineWithOr() {
    Specification<Order> isPending = order -> order.status == Status.PENDING;
    Specification<Order> isExpensive = order -> order.amount > 1000;

    Specification<Order> combined = isPending.or(isExpensive);

    assertTrue(combined.isSatisfiedBy(new Order(Status.PENDING, 500)));
    assertTrue(combined.isSatisfiedBy(new Order(Status.COMPLETED, 2000)));
    assertFalse(combined.isSatisfiedBy(new Order(Status.COMPLETED, 500)));
  }

  @Test
  void shouldNegate() {
    Specification<Order> isPending = order -> order.status == Status.PENDING;
    Specification<Order> notPending = isPending.not();

    assertFalse(notPending.isSatisfiedBy(new Order(Status.PENDING, 100)));
    assertTrue(notPending.isSatisfiedBy(new Order(Status.COMPLETED, 100)));
  }

  @Test
  void shouldWorkWithAlwaysTrueAndFalse() {
    Specification<Order> alwaysTrue = Specification.alwaysTrue();
    Specification<Order> alwaysFalse = Specification.alwaysFalse();

    assertTrue(alwaysTrue.isSatisfiedBy(new Order(Status.PENDING, 100)));
    assertFalse(alwaysFalse.isSatisfiedBy(new Order(Status.PENDING, 100)));
  }

  @Test
  void shouldCreateFromPredicate() {
    Specification<Order> spec = Specification.of(order -> order.amount > 500);

    assertTrue(spec.isSatisfiedBy(new Order(Status.PENDING, 1000)));
    assertFalse(spec.isSatisfiedBy(new Order(Status.PENDING, 100)));
  }

  @Test
  void shouldWorkAsPredicateInStreams() {
    var orders =
        java.util.List.of(
            new Order(Status.PENDING, 100),
            new Order(Status.PENDING, 2000),
            new Order(Status.COMPLETED, 500));

    Specification<Order> isPendingAndExpensive =
        ((Specification<Order>) order -> order.status == Status.PENDING)
            .and(order -> order.amount > 1000);

    var filtered = orders.stream().filter(isPendingAndExpensive).toList();

    assertEquals(1, filtered.size());
    assertEquals(2000, filtered.get(0).amount);
  }

  // Test data
  record Order(Status status, int amount) {}

  enum Status {
    PENDING,
    COMPLETED
  }
}
