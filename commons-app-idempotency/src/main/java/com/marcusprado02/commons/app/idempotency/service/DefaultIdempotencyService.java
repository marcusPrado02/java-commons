package com.marcusprado02.commons.app.idempotency.service;

import com.marcusprado02.commons.app.idempotency.model.IdempotencyKey;
import com.marcusprado02.commons.app.idempotency.model.IdempotencyRecord;
import com.marcusprado02.commons.app.idempotency.model.IdempotencyStatus;
import com.marcusprado02.commons.app.idempotency.port.IdempotencyStorePort;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public final class DefaultIdempotencyService implements IdempotencyService {

  private static final Duration FALLBACK_DEFAULT_TTL = Duration.ofMinutes(5);

  private final IdempotencyStorePort store;
  private final Duration defaultTtl;

  public DefaultIdempotencyService(IdempotencyStorePort store, Duration defaultTtl) {
    this.store = Objects.requireNonNull(store, "store must not be null");
    this.defaultTtl = normalizeTtl(defaultTtl, FALLBACK_DEFAULT_TTL);
  }

  public DefaultIdempotencyService(IdempotencyStorePort store) {
    this(store, FALLBACK_DEFAULT_TTL);
  }

  @Override
  public <T> IdempotencyResult<T> execute(IdempotencyKey key, Duration ttl, Supplier<T> action) {
    return execute(key, ttl, action, ignored -> null);
  }

  @Override
  public <T> IdempotencyResult<T> execute(
      IdempotencyKey key, Duration ttl, Supplier<T> action, Function<T, String> resultRefMapper) {

    Objects.requireNonNull(key, "key must not be null");
    Objects.requireNonNull(action, "action must not be null");
    Objects.requireNonNull(resultRefMapper, "resultRefMapper must not be null");

    Duration safeTtl = normalizeTtl(ttl, defaultTtl);

    Optional<IdempotencyRecord> existing = store.find(key);
    if (existing.isPresent() && existing.get().status() == IdempotencyStatus.COMPLETED) {
      return new IdempotencyResult<>(false, existing.get().resultRef(), null);
    }

    boolean acquired = store.tryAcquire(key, safeTtl);
    if (!acquired) {
      return new IdempotencyResult<>(false, null, null);
    }

    try {
      T value = action.get();
      store.markCompleted(key, resultRefMapper.apply(value));
      return new IdempotencyResult<>(true, null, value);
    } catch (RuntimeException ex) {
      store.markFailed(key, ex.getClass().getSimpleName());
      throw ex;
    }
  }

  private static Duration normalizeTtl(Duration ttl, Duration fallback) {
    if (ttl == null || ttl.isNegative() || ttl.isZero()) {
      return fallback;
    }
    return ttl;
  }
}
