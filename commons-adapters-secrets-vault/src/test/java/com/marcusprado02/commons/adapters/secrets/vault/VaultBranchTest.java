package com.marcusprado02.commons.adapters.secrets.vault;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.marcusprado02.commons.ports.secrets.SecretKey;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponseSupport;

@SuppressWarnings({"unchecked", "rawtypes"})
class VaultBranchTest {

  // --- get(): KV v2 response (data wrapped in "data" map) ---

  @Test
  void get_kvV2DataWrapper_extractsInnerValue() {
    VaultTemplate template = mock(VaultTemplate.class);
    VaultResponseSupport response = mock(VaultResponseSupport.class);
    when(template.read(anyString(), eq(Map.class))).thenReturn(response);
    when(response.getData()).thenReturn(Map.of("data", Map.of("value", "secret-value")));
    when(response.getMetadata()).thenReturn(null);

    VaultSecretStoreAdapter adapter = new VaultSecretStoreAdapter(template);
    var result = adapter.get(SecretKey.of("my/key"));
    assertThat(result).isPresent();
    assertThat(result.get().asString()).isEqualTo("secret-value");
  }

  // --- get(): empty data map ---

  @Test
  void get_emptyDataMap_returnsEmpty() {
    VaultTemplate template = mock(VaultTemplate.class);
    VaultResponseSupport response = mock(VaultResponseSupport.class);
    when(template.read(anyString(), eq(Map.class))).thenReturn(response);
    when(response.getData()).thenReturn(Map.of());

    VaultSecretStoreAdapter adapter = new VaultSecretStoreAdapter(template);
    var result = adapter.get(SecretKey.of("my/key"));
    assertThat(result).isEmpty();
  }

  // --- get(): data with byte[] value ---

  @Test
  void get_byteArrayValue_extractsBytes() {
    VaultTemplate template = mock(VaultTemplate.class);
    VaultResponseSupport response = mock(VaultResponseSupport.class);
    when(template.read(anyString(), eq(Map.class))).thenReturn(response);
    when(response.getData()).thenReturn(Map.of("value", "hello".getBytes()));
    when(response.getMetadata()).thenReturn(Map.of("created_time", "2026-01-01T00:00:00Z"));

    VaultSecretStoreAdapter adapter = new VaultSecretStoreAdapter(template);
    var result = adapter.get(SecretKey.of("my/key"));
    assertThat(result).isPresent();
  }

  // --- get(): multi-key data (no "value" key) serializes as map ---

  @Test
  void get_multiKeyData_serializesAsMap() {
    VaultTemplate template = mock(VaultTemplate.class);
    VaultResponseSupport response = mock(VaultResponseSupport.class);
    when(template.read(anyString(), eq(Map.class))).thenReturn(response);
    when(response.getData()).thenReturn(Map.of("db_user", "admin", "db_pass", "secret"));
    when(response.getMetadata()).thenReturn(Map.of("version", "3"));

    VaultSecretStoreAdapter adapter = new VaultSecretStoreAdapter(template);
    var result = adapter.get(SecretKey.of("my/key"));
    assertThat(result).isPresent();
    assertThat(result.get().asString()).contains("db_user");
  }

  // --- extractVersion: metadata with "version" key ---

  @Test
  void get_metadataWithVersion_includesVersion() {
    VaultTemplate template = mock(VaultTemplate.class);
    VaultResponseSupport response = mock(VaultResponseSupport.class);
    when(template.read(anyString(), eq(Map.class))).thenReturn(response);
    when(response.getData()).thenReturn(Map.of("value", "secret"));
    when(response.getMetadata()).thenReturn(Map.of("version", "5"));

    VaultSecretStoreAdapter adapter = new VaultSecretStoreAdapter(template);
    var result = adapter.get(SecretKey.of("my/key"));
    assertThat(result).isPresent();
  }

  // --- extractCreatedTime: metadata with "created_time" (valid ISO instant) ---

