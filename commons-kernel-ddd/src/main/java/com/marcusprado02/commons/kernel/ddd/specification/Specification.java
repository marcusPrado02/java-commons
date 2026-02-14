package com.marcusprado02.commons.kernel.ddd.specification;

import java.util.function.Predicate;

/**
 * Specification pattern for defining reusable, composable business rules.
 *
 * <p>A Specification represents a boolean predicate that can be applied to domain objects. It
 * allows you to:
 *
 * <ul>
 *   <li>Express business rules in a type-safe, composable way
 *   <li>Combine specifications using AND, OR, NOT operators
 *   <li>Reuse validation logic across the application
 *   <li>Implement complex queries in repositories
 * </ul>
 *
 * <p><strong>Usage:</strong>
 *
 * <pre>{@code
 * // Define specifications
 * Specification<Order> isPending = order -> order.status() == OrderStatus.PENDING;
 * Specification<Order> isExpensive = order -> order.total().compareTo(Money.of(1000)) > 0;
 *
 * // Combine them
 * Specification<Order> expensivePendingOrders = isPending.and(isExpensive);
 *
 * // Use in repository
 * List<Order> orders = orderRepository.findMatching(expensivePendingOrders);
 * }</pre>
 *
 * <p><strong>Class-based specifications:</strong>
 *
 * <pre>{@code
 * public class CustomerHasActiveOrders implements Specification<Customer> {
 *     @Override
 *     public boolean isSatisfiedBy(Customer customer) {
 *         return !customer.orders().stream()
 *             .filter(Order::isActive)
 *             .toList()
 *             .isEmpty();
 *     }
 * }
 * }</pre>
 *
 * @param <T> the type of object being specified
 */
@FunctionalInterface
public interface Specification<T> extends Predicate<T> {

  /**
   * Checks if the given candidate satisfies this specification.
   *
   * @param candidate the object to test
   * @return true if the candidate satisfies the specification
   */
  boolean isSatisfiedBy(T candidate);

  /**
   * Predicate test method delegates to isSatisfiedBy.
   *
   * @param candidate the object to test
   * @return true if the candidate satisfies the specification
   */
  @Override
  default boolean test(T candidate) {
    return isSatisfiedBy(candidate);
  }

  /**
   * Creates a specification that is the logical AND of this specification and another.
   *
   * @param other the other specification
   * @return a new specification that is satisfied only if both specifications are satisfied
   */
  default Specification<T> and(Specification<T> other) {
    return candidate -> this.isSatisfiedBy(candidate) && other.isSatisfiedBy(candidate);
  }

  /**
   * Creates a specification that is the logical OR of this specification and another.
   *
   * @param other the other specification
   * @return a new specification that is satisfied if either specification is satisfied
   */
  default Specification<T> or(Specification<T> other) {
    return candidate -> this.isSatisfiedBy(candidate) || other.isSatisfiedBy(candidate);
  }

  /**
   * Creates a specification that is the logical NOT of this specification.
   *
   * @return a new specification that is satisfied only if this specification is not satisfied
   */
  default Specification<T> not() {
    return candidate -> !this.isSatisfiedBy(candidate);
  }

  /**
   * Creates a specification that is always satisfied.
   *
   * @param <T> the type of object
   * @return a specification that always returns true
   */
  static <T> Specification<T> alwaysTrue() {
    return candidate -> true;
  }

  /**
   * Creates a specification that is never satisfied.
   *
   * @param <T> the type of object
   * @return a specification that always returns false
   */
  static <T> Specification<T> alwaysFalse() {
    return candidate -> false;
  }

  /**
   * Creates a specification from a predicate.
   *
   * @param predicate the predicate
   * @param <T> the type of object
   * @return a specification wrapping the predicate
   */
  static <T> Specification<T> of(Predicate<T> predicate) {
    return predicate::test;
  }
}
