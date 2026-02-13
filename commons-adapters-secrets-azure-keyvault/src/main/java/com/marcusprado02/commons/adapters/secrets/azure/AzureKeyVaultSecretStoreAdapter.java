package com.marcusprado02.commons.adapters.secrets.azure;

import com.azure.core.exception.ResourceNotFoundException;
import com.azure.core.util.polling.SyncPoller;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.models.*;
import com.marcusprado02.commons.ports.secrets.SecretKey;
import com.marcusprado02.commons.ports.secrets.SecretStorePort;
import com.marcusprado02.commons.ports.secrets.SecretValue;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Azure Key Vault implementation of SecretStorePort.
 *
 * <p>Features:
 *
 * <ul>
 *   <li>Managed Identity support
 *   <li>Secret versioning
 *   <li>Secret expiration
 *   <li>Tags and metadata
 *   <li>Soft delete and recovery
 * </ul>
 */
public final class AzureKeyVaultSecretStoreAdapter implements SecretStorePort {

  private static final Logger log = LoggerFactory.getLogger(AzureKeyVaultSecretStoreAdapter.class);

  private final SecretClient secretClient;

  /**
   * Creates a new AzureKeyVaultSecretStoreAdapter.
   *
   * @param secretClient Azure SecretClient
   */
  public AzureKeyVaultSecretStoreAdapter(SecretClient secretClient) {
    this.secretClient = Objects.requireNonNull(secretClient, "secretClient cannot be null");
  }

  @Override
  public Optional<SecretValue> get(SecretKey key) {
    try {
      String secretName = normalizeSecretName(key);
      KeyVaultSecret secret = secretClient.getSecret(secretName);

      return Optional.of(extractSecretValue(secret));

    } catch (ResourceNotFoundException e) {
      log.debug("Secret not found: {}", key);
      return Optional.empty();
    } catch (Exception e) {
      log.error("Failed to get secret: {}", key, e);
      return Optional.empty();
    }
  }

  @Override
  public Optional<SecretValue> get(SecretKey key, String version) {
    try {
      String secretName = normalizeSecretName(key);
      KeyVaultSecret secret = secretClient.getSecret(secretName, version);

      return Optional.of(extractSecretValue(secret));

    } catch (ResourceNotFoundException e) {
      log.debug("Secret version not found: {} v{}", key, version);
      return Optional.empty();
    } catch (Exception e) {
      log.error("Failed to get secret version: {} v{}", key, version, e);
      return Optional.empty();
    }
  }

  @Override
  public String put(SecretKey key, SecretValue value) {
    try {
      String secretName = normalizeSecretName(key);
      KeyVaultSecret secret = new KeyVaultSecret(secretName, value.asString());

      // Set expiration if available
      value
          .expiresAt()
          .ifPresent(
              expiresAt -> {
                SecretProperties props = secret.getProperties();
                props.setExpiresOn(
                    java.time.OffsetDateTime.ofInstant(expiresAt, java.time.ZoneOffset.UTC));
              });

      KeyVaultSecret result = secretClient.setSecret(secret);

      log.debug("Secret stored: {} v{}", key, result.getProperties().getVersion());
      return result.getProperties().getVersion();

    } catch (Exception e) {
      log.error("Failed to put secret: {}", key, e);
      throw new RuntimeException("Failed to store secret: " + key, e);
    }
  }

  @Override
  public String put(SecretKey key, Map<String, String> data) {
    try {
      String secretName = normalizeSecretName(key);

      // Serialize map as JSON-like string (simple format)
      String serialized =
          data.entrySet().stream()
              .map(e -> e.getKey() + "=" + e.getValue())
              .collect(Collectors.joining(",", "{", "}"));

      KeyVaultSecret secret = new KeyVaultSecret(secretName, serialized);

      // Add tags for each key-value pair (limited to 15 tags)
      if (data.size() <= 15) {
        secret.getProperties().setTags(data);
      }

      KeyVaultSecret result = secretClient.setSecret(secret);

      log.debug(
          "Secret map stored: {} ({} keys) v{}",
          key,
          data.size(),
          result.getProperties().getVersion());
      return result.getProperties().getVersion();

    } catch (Exception e) {
      log.error("Failed to put secret map: {}", key, e);
      throw new RuntimeException("Failed to store secret map: " + key, e);
    }
  }

  @Override
  public boolean delete(SecretKey key) {
    try {
      String secretName = normalizeSecretName(key);

      SyncPoller<DeletedSecret, Void> poller = secretClient.beginDeleteSecret(secretName);
      poller.waitForCompletion();

      log.debug("Secret deleted: {}", key);
      return true;

    } catch (ResourceNotFoundException e) {
      log.debug("Secret not found for deletion: {}", key);
      return false;
    } catch (Exception e) {
      log.error("Failed to delete secret: {}", key, e);
      return false;
    }
  }

  @Override
  public boolean exists(SecretKey key) {
    try {
      String secretName = normalizeSecretName(key);
      secretClient.getSecret(secretName);
      return true;
    } catch (ResourceNotFoundException e) {
      return false;
    } catch (Exception e) {
      log.error("Error checking secret existence: {}", key, e);
      return false;
    }
  }

  @Override
  public List<SecretKey> list(String prefix) {
    try {
      return secretClient.listPropertiesOfSecrets().stream()
          .map(props -> props.getName())
          .filter(name -> prefix == null || prefix.isBlank() || name.startsWith(prefix))
          .map(name -> SecretKey.of(denormalizeSecretName(name)))
          .collect(Collectors.toList());

    } catch (Exception e) {
      log.error("Failed to list secrets with prefix: {}", prefix, e);
      return Collections.emptyList();
    }
  }

  /**
   * Normalize secret name to Azure Key Vault requirements.
   *
   * <p>Azure Key Vault secret names:
   *
   * <ul>
   *   <li>1-127 characters
   *   <li>Alphanumeric and hyphens only
   *   <li>Cannot start with number
   * </ul>
   *
   * @param key Secret key
   * @return Normalized name
   */
  private String normalizeSecretName(SecretKey key) {
    String name = key.value();

    // Replace invalid characters with hyphens
    name = name.replaceAll("[^a-zA-Z0-9-]", "-");

    // Ensure it doesn't start with a number
    if (!name.isEmpty() && Character.isDigit(name.charAt(0))) {
      name = "s-" + name;
    }

    // Truncate if too long
    if (name.length() > 127) {
      name = name.substring(0, 127);
    }

    return name;
  }

  private String denormalizeSecretName(String name) {
    // Remove "s-" prefix if added during normalization
    if (name.startsWith("s-") && name.length() > 2 && Character.isDigit(name.charAt(2))) {
      return name.substring(2);
    }
    return name;
  }

  private SecretValue extractSecretValue(KeyVaultSecret secret) {
    String value = secret.getValue();
    String version = secret.getProperties().getVersion();

    Instant createdAt =
        secret.getProperties().getCreatedOn() != null
            ? secret.getProperties().getCreatedOn().toInstant()
            : Instant.now();

    Instant expiresAt =
        secret.getProperties().getExpiresOn() != null
            ? secret.getProperties().getExpiresOn().toInstant()
            : null;

    return SecretValue.of(value.getBytes(), version, createdAt, expiresAt);
  }
}