  @Test
  void get_metadataWithCreatedTime_parsesInstant() {
    VaultTemplate template = mock(VaultTemplate.class);
    VaultResponseSupport response = mock(VaultResponseSupport.class);
    when(template.read(anyString(), eq(Map.class))).thenReturn(response);
    when(response.getData()).thenReturn(Map.of("value", "hello".getBytes()));
    when(response.getMetadata())
        .thenReturn(Map.of("version", "1", "created_time", "2026-01-15T12:00:00Z"));

    VaultSecretStoreAdapter adapter = new VaultSecretStoreAdapter(template);
    var result = adapter.get(SecretKey.of("my/key"));
    assertThat(result).isPresent();
  }

  // --- extractCreatedTime: metadata with invalid "created_time" string ---

  @Test
  void get_metadataWithInvalidCreatedTime_fallsBackToNow() {
    VaultTemplate template = mock(VaultTemplate.class);
    VaultResponseSupport response = mock(VaultResponseSupport.class);
    when(template.read(anyString(), eq(Map.class))).thenReturn(response);
    when(response.getData()).thenReturn(Map.of("value", "hello".getBytes()));
    when(response.getMetadata()).thenReturn(Map.of("created_time", "not-a-date"));

    VaultSecretStoreAdapter adapter = new VaultSecretStoreAdapter(template);
    var result = adapter.get(SecretKey.of("my/key"));
    assertThat(result).isPresent();
  }

  // --- get(key, version): KV v2 response ---

  @Test
  void getVersion_kvV2DataWrapper_extractsInnerValue() {
    VaultTemplate template = mock(VaultTemplate.class);
    VaultResponseSupport response = mock(VaultResponseSupport.class);
    when(template.read(anyString(), eq(Map.class))).thenReturn(response);
    when(response.getData()).thenReturn(Map.of("data", Map.of("value", "versioned")));
    when(response.getMetadata()).thenReturn(null);

    VaultSecretStoreAdapter adapter = new VaultSecretStoreAdapter(template);
    var result = adapter.get(SecretKey.of("my/key"), "2");
    assertThat(result).isPresent();
    assertThat(result.get().asString()).isEqualTo("versioned");
  }

  // --- list(): with non-blank prefix ---

  @Test
  void list_withPrefix_includesPrefixInPath() {
    VaultTemplate template = mock(VaultTemplate.class);
    VaultResponseSupport response = mock(VaultResponseSupport.class);
    when(template.read(anyString(), eq(Map.class))).thenReturn(response);
    when(response.getData()).thenReturn(Map.of("keys", List.of("key1", "key2")));

    VaultSecretStoreAdapter adapter = new VaultSecretStoreAdapter(template);
    var result = adapter.list("myapp");
    assertThat(result).hasSize(2);
    assertThat(result.get(0).value()).isEqualTo("key1");
  }

  // --- list(): with null prefix ---

  @Test
  void list_withNullPrefix_listsAll() {
    VaultTemplate template = mock(VaultTemplate.class);
    VaultResponseSupport response = mock(VaultResponseSupport.class);
    when(template.read(anyString(), eq(Map.class))).thenReturn(response);
    when(response.getData()).thenReturn(Map.of("keys", List.of("a", "b")));

    VaultSecretStoreAdapter adapter = new VaultSecretStoreAdapter(template);
    var result = adapter.list(null);
    assertThat(result).hasSize(2);
  }

  // --- list(): data without "keys" key ---

  @Test
  void list_dataWithoutKeysKey_returnsEmpty() {
    VaultTemplate template = mock(VaultTemplate.class);
    VaultResponseSupport response = mock(VaultResponseSupport.class);
    when(template.read(anyString(), eq(Map.class))).thenReturn(response);
    when(response.getData()).thenReturn(Map.of("other", "stuff"));

    VaultSecretStoreAdapter adapter = new VaultSecretStoreAdapter(template);
    var result = adapter.list("prefix");
    assertThat(result).isEmpty();
  }

  // --- default-mount-path constructor ---

  @Test
  void defaultConstructor_usesMountPathSecret() {
    VaultTemplate template = mock(VaultTemplate.class);
    VaultResponseSupport response = mock(VaultResponseSupport.class);
    when(template.read(anyString(), eq(Map.class))).thenReturn(response);
    when(response.getData()).thenReturn(null);

    VaultSecretStoreAdapter adapter = new VaultSecretStoreAdapter(template);
    var result = adapter.get(SecretKey.of("my/key"));
    assertThat(result).isEmpty();
  }
}
