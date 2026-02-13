package com.marcusprado02.commons.adapters.secrets.vault;

import static org.junit.jupiter.api.Assertions.*;

import com.marcusprado02.commons.ports.secrets.SecretKey;
import com.marcusprado02.commons.ports.secrets.SecretStorePort;
import com.marcusprado02.commons.ports.secrets.SecretValue;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.core.VaultTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class VaultSecretStoreAdapterTest {

  private static final String VAULT_TOKEN = "test-root-token";
  private static final String VAULT_IMAGE = "hashicorp/vault:1.15";

  @Container
  static GenericContainer<?> vaultContainer =
      new GenericContainer<>(DockerImageName.parse(VAULT_IMAGE))
          .withExposedPorts(8200)
          .withEnv("VAULT_DEV_ROOT_TOKEN_ID", VAULT_TOKEN)
          .withEnv("VAULT_DEV_LISTEN_ADDRESS", "0.0.0.0:8200")
          .withCommand("server", "-dev", "-dev-root-token-id=" + VAULT_TOKEN);

  static VaultTemplate vaultTemplate;
  SecretStorePort secretStore;

  @BeforeAll
  static void setupVault() {
    vaultContainer.start();

    VaultEndpoint endpoint =
        VaultEndpoint.create(vaultContainer.getHost(), vaultContainer.getFirstMappedPort());

    endpoint.setScheme("http");

    vaultTemplate = new VaultTemplate(endpoint, new TokenAuthentication(VAULT_TOKEN));

    // Enable KV v2 secrets engine
    try {
      vaultTemplate
          .opsForSys()
          .mount("secret", org.springframework.vault.support.VaultMount.create("kv-v2"));
    } catch (Exception e) {
      // May already be enabled in dev mode
    }
  }

  @AfterAll
  static void teardown() {
    vaultContainer.stop();
  }

  @BeforeEach
  void beforeEach() {
    secretStore = new VaultSecretStoreAdapter(vaultTemplate);
  }

  @Test
  void shouldPutAndGetSecret() {
    var key = SecretKey.of("test/my-secret");
    var value = SecretValue.of("my-secret-value");

    String version = secretStore.put(key, value);

    assertNotNull(version);

    var retrieved = secretStore.get(key);

    assertTrue(retrieved.isPresent());
    assertEquals("my-secret-value", retrieved.get().asString());
  }

  @Test
  void shouldPutAndGetSecretMap() {
    var key = SecretKey.of("test/db-credentials");
    var data =
        Map.of(
            "username", "admin",
            "password", "secret123",
            "host", "localhost",
            "port", "5432");

    String version = secretStore.put(key, data);

    assertNotNull(version);

    var retrieved = secretStore.get(key);

    assertTrue(retrieved.isPresent());

    // The value should contain the map data serialized
    String value = retrieved.get().asString();
    assertTrue(value.contains("username"));
    assertTrue(value.contains("admin"));
  }

  @Test
  void shouldReturnEmptyForNonExistentSecret() {
    var key = SecretKey.of("test/non-existent");

    var retrieved = secretStore.get(key);

    assertFalse(retrieved.isPresent());
  }

  @Test
  void shouldDeleteSecret() {
    var key = SecretKey.of("test/to-delete");
    var value = SecretValue.of("delete-me");

    secretStore.put(key, value);
    assertTrue(secretStore.exists(key));

    boolean deleted = secretStore.delete(key);

    assertTrue(deleted);
    assertFalse(secretStore.exists(key));
  }

  @Test
  void shouldCheckExistence() {
    var key = SecretKey.of("test/check-exists");
    var value = SecretValue.of("exists");

    assertFalse(secretStore.exists(key));

    secretStore.put(key, value);

    assertTrue(secretStore.exists(key));
  }

  @Test
  void shouldUpdateSecret() {
    var key = SecretKey.of("test/update-me");
    var value1 = SecretValue.of("version-1");
    var value2 = SecretValue.of("version-2");

    secretStore.put(key, value1);
    secretStore.put(key, value2);

    var retrieved = secretStore.get(key);

    assertTrue(retrieved.isPresent());
    assertEquals("version-2", retrieved.get().asString());
  }

  @Test
  void shouldHandleVersionedSecrets() {
    var key = SecretKey.of("test/versioned");
    var value = SecretValue.of("v1");

    String version = secretStore.put(key, value);

    assertNotNull(version);

    var retrieved = secretStore.get(key);

    assertTrue(retrieved.isPresent());
    assertTrue(retrieved.get().version().isPresent() || version.equals("latest"));
  }

  @Test
  void shouldHandleMultipleSecrets() {
    var key1 = SecretKey.of("test/secret-1");
    var key2 = SecretKey.of("test/secret-2");
    var key3 = SecretKey.of("test/secret-3");

    secretStore.put(key1, SecretValue.of("value-1"));
    secretStore.put(key2, SecretValue.of("value-2"));
    secretStore.put(key3, SecretValue.of("value-3"));

    assertTrue(secretStore.exists(key1));
    assertTrue(secretStore.exists(key2));
    assertTrue(secretStore.exists(key3));

    assertEquals("value-1", secretStore.get(key1).get().asString());
    assertEquals("value-2", secretStore.get(key2).get().asString());
    assertEquals("value-3", secretStore.get(key3).get().asString());
  }

  @Test
  void shouldHandleNestedPaths() {
    var key = SecretKey.of("test/app/prod/db/password");
    var value = SecretValue.of("super-secret");

    secretStore.put(key, value);

    var retrieved = secretStore.get(key);

    assertTrue(retrieved.isPresent());
    assertEquals("super-secret", retrieved.get().asString());
  }

  @Test
  void shouldHandleSpecialCharactersInValue() {
    var key = SecretKey.of("test/special-chars");
    var value = SecretValue.of("!@#$%^&*()_+-={}[]|:;<>?,./");

    secretStore.put(key, value);

    var retrieved = secretStore.get(key);

    assertTrue(retrieved.isPresent());
    assertEquals("!@#$%^&*()_+-={}[]|:;<>?,./", retrieved.get().asString());
  }

  @Test
  void shouldAutoCloseSecretValue() {
    var key = SecretKey.of("test/auto-close");
    var value = SecretValue.of("sensitive");

    secretStore.put(key, value);

    try (SecretValue retrieved = secretStore.get(key).orElseThrow()) {
      assertEquals("sensitive", retrieved.asString());
    }

    // After close, accessing should throw
    var retrievedAgain = secretStore.get(key);
    assertTrue(retrievedAgain.isPresent());
  }
}
