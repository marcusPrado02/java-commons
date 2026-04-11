package com.marcusprado02.commons.app.idempotency.http;

import com.marcusprado02.commons.app.idempotency.model.IdempotencyKey;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/** Framework-agnostic helpers for HTTP idempotency key handling. */
public final class IdempotencyHttp {

  public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

  private IdempotencyHttp() {}

  /**
   * Resolves an idempotency key from a raw header value string.
   *
   * @param headerValue the raw header value
   * @return an Optional containing the key, or empty if absent or blank
   */
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

  /**
   * Resolves an idempotency key using a header provider function.
   *
   * @param headerProvider function to look up header values by name
   * @return an Optional containing the key, or empty if the header is absent or blank
   */
  public static Optional<IdempotencyKey> resolveFromHeaders(
      Function<String, Optional<String>> headerProvider) {
    Objects.requireNonNull(headerProvider, "headerProvider must not be null");

    return headerProvider
        .apply(IDEMPOTENCY_KEY_HEADER)
        .flatMap(IdempotencyHttp::resolveFromHeaderValue);
  }
}
