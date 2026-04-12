package com.marcusprado02.commons.adapters.secrets.azure;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.azure.core.exception.ResourceNotFoundException;
import com.azure.core.http.rest.PagedIterable;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import com.azure.security.keyvault.secrets.models.SecretProperties;
import com.marcusprado02.commons.ports.secrets.SecretKey;
import com.marcusprado02.commons.ports.secrets.SecretValue;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests covering error paths and uncovered branches in AzureKeyVaultSecretStoreAdapter. */
@ExtendWith(MockitoExtension.class)
class AzureKeyVaultSecretStoreAdapterUnitTest {

  @Mock private SecretClient secretClient;

  private AzureKeyVaultSecretStoreAdapter adapter;

  @BeforeEach
  void setUp() {
    adapter = new AzureKeyVaultSecretStoreAdapter(secretClient);
  }

  // ── get(key) error paths ──────────────────────────────────────────────────

  @Test
  void shouldReturnEmptyWhenGetThrowsGenericException() {
    when(secretClient.getSecret(anyString())).thenThrow(new RuntimeException("network error"));

    var result = adapter.get(SecretKey.of("some-key"));

    assertFalse(result.isPresent());
  }

  // ── get(key, version) error paths ────────────────────────────────────────

  @Test
  void shouldReturnEmptyWhenGetVersionThrowsResourceNotFoundException() {
    when(secretClient.getSecret(anyString(), anyString()))
        .thenThrow(new ResourceNotFoundException("not found", null));

    var result = adapter.get(SecretKey.of("some-key"), "v99");

    assertFalse(result.isPresent());
  }

  @Test
  void shouldReturnEmptyWhenGetVersionThrowsGenericException() {
    when(secretClient.getSecret(anyString(), anyString()))
        .thenThrow(new RuntimeException("vault unavailable"));

    var result = adapter.get(SecretKey.of("some-key"), "v1");

    assertFalse(result.isPresent());
  }

  // ── put(key, value) error path ────────────────────────────────────────────

  @Test
  void shouldThrowWhenPutValueThrowsException() {
    when(secretClient.setSecret(any(KeyVaultSecret.class)))
        .thenThrow(new RuntimeException("write error"));

    assertThrows(
        RuntimeException.class,
        () -> adapter.put(SecretKey.of("some-key"), SecretValue.of("value")));
  }

  // ── put(key, map) error paths ─────────────────────────────────────────────

  @Test
  void shouldThrowWhenPutMapThrowsException() {
    when(secretClient.setSecret(any(KeyVaultSecret.class)))
        .thenThrow(new RuntimeException("write error"));

    assertThrows(
        RuntimeException.class, () -> adapter.put(SecretKey.of("some-key"), Map.of("k", "v")));
  }

  @Test
  void shouldNotSetTagsWhenMapHasMoreThan15Entries() {
    // Data with > 15 entries: the tags branch is skipped
    Map<String, String> bigMap = new HashMap<>();
    for (int i = 0; i < 16; i++) {
      bigMap.put("key" + i, "val" + i);
    }

    KeyVaultSecret storedSecret = mock(KeyVaultSecret.class);
    SecretProperties props = mock(SecretProperties.class);
    when(storedSecret.getProperties()).thenReturn(props);
    when(props.getVersion()).thenReturn("v1");
    when(secretClient.setSecret(any(KeyVaultSecret.class))).thenReturn(storedSecret);

    String version = adapter.put(SecretKey.of("big-map"), bigMap);

    assertEquals("v1", version);
    // setTags should NOT have been called since size > 15
    verify(props, never()).setTags(any());
  }

  // ── delete error paths ────────────────────────────────────────────────────

  @Test
  void shouldReturnFalseWhenDeleteThrowsGenericException() {
    when(secretClient.beginDeleteSecret(anyString()))
        .thenThrow(new RuntimeException("vault error"));

    boolean result = adapter.delete(SecretKey.of("some-key"));

    assertFalse(result);
  }

  // ── exists error path ─────────────────────────────────────────────────────

  @Test
  void shouldReturnFalseWhenExistsThrowsGenericException() {
    when(secretClient.getSecret(anyString())).thenThrow(new RuntimeException("vault error"));

    boolean result = adapter.exists(SecretKey.of("some-key"));

    assertFalse(result);
  }

  // ── list error paths ──────────────────────────────────────────────────────

  @Test
  void shouldReturnEmptyListWhenListThrowsException() {
    when(secretClient.listPropertiesOfSecrets()).thenThrow(new RuntimeException("vault error"));

    var result = adapter.list("some-prefix");

    assertTrue(result.isEmpty());
  }

