package com.marcusprado02.commons.app.outbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.marcusprado02.commons.app.outbox.config.OutboxProcessorConfig;
import com.marcusprado02.commons.app.outbox.metrics.OutboxMetrics;
import com.marcusprado02.commons.app.outbox.model.OutboxMessage;
import com.marcusprado02.commons.app.outbox.model.OutboxMessageId;
import com.marcusprado02.commons.app.outbox.model.OutboxPayload;
import com.marcusprado02.commons.app.outbox.model.OutboxStatus;
import com.marcusprado02.commons.app.outbox.port.OutboxRepositoryPort;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefaultOutboxProcessorTest {

  private InMemoryOutboxRepository repository;
  private TestOutboundPublisher publisher;
  private TestOutboxMetrics metrics;
  private DefaultOutboxProcessor processor;

  @BeforeEach
  void setUp() {
    repository = new InMemoryOutboxRepository();
    publisher = new TestOutboundPublisher();
    metrics = new TestOutboxMetrics();

    OutboxProcessorConfig config = OutboxProcessorConfig.defaults();
    processor = new DefaultOutboxProcessor(repository, publisher, config, metrics);
  }

  @Test
  void shouldProcessPendingMessagesSuccessfully() {
    OutboxMessage msg = createMessage("msg-1", OutboxStatus.PENDING);
    repository.append(msg);

    processor.processAll();

    assertEquals(1, publisher.publishedCount);
    assertEquals(1, metrics.publishedCount);
    assertEquals(0, metrics.failedCount);

    Optional<OutboxMessage> result = repository.findById(msg.id());
    assertTrue(result.isPresent());
    assertEquals(OutboxStatus.PUBLISHED, result.get().status());
  }

  @Test
  void shouldHandlePublishingFailure() {
    publisher.shouldFail = true;

    OutboxMessage msg = createMessage("msg-1", OutboxStatus.PENDING);
    repository.append(msg);

    processor.processAll();

    assertEquals(0, publisher.publishedCount);
    assertEquals(1, metrics.failedCount);

    Optional<OutboxMessage> result = repository.findById(msg.id());
    assertTrue(result.isPresent());
    assertEquals(OutboxStatus.FAILED, result.get().status());
    assertEquals(1, result.get().attempts());
  }

  @Test
  void shouldMoveToDeadAfterMaxAttempts() {
    publisher.shouldFail = true;

    OutboxMessage msg = new OutboxMessage(
        new OutboxMessageId("msg-1"),
        "Order",
        "order-1",
        "OrderCreated",
        "orders.created",
        new OutboxPayload("application/json", new byte[0]),
        Map.of(),
        Instant.now(),
        OutboxStatus.PENDING,
        4); // One attempt away from max
    repository.append(msg);

    processor.processAll();

    Optional<OutboxMessage> result = repository.findById(msg.id());
    assertTrue(result.isPresent());
    assertEquals(OutboxStatus.DEAD, result.get().status());
    assertEquals(1, metrics.deadCount);
  }

  @Test
  void shouldProcessBatchLimitedByConfig() {
    OutboxProcessorConfig config = new OutboxProcessorConfig(
        2, 5, Duration.ofSeconds(1), Duration.ofMinutes(5), 2.0, false);
    processor = new DefaultOutboxProcessor(repository, publisher, config, metrics);

    repository.append(createMessage("msg-1", OutboxStatus.PENDING));
    repository.append(createMessage("msg-2", OutboxStatus.PENDING));
    repository.append(createMessage("msg-3", OutboxStatus.PENDING));

    processor.processAll();

    assertEquals(2, publisher.publishedCount);
  }

  @Test
  void shouldHandleConcurrentProcessing() {
    OutboxMessage msg = createMessage("msg-1", OutboxStatus.PENDING);
    repository.append(msg);

    boolean marked1 = repository.markProcessing(msg.id(), Instant.now());
    boolean marked2 = repository.markProcessing(msg.id(), Instant.now());

    assertTrue(marked1);
    assertFalse(marked2);
  }

  private OutboxMessage createMessage(String id, OutboxStatus status) {
    return new OutboxMessage(
        new OutboxMessageId(id),
        "Order",
        "order-1",
        "OrderCreated",
        "orders.created",
        new OutboxPayload("application/json", new byte[0]),
        Map.of(),
        Instant.now(),
        status,
        0);
  }

  static class InMemoryOutboxRepository implements OutboxRepositoryPort {
    private final Map<OutboxMessageId, OutboxMessage> store = new ConcurrentHashMap<>();

    @Override
    public void append(OutboxMessage message) {
      store.put(message.id(), message);
    }

    @Override
    public List<OutboxMessage> fetchBatch(OutboxStatus status, int limit) {
      return store.values().stream()
          .filter(msg -> msg.status() == status)
          .limit(limit)
          .toList();
    }

    @Override
    public boolean markProcessing(OutboxMessageId id, Instant processingAt) {
      OutboxMessage msg = store.get(id);
      if (msg == null || msg.status() != OutboxStatus.PENDING) {
        return false;
      }
      OutboxMessage updated = new OutboxMessage(
          msg.id(), msg.aggregateType(), msg.aggregateId(), msg.eventType(),
          msg.topic(), msg.payload(), msg.headers(), msg.occurredAt(),
          OutboxStatus.PROCESSING, msg.attempts(), msg.priority());
      store.put(id, updated);
      return true;
    }

    @Override
    public void markPublished(OutboxMessageId id, Instant publishedAt) {
      OutboxMessage msg = store.get(id);
      OutboxMessage updated = new OutboxMessage(
          msg.id(), msg.aggregateType(), msg.aggregateId(), msg.eventType(),
          msg.topic(), msg.payload(), msg.headers(), msg.occurredAt(),
          OutboxStatus.PUBLISHED, msg.attempts(), msg.priority());
      store.put(id, updated);
    }

    @Override
    public void markFailed(OutboxMessageId id, String reason, int attempts) {
      OutboxMessage msg = store.get(id);
      OutboxMessage updated = new OutboxMessage(
          msg.id(), msg.aggregateType(), msg.aggregateId(), msg.eventType(),
          msg.topic(), msg.payload(), msg.headers(), msg.occurredAt(),
          OutboxStatus.FAILED, attempts, msg.priority());
      store.put(id, updated);
    }

    @Override
    public void markDead(OutboxMessageId id, String reason, int attempts) {
      OutboxMessage msg = store.get(id);
      OutboxMessage updated = new OutboxMessage(
          msg.id(), msg.aggregateType(), msg.aggregateId(), msg.eventType(),
          msg.topic(), msg.payload(), msg.headers(), msg.occurredAt(),
          OutboxStatus.DEAD, attempts, msg.priority());
      store.put(id, updated);
    }

    @Override
    public void markRetryable(OutboxMessageId id, String reason, int attempts) {
      OutboxMessage msg = store.get(id);
      OutboxMessage updated = new OutboxMessage(
          msg.id(), msg.aggregateType(), msg.aggregateId(), msg.eventType(),
          msg.topic(), msg.payload(), msg.headers(), msg.occurredAt(),
          OutboxStatus.PENDING, attempts, msg.priority());
      store.put(id, updated);
    }

    @Override
    public Optional<OutboxMessage> findById(OutboxMessageId id) {
      return Optional.ofNullable(store.get(id));
    }

    @Override
    public long countByStatus(OutboxStatus status) {
      return store.values().stream().filter(msg -> msg.status() == status).count();
    }

    @Override
    public int deletePublishedOlderThan(Instant olderThan) {
      return 0;
    }
  }

  static class TestOutboundPublisher implements OutboundPublisher {
    int publishedCount = 0;
    boolean shouldFail = false;

    @Override
    public void publish(String topic, byte[] body, Map<String, String> headers) {
      if (shouldFail) {
        throw new RuntimeException("Publishing failed");
      }
      publishedCount++;
    }
  }

  static class TestOutboxMetrics implements OutboxMetrics {
    int publishedCount = 0;
    int failedCount = 0;
    int deadCount = 0;
    List<Long> latencies = new ArrayList<>();

    @Override
    public void recordPublished(String topic) {
      publishedCount++;
    }

    @Override
    public void recordFailed(String topic, String reason) {
      failedCount++;
    }

    @Override
    public void recordDead(String topic) {
      deadCount++;
    }

    @Override
    public void recordLatency(String topic, long durationMillis) {
      latencies.add(durationMillis);
    }

    @Override
    public void recordBatchProcessing(int batchSize, long durationMillis) {}
  }
}
