package com.marcusprado02.commons.adapters.persistence.jpa.outbox;

import com.marcusprado02.commons.adapters.persistence.jpa.shared.JpaQueries;
import com.marcusprado02.commons.app.outbox.model.OutboxMessage;
import com.marcusprado02.commons.app.outbox.model.OutboxMessageId;
import com.marcusprado02.commons.app.outbox.model.OutboxStatus;
import com.marcusprado02.commons.app.outbox.port.OutboxRepositoryPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public final class JpaOutboxRepositoryAdapter implements OutboxRepositoryPort {

  private final EntityManager em;

  public JpaOutboxRepositoryAdapter(EntityManager em) {
    this.em = Objects.requireNonNull(em);
  }

  @Override
  public void append(OutboxMessage message) {
    em.persist(OutboxJpaMapper.toEntity(message));
  }

  @Override
  public List<OutboxMessage> fetchBatch(OutboxStatus status, int limit) {
    int safeLimit = JpaQueries.safeLimit(limit, 100);

    TypedQuery<OutboxMessageEntity> q =
        em.createQuery(
            "select o from OutboxMessageEntity o "
                + "where o.status = :status "
                + "order by o.occurredAt asc",
            OutboxMessageEntity.class);
    q.setParameter("status", status);
    q.setMaxResults(safeLimit);

    return q.getResultList().stream().map(OutboxJpaMapper::toModel).toList();
  }

  @Override
  public void markPublished(OutboxMessageId id, Instant publishedAt) {
    OutboxMessageEntity e = em.find(OutboxMessageEntity.class, id.value());
    if (e == null) {
      return;
    }
    e.setStatus(OutboxStatus.PUBLISHED);
    e.setPublishedAt(publishedAt);
    e.setLastError(null);
    em.merge(e);
  }

  @Override
  public void markFailed(OutboxMessageId id, String reason, int attempts) {
    OutboxMessageEntity e = em.find(OutboxMessageEntity.class, id.value());
    if (e == null) {
      return;
    }
    e.setStatus(OutboxStatus.FAILED);
    e.setAttempts(attempts);
    e.setLastError(reason);
    em.merge(e);
  }

  @Override
  public void markDead(OutboxMessageId id, String reason, int attempts) {
    OutboxMessageEntity e = em.find(OutboxMessageEntity.class, id.value());
    if (e == null) {
      return;
    }
    e.setStatus(OutboxStatus.DEAD);
    e.setAttempts(attempts);
    e.setLastError(reason);
    em.merge(e);
  }
}
