package com.marcusprado02.commons.adapters.secrets.aws;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.marcusprado02.commons.ports.secrets.SecretKey;
import com.marcusprado02.commons.ports.secrets.SecretValue;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.*;

/**
 * Unit tests covering error paths and uncovered branches in AwsSecretsManagerSecretStoreAdapter.
 */
@ExtendWith(MockitoExtension.class)
class AwsSecretsManagerSecretStoreAdapterUnitTest {

  @Mock private SecretsManagerClient client;

  private AwsSecretsManagerSecretStoreAdapter adapter;

  @BeforeEach
  void setUp() {
    adapter = new AwsSecretsManagerSecretStoreAdapter(client);
  }

  // ── get(key) error path ───────────────────────────────────────────────────

  @Test
  void getShouldReturnEmptyOnSecretsManagerException() {
    when(client.getSecretValue(any(GetSecretValueRequest.class)))
        .thenThrow(SecretsManagerException.builder().message("access denied").build());

    assertTrue(adapter.get(SecretKey.of("key")).isEmpty());
  }

  // ── get(key, version) paths ───────────────────────────────────────────────

  @Test
  void getVersionShouldUseVersionStageForAwsPrevious() {
    when(client.getSecretValue(any(GetSecretValueRequest.class)))
        .thenReturn(
            GetSecretValueResponse.builder()
                .secretString("prev")
                .versionId("v0")
                .createdDate(Instant.parse("2026-01-01T00:00:00Z"))
                .build());

    adapter.get(SecretKey.of("k"), "AWSPREVIOUS");

    ArgumentCaptor<GetSecretValueRequest> captor =
        ArgumentCaptor.forClass(GetSecretValueRequest.class);
    verify(client).getSecretValue(captor.capture());
    assertEquals("AWSPREVIOUS", captor.getValue().versionStage());
    assertNull(captor.getValue().versionId());
  }

  @Test
  void getVersionShouldUseVersionIdForNonStageVersion() {
    when(client.getSecretValue(any(GetSecretValueRequest.class)))
        .thenReturn(
            GetSecretValueResponse.builder()
                .secretString("v1-value")
                .versionId("abc-123")
                .createdDate(Instant.parse("2026-01-01T00:00:00Z"))
                .build());

    adapter.get(SecretKey.of("k"), "abc-123");

    ArgumentCaptor<GetSecretValueRequest> captor =
        ArgumentCaptor.forClass(GetSecretValueRequest.class);
    verify(client).getSecretValue(captor.capture());
    assertNull(captor.getValue().versionStage());
    assertEquals("abc-123", captor.getValue().versionId());
  }

  @Test
  void getVersionShouldReturnEmptyOnResourceNotFoundException() {
    when(client.getSecretValue(any(GetSecretValueRequest.class)))
        .thenThrow(ResourceNotFoundException.builder().message("nope").build());

    assertTrue(adapter.get(SecretKey.of("k"), "v99").isEmpty());
  }

  @Test
  void getVersionShouldReturnEmptyOnSecretsManagerException() {
    when(client.getSecretValue(any(GetSecretValueRequest.class)))
        .thenThrow(SecretsManagerException.builder().message("access denied").build());

    assertTrue(adapter.get(SecretKey.of("k"), "v1").isEmpty());
  }

  // ── put(key, SecretValue) error paths ─────────────────────────────────────

  @Test
  void putShouldThrowOnSecretsManagerException() {
    when(client.createSecret(any(CreateSecretRequest.class)))
        .thenThrow(SecretsManagerException.builder().message("error").build());

    assertThrows(RuntimeException.class, () -> adapter.put(SecretKey.of("k"), SecretValue.of("v")));
  }

  @Test
  void putShouldStoreBinaryPayloadWhenValueIsNotValidUtf8() {
    // Bytes that are NOT valid UTF-8 (0xFF 0xFE is a BOM but not valid UTF-8 sequence here)
    byte[] binaryBytes = new byte[] {(byte) 0xFF, (byte) 0xFE, 0x00, 0x01};

    when(client.createSecret(any(CreateSecretRequest.class)))
        .thenReturn(CreateSecretResponse.builder().versionId("v1").build());

    String version =
        adapter.put(SecretKey.of("bin"), SecretValue.of(binaryBytes, "v0", null, null));

    assertEquals("v1", version);
    ArgumentCaptor<CreateSecretRequest> captor = ArgumentCaptor.forClass(CreateSecretRequest.class);
    verify(client).createSecret(captor.capture());
    assertNotNull(captor.getValue().secretBinary());
    assertNull(captor.getValue().secretString());
  }

  // ── put(key, Map) paths ───────────────────────────────────────────────────

  @Test
  void putMapShouldFallThroughToUpdateWhenSecretExists() {
    when(client.createSecret(any(CreateSecretRequest.class)))
        .thenThrow(ResourceExistsException.builder().message("exists").build());
    when(client.putSecretValue(any(PutSecretValueRequest.class)))
        .thenReturn(PutSecretValueResponse.builder().versionId("v2").build());

    String version = adapter.put(SecretKey.of("cfg"), Map.of("k", "v"));

    assertEquals("v2", version);
    verify(client).putSecretValue(any(PutSecretValueRequest.class));
  }

