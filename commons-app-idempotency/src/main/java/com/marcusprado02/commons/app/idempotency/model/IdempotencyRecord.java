package com.marcusprado02.commons.app.idempotency.model;

import java.time.Instant;

public record IdempotencyRecord(
    IdempotencyKey key,
    IdempotencyStatus status,
    Instant createdAt,
    Instant updatedAt,
    Instant expiresAt,
    String resultRef,
    String lastError) {}
