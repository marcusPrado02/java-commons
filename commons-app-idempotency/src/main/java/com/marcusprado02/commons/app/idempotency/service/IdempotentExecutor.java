package com.marcusprado02.commons.app.idempotency.service;

import com.marcusprado02.commons.app.idempotency.model.IdempotencyKey;
import java.time.Duration;
import java.util.function.Supplier;

public interface IdempotentExecutor {

  <T> IdempotencyResult<T> execute(IdempotencyKey key, Duration ttl, Supplier<T> action);
}