  @Test
  void putMapShouldThrowOnSecretsManagerException() {
    when(client.createSecret(any(CreateSecretRequest.class)))
        .thenThrow(SecretsManagerException.builder().message("error").build());

    assertThrows(RuntimeException.class, () -> adapter.put(SecretKey.of("k"), Map.of("a", "b")));
  }

  @Test
  void putMapShouldHandleNullMap() {
    when(client.createSecret(any(CreateSecretRequest.class)))
        .thenReturn(CreateSecretResponse.builder().versionId("v1").build());

    String version = adapter.put(SecretKey.of("k"), (Map<String, String>) null);

    assertEquals("v1", version);
    ArgumentCaptor<CreateSecretRequest> captor = ArgumentCaptor.forClass(CreateSecretRequest.class);
    verify(client).createSecret(captor.capture());
    assertEquals("{}", captor.getValue().secretString());
  }

  @Test
  void putMapShouldHandleEmptyMap() {
    when(client.createSecret(any(CreateSecretRequest.class)))
        .thenReturn(CreateSecretResponse.builder().versionId("v1").build());

    String version = adapter.put(SecretKey.of("k"), Collections.emptyMap());

    assertEquals("v1", version);
    ArgumentCaptor<CreateSecretRequest> captor = ArgumentCaptor.forClass(CreateSecretRequest.class);
    verify(client).createSecret(captor.capture());
    assertEquals("{}", captor.getValue().secretString());
  }

  @Test
  void putMapShouldEscapeSpecialCharacters() {
    when(client.createSecret(any(CreateSecretRequest.class)))
        .thenReturn(CreateSecretResponse.builder().versionId("v1").build());

    adapter.put(SecretKey.of("k"), Map.of("k\"ey", "va\\lue"));

    ArgumentCaptor<CreateSecretRequest> captor = ArgumentCaptor.forClass(CreateSecretRequest.class);
    verify(client).createSecret(captor.capture());
    assertTrue(captor.getValue().secretString().contains("\\\""));
    assertTrue(captor.getValue().secretString().contains("\\\\"));
  }

  // ── delete error path ─────────────────────────────────────────────────────

  @Test
  void deleteShouldReturnFalseOnSecretsManagerException() {
    doThrow(SecretsManagerException.builder().message("error").build())
        .when(client)
        .deleteSecret(any(DeleteSecretRequest.class));

    assertFalse(adapter.delete(SecretKey.of("k")));
  }

  // ── exists error paths ────────────────────────────────────────────────────

  @Test
  void existsShouldReturnFalseWhenNotFound() {
    when(client.describeSecret(any(DescribeSecretRequest.class)))
        .thenThrow(ResourceNotFoundException.builder().message("nope").build());

    assertFalse(adapter.exists(SecretKey.of("missing")));
  }

  @Test
  void existsShouldReturnFalseOnSecretsManagerException() {
    when(client.describeSecret(any(DescribeSecretRequest.class)))
        .thenThrow(SecretsManagerException.builder().message("error").build());

    assertFalse(adapter.exists(SecretKey.of("k")));
  }

  // ── list error and prefix paths ───────────────────────────────────────────

  @Test
  void listShouldReturnEmptyOnSecretsManagerException() {
    when(client.listSecrets(any(ListSecretsRequest.class)))
        .thenThrow(SecretsManagerException.builder().message("error").build());

    assertTrue(adapter.list("prefix").isEmpty());
  }

  @Test
  void listShouldReturnAllWhenPrefixIsNull() {
    when(client.listSecrets(any(ListSecretsRequest.class)))
        .thenReturn(
            ListSecretsResponse.builder()
                .nextToken(null)
                .secretList(
                    SecretListEntry.builder().name("a").build(),
                    SecretListEntry.builder().name("b").build())
                .build());

    var result = adapter.list(null);

    assertEquals(2, result.size());
  }

  @Test
  void listShouldReturnAllWhenPrefixIsBlank() {
    when(client.listSecrets(any(ListSecretsRequest.class)))
        .thenReturn(
            ListSecretsResponse.builder()
                .nextToken(null)
                .secretList(SecretListEntry.builder().name("x").build())
                .build());

    var result = adapter.list("   ");

    assertEquals(1, result.size());
  }

  // ── toSecretValue: null createdDate and empty-body paths ─────────────────

  @Test
  void getShouldHandleNullCreatedDate() {
    when(client.getSecretValue(any(GetSecretValueRequest.class)))
        .thenReturn(
            GetSecretValueResponse.builder()
                .secretString("value")
                .versionId("v1")
                // no createdDate set — SDK returns null
                .build());

    var result = adapter.get(SecretKey.of("k"));

    assertTrue(result.isPresent());
    assertEquals("value", result.get().asString());
  }

  @Test
  void getShouldHandleResponseWithNeitherStringNorBinary() {
    when(client.getSecretValue(any(GetSecretValueRequest.class)))
        .thenReturn(
            GetSecretValueResponse.builder()
                .versionId("v1")
                .createdDate(Instant.parse("2026-01-01T00:00:00Z"))
                // no secretString, no secretBinary
                .build());

    var result = adapter.get(SecretKey.of("k"));

    assertTrue(result.isPresent());
    assertEquals(0, result.get().asBytes().length);
  }
}
