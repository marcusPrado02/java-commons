package com.marcusprado02.commons.kernel.ddd.specification;

/**
 * Abstract base class for specifications that provides named specifications.
 *
 * <p>Useful for creating reusable specifications with meaningful names and encapsulated logic.
 *
 * <p><strong>Usage:</strong>
 *
 * <pre>{@code
 * public class OrderSpecifications {
 *     public static Specification<Order> isPending() {
 *         return new AbstractSpecification<>() {
 *             @Override
 *             public boolean isSatisfiedBy(Order order) {
 *                 return order.status() == OrderStatus.PENDING;
 *             }
 *         };
 *     }
 *
 *     public static Specification<Order> totalGreaterThan(Money amount) {
 *         return new AbstractSpecification<>() {
 *             @Override
 *             public boolean isSatisfiedBy(Order order) {
 *                 return order.total().compareTo(amount) > 0;
 *             }
 *         };
 *     }
 * }
 * }</pre>
 *
 * @param <T> the type of object being specified
 */
public abstract class AbstractSpecification<T> implements Specification<T> {

  @Override
  public abstract boolean isSatisfiedBy(T candidate);
}
