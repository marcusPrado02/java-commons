package com.marcusprado02.commons.ports.secrets;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class SecretsModelTest {

  @Test
  void secretKey_of_valid_value() {
    SecretKey k = SecretKey.of("my/secret/path");
    assertEquals("my/secret/path", k.value());
    assertEquals("my/secret/path", k.toString());
  }

  @Test
  void secretKey_rejects_null() {
    assertThrows(NullPointerException.class, () -> SecretKey.of(null));
  }

  @Test
  void secretKey_rejects_blank() {
    assertThrows(IllegalArgumentException.class, () -> SecretKey.of("  "));
  }

  @Test
  void secretValue_of_string_stores_value() {
    SecretValue v = SecretValue.of("mysecret");
    assertEquals("mysecret", v.asString());
    assertNotNull(v.createdAt());
    assertTrue(v.version().isEmpty());
    assertTrue(v.expiresAt().isEmpty());
    assertFalse(v.isExpired());
  }

  @Test
  void secretValue_of_string_with_version() {
    SecretValue v = SecretValue.of("secret", "v2");
    assertEquals("secret", v.asString());
    assertTrue(v.version().isPresent());
    assertEquals("v2", v.version().get());
  }

  @Test
  void secretValue_of_bytes() {
    byte[] data = "data".getBytes();
    SecretValue v = SecretValue.of(data);
    assertArrayEquals(data, v.asBytes());
  }

  @Test
  void secretValue_of_full_constructor_with_expiry() {
    Instant past = Instant.now().minusSeconds(10);
    SecretValue v = SecretValue.of("s".getBytes(), "v1", Instant.now().minusSeconds(60), past);
    assertTrue(v.isExpired());
    assertTrue(v.expiresAt().isPresent());
  }

  @Test
  void secretValue_not_expired_with_future_expiry() {
    SecretValue v =
        SecretValue.of("s".getBytes(), "v1", Instant.now(), Instant.now().plusSeconds(60));
    assertFalse(v.isExpired());
  }

  @Test
  void secretValue_close_zeroes_data() {
    SecretValue v = SecretValue.of("secret");
    v.close();
    assertThrows(IllegalStateException.class, v::asString);
    assertThrows(IllegalStateException.class, v::asBytes);
  }

  @Test
  void secretValue_double_close_is_safe() {
    SecretValue v = SecretValue.of("s");
    v.close();
    assertDoesNotThrow(v::close);
  }
}
