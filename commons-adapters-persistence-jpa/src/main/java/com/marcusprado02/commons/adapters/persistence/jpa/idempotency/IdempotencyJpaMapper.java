package com.marcusprado02.commons.adapters.persistence.jpa.idempotency;

import com.marcusprado02.commons.app.idempotency.model.IdempotencyKey;
import com.marcusprado02.commons.app.idempotency.model.IdempotencyRecord;

public final class IdempotencyJpaMapper {

  private IdempotencyJpaMapper() {}

  public static IdempotencyRecord toModel(IdempotencyRecordEntity e) {
    return new IdempotencyRecord(
        new IdempotencyKey(e.getKey()),
        e.getStatus(),
        e.getCreatedAt(),
        e.getUpdatedAt(),
        e.getResultRef());
  }
}
