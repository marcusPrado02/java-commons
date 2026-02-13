package com.marcusprado02.commons.app.idempotency.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class IdempotencyKeyTest {

  @Test
  void shouldTrimAndNormalize() {
    IdempotencyKey key = new IdempotencyKey("  abc  ");
    assertThat(key.value()).isEqualTo("abc");
  }

  @Test
  void shouldRejectBlank() {
    assertThatThrownBy(() -> new IdempotencyKey("   "))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRejectTooLong() {
    String longValue = "x".repeat(161);
    assertThatThrownBy(() -> new IdempotencyKey(longValue))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
