package com.marcusprado02.commons.app.idempotency.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class IdempotencyKeyTest {

  @Test
  void shouldTrimAndNormalize() {
    IdempotencyKey key = new IdempotencyKey("  abc  ");
    assertEquals("abc", key.value());
  }

  @Test
  void shouldRejectBlank() {
    assertThrows(IllegalArgumentException.class, () -> new IdempotencyKey("   "));
  }

  @Test
  void shouldRejectTooLong() {
    String longValue = "x".repeat(161);
    assertThrows(IllegalArgumentException.class, () -> new IdempotencyKey(longValue));
  }
}
