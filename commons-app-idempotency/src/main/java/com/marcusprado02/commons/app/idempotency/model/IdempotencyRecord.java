package com.marcusprado02.commons.app.idempotency.model;

import java.time.Instant;

/** Persisted record tracking the state and result of an idempotent operation. */
public record IdempotencyRecord(
    IdempotencyKey key,
    IdempotencyStatus status,
    Instant createdAt,
    Instant updatedAt,
    Instant expiresAt,
    String resultRef,
    String lastError) {}
