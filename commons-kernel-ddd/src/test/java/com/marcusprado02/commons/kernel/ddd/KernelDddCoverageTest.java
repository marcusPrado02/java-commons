package com.marcusprado02.commons.kernel.ddd;

import static org.junit.jupiter.api.Assertions.*;

import com.marcusprado02.commons.kernel.ddd.audit.ActorId;
import com.marcusprado02.commons.kernel.ddd.audit.AuditStamp;
import com.marcusprado02.commons.kernel.ddd.audit.AuditTrail;
import com.marcusprado02.commons.kernel.ddd.audit.DeletionStamp;
import com.marcusprado02.commons.kernel.ddd.context.AuditFactory;
import com.marcusprado02.commons.kernel.ddd.context.EventMetadataFactory;
import com.marcusprado02.commons.kernel.ddd.context.FixedActorProvider;
import com.marcusprado02.commons.kernel.ddd.context.FixedCorrelationProvider;
import com.marcusprado02.commons.kernel.ddd.context.FixedTenantProvider;
import com.marcusprado02.commons.kernel.ddd.entity.AggregateRoot;
import com.marcusprado02.commons.kernel.ddd.entity.AggregateSnapshot;
import com.marcusprado02.commons.kernel.ddd.entity.EntityFactory;
import com.marcusprado02.commons.kernel.ddd.event.DomainEvent;
import com.marcusprado02.commons.kernel.ddd.event.EventId;
import com.marcusprado02.commons.kernel.ddd.event.EventMetadata;
import com.marcusprado02.commons.kernel.ddd.id.UuidIdentifier;
import com.marcusprado02.commons.kernel.ddd.invariant.Invariant;
import com.marcusprado02.commons.kernel.ddd.specification.AbstractSpecification;
import com.marcusprado02.commons.kernel.ddd.specification.Specification;
import com.marcusprado02.commons.kernel.ddd.tenant.TenantId;
import com.marcusprado02.commons.kernel.ddd.time.ClockProvider;
import com.marcusprado02.commons.kernel.ddd.version.EntityVersion;
import com.marcusprado02.commons.kernel.ddd.vo.CompositeValueObject;
import com.marcusprado02.commons.kernel.ddd.vo.SingleValueObject;
import com.marcusprado02.commons.kernel.errors.ErrorCategory;
import com.marcusprado02.commons.kernel.errors.ErrorCode;
import com.marcusprado02.commons.kernel.errors.Problem;
import com.marcusprado02.commons.kernel.errors.Severity;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class KernelDddCoverageTest {

  // ── ActorId ─────────────────────────────────────────────────────────────────

  @Test
  void actorId_of_returns_instance() {
    ActorId actor = ActorId.of("user-1");
    assertEquals("user-1", actor.value());
  }

  @Test
  void actorId_system_returns_system() {
    assertEquals("system", ActorId.system().value());
  }

  @Test
  void actorId_rejects_null() {
    assertThrows(NullPointerException.class, () -> ActorId.of(null));
  }

  @Test
  void actorId_rejects_blank() {
    assertThrows(IllegalArgumentException.class, () -> ActorId.of("  "));
  }

  // ── AuditStamp ───────────────────────────────────────────────────────────────

  @Test
  void auditStamp_of_stores_fields() {
    Instant now = Instant.now();
    ActorId actor = ActorId.of("a");
    AuditStamp stamp = AuditStamp.of(now, actor);
    assertEquals(now, stamp.at());
    assertEquals(actor, stamp.by());
  }

  @Test
  void auditStamp_rejects_null_at() {
    assertThrows(NullPointerException.class, () -> AuditStamp.of(null, ActorId.of("a")));
  }

  @Test
  void auditStamp_rejects_null_by() {
    assertThrows(NullPointerException.class, () -> AuditStamp.of(Instant.now(), null));
  }

  // ── AuditTrail ───────────────────────────────────────────────────────────────

  @Test
  void auditTrail_createdNow_sets_created_and_updated() {
    AuditStamp stamp = AuditStamp.of(Instant.now(), ActorId.of("a"));
    AuditTrail trail = AuditTrail.createdNow(stamp);
    assertEquals(stamp, trail.created());
    assertEquals(stamp, trail.updated());
  }

  @Test
  void auditTrail_touch_updates_stamp() {
    AuditStamp created = AuditStamp.of(Instant.parse("2026-01-01T00:00:00Z"), ActorId.of("a"));
    AuditTrail trail = AuditTrail.createdNow(created);
    AuditStamp updated = AuditStamp.of(Instant.parse("2026-06-01T00:00:00Z"), ActorId.of("b"));
    trail.touch(updated);
    assertEquals(updated, trail.updated());
    assertEquals(created, trail.created());
  }

  @Test
  void auditTrail_touch_rejects_null() {
    AuditTrail trail = AuditTrail.createdNow(AuditStamp.of(Instant.now(), ActorId.of("a")));
    assertThrows(NullPointerException.class, () -> trail.touch(null));
  }

  // ── DeletionStamp ────────────────────────────────────────────────────────────

  @Test
  void deletionStamp_of_stores_fields() {
    Instant now = Instant.now();
    ActorId actor = ActorId.of("x");
    DeletionStamp stamp = DeletionStamp.of(now, actor);
    assertEquals(now, stamp.at());
    assertEquals(actor, stamp.by());
  }

  @Test
  void deletionStamp_rejects_null_at() {
    assertThrows(NullPointerException.class, () -> DeletionStamp.of(null, ActorId.of("x")));
  }

  @Test
  void deletionStamp_rejects_null_by() {
    assertThrows(NullPointerException.class, () -> DeletionStamp.of(Instant.now(), null));
  }

  // ── FixedActorProvider ───────────────────────────────────────────────────────

  @Test
  void fixedActorProvider_returns_actor() {
    ActorId actor = ActorId.of("u");
    assertEquals(actor, new FixedActorProvider(actor).currentActor());
  }

  @Test
  void fixedActorProvider_rejects_null() {
    assertThrows(NullPointerException.class, () -> new FixedActorProvider(null));
  }

  // ── FixedCorrelationProvider ─────────────────────────────────────────────────

  @Test
  void fixedCorrelationProvider_single_arg() {
    FixedCorrelationProvider p = new FixedCorrelationProvider("corr-1");
    assertEquals("corr-1", p.correlationId());
    assertNull(p.causationId());
  }

  @Test
  void fixedCorrelationProvider_two_args() {
    FixedCorrelationProvider p = new FixedCorrelationProvider("corr-1", "cause-1");
    assertEquals("corr-1", p.correlationId());
    assertEquals("cause-1", p.causationId());
  }

  @Test
  void fixedCorrelationProvider_rejects_null_correlationId() {
    assertThrows(NullPointerException.class, () -> new FixedCorrelationProvider(null));
  }

  @Test
  void fixedCorrelationProvider_rejects_blank_correlationId() {
    assertThrows(IllegalArgumentException.class, () -> new FixedCorrelationProvider("  "));
  }

  // ── FixedTenantProvider ──────────────────────────────────────────────────────

  @Test
  void fixedTenantProvider_returns_tenant() {
    TenantId tenant = TenantId.of("t1");
    assertEquals(tenant, new FixedTenantProvider(tenant).currentTenant());
  }

  @Test
  void fixedTenantProvider_rejects_null() {
    assertThrows(NullPointerException.class, () -> new FixedTenantProvider(null));
  }

  // ── EventMetadataFactory ─────────────────────────────────────────────────────

  @Test
  void eventMetadataFactory_base_uses_providers() {
    TenantId tenant = TenantId.of("t");
    FixedTenantProvider tp = new FixedTenantProvider(tenant);
    FixedCorrelationProvider cp = new FixedCorrelationProvider("c-1", "ca-1");
    EventMetadataFactory factory = new EventMetadataFactory(tp, cp);
    EventMetadata meta = factory.base();
    assertEquals("c-1", meta.correlationId());
    assertEquals("ca-1", meta.causationId());
    assertEquals(tenant, meta.tenantId());
    assertTrue(meta.attributes().isEmpty());
  }

  @Test
  void eventMetadataFactory_withAttributes_stores_attributes() {
    FixedTenantProvider tp = new FixedTenantProvider(TenantId.of("t"));
    FixedCorrelationProvider cp = new FixedCorrelationProvider("c");
    EventMetadataFactory factory = new EventMetadataFactory(tp, cp);
    EventMetadata meta = factory.withAttributes(Map.of("k", "v"));
    assertEquals("v", meta.attributes().get("k"));
  }

  @Test
  void eventMetadataFactory_withAttributes_rejects_null() {
    FixedTenantProvider tp = new FixedTenantProvider(TenantId.of("t"));
    FixedCorrelationProvider cp = new FixedCorrelationProvider("c");
    EventMetadataFactory factory = new EventMetadataFactory(tp, cp);
    assertThrows(NullPointerException.class, () -> factory.withAttributes(null));
  }

  // ── EventId ──────────────────────────────────────────────────────────────────

  @Test
  void eventId_newId_generates_unique_ids() {
    EventId a = EventId.newId();
    EventId b = EventId.newId();
    assertNotNull(a.value());
    assertNotEquals(a, b);
  }

  @Test
  void eventId_rejects_null() {
    assertThrows(NullPointerException.class, () -> new EventId(null));
  }

  // ── EventMetadata ────────────────────────────────────────────────────────────

  @Test
  void eventMetadata_empty_has_no_data() {
    EventMetadata m = EventMetadata.empty();
    assertNull(m.correlationId());
    assertNull(m.causationId());
    assertNull(m.tenantId());
    assertTrue(m.attributes().isEmpty());
  }

  @Test
  void eventMetadata_withTenant_sets_tenant() {
    TenantId t = TenantId.of("x");
    EventMetadata m = EventMetadata.withTenant(t);
    assertEquals(t, m.tenantId());
  }

  @Test
  void eventMetadata_null_attributes_become_empty_map() {
    EventMetadata m = new EventMetadata(null, null, null, null);
    assertNotNull(m.attributes());
    assertTrue(m.attributes().isEmpty());
  }

  // ── TenantId ─────────────────────────────────────────────────────────────────

  @Test
  void tenantId_of_stores_value() {
    assertEquals("tenant-x", TenantId.of("tenant-x").value());
  }

  @Test
  void tenantId_rejects_null() {
    assertThrows(NullPointerException.class, () -> TenantId.of(null));
  }

  @Test
  void tenantId_rejects_blank() {
    assertThrows(IllegalArgumentException.class, () -> TenantId.of(""));
  }

  // ── EntityVersion ────────────────────────────────────────────────────────────

  @Test
  void entityVersion_initial_is_zero() {
    assertEquals(0L, EntityVersion.initial().value());
  }

  @Test
  void entityVersion_next_increments() {
    assertEquals(1L, EntityVersion.initial().next().value());
    assertEquals(3L, EntityVersion.initial().next().next().next().value());
  }

  @Test
  void entityVersion_rejects_negative() {
    assertThrows(IllegalArgumentException.class, () -> new EntityVersion(-1));
  }

  // ── UuidIdentifier ───────────────────────────────────────────────────────────

  static final class TestId extends UuidIdentifier {
    TestId(UUID value) {
      super(value);
    }

    TestId(String value) {
      super(value);
    }
  }

  @Test
  void uuidIdentifier_uuid_constructor() {
    UUID uuid = UUID.randomUUID();
    TestId id = new TestId(uuid);
    assertEquals(uuid, id.value());
    assertEquals(uuid.toString(), id.asString());
  }

  @Test
  void uuidIdentifier_string_constructor() {
    UUID uuid = UUID.randomUUID();
    TestId id = new TestId(uuid.toString());
    assertEquals(uuid, id.value());
  }

  @Test
  void uuidIdentifier_equals_same_instance() {
    UUID uuid = UUID.randomUUID();
    TestId id = new TestId(uuid);
    assertEquals(id, id);
  }

  @Test
  void uuidIdentifier_equals_same_value() {
    UUID uuid = UUID.randomUUID();
    assertEquals(new TestId(uuid), new TestId(uuid));
  }

  @Test
  void uuidIdentifier_not_equals_different_value() {
    assertNotEquals(new TestId(UUID.randomUUID()), new TestId(UUID.randomUUID()));
  }

  @Test
  void uuidIdentifier_not_equals_null() {
    assertNotEquals(new TestId(UUID.randomUUID()), null);
  }

  @Test
  void uuidIdentifier_hashcode_consistent() {
    UUID uuid = UUID.randomUUID();
    assertEquals(new TestId(uuid).hashCode(), new TestId(uuid).hashCode());
  }

  @Test
  void uuidIdentifier_rejects_null_uuid() {
    assertThrows(NullPointerException.class, () -> new TestId((UUID) null));
  }

  // ── AggregateSnapshot ────────────────────────────────────────────────────────

  @Test
  void aggregateSnapshot_stores_fields() {
    TenantId t = TenantId.of("t");
    AggregateSnapshot<String> snap = new AggregateSnapshot<>("id", "Order", t, 2L);
    assertEquals("id", snap.id());
    assertEquals("Order", snap.type());
    assertEquals(t, snap.tenantId());
    assertEquals(2L, snap.version());
  }

  // ── Entity / AggregateRoot via TestAggregate ─────────────────────────────────

  static final class OrderId extends UuidIdentifier {
    OrderId(UUID v) {
      super(v);
    }

    static OrderId generate() {
      return new OrderId(UUID.randomUUID());
    }
  }

  static final class TestEvent implements DomainEvent {
    private final AggregateSnapshot<OrderId> snap;

    TestEvent(AggregateSnapshot<OrderId> snap) {
      this.snap = snap;
    }

    @Override
    public EventId eventId() {
      return EventId.newId();
    }

    @Override
    public Instant occurredAt() {
      return Instant.now();
    }

    @Override
    public String aggregateType() {
      return snap.type();
    }

    @Override
    public String aggregateId() {
      return snap.id().asString();
    }

    @Override
    public long aggregateVersion() {
      return snap.version();
    }

    @Override
    public EventMetadata metadata() {
      return EventMetadata.empty();
    }
  }

  static final class TestAggregate extends AggregateRoot<OrderId> {
    TestAggregate(OrderId id, TenantId tenantId, AuditStamp created) {
      super(id, tenantId, created);
    }

    void change(AuditStamp stamp) {
      recordChange(stamp, () -> {}, snap -> new TestEvent(snap));
    }

    void delete(DeletionStamp deletion, AuditStamp stamp) {
      recordSoftDelete(deletion, stamp, snap -> new TestEvent(snap));
    }

    void undelete(AuditStamp stamp) {
      recordRestore(stamp, snap -> new TestEvent(snap));
    }
  }

  private AuditStamp stamp() {
    return AuditStamp.of(Instant.now(), ActorId.of("u"));
  }

  private DeletionStamp deletionStamp() {
    return DeletionStamp.of(Instant.now(), ActorId.of("u"));
  }

  @Test
  void entity_initial_state() {
    TenantId tenant = TenantId.of("t");
    OrderId id = OrderId.generate();
    AuditStamp created = stamp();
    TestAggregate agg = new TestAggregate(id, tenant, created);

    assertEquals(id, agg.id());
    assertEquals(tenant, agg.tenantId());
    assertEquals(0L, agg.version().value());
    assertFalse(agg.isDeleted());
    assertTrue(agg.deletion().isEmpty());
    assertTrue(agg.peekDomainEvents().isEmpty());
  }

  @Test
  void entity_touch_increments_version() {
    TestAggregate agg = new TestAggregate(OrderId.generate(), TenantId.of("t"), stamp());
    agg.change(stamp());
    assertEquals(1L, agg.version().value());
  }

  @Test
  void entity_softDelete_marks_deleted_and_is_idempotent() {
    TestAggregate agg = new TestAggregate(OrderId.generate(), TenantId.of("t"), stamp());
    agg.delete(deletionStamp(), stamp());
    assertTrue(agg.isDeleted());
    assertTrue(agg.deletion().isPresent());
    long versionAfterFirst = agg.version().value();
    // idempotent — second soft delete should be no-op
    agg.delete(deletionStamp(), stamp());
    assertEquals(versionAfterFirst, agg.version().value());
  }

  @Test
  void entity_restore_undeletes_and_is_idempotent() {
    TestAggregate agg = new TestAggregate(OrderId.generate(), TenantId.of("t"), stamp());
    agg.delete(deletionStamp(), stamp());
    long versionAfterDelete = agg.version().value();
    agg.undelete(stamp());
    assertFalse(agg.isDeleted());
    assertEquals(versionAfterDelete + 1, agg.version().value());
    // idempotent — restore on non-deleted is no-op
    long versionAfterRestore = agg.version().value();
    agg.undelete(stamp());
    assertEquals(versionAfterRestore, agg.version().value());
  }

  @Test
  void aggregateRoot_recordChange_produces_event() {
    TestAggregate agg = new TestAggregate(OrderId.generate(), TenantId.of("t"), stamp());
    agg.change(stamp());
    List<DomainEvent> events = agg.pullDomainEvents();
    assertEquals(1, events.size());
    assertTrue(agg.pullDomainEvents().isEmpty());
  }

  @Test
  void aggregateRoot_recordSoftDelete_produces_event() {
    TestAggregate agg = new TestAggregate(OrderId.generate(), TenantId.of("t"), stamp());
    agg.delete(deletionStamp(), stamp());
    assertEquals(1, agg.peekDomainEvents().size());
    assertEquals(1, agg.pullDomainEvents().size());
    assertTrue(agg.pullDomainEvents().isEmpty());
  }

  @Test
  void aggregateRoot_recordRestore_produces_event() {
    TestAggregate agg = new TestAggregate(OrderId.generate(), TenantId.of("t"), stamp());
    agg.delete(deletionStamp(), stamp());
    agg.pullDomainEvents(); // clear
    agg.undelete(stamp());
    assertEquals(1, agg.pullDomainEvents().size());
  }

  @Test
  void aggregateRoot_domainEvents_returns_live_list() {
    TestAggregate agg = new TestAggregate(OrderId.generate(), TenantId.of("t"), stamp());
    agg.change(stamp());
    assertEquals(1, agg.domainEvents().size());
  }

  @Test
  void entity_equals_by_id() {
    OrderId id = OrderId.generate();
    TenantId t = TenantId.of("t");
    TestAggregate a = new TestAggregate(id, t, stamp());
    TestAggregate b = new TestAggregate(id, t, stamp());
    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
  }

  @Test
  void entity_not_equals_different_id() {
    TenantId t = TenantId.of("t");
    TestAggregate a = new TestAggregate(OrderId.generate(), t, stamp());
    TestAggregate b = new TestAggregate(OrderId.generate(), t, stamp());
    assertNotEquals(a, b);
  }

  @Test
  void entity_not_equals_null_and_other_type() {
    TestAggregate a = new TestAggregate(OrderId.generate(), TenantId.of("t"), stamp());
    assertNotEquals(a, null);
    assertNotEquals(a, "string");
  }

  // ── CompositeValueObject ─────────────────────────────────────────────────────

  static final class Address extends CompositeValueObject {
    private final String street;
    private final String city;

    Address(String street, String city) {
      this.street = street;
      this.city = city;
    }
  }

  @Test
  void compositeVO_equals_same_instance() {
    Address a = new Address("Main St", "Springfield");
    assertEquals(a, a);
  }

  @Test
  void compositeVO_equals_same_fields() {
    assertEquals(new Address("Main St", "Springfield"), new Address("Main St", "Springfield"));
  }

  @Test
  void compositeVO_not_equals_different_fields() {
    assertNotEquals(new Address("Main St", "Springfield"), new Address("Elm St", "Springfield"));
  }

  @Test
  void compositeVO_not_equals_null() {
    assertNotEquals(new Address("x", "y"), null);
  }

  @Test
  void compositeVO_not_equals_other_type() {
    assertNotEquals(new Address("x", "y"), "string");
  }

  @Test
  void compositeVO_hashcode_consistent() {
    assertEquals(
        new Address("Main St", "Springfield").hashCode(),
        new Address("Main St", "Springfield").hashCode());
  }

  // ── Invariant ────────────────────────────────────────────────────────────────

  @Test
  void invariant_check_passes_when_true() {
    assertDoesNotThrow(
        () ->
            Invariant.check(
                true,
                Problem.of(ErrorCode.of("X"), ErrorCategory.VALIDATION, Severity.ERROR, "msg")));
  }

  @Test
  void invariant_check_throws_when_false() {
    assertThrows(
        Exception.class,
        () ->
            Invariant.check(
                false,
                Problem.of(ErrorCode.of("X"), ErrorCategory.VALIDATION, Severity.ERROR, "msg")));
  }

  // ── ClockProvider ────────────────────────────────────────────────────────────

  @Test
  void clockProvider_system_returns_non_null() {
    assertNotNull(ClockProvider.system().now());
  }

  @Test
  void clockProvider_fixed_returns_given_instant() {
    Instant fixed = Instant.parse("2026-01-01T00:00:00Z");
    assertEquals(fixed, ClockProvider.fixed(fixed).now());
  }

  // ── SingleValueObject ────────────────────────────────────────────────────────

  static final class Email extends SingleValueObject<String> {
    Email(String value) {
      super(value);
    }
  }

  @Test
  void singleVO_value_returns_wrapped() {
    assertEquals("a@b.com", new Email("a@b.com").value());
  }

  @Test
  void singleVO_equals_same_instance() {
    Email e = new Email("a@b.com");
    assertEquals(e, e);
  }

  @Test
  void singleVO_equals_same_value() {
    assertEquals(new Email("a@b.com"), new Email("a@b.com"));
  }

  @Test
  void singleVO_not_equals_different_value() {
    assertNotEquals(new Email("a@b.com"), new Email("x@y.com"));
  }

  @Test
  void singleVO_not_equals_null() {
    assertNotEquals(new Email("a@b.com"), null);
  }

  @Test
  void singleVO_not_equals_other_type() {
    assertNotEquals(new Email("a@b.com"), "a@b.com");
  }

  @Test
  void singleVO_hashcode_consistent() {
    assertEquals(new Email("a@b.com").hashCode(), new Email("a@b.com").hashCode());
  }

  @Test
  void singleVO_toString_includes_value() {
    assertTrue(new Email("a@b.com").toString().contains("a@b.com"));
  }

  @Test
  void singleVO_rejects_null() {
    assertThrows(NullPointerException.class, () -> new Email(null));
  }

  // ── AuditFactory ─────────────────────────────────────────────────────────────

  @Test
  void auditFactory_created_returns_stamp_with_clock_and_actor() {
    Instant fixed = Instant.parse("2026-06-01T00:00:00Z");
    ActorId actor = ActorId.of("sys");
    AuditFactory factory =
        new AuditFactory(ClockProvider.fixed(fixed), new FixedActorProvider(actor));
    AuditStamp stamp = factory.created();
    assertEquals(fixed, stamp.at());
    assertEquals(actor, stamp.by());
  }

  @Test
  void auditFactory_updated_returns_stamp() {
    Instant fixed = Instant.parse("2026-06-01T00:00:00Z");
    ActorId actor = ActorId.of("sys");
    AuditFactory factory =
        new AuditFactory(ClockProvider.fixed(fixed), new FixedActorProvider(actor));
    assertEquals(fixed, factory.updated().at());
  }

  @Test
  void auditFactory_deleted_returns_deletion_stamp() {
    Instant fixed = Instant.parse("2026-06-01T00:00:00Z");
    ActorId actor = ActorId.of("sys");
    AuditFactory factory =
        new AuditFactory(ClockProvider.fixed(fixed), new FixedActorProvider(actor));
    DeletionStamp ds = factory.deleted();
    assertEquals(fixed, ds.at());
    assertEquals(actor, ds.by());
  }

  @Test
  void auditFactory_rejects_null_clock() {
    assertThrows(
        NullPointerException.class,
        () -> new AuditFactory(null, new FixedActorProvider(ActorId.of("u"))));
  }

  @Test
  void auditFactory_rejects_null_actor() {
    assertThrows(NullPointerException.class, () -> new AuditFactory(ClockProvider.system(), null));
  }

  // ── EntityFactory ─────────────────────────────────────────────────────────────

  @Test
  void entityFactory_instance_create_uses_providers() {
    TenantId tenant = TenantId.of("t");
    ActorId actor = ActorId.of("u");
    EntityFactory factory =
        new EntityFactory(new FixedTenantProvider(tenant), new FixedActorProvider(actor));
    TestAggregate agg =
        factory.create((tid, stamp) -> new TestAggregate(OrderId.generate(), tid, stamp));
    assertEquals(tenant, agg.tenantId());
  }

  @Test
  void entityFactory_static_create_uses_explicit_values() {
    TenantId tenant = TenantId.of("t");
    ActorId actor = ActorId.of("u");
    Instant now = Instant.now();
    TestAggregate agg =
        EntityFactory.create(
            tenant, actor, now, (tid, stamp) -> new TestAggregate(OrderId.generate(), tid, stamp));
    assertEquals(tenant, agg.tenantId());
  }

  // ── AbstractSpecification ─────────────────────────────────────────────────────

  @Test
  void abstractSpecification_isSatisfiedBy_delegates_to_impl() {
    AbstractSpecification<Integer> positive =
        new AbstractSpecification<Integer>() {
          @Override
          public boolean isSatisfiedBy(Integer n) {
            return n > 0;
          }
        };
    assertTrue(positive.isSatisfiedBy(1));
    assertFalse(positive.isSatisfiedBy(-1));
  }

  // ── Specification ─────────────────────────────────────────────────────────────

  @Test
  void specification_test_delegates_to_isSatisfiedBy() {
    Specification<String> nonEmpty = s -> !s.isEmpty();
    assertTrue(nonEmpty.test("hello"));
    assertFalse(nonEmpty.test(""));
  }

  @Test
  void specification_and_both_must_be_true() {
    Specification<Integer> positive = n -> n > 0;
    Specification<Integer> even = n -> n % 2 == 0;
    assertTrue(positive.and(even).isSatisfiedBy(2));
    assertFalse(positive.and(even).isSatisfiedBy(3));
    assertFalse(positive.and(even).isSatisfiedBy(-2));
  }

  @Test
  void specification_or_either_suffices() {
    Specification<Integer> positive = n -> n > 0;
    Specification<Integer> even = n -> n % 2 == 0;
    assertTrue(positive.or(even).isSatisfiedBy(1));
    assertTrue(positive.or(even).isSatisfiedBy(-2));
    assertFalse(positive.or(even).isSatisfiedBy(-1));
  }

  @Test
  void specification_not_negates() {
    Specification<Integer> positive = n -> n > 0;
    assertFalse(positive.not().isSatisfiedBy(1));
    assertTrue(positive.not().isSatisfiedBy(-1));
  }

  @Test
  void specification_alwaysTrue_always_satisfied() {
    assertTrue(Specification.<String>alwaysTrue().isSatisfiedBy("x"));
  }

  @Test
  void specification_alwaysFalse_never_satisfied() {
    assertFalse(Specification.<String>alwaysFalse().isSatisfiedBy("x"));
  }

  @Test
  void specification_of_wraps_predicate() {
    Specification<String> spec = Specification.of(String::isEmpty);
    assertTrue(spec.isSatisfiedBy(""));
    assertFalse(spec.isSatisfiedBy("x"));
  }

  // ── DomainEventRecorder (default recordEvent) ─────────────────────────────────

  @Test
  void domainEventRecorder_recordEvent_adds_event() {
    TestAggregate agg = new TestAggregate(OrderId.generate(), TenantId.of("t"), stamp());
    AggregateSnapshot<OrderId> snap =
        new AggregateSnapshot<>(agg.id(), "TestAggregate", TenantId.of("t"), 0L);
    agg.recordEvent(new TestEvent(snap));
    assertEquals(1, agg.peekDomainEvents().size());
  }
}
