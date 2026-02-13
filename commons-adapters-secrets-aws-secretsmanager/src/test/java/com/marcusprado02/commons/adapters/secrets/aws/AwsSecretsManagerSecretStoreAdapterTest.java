package com.marcusprado02.commons.adapters.secrets.aws;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.marcusprado02.commons.ports.secrets.SecretKey;
import com.marcusprado02.commons.ports.secrets.SecretValue;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.*;

class AwsSecretsManagerSecretStoreAdapterTest {

  @Test
  void getShouldReturnEmptyWhenNotFound() {
    SecretsManagerClient client = mock(SecretsManagerClient.class);
    AwsSecretsManagerSecretStoreAdapter adapter = new AwsSecretsManagerSecretStoreAdapter(client);

    SecretKey key = SecretKey.of("missing");
    when(client.getSecretValue(any(GetSecretValueRequest.class)))
        .thenThrow(ResourceNotFoundException.builder().message("nope").build());

    assertTrue(adapter.get(key).isEmpty());
  }

  @Test
  void getShouldReturnSecretString() {
    SecretsManagerClient client = mock(SecretsManagerClient.class);
    AwsSecretsManagerSecretStoreAdapter adapter = new AwsSecretsManagerSecretStoreAdapter(client);

    SecretKey key = SecretKey.of("k");
    Instant created = Instant.parse("2026-02-13T00:00:00Z");

    when(client.getSecretValue(any(GetSecretValueRequest.class)))
        .thenReturn(
            GetSecretValueResponse.builder()
                .secretString("hello")
                .versionId("v1")
                .createdDate(created)
                .build());

    Optional<SecretValue> value = adapter.get(key);
    assertTrue(value.isPresent());
    assertEquals("hello", value.get().asString());
    assertEquals("v1", value.get().version().orElseThrow());
    assertEquals(created, value.get().createdAt());
  }

  @Test
  void getShouldReturnSecretBinary() {
    SecretsManagerClient client = mock(SecretsManagerClient.class);
    AwsSecretsManagerSecretStoreAdapter adapter = new AwsSecretsManagerSecretStoreAdapter(client);

    SecretKey key = SecretKey.of("k");

    when(client.getSecretValue(any(GetSecretValueRequest.class)))
        .thenReturn(
            GetSecretValueResponse.builder()
                .secretBinary(SdkBytes.fromByteArray(new byte[] {0, 1, 2}))
                .versionId("v1")
                .createdDate(Instant.parse("2026-02-13T00:00:00Z"))
                .build());

    SecretValue value = adapter.get(key).orElseThrow();
    assertArrayEquals(new byte[] {0, 1, 2}, value.asBytes());
  }

  @Test
  void getVersionShouldUseVersionStageForAwsCurrent() {
    SecretsManagerClient client = mock(SecretsManagerClient.class);
    AwsSecretsManagerSecretStoreAdapter adapter = new AwsSecretsManagerSecretStoreAdapter(client);

    SecretKey key = SecretKey.of("k");

    when(client.getSecretValue(any(GetSecretValueRequest.class)))
        .thenReturn(
            GetSecretValueResponse.builder()
                .secretString("hello")
                .versionId("v-current")
                .createdDate(Instant.parse("2026-02-13T00:00:00Z"))
                .build());

    adapter.get(key, "AWSCURRENT");

    ArgumentCaptor<GetSecretValueRequest> captor = ArgumentCaptor.forClass(GetSecretValueRequest.class);
    verify(client).getSecretValue(captor.capture());

    assertEquals("k", captor.getValue().secretId());
    assertEquals("AWSCURRENT", captor.getValue().versionStage());
    assertNull(captor.getValue().versionId());
  }

