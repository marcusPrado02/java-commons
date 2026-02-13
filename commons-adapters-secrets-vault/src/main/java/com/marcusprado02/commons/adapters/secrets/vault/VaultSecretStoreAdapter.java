package com.marcusprado02.commons.adapters.secrets.vault;

import com.marcusprado02.commons.ports.secrets.SecretKey;
import com.marcusprado02.commons.ports.secrets.SecretStorePort;
import com.marcusprado02.commons.ports.secrets.SecretValue;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponseSupport;

/**
 * HashiCorp Vault implementation of SecretStorePort.
 *
 * <p>Supports:
 *
 * <ul>
 *   <li>KV Secrets Engine v2 (versioned secrets)
 *   <li>Dynamic secrets
 *   <li>Secret rotation
 *   <li>Metadata retrieval
 * </ul>
 */
public final class VaultSecretStoreAdapter implements SecretStorePort {

  private static final Logger log = LoggerFactory.getLogger(VaultSecretStoreAdapter.class);

  private final VaultTemplate vaultTemplate;
  private final String mountPath;

  /**
   * Creates a new VaultSecretStoreAdapter.
   *
   * @param vaultTemplate Spring Vault template
   * @param mountPath KV mount path (e.g., "secret/")
   */
  public VaultSecretStoreAdapter(VaultTemplate vaultTemplate, String mountPath) {
    this.vaultTemplate = Objects.requireNonNull(vaultTemplate, "vaultTemplate cannot be null");
    this.mountPath = Objects.requireNonNull(mountPath, "mountPath cannot be null");
  }

  /**
   * Creates adapter with default mount path "secret/".
   *
   * @param vaultTemplate Spring Vault template
   */
  public VaultSecretStoreAdapter(VaultTemplate vaultTemplate) {
    this(vaultTemplate, "secret");
  }

  @Override
  public Optional<SecretValue> get(SecretKey key) {
    try {
      String path = resolvePath(key);
      VaultResponseSupport<Map> response = vaultTemplate.read(path, Map.class);

      if (response == null || response.getData() == null) {
        log.debug("Secret not found: {}", key);
        return Optional.empty();
      }

      Map<String, Object> data = response.getData();

      // Check if it's a KV v2 response (has "data" wrapper)
      if (data.containsKey("data") && data.get("data") instanceof Map) {
        @SuppressWarnings("unchecked")
        Map<String, Object> secretData = (Map<String, Object>) data.get("data");
        return extractSecretValue(secretData, response);
      }

      // KV v1 or direct secret
      return extractSecretValue(data, response);

    } catch (Exception e) {
      log.error("Failed to get secret: {}", key, e);
      return Optional.empty();
    }
  }

  @Override
  public Optional<SecretValue> get(SecretKey key, String version) {
    try {
      String path = resolvePath(key) + "?version=" + version;
      VaultResponseSupport<Map> response = vaultTemplate.read(path, Map.class);

      if (response == null || response.getData() == null) {
        log.debug("Secret version not found: {} v{}", key, version);
        return Optional.empty();
      }

      Map<String, Object> data = response.getData();
      if (data.containsKey("data") && data.get("data") instanceof Map) {
        @SuppressWarnings("unchecked")
        Map<String, Object> secretData = (Map<String, Object>) data.get("data");
        return extractSecretValue(secretData, response);
      }

      return extractSecretValue(data, response);

    } catch (Exception e) {
      log.error("Failed to get secret version: {} v{}", key, version, e);
      return Optional.empty();
    }
  }

  @Override
  public String put(SecretKey key, SecretValue value) {
    try {
      String path = resolvePath(key);

      // KV v2 requires data to be wrapped in "data" object
      Map<String, Object> payload = Map.of("data", Map.of("value", value.asString()));

      vaultTemplate.write(path, payload);

      log.debug("Secret stored: {}", key);
      return "latest"; // Vault KV v2 auto-versions

    } catch (Exception e) {
      log.error("Failed to put secret: {}", key, e);
      throw new RuntimeException("Failed to store secret: " + key, e);
    }
  }

  @Override
  public String put(SecretKey key, Map<String, String> data) {
    try {
      String path = resolvePath(key);

      // KV v2 requires data to be wrapped in "data" object
      Map<String, Object> payload = Map.of("data", data);

      vaultTemplate.write(path, payload);

      log.debug("Secret map stored: {} ({} keys)", key, data.size());
      return "latest";

    } catch (Exception e) {
      log.error("Failed to put secret map: {}", key, e);
      throw new RuntimeException("Failed to store secret map: " + key, e);
    }
  }

  @Override
  public boolean delete(SecretKey key) {
    try {
      String path = resolvePath(key);
      vaultTemplate.delete(path);

      log.debug("Secret deleted: {}", key);
      return true;

    } catch (Exception e) {
      log.error("Failed to delete secret: {}", key, e);
      return false;
    }
  }

  @Override
  public boolean exists(SecretKey key) {
    return get(key).isPresent();
  }

  @Override
  public List<SecretKey> list(String prefix) {
    try {
      String listPath = mountPath + "/metadata";
      if (prefix != null && !prefix.isBlank()) {
        listPath += "/" + prefix;
      }

      VaultResponseSupport<Map> response = vaultTemplate.read(listPath + "?list=true", Map.class);

      if (response == null || response.getData() == null) {
        return Collections.emptyList();
      }

      Map<String, Object> data = response.getData();
      if (data.containsKey("keys") && data.get("keys") instanceof List) {
        @SuppressWarnings("unchecked")
        List<String> keys = (List<String>) data.get("keys");
        return keys.stream().map(SecretKey::of).collect(Collectors.toList());
      }

      return Collections.emptyList();

    } catch (Exception e) {
      log.error("Failed to list secrets with prefix: {}", prefix, e);
      return Collections.emptyList();
    }
  }

  private String resolvePath(SecretKey key) {
    String keyValue = key.value();
    if (keyValue.startsWith("/")) {
      keyValue = keyValue.substring(1);
    }
    return mountPath + "/data/" + keyValue;
  }

  private Optional<SecretValue> extractSecretValue(
      Map<String, Object> data, VaultResponseSupport<Map> response) {

    if (data.isEmpty()) {
      return Optional.empty();
    }

    // Try to find "value" key first (standard single-value secret)
    if (data.containsKey("value")) {
      Object value = data.get("value");
      String version = extractVersion(response);
      Instant createdAt = extractCreatedTime(response);

      if (value instanceof String) {
        return Optional.of(SecretValue.of((String) value, version));
      } else if (value instanceof byte[]) {
        return Optional.of(SecretValue.of((byte[]) value, version, createdAt, null));
      }
    }

    // Multiple key-value pairs - serialize as JSON-like string
    String serialized =
        data.entrySet().stream()
            .map(e -> e.getKey() + "=" + e.getValue())
            .collect(Collectors.joining(",", "{", "}"));

    String version = extractVersion(response);
    return Optional.of(SecretValue.of(serialized, version));
  }

  private String extractVersion(VaultResponseSupport<Map> response) {
    if (response.getMetadata() != null && response.getMetadata().containsKey("version")) {
      return String.valueOf(response.getMetadata().get("version"));
    }
    return null;
  }

  private Instant extractCreatedTime(VaultResponseSupport<Map> response) {
    if (response.getMetadata() != null && response.getMetadata().containsKey("created_time")) {
      String createdTime = String.valueOf(response.getMetadata().get("created_time"));
      try {
        return Instant.parse(createdTime);
      } catch (Exception e) {
        log.debug("Failed to parse created_time: {}", createdTime);
      }
    }
    return Instant.now();
  }
}
