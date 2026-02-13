package com.marcusprado02.commons.app.idempotency.service;

import com.marcusprado02.commons.app.idempotency.model.IdempotencyKey;
import com.marcusprado02.commons.app.idempotency.port.IdempotencyStorePort;
import java.time.Duration;
import java.util.Objects;
import java.util.function.Supplier;

public final class DefaultIdempotentExecutor implements IdempotentExecutor {

  private final IdempotencyService service;

  public DefaultIdempotentExecutor(IdempotencyStorePort store) {
    this(store, Duration.ofMinutes(5));
  }

  public DefaultIdempotentExecutor(IdempotencyStorePort store, Duration defaultTtl) {
    Objects.requireNonNull(store, "store must not be null");
    this.service = new DefaultIdempotencyService(store, defaultTtl);
  }

  @Override
  public <T> IdempotencyResult<T> execute(IdempotencyKey key, Duration ttl, Supplier<T> action) {
    return service.execute(key, ttl, action);
  }
}
