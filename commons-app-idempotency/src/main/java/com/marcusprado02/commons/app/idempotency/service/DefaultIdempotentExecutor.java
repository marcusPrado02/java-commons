package com.marcusprado02.commons.app.idempotency.service;

import com.marcusprado02.commons.app.idempotency.model.IdempotencyKey;
import com.marcusprado02.commons.app.idempotency.model.IdempotencyRecord;
import com.marcusprado02.commons.app.idempotency.model.IdempotencyStatus;
import com.marcusprado02.commons.app.idempotency.port.IdempotencyStorePort;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public final class DefaultIdempotentExecutor implements IdempotentExecutor {

  private final IdempotencyStorePort store;

  public DefaultIdempotentExecutor(IdempotencyStorePort store) {
    this.store = Objects.requireNonNull(store);
  }

  @Override
  public <T> IdempotencyResult<T> execute(IdempotencyKey key, Duration ttl, Supplier<T> action) {
    Objects.requireNonNull(key, "key must not be null");
    Objects.requireNonNull(action, "action must not be null");

    Duration safeTtl =
        (ttl == null || ttl.isNegative() || ttl.isZero()) ? Duration.ofMinutes(5) : ttl;

    Optional<IdempotencyRecord> existing = store.find(key);
    if (existing.isPresent() && existing.get().status() == IdempotencyStatus.COMPLETED) {
      return new IdempotencyResult<>(false, existing.get().resultRef(), null);
    }

    boolean acquired = store.tryAcquire(key, safeTtl);
    if (!acquired) {
      // someone else owns it; caller can decide to retry/poll
      return new IdempotencyResult<>(false, null, null);
    }

    try {
      T value = action.get();
      // A forma de gerar resultRef será definida no uso (id do recurso criado, etc.)
      store.markCompleted(key, null);
      return new IdempotencyResult<>(true, null, value);
    } catch (RuntimeException ex) {
      store.markFailed(key, ex.getClass().getSimpleName());
      throw ex;
    }
  }
}