  @Test
  void putShouldCreateSecretWhenMissing() {
    SecretsManagerClient client = mock(SecretsManagerClient.class);
    AwsSecretsManagerSecretStoreAdapter adapter = new AwsSecretsManagerSecretStoreAdapter(client);

    when(client.createSecret(any(CreateSecretRequest.class)))
        .thenReturn(CreateSecretResponse.builder().versionId("v1").build());

    String version = adapter.put(SecretKey.of("k"), SecretValue.of("value"));
    assertEquals("v1", version);

    ArgumentCaptor<CreateSecretRequest> captor = ArgumentCaptor.forClass(CreateSecretRequest.class);
    verify(client).createSecret(captor.capture());
    assertEquals("k", captor.getValue().name());
    assertEquals("value", captor.getValue().secretString());
  }

  @Test
  void putShouldPutSecretValueWhenExists() {
    SecretsManagerClient client = mock(SecretsManagerClient.class);
    AwsSecretsManagerSecretStoreAdapter adapter = new AwsSecretsManagerSecretStoreAdapter(client);

    when(client.createSecret(any(CreateSecretRequest.class)))
        .thenThrow(ResourceExistsException.builder().message("exists").build());
    when(client.putSecretValue(any(PutSecretValueRequest.class)))
        .thenReturn(PutSecretValueResponse.builder().versionId("v2").build());

    String version = adapter.put(SecretKey.of("k"), SecretValue.of("value"));
    assertEquals("v2", version);

    verify(client).putSecretValue(any(PutSecretValueRequest.class));
  }

  @Test
  void putMapShouldSerializeDeterministically() {
    SecretsManagerClient client = mock(SecretsManagerClient.class);
    AwsSecretsManagerSecretStoreAdapter adapter = new AwsSecretsManagerSecretStoreAdapter(client);

    when(client.createSecret(any(CreateSecretRequest.class)))
        .thenReturn(CreateSecretResponse.builder().versionId("v1").build());

    adapter.put(SecretKey.of("cfg"), Map.of("b", "2", "a", "1"));

    ArgumentCaptor<CreateSecretRequest> captor = ArgumentCaptor.forClass(CreateSecretRequest.class);
    verify(client).createSecret(captor.capture());

    assertEquals("cfg", captor.getValue().name());
    assertEquals("{\"a\":\"1\",\"b\":\"2\"}", captor.getValue().secretString());
  }

  @Test
  void deleteShouldReturnFalseWhenNotFound() {
    SecretsManagerClient client = mock(SecretsManagerClient.class);
    AwsSecretsManagerSecretStoreAdapter adapter = new AwsSecretsManagerSecretStoreAdapter(client);

    doThrow(ResourceNotFoundException.builder().message("nope").build())
        .when(client)
        .deleteSecret(any(DeleteSecretRequest.class));

    assertFalse(adapter.delete(SecretKey.of("k")));
  }

  @Test
  void existsShouldReturnTrueWhenDescribeSucceeds() {
    SecretsManagerClient client = mock(SecretsManagerClient.class);
    AwsSecretsManagerSecretStoreAdapter adapter = new AwsSecretsManagerSecretStoreAdapter(client);

    when(client.describeSecret(any(DescribeSecretRequest.class)))
        .thenReturn(DescribeSecretResponse.builder().name("k").build());

    assertTrue(adapter.exists(SecretKey.of("k")));
  }

  @Test
  void listShouldPaginateAndFilterByPrefix() {
    SecretsManagerClient client = mock(SecretsManagerClient.class);
    AwsSecretsManagerSecretStoreAdapter adapter = new AwsSecretsManagerSecretStoreAdapter(client);

    when(client.listSecrets(ListSecretsRequest.builder().nextToken(null).build()))
        .thenReturn(
            ListSecretsResponse.builder()
                .nextToken("t1")
                .secretList(
                    SecretListEntry.builder().name("app/a").build(),
                    SecretListEntry.builder().name("other/x").build())
                .build());

    when(client.listSecrets(ListSecretsRequest.builder().nextToken("t1").build()))
        .thenReturn(
            ListSecretsResponse.builder()
                .nextToken(null)
                .secretList(SecretListEntry.builder().name("app/b").build())
                .build());

    List<SecretKey> listed = adapter.list("app/");

    assertEquals(2, listed.size());
    assertEquals(List.of(SecretKey.of("app/a"), SecretKey.of("app/b")), listed);
  }
}
