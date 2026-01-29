package com.marcusprado02.commons.app.idempotency.port;

import com.marcusprado02.commons.app.idempotency.model.IdempotencyKey;
import com.marcusprado02.commons.app.idempotency.model.IdempotencyRecord;
import java.time.Duration;
import java.util.Optional;

public interface IdempotencyStorePort {

  Optional<IdempotencyRecord> find(IdempotencyKey key);

  /**
   * Try to create an IN_PROGRESS record if absent.
   *
   * @return true if acquired, false if already exists
   */
  boolean tryAcquire(IdempotencyKey key, Duration ttl);

  void markCompleted(IdempotencyKey key, String resultRef);

  void markFailed(IdempotencyKey key, String reason);
}
