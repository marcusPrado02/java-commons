package com.marcusprado02.commons.app.idempotency.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.marcusprado02.commons.app.idempotency.model.IdempotencyKey;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class IdempotencyHttpTest {

  @Test
  void shouldResolveFromHeaderValue() {
    Optional<IdempotencyKey> key = IdempotencyHttp.resolveFromHeaderValue("  abc  ");
    assertTrue(key.isPresent());
    assertEquals("abc", key.get().value());
  }

  @Test
  void shouldIgnoreBlankHeaderValue() {
    assertTrue(IdempotencyHttp.resolveFromHeaderValue("   ").isEmpty());
  }

  @Test
  void shouldResolveFromHeadersProvider() {
    Map<String, String> headers = Map.of(IdempotencyHttp.IDEMPOTENCY_KEY_HEADER, "k1");

    Optional<IdempotencyKey> key =
        IdempotencyHttp.resolveFromHeaders(name -> Optional.ofNullable(headers.get(name)));

    assertFalse(key.isEmpty());
    assertEquals("k1", key.get().value());
  }
}
