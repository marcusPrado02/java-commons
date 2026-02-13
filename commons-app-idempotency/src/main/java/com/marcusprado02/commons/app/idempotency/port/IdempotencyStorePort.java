package com.marcusprado02.commons.app.idempotency.port;

import com.marcusprado02.commons.app.idempotency.model.IdempotencyKey;
import com.marcusprado02.commons.app.idempotency.model.IdempotencyRecord;
import java.time.Duration;
import java.util.Optional;

public interface IdempotencyStorePort {

  Optional<IdempotencyRecord> find(IdempotencyKey key);

  /**
    * Try to acquire an idempotency key.
    *
    * <p>Implementations should create (or transition) an {@code IN_PROGRESS} record when:
    *
    * <ul>
    *   <li>the key does not exist, or
    *   <li>the existing record is expired (based on the provided TTL)
    * </ul>
    *
    * <p>When the record exists and is not expired, this call should return {@code false}.
   *
   * @return true if acquired, false if already exists
   */
  boolean tryAcquire(IdempotencyKey key, Duration ttl);

  void markCompleted(IdempotencyKey key, String resultRef);

  void markFailed(IdempotencyKey key, String reason);
}
