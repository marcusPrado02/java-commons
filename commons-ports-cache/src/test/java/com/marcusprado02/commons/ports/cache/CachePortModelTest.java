package com.marcusprado02.commons.ports.cache;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CachePortModelTest {

  @Test
  void cacheValue_of_without_expiry_is_not_expired() {
    CacheValue<String> v = CacheValue.of("hello");
    assertEquals("hello", v.value());
    assertNotNull(v.cachedAt());
    assertEquals(Optional.empty(), v.expiresAt());
    assertFalse(v.isExpired());
  }

  @Test
  void cacheValue_of_with_past_expiry_is_expired() {
    CacheValue<String> v = CacheValue.of("x", Instant.now().minusSeconds(10));
    assertTrue(v.isExpired());
  }

  @Test
  void cacheValue_of_with_future_expiry_is_not_expired() {
    CacheValue<String> v = CacheValue.of("x", Instant.now().plusSeconds(60));
    assertFalse(v.isExpired());
  }

  @Test
  void cacheValue_constructor_rejects_null_value() {
    assertThrows(NullPointerException.class,
        () -> new CacheValue<>(null, Instant.now(), Optional.empty()));
  }

  @Test
  void cacheValue_constructor_rejects_null_cachedAt() {
    assertThrows(NullPointerException.class,
        () -> new CacheValue<>("v", null, Optional.empty()));
  }

  @Test
  void cacheValue_constructor_rejects_null_expiresAt() {
    assertThrows(NullPointerException.class,
        () -> new CacheValue<>("v", Instant.now(), null));
  }

  @Test
  void cacheKey_of_with_namespace_and_key() {
    CacheKey k = CacheKey.of("user", "123");
    assertEquals("user", k.namespace());
    assertEquals("123", k.key());
    assertEquals("user:123", k.toFullKey());
    assertEquals("user:123", k.toString());
  }

  @Test
  void cacheKey_of_with_key_only_uses_default_namespace() {
    CacheKey k = CacheKey.of("abc");
    assertEquals("default", k.namespace());
    assertEquals("abc", k.key());
    assertEquals("default:abc", k.toFullKey());
  }

  @Test
  void cacheKey_rejects_null_namespace() {
    assertThrows(NullPointerException.class, () -> new CacheKey(null, "k"));
  }

  @Test
  void cacheKey_rejects_null_key() {
    assertThrows(NullPointerException.class, () -> CacheKey.of("ns", null));
  }
}
