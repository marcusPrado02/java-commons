package com.marcusprado02.commons.adapters.persistence.jpa.idempotency;

import com.marcusprado02.commons.app.idempotency.model.IdempotencyKey;
import com.marcusprado02.commons.app.idempotency.model.IdempotencyRecord;
import com.marcusprado02.commons.app.idempotency.model.IdempotencyStatus;
import com.marcusprado02.commons.app.idempotency.port.IdempotencyStorePort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceException;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class JpaIdempotencyStoreAdapter implements IdempotencyStorePort {

  private final EntityManager em;

  public JpaIdempotencyStoreAdapter(EntityManager em) {
    this.em = Objects.requireNonNull(em);
  }

  @Override
  public Optional<IdempotencyRecord> find(IdempotencyKey key) {
    IdempotencyRecordEntity e = em.find(IdempotencyRecordEntity.class, key.value());
    if (e == null) {
      return Optional.empty();
    }
    Instant now = Instant.now();
    if (e.getExpiresAt() != null && !e.getExpiresAt().isAfter(now)) {
      return Optional.empty();
    }
    return Optional.of(IdempotencyJpaMapper.toModel(e));
  }

  @Override
  public boolean tryAcquire(IdempotencyKey key, Duration ttl) {
    Objects.requireNonNull(ttl, "ttl must not be null");
    Instant now = Instant.now();
    Instant expiresAt = now.plus(ttl);

    IdempotencyRecordEntity e = new IdempotencyRecordEntity();
    e.setKey(key.value());
    e.setStatus(IdempotencyStatus.IN_PROGRESS);
    e.setCreatedAt(now);
    e.setUpdatedAt(now);
    e.setExpiresAt(expiresAt);

    try {
      em.persist(e);
      return true;
    } catch (PersistenceException ex) {
      IdempotencyRecordEntity existing =
          em.find(IdempotencyRecordEntity.class, key.value(), LockModeType.PESSIMISTIC_WRITE);
      if (existing == null) {
        return false;
      }
      if (existing.getExpiresAt() != null && !existing.getExpiresAt().isAfter(now)) {
        existing.setStatus(IdempotencyStatus.IN_PROGRESS);
        existing.setCreatedAt(now);
        existing.setUpdatedAt(now);
        existing.setExpiresAt(expiresAt);
        existing.setResultRef(null);
        existing.setLastError(null);
        em.merge(existing);
        return true;
      }
      return false;
    }
  }

  @Override
  public void markCompleted(IdempotencyKey key, String resultRef) {
    IdempotencyRecordEntity e =
        em.find(IdempotencyRecordEntity.class, key.value(), LockModeType.PESSIMISTIC_WRITE);
    if (e == null) {
      return;
    }
    e.setStatus(IdempotencyStatus.COMPLETED);
    e.setResultRef(resultRef);
    e.setLastError(null);
    e.setUpdatedAt(Instant.now());
    em.merge(e);
  }

  @Override
  public void markFailed(IdempotencyKey key, String reason) {
    IdempotencyRecordEntity e =
        em.find(IdempotencyRecordEntity.class, key.value(), LockModeType.PESSIMISTIC_WRITE);
    if (e == null) {
      return;
    }
    e.setStatus(IdempotencyStatus.FAILED);
    e.setLastError(reason);
    e.setUpdatedAt(Instant.now());
    em.merge(e);
  }
}
