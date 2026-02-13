package com.marcusprado02.commons.adapters.secrets.azure;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.azure.core.exception.ResourceNotFoundException;
import com.azure.core.http.rest.PagedIterable;
import com.azure.core.util.polling.SyncPoller;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.models.*;
import com.marcusprado02.commons.ports.secrets.SecretKey;
import com.marcusprado02.commons.ports.secrets.SecretValue;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AzureKeyVaultSecretStoreAdapterTest {

  @Mock private SecretClient secretClient;

  private AzureKeyVaultSecretStoreAdapter adapter;

  @BeforeEach
  void setUp() {
    adapter = new AzureKeyVaultSecretStoreAdapter(secretClient);
  }

  @Test
  void shouldPutAndGetSecret() {
    // Given
    SecretKey key = SecretKey.of("api-key");
    SecretValue value = SecretValue.of("my-secret-value");

    KeyVaultSecret storedSecret = mock(KeyVaultSecret.class);
    SecretProperties props = mock(SecretProperties.class);

    when(storedSecret.getValue()).thenReturn("my-secret-value");
    when(storedSecret.getProperties()).thenReturn(props);
    when(props.getVersion()).thenReturn("v1");
    when(props.getCreatedOn()).thenReturn(OffsetDateTime.now());

    when(secretClient.setSecret(any(KeyVaultSecret.class))).thenReturn(storedSecret);
    when(secretClient.getSecret("api-key")).thenReturn(storedSecret);

    // When
    String version = adapter.put(key, value);
    Optional<SecretValue> retrieved = adapter.get(key);

    // Then
    assertEquals("v1", version);
    assertTrue(retrieved.isPresent());
    assertEquals("my-secret-value", retrieved.get().asString());
  }

  @Test
  void shouldPutAndGetSecretMap() {
    // Given
    SecretKey key = SecretKey.of("db-config");
    Map<String, String> data = new HashMap<>();
    data.put("host", "localhost");
    data.put("port", "5432");

    KeyVaultSecret storedSecret = mock(KeyVaultSecret.class);
    SecretProperties props = mock(SecretProperties.class);

    when(storedSecret.getValue()).thenReturn("{host=localhost,port=5432}");
    when(storedSecret.getProperties()).thenReturn(props);
    when(props.getVersion()).thenReturn("v1");
    when(props.getCreatedOn()).thenReturn(OffsetDateTime.now());

    when(secretClient.setSecret(any(KeyVaultSecret.class))).thenReturn(storedSecret);
    when(secretClient.getSecret("db-config")).thenReturn(storedSecret);

    // When
    String version = adapter.put(key, data);
    Optional<SecretValue> retrieved = adapter.get(key);

    // Then
    assertEquals("v1", version);
    assertTrue(retrieved.isPresent());
    assertTrue(retrieved.get().asString().contains("localhost"));
  }

  @Test
  void shouldReturnEmptyForNonExistentSecret() {
    // Given
    SecretKey key = SecretKey.of("nonexistent");

    when(secretClient.getSecret("nonexistent"))
        .thenThrow(new ResourceNotFoundException("Not found", null));

    // When
    Optional<SecretValue> result = adapter.get(key);

    // Then
    assertFalse(result.isPresent());
  }

  @Test
  void shouldDeleteSecret() {
    // Given
    SecretKey key = SecretKey.of("old-secret");

    @SuppressWarnings("unchecked")
    SyncPoller<DeletedSecret, Void> poller = mock(SyncPoller.class);
    when(secretClient.beginDeleteSecret("old-secret")).thenReturn(poller);
    when(poller.waitForCompletion()).thenReturn(null);

    // When
    boolean result = adapter.delete(key);

    // Then
    assertTrue(result);
    verify(secretClient).beginDeleteSecret("old-secret");
  }

  @Test
  void shouldReturnFalseWhenDeletingNonExistentSecret() {
    // Given
    SecretKey key = SecretKey.of("nonexistent");

    when(secretClient.beginDeleteSecret("nonexistent"))
        .thenThrow(new ResourceNotFoundException("Not found", null));

    // When
    boolean result = adapter.delete(key);

    // Then
    assertFalse(result);
  }

  @Test
  void shouldCheckExistence() {
    // Given
    KeyVaultSecret existingSecret = mock(KeyVaultSecret.class);
    when(secretClient.getSecret("existing")).thenReturn(existingSecret);
    when(secretClient.getSecret("nonexisting"))
        .thenThrow(new ResourceNotFoundException("Not found", null));

    // When/Then
    assertTrue(adapter.exists(SecretKey.of("existing")));
    assertFalse(adapter.exists(SecretKey.of("nonexisting")));
  }

  @Test
  void shouldUpdateSecret() {
    // Given
    SecretKey key = SecretKey.of("updatable");
    SecretValue oldValue = SecretValue.of("old-value");
    SecretValue newValue = SecretValue.of("new-value");

    KeyVaultSecret oldSecret = mock(KeyVaultSecret.class);
    KeyVaultSecret newSecret = mock(KeyVaultSecret.class);
    SecretProperties oldProps = mock(SecretProperties.class);
    SecretProperties newProps = mock(SecretProperties.class);

    when(oldSecret.getProperties()).thenReturn(oldProps);
    when(oldProps.getVersion()).thenReturn("v1");

    when(newSecret.getValue()).thenReturn("new-value");
    when(newSecret.getProperties()).thenReturn(newProps);
    when(newProps.getVersion()).thenReturn("v2");
    when(newProps.getCreatedOn()).thenReturn(OffsetDateTime.now());

    when(secretClient.setSecret(any(KeyVaultSecret.class))).thenReturn(oldSecret, newSecret);
    when(secretClient.getSecret("updatable")).thenReturn(newSecret);

    // When
    adapter.put(key, oldValue);
    String newVersion = adapter.put(key, newValue);
    Optional<SecretValue> retrieved = adapter.get(key);

    // Then
    assertEquals("v2", newVersion);
    assertTrue(retrieved.isPresent());
    assertEquals("new-value", retrieved.get().asString());
  }

  @Test
  void shouldHandleVersionedSecrets() {
    // Given
    SecretKey key = SecretKey.of("versioned");

    KeyVaultSecret v1Secret = mock(KeyVaultSecret.class);
    KeyVaultSecret v2Secret = mock(KeyVaultSecret.class);
    SecretProperties v1Props = mock(SecretProperties.class);
    SecretProperties v2Props = mock(SecretProperties.class);

    when(v1Secret.getValue()).thenReturn("value-v1");
    when(v1Secret.getProperties()).thenReturn(v1Props);
    when(v1Props.getVersion()).thenReturn("v1");
    when(v1Props.getCreatedOn()).thenReturn(OffsetDateTime.now());

    when(v2Secret.getValue()).thenReturn("value-v2");
    when(v2Secret.getProperties()).thenReturn(v2Props);
    when(v2Props.getVersion()).thenReturn("v2");
    when(v2Props.getCreatedOn()).thenReturn(OffsetDateTime.now());

    when(secretClient.getSecret("versioned", "v1")).thenReturn(v1Secret);
    when(secretClient.getSecret("versioned", "v2")).thenReturn(v2Secret);

    // When
    Optional<SecretValue> retrievedV1 = adapter.get(key, "v1");
    Optional<SecretValue> retrievedV2 = adapter.get(key, "v2");

    // Then
    assertTrue(retrievedV1.isPresent());
    assertEquals("value-v1", retrievedV1.get().asString());

    assertTrue(retrievedV2.isPresent());
    assertEquals("value-v2", retrievedV2.get().asString());
  }

  @Test
  void shouldListSecrets() {
    // Given
    SecretProperties props1 = mock(SecretProperties.class);
    SecretProperties props2 = mock(SecretProperties.class);
    SecretProperties props3 = mock(SecretProperties.class);

    when(props1.getName()).thenReturn("app-key-1");
    when(props2.getName()).thenReturn("app-key-2");
    when(props3.getName()).thenReturn("db-password");

    List<SecretProperties> allProps = Arrays.asList(props1, props2, props3);

    @SuppressWarnings("unchecked")
    PagedIterable<SecretProperties> pagedIterable = mock(PagedIterable.class);

    // Mock stream() to return a new stream each time
    when(pagedIterable.stream())
        .thenAnswer(invocation -> allProps.stream())
        .thenAnswer(invocation -> allProps.stream());

    when(secretClient.listPropertiesOfSecrets()).thenReturn(pagedIterable);

    // When
    List<SecretKey> allSecrets = adapter.list(null);
    List<SecretKey> appSecrets = adapter.list("app");

    // Then
    assertEquals(3, allSecrets.size());
    assertEquals(2, appSecrets.size());
    assertTrue(appSecrets.stream().allMatch(k -> k.value().startsWith("app")));
  }

  @Test
  void shouldNormalizeSecretNames() {
    // Given
    SecretKey keyWithSlash = SecretKey.of("config/database");
    SecretKey keyWithNumber = SecretKey.of("123-secret");

    KeyVaultSecret secret1 = mock(KeyVaultSecret.class);
    KeyVaultSecret secret2 = mock(KeyVaultSecret.class);
    SecretProperties props1 = mock(SecretProperties.class);
    SecretProperties props2 = mock(SecretProperties.class);

    when(secret1.getProperties()).thenReturn(props1);
    when(props1.getVersion()).thenReturn("v1");

    when(secret2.getProperties()).thenReturn(props2);
    when(props2.getVersion()).thenReturn("v1");

    when(secretClient.setSecret(any(KeyVaultSecret.class))).thenReturn(secret1).thenReturn(secret2);

    // When
    adapter.put(keyWithSlash, SecretValue.of("value1"));
    adapter.put(keyWithNumber, SecretValue.of("value2"));

    // Then
    verify(secretClient).setSecret(argThat(s -> s.getName().equals("config-database")));
    verify(secretClient).setSecret(argThat(s -> s.getName().equals("s-123-secret")));
  }

  @Test
  void shouldHandleExpirableSecrets() {
    // Given
    SecretKey key = SecretKey.of("expiring");
    Instant expiresAt = Instant.now().plusSeconds(3600);
    SecretValue value = SecretValue.of("secret-data".getBytes(), "v1", Instant.now(), expiresAt);

    KeyVaultSecret storedSecret = mock(KeyVaultSecret.class);
    SecretProperties props = mock(SecretProperties.class);

    when(storedSecret.getProperties()).thenReturn(props);
    when(props.getVersion()).thenReturn("v1");

    when(secretClient.setSecret(any(KeyVaultSecret.class))).thenReturn(storedSecret);

    // When
    String version = adapter.put(key, value);

    // Then
    assertEquals("v1", version);
    verify(secretClient)
        .setSecret(
            argThat(
                s -> {
                  SecretProperties p = s.getProperties();
                  return p.getExpiresOn() != null;
                }));
  }

  @Test
  void shouldAutoCloseSecretValue() {
    // Given
    KeyVaultSecret secret = mock(KeyVaultSecret.class);
    SecretProperties props = mock(SecretProperties.class);

    when(secret.getValue()).thenReturn("sensitive-data");
    when(secret.getProperties()).thenReturn(props);
    when(props.getVersion()).thenReturn("v1");
    when(props.getCreatedOn()).thenReturn(OffsetDateTime.now());

    when(secretClient.getSecret("closeable")).thenReturn(secret);

    // When/Then
    try (SecretValue value = adapter.get(SecretKey.of("closeable")).orElseThrow()) {
      assertEquals("sensitive-data", value.asString());
    }
    // After close(), memory should be zeroed
  }
}
