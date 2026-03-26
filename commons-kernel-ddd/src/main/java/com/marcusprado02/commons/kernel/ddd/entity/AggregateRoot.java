package com.marcusprado02.commons.kernel.ddd.entity;

import com.marcusprado02.commons.kernel.ddd.audit.AuditStamp;
import com.marcusprado02.commons.kernel.ddd.audit.DeletionStamp;
import com.marcusprado02.commons.kernel.ddd.event.DomainEvent;
import com.marcusprado02.commons.kernel.ddd.event.DomainEventRecorder;
import com.marcusprado02.commons.kernel.ddd.tenant.TenantId;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Base class for aggregate roots in a DDD context.
 *
 * <p>An Aggregate Root is an Entity that serves as the entry point to an aggregate, ensuring the
 * integrity of the aggregate's invariants and managing its lifecycle.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * public class Order extends AggregateRoot<OrderId> {
 *     public Order(OrderId id, TenantId tenantId, AuditStamp created) {
 *         super(id, tenantId, created);
 *     }
 * }
 * }</pre>
 *
 * @param <I> Type of the aggregate root identifier.
 * @see Entity
 * @see DomainEvent
 * @see AggregateSnapshot
 */
public abstract class AggregateRoot<I> extends Entity<I> implements DomainEventRecorder {

  private final List<DomainEvent> events = DomainEventRecorder.newBuffer();

  protected AggregateRoot(I id, TenantId tenantId, AuditStamp created) {
    super(id, tenantId, created);
  }

  /**
   * Common pattern to record a state change with an associated domain event.
   *
   * <p>Updates the entity state via the provided mutation, touches the entity to update
   * audit/version, and creates and registers a domain event using the provided factory.
   *
   * @param updated the audit stamp for the change
   * @param stateMutation the mutation to apply
   * @param eventFactory factory to produce the domain event from the resulting snapshot
   */
  protected final void recordChange(
      AuditStamp updated,
      Runnable stateMutation,
      Function<AggregateSnapshot<I>, DomainEvent> eventFactory) {
    Objects.requireNonNull(updated, "updated");
    Objects.requireNonNull(stateMutation, "stateMutation");
    Objects.requireNonNull(eventFactory, "eventFactory");

    stateMutation.run();
    touch(updated);

    DomainEvent event = Objects.requireNonNull(eventFactory.apply(snapshot()), "event");
    events.add(event);
  }

  /**
   * Soft delete with event recording.
   *
   * @param deleted the deletion stamp
   * @param updated the audit stamp for the update
   * @param eventFactory factory to produce the domain event from the resulting snapshot
   */
  protected final void recordSoftDelete(
      DeletionStamp deleted,
      AuditStamp updated,
      Function<AggregateSnapshot<I>, DomainEvent> eventFactory) {
    Objects.requireNonNull(deleted, "deleted");
    Objects.requireNonNull(updated, "updated");
    Objects.requireNonNull(eventFactory, "eventFactory");

    softDelete(deleted, updated);
    DomainEvent event = Objects.requireNonNull(eventFactory.apply(snapshot()), "event");
    events.add(event);
  }

  /**
   * Restore with event recording.
   *
   * @param updated the audit stamp for the restore
   * @param eventFactory factory to produce the domain event from the resulting snapshot
   */
  protected final void recordRestore(
      AuditStamp updated, Function<AggregateSnapshot<I>, DomainEvent> eventFactory) {
    Objects.requireNonNull(updated, "updated");
    Objects.requireNonNull(eventFactory, "eventFactory");

    restore(updated);
    DomainEvent event = Objects.requireNonNull(eventFactory.apply(snapshot()), "event");
    events.add(event);
  }

  /**
   * Pulls and clears all pending domain events.
   *
   * @return immutable copy of pending events; the buffer is cleared
   */
  public final List<DomainEvent> pullDomainEvents() {
    if (events.isEmpty()) {
      return List.of();
    }
    List<DomainEvent> copy = List.copyOf(events);
    events.clear();
    return copy;
  }

  /**
   * Returns a read-only view of pending domain events without clearing the buffer.
   *
   * @return unmodifiable view of pending events
   */
  public final List<DomainEvent> peekDomainEvents() {
    return Collections.unmodifiableList(events);
  }

  /**
   * Returns a snapshot of the current aggregate state.
   *
   * @return aggregate snapshot
   */
  protected final AggregateSnapshot<I> snapshot() {
    return new AggregateSnapshot<>(id(), getClass().getSimpleName(), tenantId(), version().value());
  }

  @Override
  public final List<DomainEvent> domainEvents() {
    return events;
  }
}
