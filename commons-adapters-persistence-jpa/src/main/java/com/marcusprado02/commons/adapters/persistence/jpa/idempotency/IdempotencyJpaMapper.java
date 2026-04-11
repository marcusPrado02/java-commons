package com.marcusprado02.commons.adapters.persistence.jpa.idempotency;

import com.marcusprado02.commons.app.idempotency.model.IdempotencyKey;
import com.marcusprado02.commons.app.idempotency.model.IdempotencyRecord;

/**
 * Maps between {@link com.marcusprado02.commons.app.idempotency.model.IdempotencyRecord} and JPA
 * entity.
 */
public final class IdempotencyJpaMapper {

  private IdempotencyJpaMapper() {}

  /**
   * Maps a JPA entity to the domain model.
   *
   * @param e the entity
   * @return domain record
   */
  public static IdempotencyRecord toModel(IdempotencyRecordEntity e) {
    return new IdempotencyRecord(
        new IdempotencyKey(e.getKey()),
        e.getStatus(),
        e.getCreatedAt(),
        e.getUpdatedAt(),
        e.getExpiresAt(),
        e.getResultRef(),
        e.getLastError());
  }
}
