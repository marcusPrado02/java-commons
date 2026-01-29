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
    return e == null ? Optional.empty() : Optional.of(IdempotencyJpaMapper.toModel(e));
  }

  @Override
  public boolean tryAcquire(IdempotencyKey key, Duration ttl) {
    // TTL será usado depois para cleanup/expiração. Aqui é “acquire” via insert.
    Instant now = Instant.now();

    IdempotencyRecordEntity e = new IdempotencyRecordEntity();
    e.setKey(key.value());
    e.setStatus(IdempotencyStatus.IN_PROGRESS);
    e.setCreatedAt(now);
    e.setUpdatedAt(now);

    try {
      em.persist(e);
      return true;
    } catch (PersistenceException ex) {
      // já existe (unique/PK)
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
