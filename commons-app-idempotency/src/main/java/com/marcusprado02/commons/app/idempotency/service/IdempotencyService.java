package com.marcusprado02.commons.app.idempotency.service;

import com.marcusprado02.commons.app.idempotency.model.IdempotencyKey;
import java.time.Duration;
import java.util.function.Function;
import java.util.function.Supplier;

public interface IdempotencyService {

  <T> IdempotencyResult<T> execute(IdempotencyKey key, Duration ttl, Supplier<T> action);

  <T> IdempotencyResult<T> execute(
      IdempotencyKey key, Duration ttl, Supplier<T> action, Function<T, String> resultRefMapper);
}
