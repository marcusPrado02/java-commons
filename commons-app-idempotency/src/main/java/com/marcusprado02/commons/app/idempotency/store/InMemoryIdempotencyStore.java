package com.marcusprado02.commons.app.idempotency.store;

import com.marcusprado02.commons.app.idempotency.model.IdempotencyKey;
import com.marcusprado02.commons.app.idempotency.model.IdempotencyRecord;
import com.marcusprado02.commons.app.idempotency.model.IdempotencyStatus;
import com.marcusprado02.commons.app.idempotency.port.IdempotencyStorePort;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class InMemoryIdempotencyStore implements IdempotencyStorePort {

  private static final Duration DEFAULT_TTL = Duration.ofMinutes(5);

  private final Clock clock;
  private final ConcurrentMap<String, IdempotencyRecord> records = new ConcurrentHashMap<>();

  public InMemoryIdempotencyStore() {
    this(Clock.systemUTC());
  }

  public InMemoryIdempotencyStore(Clock clock) {
    this.clock = Objects.requireNonNull(clock, "clock must not be null");
  }

  @Override
  public Optional<IdempotencyRecord> find(IdempotencyKey key) {
    Objects.requireNonNull(key, "key must not be null");

    Instant now = clock.instant();
    IdempotencyRecord record = records.get(key.value());
    if (record == null) {
      return Optional.empty();
    }

    if (isExpired(record, now)) {
      records.remove(key.value(), record);
      return Optional.empty();
    }

    return Optional.of(record);
  }

  @Override
  public boolean tryAcquire(IdempotencyKey key, Duration ttl) {
    Objects.requireNonNull(key, "key must not be null");
    Objects.requireNonNull(ttl, "ttl must not be null");

    Duration safeTtl = ttl.isNegative() || ttl.isZero() ? DEFAULT_TTL : ttl;

    Instant now = clock.instant();
    Instant expiresAt = now.plus(safeTtl);

    AtomicBoolean acquired = new AtomicBoolean(false);
    records.compute(
        key.value(),
        (k, existing) -> {
          if (existing == null || isExpired(existing, now)) {
            acquired.set(true);
            return new IdempotencyRecord(
                key, IdempotencyStatus.IN_PROGRESS, now, now, expiresAt, null, null);
          }
          return existing;
        });

    return acquired.get();
  }

  @Override
  public void markCompleted(IdempotencyKey key, String resultRef) {
    Objects.requireNonNull(key, "key must not be null");

    Instant now = clock.instant();
    records.computeIfPresent(
        key.value(),
        (k, existing) ->
            new IdempotencyRecord(
                existing.key(),
                IdempotencyStatus.COMPLETED,
                existing.createdAt(),
                now,
                existing.expiresAt(),
                resultRef,
                null));
  }

  @Override
  public void markFailed(IdempotencyKey key, String reason) {
    Objects.requireNonNull(key, "key must not be null");

    Instant now = clock.instant();
    records.computeIfPresent(
        key.value(),
        (k, existing) ->
            new IdempotencyRecord(
                existing.key(),
                IdempotencyStatus.FAILED,
                existing.createdAt(),
                now,
                existing.expiresAt(),
                existing.resultRef(),
                reason));
  }

  private static boolean isExpired(IdempotencyRecord record, Instant now) {
    return record.expiresAt() != null && !record.expiresAt().isAfter(now);
  }
}
