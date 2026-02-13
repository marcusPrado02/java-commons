package com.marcusprado02.commons.app.idempotency.http;

import com.marcusprado02.commons.app.idempotency.model.IdempotencyKey;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/** Framework-agnostic helpers for HTTP idempotency key handling. */
public final class IdempotencyHttp {

  public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

  private IdempotencyHttp() {}

  public static Optional<IdempotencyKey> resolveFromHeaderValue(String headerValue) {
    if (headerValue == null) {
      return Optional.empty();
    }

    String trimmed = headerValue.trim();
    if (trimmed.isBlank()) {
      return Optional.empty();
    }

    return Optional.of(new IdempotencyKey(trimmed));
  }

  public static Optional<IdempotencyKey> resolveFromHeaders(
      Function<String, Optional<String>> headerProvider) {
    Objects.requireNonNull(headerProvider, "headerProvider must not be null");

    return headerProvider
        .apply(IDEMPOTENCY_KEY_HEADER)
        .flatMap(IdempotencyHttp::resolveFromHeaderValue);
  }
}
