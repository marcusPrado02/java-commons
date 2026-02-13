package com.marcusprado02.commons.adapters.persistence.jpa.outbox;

import static org.junit.jupiter.api.Assertions.*;

import com.marcusprado02.commons.adapters.persistence.jpa.factory.JpaRepositoryFactory;
import com.marcusprado02.commons.app.outbox.model.OutboxMessage;
import com.marcusprado02.commons.app.outbox.model.OutboxMessageId;
import com.marcusprado02.commons.app.outbox.model.OutboxPayload;
import com.marcusprado02.commons.app.outbox.model.OutboxStatus;
import com.marcusprado02.commons.app.outbox.port.OutboxRepositoryPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class JpaOutboxRepositoryAdapterTest {

  @Container
  static PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:15.4")
          .withDatabaseName("testdb")
          .withUsername("test")
          .withPassword("test");

  static EntityManagerFactory emf;

  @BeforeAll
  static void setup() {
    POSTGRES.start();

    Map<String, Object> props =
        Map.of(
            "jakarta.persistence.jdbc.url",
            POSTGRES.getJdbcUrl(),
            "jakarta.persistence.jdbc.user",
            POSTGRES.getUsername(),
            "jakarta.persistence.jdbc.password",
            POSTGRES.getPassword(),
            "jakarta.persistence.jdbc.driver",
            "org.postgresql.Driver",
            "hibernate.hbm2ddl.auto",
            "create-drop",
            "hibernate.show_sql",
            "false",
            "hibernate.format_sql",
            "false");

    emf = JpaRepositoryFactory.createEntityManagerFactory("test-pu", props);
  }

  @AfterAll
  static void teardown() {
    if (emf != null) emf.close();
    POSTGRES.stop();
  }

  EntityManager em;
  OutboxRepositoryPort repository;
  static int testCounter = 0;

  @BeforeEach
  void beforeEach() {
    em = JpaRepositoryFactory.createEntityManager(emf);
    repository = new JpaOutboxRepositoryAdapter(em);
    testCounter++;
  }

  @Test
  void shouldAppendMessage() {
    var message = createMessage("msg-" + testCounter, OutboxStatus.PENDING);

    em.getTransaction().begin();
    repository.append(message);
    em.getTransaction().commit();

    em.clear();

    em.getTransaction().begin();
    var found = repository.findById(message.id());
    em.getTransaction().commit();

    assertTrue(found.isPresent());
    assertEquals("msg-" + testCounter, found.get().id().value());
    assertEquals(OutboxStatus.PENDING, found.get().status());
  }

  @Test
  void shouldFetchBatch() {
    em.getTransaction().begin();
    repository.append(createMessage("msg-" + testCounter + "-1", OutboxStatus.PENDING));
    repository.append(createMessage("msg-" + testCounter + "-2", OutboxStatus.PENDING));
    repository.append(createMessage("msg-" + testCounter + "-3", OutboxStatus.PROCESSING));
    em.getTransaction().commit();

    em.clear();

    em.getTransaction().begin();
    var pending = repository.fetchBatch(OutboxStatus.PENDING, 10);
    em.getTransaction().commit();

    assertEquals(2, pending.size());
  }

  @Test
  void shouldMarkProcessing() {
    var message = createMessage("msg-" + testCounter, OutboxStatus.PENDING);

    em.getTransaction().begin();
    repository.append(message);
    em.getTransaction().commit();

    em.clear();

    var now = Instant.now();

    em.getTransaction().begin();
    boolean marked = repository.markProcessing(message.id(), now);
    em.getTransaction().commit();

    assertTrue(marked);

    em.clear();

    em.getTransaction().begin();
    var found = repository.findById(message.id());
    em.getTransaction().commit();

    assertTrue(found.isPresent());
    assertEquals(OutboxStatus.PROCESSING, found.get().status());
    assertEquals(1, found.get().attempts());
  }

  @Test
  void shouldNotMarkProcessingIfAlreadyProcessing() {
    var message = createMessage("msg-" + testCounter, OutboxStatus.PENDING);

    em.getTransaction().begin();
    repository.append(message);
    em.getTransaction().commit();

    em.clear();

    var now = Instant.now();

    em.getTransaction().begin();
    boolean first = repository.markProcessing(message.id(), now);
    em.getTransaction().commit();

    assertTrue(first);

    em.clear();

    em.getTransaction().begin();
    boolean second = repository.markProcessing(message.id(), now);
    em.getTransaction().commit();

    assertFalse(second, "Should not mark as processing again");
  }

  @Test
  void shouldMarkPublished() {
    var message = createMessage("msg-" + testCounter, OutboxStatus.PROCESSING);

    em.getTransaction().begin();
    repository.append(message);
    em.getTransaction().commit();

    em.clear();

    var now = Instant.now();

    em.getTransaction().begin();
    repository.markPublished(message.id(), now);
    em.getTransaction().commit();

    em.clear();

    em.getTransaction().begin();
    var found = repository.findById(message.id());
    em.getTransaction().commit();

    assertTrue(found.isPresent());
    assertEquals(OutboxStatus.PUBLISHED, found.get().status());
  }

  @Test
  void shouldMarkFailed() {
    var message = createMessage("msg-" + testCounter, OutboxStatus.PROCESSING);

    em.getTransaction().begin();
    repository.append(message);
    em.getTransaction().commit();

    em.clear();

    em.getTransaction().begin();
    repository.markFailed(message.id(), "Connection timeout", 1);
    em.getTransaction().commit();

    em.clear();

    em.getTransaction().begin();
    var found = repository.findById(message.id());
    em.getTransaction().commit();

    assertTrue(found.isPresent());
    assertEquals(OutboxStatus.FAILED, found.get().status());
    assertEquals(1, found.get().attempts());
  }

  @Test
  void shouldMarkDead() {
    var message = createMessage("msg-" + testCounter, OutboxStatus.FAILED);

    em.getTransaction().begin();
    repository.append(message);
    em.getTransaction().commit();

    em.clear();

    em.getTransaction().begin();
    repository.markDead(message.id(), "Max retries exceeded", 5);
    em.getTransaction().commit();

    em.clear();

    em.getTransaction().begin();
    var found = repository.findById(message.id());
    em.getTransaction().commit();

    assertTrue(found.isPresent());
    assertEquals(OutboxStatus.DEAD, found.get().status());
    assertEquals(5, found.get().attempts());
  }

  @Test
  void shouldMarkRetryable() {
    var message = createMessage("msg-" + testCounter, OutboxStatus.FAILED);

    em.getTransaction().begin();
    repository.append(message);
    em.getTransaction().commit();

    em.clear();

    em.getTransaction().begin();
    repository.markRetryable(message.id(), "Retry after backoff", 2);
    em.getTransaction().commit();

    em.clear();

    em.getTransaction().begin();
    var found = repository.findById(message.id());
    em.getTransaction().commit();

    assertTrue(found.isPresent());
    assertEquals(OutboxStatus.PENDING, found.get().status());
    assertEquals(2, found.get().attempts());
  }

  @Test
  void shouldCountByStatus() {
    em.getTransaction().begin();
    repository.append(createMessage("msg-" + testCounter + "-1", OutboxStatus.PENDING));
    repository.append(createMessage("msg-" + testCounter + "-2", OutboxStatus.PENDING));
    repository.append(createMessage("msg-" + testCounter + "-3", OutboxStatus.PROCESSING));
    repository.append(createMessage("msg-" + testCounter + "-4", OutboxStatus.PUBLISHED));
    em.getTransaction().commit();

    em.clear();

    em.getTransaction().begin();
    long pendingCount = repository.countByStatus(OutboxStatus.PENDING);
    long processingCount = repository.countByStatus(OutboxStatus.PROCESSING);
    long publishedCount = repository.countByStatus(OutboxStatus.PUBLISHED);
    em.getTransaction().commit();

    assertTrue(pendingCount >= 2, "Should have at least 2 pending messages");
    assertTrue(processingCount >= 1, "Should have at least 1 processing message");
    assertTrue(publishedCount >= 1, "Should have at least 1 published message");
  }

  @Test
  void shouldDeletePublishedOlderThan() {
    var old = Instant.now().minus(2, ChronoUnit.DAYS);
    var recent = Instant.now();

    em.getTransaction().begin();
    var msg1 = createMessage("msg-" + testCounter + "-1", OutboxStatus.PUBLISHED);
    repository.append(msg1);
    repository.markPublished(msg1.id(), old);

    var msg2 = createMessage("msg-" + testCounter + "-2", OutboxStatus.PUBLISHED);
    repository.append(msg2);
    repository.markPublished(msg2.id(), recent);
    em.getTransaction().commit();

    em.clear();

    var cutoff = Instant.now().minus(1, ChronoUnit.DAYS);

    em.getTransaction().begin();
    int deleted = repository.deletePublishedOlderThan(cutoff);
    em.getTransaction().commit();

    assertEquals(1, deleted);

    em.clear();

    em.getTransaction().begin();
    long remaining = repository.countByStatus(OutboxStatus.PUBLISHED);
    em.getTransaction().commit();

    assertEquals(1, remaining);
  }

  @Test
  void shouldHandleConcurrentMarkProcessing() throws Exception {
    var message = createMessage("msg-" + testCounter, OutboxStatus.PENDING);

    em.getTransaction().begin();
    repository.append(message);
    em.getTransaction().commit();

    em.clear();

    int threadCount = 5;
    CountDownLatch latch = new CountDownLatch(threadCount);
    AtomicInteger successCount = new AtomicInteger(0);

    for (int i = 0; i < threadCount; i++) {
      new Thread(
              () -> {
                try {
                  EntityManager threadEm = JpaRepositoryFactory.createEntityManager(emf);
                  OutboxRepositoryPort threadRepo = new JpaOutboxRepositoryAdapter(threadEm);

                  threadEm.getTransaction().begin();
                  boolean success = threadRepo.markProcessing(message.id(), Instant.now());
                  threadEm.getTransaction().commit();

                  if (success) {
                    successCount.incrementAndGet();
                  }

                  threadEm.close();
                } finally {
                  latch.countDown();
                }
              })
          .start();
    }

    latch.await();

    assertEquals(
        1, successCount.get(), "Only one thread should successfully mark message as processing");
  }

  private OutboxMessage createMessage(String id, OutboxStatus status) {
    return new OutboxMessage(
        new OutboxMessageId(id),
        "Order",
        "order-123",
        "OrderCreated",
        "orders.created",
        new OutboxPayload("application/json", "{\"orderId\":\"123\"}".getBytes(StandardCharsets.UTF_8)),
        Map.of("trace-id", "xyz"),
        Instant.now(),
        status,
        0);
  }
}
