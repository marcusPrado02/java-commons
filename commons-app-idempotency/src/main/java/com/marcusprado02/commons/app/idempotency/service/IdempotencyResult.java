package com.marcusprado02.commons.app.idempotency.service;

public record IdempotencyResult<T>(boolean executed, String existingResultRef, T value) {}
