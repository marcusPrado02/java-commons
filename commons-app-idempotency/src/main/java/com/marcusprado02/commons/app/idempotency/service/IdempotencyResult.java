package com.marcusprado02.commons.app.idempotency.service;

/** Result of an idempotent execution, indicating whether the action was newly executed. */
public record IdempotencyResult<T>(boolean executed, String existingResultRef, T value) {}
