package com.marcusprado02.commons.adapters.secrets.azure;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.marcusprado02.commons.ports.secrets.SecretKey;
import com.marcusprado02.commons.ports.secrets.SecretStorePort;
import com.marcusprado02.commons.ports.secrets.SecretValue;
import java.time.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CachedSecretStorePortTest {

  @Test
  void shouldCacheWithinTtl() {
    SecretStorePort delegate = mock(SecretStorePort.class);
    Clock clock = Clock.fixed(Instant.parse("2026-02-13T00:00:00Z"), ZoneOffset.UTC);

    SecretKey key = SecretKey.of("k");
    when(delegate.get(key)).thenReturn(Optional.of(SecretValue.of("v1")));

    try (CachedSecretStorePort cached = new CachedSecretStorePort(delegate, Duration.ofSeconds(30), clock, null)) {
      Optional<SecretValue> first = cached.get(key);
      Optional<SecretValue> second = cached.get(key);

      assertTrue(first.isPresent());
      assertTrue(second.isPresent());
      assertEquals("v1", first.get().asString());
      assertEquals("v1", second.get().asString());

      verify(delegate, times(1)).get(key);
    }
  }

  @Test
  void shouldInvalidateOnPut() {
    SecretStorePort delegate = mock(SecretStorePort.class);
    Clock clock = Clock.systemUTC();

    SecretKey key = SecretKey.of("k");
    when(delegate.get(key))
        .thenReturn(Optional.of(SecretValue.of("v1")))
        .thenReturn(Optional.of(SecretValue.of("v2")));
    when(delegate.put(eq(key), any(SecretValue.class))).thenReturn("ver");

    try (CachedSecretStorePort cached = new CachedSecretStorePort(delegate, Duration.ofMinutes(5), clock, null)) {
      assertEquals("v1", cached.get(key).orElseThrow().asString());
      cached.put(key, SecretValue.of("new"));
      assertEquals("v2", cached.get(key).orElseThrow().asString());

      verify(delegate, times(2)).get(key);
      verify(delegate, times(1)).put(eq(key), any(SecretValue.class));
    }
  }
}