  @Test
  void shouldReturnAllSecretsWhenListPrefixIsBlank() {
    SecretProperties props1 = mock(SecretProperties.class);
    SecretProperties props2 = mock(SecretProperties.class);
    when(props1.getName()).thenReturn("app-secret");
    when(props2.getName()).thenReturn("db-password");
    List<SecretProperties> allProps = Arrays.asList(props1, props2);

    @SuppressWarnings("unchecked")
    PagedIterable<SecretProperties> pagedIterable = mock(PagedIterable.class);
    when(pagedIterable.stream()).thenAnswer(inv -> allProps.stream());
    when(secretClient.listPropertiesOfSecrets()).thenReturn(pagedIterable);

    var result = adapter.list("   ");

    assertEquals(2, result.size());
  }

  // ── extractSecretValue: null createdOn and expiresOn branches ────────────

  @Test
  void shouldHandleNullCreatedOnAndExpiresOn() {
    KeyVaultSecret secret = mock(KeyVaultSecret.class);
    SecretProperties props = mock(SecretProperties.class);

    when(secret.getValue()).thenReturn("my-value");
    when(secret.getProperties()).thenReturn(props);
    when(props.getVersion()).thenReturn("v1");
    when(props.getCreatedOn()).thenReturn(null); // null branch in ternary
    when(props.getExpiresOn()).thenReturn(null); // null branch in ternary

    when(secretClient.getSecret("my-key")).thenReturn(secret);

    var result = adapter.get(SecretKey.of("my-key"));

    assertTrue(result.isPresent());
    assertEquals("my-value", result.get().asString());
  }

  @Test
  void shouldHandleNonNullExpiresOn() {
    KeyVaultSecret secret = mock(KeyVaultSecret.class);
    SecretProperties props = mock(SecretProperties.class);

    when(secret.getValue()).thenReturn("expiring-value");
    when(secret.getProperties()).thenReturn(props);
    when(props.getVersion()).thenReturn("v1");
    when(props.getCreatedOn()).thenReturn(OffsetDateTime.now());
    when(props.getExpiresOn()).thenReturn(OffsetDateTime.now().plusHours(1));

    when(secretClient.getSecret("expiring-key")).thenReturn(secret);

    var result = adapter.get(SecretKey.of("expiring-key"));

    assertTrue(result.isPresent());
    assertTrue(result.get().expiresAt().isPresent());
  }

  // ── normalizeSecretName: long name truncation ────────────────────────────

  @Test
  void shouldTruncateSecretNameLongerThan127Chars() {
    String longName = "a".repeat(200);
    SecretKey key = SecretKey.of(longName);

    KeyVaultSecret storedSecret = mock(KeyVaultSecret.class);
    SecretProperties props = mock(SecretProperties.class);
    when(storedSecret.getProperties()).thenReturn(props);
    when(props.getVersion()).thenReturn("v1");
    when(secretClient.setSecret(any(KeyVaultSecret.class))).thenReturn(storedSecret);

    adapter.put(key, SecretValue.of("value"));

    verify(secretClient).setSecret(argThat(s -> s.getName().length() == 127));
  }

  // ── denormalizeSecretName: s- prefix in list ─────────────────────────────

  @Test
  void shouldDenormalizeSecretNameWithSPrefix() {
    SecretProperties props = mock(SecretProperties.class);
    // "s-123abc" was normalized from "123abc" (started with a digit)
    when(props.getName()).thenReturn("s-123abc");

    @SuppressWarnings("unchecked")
    PagedIterable<SecretProperties> pagedIterable = mock(PagedIterable.class);
    when(pagedIterable.stream()).thenAnswer(inv -> Collections.singletonList(props).stream());
    when(secretClient.listPropertiesOfSecrets()).thenReturn(pagedIterable);

    var result = adapter.list(null);

    assertEquals(1, result.size());
    // Should be denormalized back to "123abc"
    assertEquals("123abc", result.get(0).value());
  }

  @Test
  void shouldNotDenormalizeNameWithSPrefixNotFollowedByDigit() {
    SecretProperties props = mock(SecretProperties.class);
    // "s-abc" was NOT normalized — it already started with a letter
    when(props.getName()).thenReturn("s-abc");

    @SuppressWarnings("unchecked")
    PagedIterable<SecretProperties> pagedIterable = mock(PagedIterable.class);
    when(pagedIterable.stream()).thenAnswer(inv -> Collections.singletonList(props).stream());
    when(secretClient.listPropertiesOfSecrets()).thenReturn(pagedIterable);

    var result = adapter.list(null);

    assertEquals(1, result.size());
    // Should NOT be denormalized — 's' after "s-" is not a digit
    assertEquals("s-abc", result.get(0).value());
  }
}
