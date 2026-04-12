package com.marcusprado02.commons.adapters.secrets.vault;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.marcusprado02.commons.ports.secrets.SecretKey;
import com.marcusprado02.commons.ports.secrets.SecretValue;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.vault.core.VaultTemplate;

/** Unit tests covering error paths and uncovered branches in VaultSecretStoreAdapter. */
@ExtendWith(MockitoExtension.class)
class VaultSecretStoreAdapterUnitTest {

  @Mock VaultTemplate vaultTemplate;

  VaultSecretStoreAdapter adapter;

  @BeforeEach
  void setUp() {
    adapter = new VaultSecretStoreAdapter(vaultTemplate);
  }

  // ── get(key) error paths ──────────────────────────────────────────────────

  @Test
  void shouldReturnEmptyWhenGetReturnsNullResponse() {
    when(vaultTemplate.read(anyString(), eq(Map.class))).thenReturn(null);

    var result = adapter.get(SecretKey.of("some/key"));

    assertFalse(result.isPresent());
  }

  @Test
  void shouldReturnEmptyWhenGetThrowsException() {
    when(vaultTemplate.read(anyString(), eq(Map.class)))
        .thenThrow(new RuntimeException("vault unavailable"));

    var result = adapter.get(SecretKey.of("some/key"));

    assertFalse(result.isPresent());
  }

  // ── get(key, version) error paths ────────────────────────────────────────

  @Test
  void shouldReturnEmptyWhenGetVersionReturnsNullResponse() {
    when(vaultTemplate.read(anyString(), eq(Map.class))).thenReturn(null);

    var result = adapter.get(SecretKey.of("some/key"), "1");

    assertFalse(result.isPresent());
  }

  @Test
  void shouldReturnEmptyWhenGetVersionThrowsException() {
    when(vaultTemplate.read(anyString(), eq(Map.class)))
        .thenThrow(new RuntimeException("vault unavailable"));

    var result = adapter.get(SecretKey.of("some/key"), "1");

    assertFalse(result.isPresent());
  }

  // ── put(key, value) error path ────────────────────────────────────────────

  @Test
  void shouldThrowRuntimeExceptionWhenPutValueFails() {
    doThrow(new RuntimeException("vault write error"))
        .when(vaultTemplate)
        .write(anyString(), any());

    assertThrows(
        RuntimeException.class,
        () -> adapter.put(SecretKey.of("some/key"), SecretValue.of("value")));
  }

  // ── put(key, map) error path ──────────────────────────────────────────────

  @Test
  void shouldThrowRuntimeExceptionWhenPutMapFails() {
    doThrow(new RuntimeException("vault write error"))
        .when(vaultTemplate)
        .write(anyString(), any());

    assertThrows(
        RuntimeException.class, () -> adapter.put(SecretKey.of("some/key"), Map.of("k", "v")));
  }

  // ── delete error path ─────────────────────────────────────────────────────

  @Test
  void shouldReturnFalseWhenDeleteThrowsException() {
    doThrow(new RuntimeException("vault delete error")).when(vaultTemplate).delete(anyString());

    var result = adapter.delete(SecretKey.of("some/key"));

    assertFalse(result);
  }

  // ── list error paths ──────────────────────────────────────────────────────

  @Test
  void shouldReturnEmptyListWhenListReturnsNullResponse() {
    when(vaultTemplate.read(anyString(), eq(Map.class))).thenReturn(null);

    var result = adapter.list("some/prefix");

    assertTrue(result.isEmpty());
  }

  @Test
  void shouldReturnEmptyListWhenListThrowsException() {
    when(vaultTemplate.read(anyString(), eq(Map.class)))
        .thenThrow(new RuntimeException("vault unavailable"));

    var result = adapter.list("some/prefix");

    assertTrue(result.isEmpty());
  }

  // ── resolvePath: key with leading slash ───────────────────────────────────

  @Test
  void shouldHandleKeyWithLeadingSlash() {
    when(vaultTemplate.read(anyString(), eq(Map.class))).thenReturn(null);

    var result = adapter.get(SecretKey.of("/leading/slash/key"));

    assertFalse(result.isPresent());
  }
}
