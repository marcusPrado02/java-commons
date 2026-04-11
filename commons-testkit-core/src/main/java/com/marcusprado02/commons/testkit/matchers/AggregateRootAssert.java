package com.marcusprado02.commons.testkit.matchers;

import com.marcusprado02.commons.kernel.ddd.entity.AggregateRoot;
import com.marcusprado02.commons.kernel.ddd.event.DomainEvent;
import java.util.List;
import org.assertj.core.api.AbstractAssert;

/**
 * AssertJ custom assertion for AggregateRoot domain events.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * Order order = new Order(...);
 * order.confirmOrder();
 *
 * assertThat(order)
 *     .hasDomainEvents(1)
 *     .hasDomainEventOfType(OrderConfirmedEvent.class);
 * }</pre>
 */
public class AggregateRootAssert<T extends AggregateRoot<?>>
    extends AbstractAssert<AggregateRootAssert<T>, T> {

  protected AggregateRootAssert(T actual) {
    super(actual, AggregateRootAssert.class);
  }

  public static <T extends AggregateRoot<?>> AggregateRootAssert<T> assertThat(T actual) {
    return new AggregateRootAssert<>(actual);
  }

  /**
   * Asserts that the aggregate root has exactly the given number of domain events.
   *
   * @param count expected number of domain events
   * @return this assertion for chaining
   */
  public AggregateRootAssert<T> hasDomainEvents(int count) {
    isNotNull();
    List<DomainEvent> events = actual.pullDomainEvents();
    if (events.size() != count) {
      failWithMessage("Expected <%d> domain events but found <%d>", count, events.size());
    }
    return this;
  }

  /**
   * Asserts that the aggregate root has no domain events.
   *
   * @return this assertion for chaining
   */
  public AggregateRootAssert<T> hasNoDomainEvents() {
    return hasDomainEvents(0);
  }

  /**
   * Asserts that the aggregate root has a domain event of the specified type.
   *
   * @param eventType the expected domain event class
   * @return this assertion for chaining
   */
  public AggregateRootAssert<T> hasDomainEventOfType(Class<? extends DomainEvent> eventType) {
    isNotNull();
    List<DomainEvent> events = actual.pullDomainEvents();
    boolean found = events.stream().anyMatch(e -> eventType.isInstance(e));
    if (!found) {
      failWithMessage(
          "Expected to find domain event of type <%s> but found: %s",
          eventType.getSimpleName(), events);
    }
    return this;
  }

  /**
   * Asserts that no domain event of the given type was raised.
   *
   * @param eventType expected absent event type
   * @return this assertion
   */
  public AggregateRootAssert<T> doesNotHaveDomainEventOfType(
      Class<? extends DomainEvent> eventType) {
    isNotNull();
    List<DomainEvent> events = actual.pullDomainEvents();
    boolean found = events.stream().anyMatch(e -> eventType.isInstance(e));
    if (found) {
      failWithMessage(
          "Expected not to find domain event of type <%s> but found it in: %s",
          eventType.getSimpleName(), events);
    }
    return this;
  }
}
