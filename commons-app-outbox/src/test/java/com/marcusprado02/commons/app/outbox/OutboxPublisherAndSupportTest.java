package com.marcusprado02.commons.app.outbox;

import static org.junit.jupiter.api.Assertions.*;

import com.marcusprado02.commons.app.outbox.model.OutboxMessage;
import com.marcusprado02.commons.app.outbox.model.OutboxMessageId;
import com.marcusprado02.commons.app.outbox.model.OutboxStatus;
import com.marcusprado02.commons.app.outbox.port.OutboxRepositoryPort;
import com.marcusprado02.commons.kernel.ddd.audit.ActorId;
import com.marcusprado02.commons.kernel.ddd.audit.AuditStamp;
import com.marcusprado02.commons.kernel.ddd.entity.AggregateRoot;
import com.marcusprado02.commons.kernel.ddd.event.DomainEvent;
import com.marcusprado02.commons.kernel.ddd.event.EventId;
import com.marcusprado02.commons.kernel.ddd.event.EventMetadata;
import com.marcusprado02.commons.kernel.ddd.tenant.TenantId;
import com.marcusprado02.commons.kernel.ddd.time.ClockProvider;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class OutboxPublisherAndSupportTest {

  private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
  private static final ActorId ACTOR = ActorId.of("actor-1");
  private static final AuditStamp STAMP = AuditStamp.of(NOW, ACTOR);

  // --- minimal stubs ---

  private static class RecordingRepository implements OutboxRepositoryPort {
    final List<OutboxMessage> appended = new ArrayList<>();

    @Override
    public void append(OutboxMessage message) {
      appended.add(message);
    }

    @Override
    public List<OutboxMessage> fetchBatch(OutboxStatus status, int limit) {
      return List.of();
    }

    @Override
    public boolean markProcessing(OutboxMessageId id, Instant processingAt) {
      return false;
    }

    @Override
    public void markPublished(OutboxMessageId id, Instant publishedAt) {}

    @Override
    public void markFailed(OutboxMessageId id, String reason, int attempts) {}

    @Override
    public void markDead(OutboxMessageId id, String reason, int attempts) {}

    @Override
    public void markRetryable(OutboxMessageId id, String reason, int attempts) {}

    @Override
    public Optional<OutboxMessage> findById(OutboxMessageId id) {
      return Optional.empty();
    }

    @Override
    public long countByStatus(OutboxStatus status) {
      return 0;
    }

    @Override
    public int deletePublishedOlderThan(Instant olderThan) {
      return 0;
    }
  }

  private static class TestAggregate extends AggregateRoot<String> {
    TestAggregate(String id) {
      super(id, TenantId.of("t-1"), STAMP);
    }

    void addEvent(DomainEvent event) {
      recordChange(STAMP, () -> {}, snap -> event);
    }
  }

  private record TestEvent(String aggregateId) implements DomainEvent {
    @Override
    public EventId eventId() {
      return EventId.newId();
    }

    @Override
    public Instant occurredAt() {
      return NOW;
    }

    @Override
    public String aggregateType() {
      return "TestAggregate";
    }

    @Override
    public String aggregateId() {
      return aggregateId;
    }

    @Override
    public long aggregateVersion() {
      return 1L;
    }

    @Override
    public EventMetadata metadata() {
      return EventMetadata.empty();
    }
  }

  private DefaultOutboxPublisher publisher(RecordingRepository repo) {
    OutboxMetadataEnricher enricher =
        new OutboxMetadataEnricher(
            () -> TenantId.of("tenant-1"),
            new com.marcusprado02.commons.kernel.ddd.context.CorrelationProvider() {
              @Override
              public String correlationId() {
                return "corr-id-1";
              }

              @Override
              public String causationId() {
                return "cause-id-1";
              }
            },
            () -> ActorId.of("actor-1"));
    return new DefaultOutboxPublisher(repo, event -> "{}", enricher, ClockProvider.fixed(NOW));
  }

  // --- DefaultOutboxPublisher tests ---

  @Test
  void publishFrom_null_aggregate_throws() {
    RecordingRepository repo = new RecordingRepository();
    assertThrows(NullPointerException.class, () -> publisher(repo).publishFrom(null));
  }

  @Test
  void publishFrom_no_events_appends_nothing() {
    RecordingRepository repo = new RecordingRepository();
    TestAggregate agg = new TestAggregate("agg-1");

    publisher(repo).publishFrom(agg);

    assertTrue(repo.appended.isEmpty());
  }

  @Test
  void publishFrom_with_one_event_appends_one_message() {
    RecordingRepository repo = new RecordingRepository();
    TestAggregate agg = new TestAggregate("agg-1");
    agg.addEvent(new TestEvent("agg-1"));

    publisher(repo).publishFrom(agg);

    assertEquals(1, repo.appended.size());
    OutboxMessage msg = repo.appended.get(0);
    assertEquals("TestAggregate", msg.aggregateType());
    assertEquals("agg-1", msg.aggregateId());
    assertEquals(OutboxStatus.PENDING, msg.status());
    assertEquals(0, msg.attempts());
    assertNotNull(msg.id());
    assertNotNull(msg.payload());
    assertEquals("application/json", msg.payload().contentType());
  }

  @Test
  void publishFrom_headers_include_enrichment_non_null_values() {
    RecordingRepository repo = new RecordingRepository();
    TestAggregate agg = new TestAggregate("agg-1");
    agg.addEvent(new TestEvent("agg-1"));

    publisher(repo).publishFrom(agg);

    Map<String, String> headers = repo.appended.get(0).headers();
    assertEquals("tenant-1", headers.get("tenantId"));
    assertEquals("corr-id-1", headers.get("correlationId"));
    assertEquals("cause-id-1", headers.get("causationId"));
    assertEquals("actor-1", headers.get("actorId"));
  }

  @Test
  void publishFrom_with_two_events_appends_two_messages() {
    RecordingRepository repo = new RecordingRepository();
    TestAggregate agg = new TestAggregate("agg-1");
    agg.addEvent(new TestEvent("agg-1"));
    agg.addEvent(new TestEvent("agg-1"));

    publisher(repo).publishFrom(agg);

    assertEquals(2, repo.appended.size());
  }

  // --- OutboxSupport tests ---

  @Test
  void persistAndPublish_runs_persist_action_and_returns_aggregate() {
    boolean[] ran = {false};
    TestAggregate agg = new TestAggregate("agg-2");
    OutboxSupport support = new OutboxSupport(a -> {});

    TestAggregate returned = support.persistAndPublish(agg, () -> ran[0] = true);

    assertTrue(ran[0]);
    assertSame(agg, returned);
  }

  @Test
  void persistAndPublish_null_aggregate_throws() {
    OutboxSupport support = new OutboxSupport(a -> {});
    assertThrows(NullPointerException.class, () -> support.persistAndPublish(null, () -> {}));
  }

  @Test
  void persistAndPublish_null_persistAction_throws() {
    OutboxSupport support = new OutboxSupport(a -> {});
    TestAggregate agg = new TestAggregate("agg-3");
    assertThrows(NullPointerException.class, () -> support.persistAndPublish(agg, null));
  }

  @Test
  void outboxSupport_null_publisher_throws() {
    assertThrows(NullPointerException.class, () -> new OutboxSupport(null));
  }

  // --- NoOpOutboxMetrics direct coverage ---

  @Test
  void noOpOutboxMetrics_all_methods_no_op() {
    var m = com.marcusprado02.commons.app.outbox.metrics.NoOpOutboxMetrics.INSTANCE;
    m.recordPublished("t");
    m.recordFailed("t", "r");
    m.recordDead("t");
    m.recordLatency("t", 100L);
    m.recordBatchProcessing(5, 200L);
  }
}
