package com.marcusprado02.commons.adapters.persistence.jpa.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marcusprado02.commons.adapters.persistence.jpa.exception.JpaPersistenceException;
import com.marcusprado02.commons.adapters.persistence.jpa.factory.JpaRepositoryFactory;
import com.marcusprado02.commons.adapters.persistence.jpa.idempotency.IdempotencyJpaMapper;
import com.marcusprado02.commons.adapters.persistence.jpa.idempotency.IdempotencyRecordEntity;
import com.marcusprado02.commons.adapters.persistence.jpa.outbox.JpaOutboxRepositoryAdapter;
import com.marcusprado02.commons.adapters.persistence.jpa.outbox.OutboxJpaMapper;
import com.marcusprado02.commons.adapters.persistence.jpa.outbox.OutboxMessageEntity;
import com.marcusprado02.commons.adapters.persistence.jpa.repository.PageableJpaRepository;
import com.marcusprado02.commons.adapters.persistence.jpa.shared.JpaQueries;
import com.marcusprado02.commons.adapters.persistence.jpa.transaction.Transactional;
import com.marcusprado02.commons.app.idempotency.model.IdempotencyRecord;
import com.marcusprado02.commons.app.idempotency.model.IdempotencyStatus;
import com.marcusprado02.commons.app.outbox.model.OutboxMessage;
import com.marcusprado02.commons.app.outbox.model.OutboxMessageId;
import com.marcusprado02.commons.app.outbox.model.OutboxPayload;
import com.marcusprado02.commons.app.outbox.model.OutboxStatus;
import com.marcusprado02.commons.ports.persistence.model.Order;
import com.marcusprado02.commons.ports.persistence.model.PageRequest;
import com.marcusprado02.commons.ports.persistence.model.PageResult;
import com.marcusprado02.commons.ports.persistence.model.Sort;
import com.marcusprado02.commons.ports.persistence.specification.Specification;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class JpaSimpleTest {

  // ── JpaQueries ──────────────────────────────────────────────────────────

  @Test
  void safeLimitReturnsValueWhenPositive() {
    assertEquals(10, JpaQueries.safeLimit(10, 50));
  }

  @Test
  void safeLimitReturnsFallbackWhenZero() {
    assertEquals(50, JpaQueries.safeLimit(0, 50));
  }

  @Test
  void safeLimitReturnsFallbackWhenNegative() {
    assertEquals(50, JpaQueries.safeLimit(-5, 50));
  }

  // ── JpaPersistenceException ──────────────────────────────────────────────

  @Test
  void exceptionWrapsMessageAndCause() {
    RuntimeException cause = new RuntimeException("db error");
    JpaPersistenceException ex = new JpaPersistenceException("wrap", cause);
    assertEquals("wrap", ex.getMessage());
    assertSame(cause, ex.getCause());
  }

  // ── IdempotencyJpaMapper ─────────────────────────────────────────────────

  @Test
  void toModelMapsAllFields() {
    Instant now = Instant.now();
    Instant expires = now.plusSeconds(300);

    IdempotencyRecordEntity entity = new IdempotencyRecordEntity();
    entity.setKey("key-123");
    entity.setStatus(IdempotencyStatus.COMPLETED);
    entity.setCreatedAt(now);
    entity.setUpdatedAt(now);
    entity.setExpiresAt(expires);
    entity.setResultRef("ref-abc");
    entity.setLastError(null);

    IdempotencyRecord model = IdempotencyJpaMapper.toModel(entity);

    assertNotNull(model);
    assertEquals("key-123", model.key().value());
    assertEquals(IdempotencyStatus.COMPLETED, model.status());
    assertEquals(now, model.createdAt());
    assertEquals(now, model.updatedAt());
    assertEquals(expires, model.expiresAt());
    assertEquals("ref-abc", model.resultRef());
  }

  // ── Transactional ────────────────────────────────────────────────────────

  @Test
  void transactionalRunWithSupplierBeginsAndCommitsWhenTxNotActive() {
    EntityManager em = mock(EntityManager.class);
    EntityTransaction tx = mock(EntityTransaction.class);
    when(em.getTransaction()).thenReturn(tx);
    when(tx.isActive()).thenReturn(false);

    String result = Transactional.run(em, () -> "ok");

    assertEquals("ok", result);
    verify(tx).begin();
    verify(tx).commit();
  }

  @Test
  void transactionalRunWithSupplierSkipsBeginCommitWhenTxAlreadyActive() {
    EntityManager em = mock(EntityManager.class);
    EntityTransaction tx = mock(EntityTransaction.class);
    when(em.getTransaction()).thenReturn(tx);
    when(tx.isActive()).thenReturn(true);

    String result = Transactional.run(em, () -> "active");

    assertEquals("active", result);
    verify(tx, never()).begin();
    verify(tx, never()).commit();
  }

  @Test
  void transactionalRunRollsBackWhenExceptionThrownAndTxWasNotActive() {
    EntityManager em = mock(EntityManager.class);
    EntityTransaction tx = mock(EntityTransaction.class);
    when(em.getTransaction()).thenReturn(tx);
    when(tx.isActive()).thenReturn(false).thenReturn(true);

    Supplier<String> failingSupplier =
        () -> {
          throw new RuntimeException("fail");
        };
    assertThrows(RuntimeException.class, () -> Transactional.run(em, failingSupplier));

    verify(tx).rollback();
  }

  @Test
  void transactionalRunWithRunnableExecutesAction() {
    EntityManager em = mock(EntityManager.class);
    EntityTransaction tx = mock(EntityTransaction.class);
    when(em.getTransaction()).thenReturn(tx);
    when(tx.isActive()).thenReturn(false);

    boolean[] ran = {false};
    Transactional.run(em, () -> ran[0] = true);

    assertTrue(ran[0]);
    verify(tx).begin();
    verify(tx).commit();
  }

  // ── OutboxJpaMapper – null headers branch ────────────────────────────────

  @Test
  void headersToJsonReturnsNullWhenHeadersAreNull() {
    OutboxMessage message =
        new OutboxMessage(
            new OutboxMessageId("id-null-headers"),
            "AggType",
            "agg-1",
            "EventType",
            "topic",
            new OutboxPayload("application/json", "{}".getBytes(StandardCharsets.UTF_8)),
            null,
            Instant.now(),
            OutboxStatus.PENDING,
            0);

    OutboxMessageEntity entity = OutboxJpaMapper.toEntity(message);

    assertNull(entity.getHeadersJson());
  }

  // ── JpaOutboxRepositoryAdapter – null-entity guard branches ─────────────

  @Test
  void markPublishedDoesNothingWhenEntityNotFound() {
    EntityManager em = mock(EntityManager.class);
    OutboxMessageId id = new OutboxMessageId("missing-id");
    when(em.find(OutboxMessageEntity.class, "missing-id")).thenReturn(null);

    JpaOutboxRepositoryAdapter adapter = new JpaOutboxRepositoryAdapter(em);
    adapter.markPublished(id, Instant.now());

    verify(em, never()).merge(any());
  }

  @Test
  void markFailedDoesNothingWhenEntityNotFound() {
    EntityManager em = mock(EntityManager.class);
    OutboxMessageId id = new OutboxMessageId("missing-id-2");
    when(em.find(OutboxMessageEntity.class, "missing-id-2")).thenReturn(null);

    JpaOutboxRepositoryAdapter adapter = new JpaOutboxRepositoryAdapter(em);
    adapter.markFailed(id, "reason", 1);

    verify(em, never()).merge(any());
  }

  @Test
  void markDeadDoesNothingWhenEntityNotFound() {
    EntityManager em = mock(EntityManager.class);
    OutboxMessageId id = new OutboxMessageId("missing-id-3");
    when(em.find(OutboxMessageEntity.class, "missing-id-3")).thenReturn(null);

    JpaOutboxRepositoryAdapter adapter = new JpaOutboxRepositoryAdapter(em);
    adapter.markDead(id, "reason", 5);

    verify(em, never()).merge(any());
  }

  @Test
  void markRetryableDoesNothingWhenEntityNotFound() {
    EntityManager em = mock(EntityManager.class);
    OutboxMessageId id = new OutboxMessageId("missing-id-4");
    when(em.find(OutboxMessageEntity.class, "missing-id-4")).thenReturn(null);

    JpaOutboxRepositoryAdapter adapter = new JpaOutboxRepositoryAdapter(em);
    adapter.markRetryable(id, "reason", 2);

    verify(em, never()).merge(any());
  }

  // ── PageableJpaRepository#search – spec/sort branch coverage ─────────────

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  void searchWithNullSpecAndNullSortReturnsResults() {
    EntityManager em = mock(EntityManager.class);
    CriteriaBuilder cb = mock(CriteriaBuilder.class);
    CriteriaQuery<Long> countQuery = mock(CriteriaQuery.class);
    Root<Object> countRoot = mock(Root.class);
    TypedQuery<Long> countTypedQuery = mock(TypedQuery.class);
    CriteriaQuery<Object> selectQuery = mock(CriteriaQuery.class);
    Root<Object> selectRoot = mock(Root.class);
    TypedQuery<Object> selectTypedQuery = mock(TypedQuery.class);

    when(em.getCriteriaBuilder()).thenReturn(cb);
    when(cb.createQuery(Long.class)).thenReturn(countQuery);
    when(countQuery.from(any(Class.class))).thenReturn(countRoot);
    when(cb.count(any())).thenReturn(mock(Expression.class));
    when(countQuery.select(any())).thenReturn(countQuery);
    when(em.createQuery(eq(countQuery))).thenReturn(countTypedQuery);
    when(countTypedQuery.getSingleResult()).thenReturn(0L);
    when(cb.createQuery(Object.class)).thenReturn((CriteriaQuery) selectQuery);
    when(selectQuery.from(any(Class.class))).thenReturn(selectRoot);
    when(selectQuery.select(any())).thenReturn(selectQuery);
    when(em.createQuery(eq(selectQuery))).thenReturn((TypedQuery) selectTypedQuery);
    when(selectTypedQuery.setFirstResult(0)).thenReturn(selectTypedQuery);
    when(selectTypedQuery.setMaxResults(10)).thenReturn(selectTypedQuery);
    when(selectTypedQuery.getResultList()).thenReturn(List.of());

    PageableJpaRepository<Object, Long> repo =
        JpaRepositoryFactory.createRepository(Object.class, Long.class, em);
    PageResult<Object> result =
        repo.search(new PageRequest(0, 10), (Specification<Object>) null, null);

    assertNotNull(result);
    assertEquals(0L, result.totalElements());
    verify(selectQuery, never()).where(any(Predicate.class));
    verify(selectQuery, never()).orderBy(any(List.class));
  }

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  void searchWithSpecAndAscSortAppliesWhereAndOrder() {
    EntityManager em = mock(EntityManager.class);
    CriteriaBuilder cb = mock(CriteriaBuilder.class);
    CriteriaQuery<Long> countQuery = mock(CriteriaQuery.class);
    Root<Object> countRoot = mock(Root.class);
    TypedQuery<Long> countTypedQuery = mock(TypedQuery.class);
    CriteriaQuery<Object> selectQuery = mock(CriteriaQuery.class);
    Root<Object> selectRoot = mock(Root.class);
    TypedQuery<Object> selectTypedQuery = mock(TypedQuery.class);
    Predicate predicate = mock(Predicate.class);
    jakarta.persistence.criteria.Order jpaOrder = mock(jakarta.persistence.criteria.Order.class);
    Path<Object> path = mock(Path.class);

    when(em.getCriteriaBuilder()).thenReturn(cb);
    when(cb.createQuery(Long.class)).thenReturn(countQuery);
    when(countQuery.from(any(Class.class))).thenReturn(countRoot);
    when(cb.count(any())).thenReturn(mock(Expression.class));
    when(countQuery.select(any())).thenReturn(countQuery);
    when(em.createQuery(eq(countQuery))).thenReturn(countTypedQuery);
    when(countTypedQuery.getSingleResult()).thenReturn(2L);
    when(cb.createQuery(Object.class)).thenReturn((CriteriaQuery) selectQuery);
    when(selectQuery.from(any(Class.class))).thenReturn(selectRoot);
    when(selectQuery.select(any())).thenReturn(selectQuery);
    when(em.createQuery(eq(selectQuery))).thenReturn((TypedQuery) selectTypedQuery);
    when(selectTypedQuery.setFirstResult(0)).thenReturn(selectTypedQuery);
    when(selectTypedQuery.setMaxResults(10)).thenReturn(selectTypedQuery);
    when(selectTypedQuery.getResultList()).thenReturn(List.of());
    when(selectRoot.get("name")).thenReturn(path);
    when(cb.asc(path)).thenReturn(jpaOrder);

    Specification<Object> spec = (root, query, builder) -> predicate;

    PageableJpaRepository<Object, Long> repo =
        JpaRepositoryFactory.createRepository(Object.class, Long.class, em);
    Sort sort = Sort.of(new Order("name", Order.Direction.ASC));
    PageResult<Object> result = repo.search(new PageRequest(0, 10), spec, sort);

    assertNotNull(result);
    assertEquals(2L, result.totalElements());
    verify(selectQuery).where(predicate);
    verify(selectQuery).orderBy(any(List.class));
  }

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  void searchWithDescSortAppliesDescOrder() {
    EntityManager em = mock(EntityManager.class);
    CriteriaBuilder cb = mock(CriteriaBuilder.class);
    CriteriaQuery<Long> countQuery = mock(CriteriaQuery.class);
    Root<Object> countRoot = mock(Root.class);
    TypedQuery<Long> countTypedQuery = mock(TypedQuery.class);
    CriteriaQuery<Object> selectQuery = mock(CriteriaQuery.class);
    Root<Object> selectRoot = mock(Root.class);
    TypedQuery<Object> selectTypedQuery = mock(TypedQuery.class);
    jakarta.persistence.criteria.Order jpaOrder = mock(jakarta.persistence.criteria.Order.class);
    Path<Object> path = mock(Path.class);

    when(em.getCriteriaBuilder()).thenReturn(cb);
    when(cb.createQuery(Long.class)).thenReturn(countQuery);
    when(countQuery.from(any(Class.class))).thenReturn(countRoot);
    when(cb.count(any())).thenReturn(mock(Expression.class));
    when(countQuery.select(any())).thenReturn(countQuery);
    when(em.createQuery(eq(countQuery))).thenReturn(countTypedQuery);
    when(countTypedQuery.getSingleResult()).thenReturn(0L);
    when(cb.createQuery(Object.class)).thenReturn((CriteriaQuery) selectQuery);
    when(selectQuery.from(any(Class.class))).thenReturn(selectRoot);
    when(selectQuery.select(any())).thenReturn(selectQuery);
    when(em.createQuery(eq(selectQuery))).thenReturn((TypedQuery) selectTypedQuery);
    when(selectTypedQuery.setFirstResult(0)).thenReturn(selectTypedQuery);
    when(selectTypedQuery.setMaxResults(10)).thenReturn(selectTypedQuery);
    when(selectTypedQuery.getResultList()).thenReturn(List.of());
    when(selectRoot.get("createdAt")).thenReturn(path);
    when(cb.desc(path)).thenReturn(jpaOrder);

    PageableJpaRepository<Object, Long> repo =
        JpaRepositoryFactory.createRepository(Object.class, Long.class, em);
    Sort sort = Sort.of(new Order("createdAt", Order.Direction.DESC));
    PageResult<Object> result = repo.search(new PageRequest(0, 10), null, sort);

    assertNotNull(result);
    verify(selectQuery).orderBy(any(List.class));
    verify(cb).desc(path);
  }
}
