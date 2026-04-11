package com.marcusprado02.commons.adapters.persistence.jpa.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marcusprado02.commons.adapters.persistence.jpa.idempotency.IdempotencyRecordEntity;
import com.marcusprado02.commons.adapters.persistence.jpa.idempotency.JpaIdempotencyStoreAdapter;
import com.marcusprado02.commons.app.idempotency.model.IdempotencyKey;
import com.marcusprado02.commons.app.idempotency.model.IdempotencyRecord;
import com.marcusprado02.commons.app.idempotency.model.IdempotencyStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JpaIdempotencyStoreAdapterTest {

  private EntityManager em;
  private JpaIdempotencyStoreAdapter adapter;
  private final IdempotencyKey key = new IdempotencyKey("test-key");

  @BeforeEach
  void setUp() {
    em = mock(EntityManager.class);
    adapter = new JpaIdempotencyStoreAdapter(em);
  }

  // ── find() ──────────────────────────────────────────────────────────────

  @Test
  void findReturnsEmptyWhenEntityNotFound() {
    when(em.find(IdempotencyRecordEntity.class, "test-key")).thenReturn(null);

    Optional<IdempotencyRecord> result = adapter.find(key);

    assertTrue(result.isEmpty());
  }

  @Test
  void findReturnsEmptyWhenEntityIsExpired() {
    IdempotencyRecordEntity entity =
        buildEntity("test-key", IdempotencyStatus.IN_PROGRESS, Instant.now().minusSeconds(10));
    when(em.find(IdempotencyRecordEntity.class, "test-key")).thenReturn(entity);

    Optional<IdempotencyRecord> result = adapter.find(key);

    assertTrue(result.isEmpty());
  }

  @Test
  void findReturnsRecordWhenEntityIsValid() {
    IdempotencyRecordEntity entity =
        buildEntity("test-key", IdempotencyStatus.COMPLETED, Instant.now().plusSeconds(300));
    when(em.find(IdempotencyRecordEntity.class, "test-key")).thenReturn(entity);

    Optional<IdempotencyRecord> result = adapter.find(key);

    assertTrue(result.isPresent());
    assertEquals("test-key", result.get().key().value());
    assertEquals(IdempotencyStatus.COMPLETED, result.get().status());
  }

  // ── tryAcquire() ─────────────────────────────────────────────────────────

  @Test
  void tryAcquireReturnsTrueWhenPersistSucceeds() {
    boolean result = adapter.tryAcquire(key, Duration.ofMinutes(5));
    assertTrue(result);
    verify(em).persist(any(IdempotencyRecordEntity.class));
  }

  @Test
  void tryAcquireReturnsFalseWhenPersistFailsAndNoExistingRecord() {
    doThrow(new PersistenceException("duplicate")).when(em).persist(any());
    when(em.find(
            eq(IdempotencyRecordEntity.class), eq("test-key"), eq(LockModeType.PESSIMISTIC_WRITE)))
        .thenReturn(null);

    boolean result = adapter.tryAcquire(key, Duration.ofMinutes(5));

    assertFalse(result);
  }

  @Test
  void tryAcquireReturnsTrueWhenExistingRecordIsExpired() {
    doThrow(new PersistenceException("duplicate")).when(em).persist(any());
    IdempotencyRecordEntity existing =
        buildEntity("test-key", IdempotencyStatus.IN_PROGRESS, Instant.now().minusSeconds(10));
    when(em.find(
            eq(IdempotencyRecordEntity.class), eq("test-key"), eq(LockModeType.PESSIMISTIC_WRITE)))
        .thenReturn(existing);

    boolean result = adapter.tryAcquire(key, Duration.ofMinutes(5));

    assertTrue(result);
    verify(em).merge(existing);
  }

  @Test
  void tryAcquireReturnsFalseWhenExistingRecordIsStillActive() {
    doThrow(new PersistenceException("duplicate")).when(em).persist(any());
    IdempotencyRecordEntity existing =
        buildEntity("test-key", IdempotencyStatus.IN_PROGRESS, Instant.now().plusSeconds(300));
    when(em.find(
            eq(IdempotencyRecordEntity.class), eq("test-key"), eq(LockModeType.PESSIMISTIC_WRITE)))
        .thenReturn(existing);

    boolean result = adapter.tryAcquire(key, Duration.ofMinutes(5));

    assertFalse(result);
    verify(em, never()).merge(existing);
  }

  // ── markCompleted() ──────────────────────────────────────────────────────

  @Test
  void markCompletedDoesNothingWhenEntityNotFound() {
    when(em.find(
            eq(IdempotencyRecordEntity.class), eq("test-key"), eq(LockModeType.PESSIMISTIC_WRITE)))
        .thenReturn(null);

    adapter.markCompleted(key, "result-ref");

    verify(em, never()).merge(any());
  }

  @Test
  void markCompletedUpdatesStatusAndRef() {
    IdempotencyRecordEntity entity =
        buildEntity("test-key", IdempotencyStatus.IN_PROGRESS, Instant.now().plusSeconds(300));
    when(em.find(
            eq(IdempotencyRecordEntity.class), eq("test-key"), eq(LockModeType.PESSIMISTIC_WRITE)))
        .thenReturn(entity);

    adapter.markCompleted(key, "my-result");

    assertEquals(IdempotencyStatus.COMPLETED, entity.getStatus());
    assertEquals("my-result", entity.getResultRef());
    verify(em).merge(entity);
  }

  // ── markFailed() ─────────────────────────────────────────────────────────

  @Test
  void markFailedDoesNothingWhenEntityNotFound() {
    when(em.find(
            eq(IdempotencyRecordEntity.class), eq("test-key"), eq(LockModeType.PESSIMISTIC_WRITE)))
        .thenReturn(null);

    adapter.markFailed(key, "error msg");

    verify(em, never()).merge(any());
  }

  @Test
  void markFailedUpdatesStatusAndError() {
    IdempotencyRecordEntity entity =
        buildEntity("test-key", IdempotencyStatus.IN_PROGRESS, Instant.now().plusSeconds(300));
    when(em.find(
            eq(IdempotencyRecordEntity.class), eq("test-key"), eq(LockModeType.PESSIMISTIC_WRITE)))
        .thenReturn(entity);

    adapter.markFailed(key, "something broke");

    assertEquals(IdempotencyStatus.FAILED, entity.getStatus());
    assertEquals("something broke", entity.getLastError());
    verify(em).merge(entity);
  }

  // ── helpers ──────────────────────────────────────────────────────────────

  private static IdempotencyRecordEntity buildEntity(
      String key, IdempotencyStatus status, Instant expiresAt) {
    Instant now = Instant.now();
    IdempotencyRecordEntity e = new IdempotencyRecordEntity();
    e.setKey(key);
    e.setStatus(status);
    e.setCreatedAt(now);
    e.setUpdatedAt(now);
    e.setExpiresAt(expiresAt);
    return e;
  }
}
