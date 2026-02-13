package com.marcusprado02.commons.adapters.secrets.aws;

import com.marcusprado02.commons.ports.secrets.SecretKey;
import com.marcusprado02.commons.ports.secrets.SecretStorePort;
import com.marcusprado02.commons.ports.secrets.SecretValue;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.*;

/**
 * AWS Secrets Manager implementation of SecretStorePort.
 *
 * <p>Notes:
 *
 * <ul>
 *   <li>Works with IAM roles / default AWS credential provider chain.
 *   <li>Reads always return the current version (AWSCURRENT).
 *   <li>Version-specific reads use versionId.
 * </ul>
 */
public final class AwsSecretsManagerSecretStoreAdapter implements SecretStorePort {

  private static final Logger log = LoggerFactory.getLogger(AwsSecretsManagerSecretStoreAdapter.class);

  private final SecretsManagerClient client;

  public AwsSecretsManagerSecretStoreAdapter(SecretsManagerClient client) {
    this.client = Objects.requireNonNull(client, "client cannot be null");
  }

  @Override
  public Optional<SecretValue> get(SecretKey key) {
    try {
      GetSecretValueResponse response =
          client.getSecretValue(GetSecretValueRequest.builder().secretId(secretId(key)).build());
      return Optional.of(toSecretValue(response));

    } catch (ResourceNotFoundException e) {
      log.debug("Secret not found: {}", key);
      return Optional.empty();
    } catch (SecretsManagerException e) {
      log.error("Failed to get secret: {}", key, e);
      return Optional.empty();
    }
  }

  @Override
  public Optional<SecretValue> get(SecretKey key, String version) {
    try {
      GetSecretValueRequest.Builder request =
          GetSecretValueRequest.builder().secretId(secretId(key));

      if (isVersionStage(version)) {
        request.versionStage(version);
      } else {
        request.versionId(version);
      }

      GetSecretValueResponse response = client.getSecretValue(request.build());
      return Optional.of(toSecretValue(response));

    } catch (ResourceNotFoundException e) {
      log.debug("Secret version not found: {} v{}", key, version);
      return Optional.empty();
    } catch (SecretsManagerException e) {
      log.error("Failed to get secret version: {} v{}", key, version, e);
      return Optional.empty();
    }
  }

  @Override
  public String put(SecretKey key, SecretValue value) {
    try {
      String id = secretId(key);

      PutSecretValueResponse putResponse;
      try {
        CreateSecretRequest create = CreateSecretRequest.builder().name(id).build();
        CreateSecretRequest.Builder createBuilder = create.toBuilder();

        SecretPayload payload = toPayload(value);
        payload.apply(createBuilder);

        CreateSecretResponse created = client.createSecret(createBuilder.build());
        return created.versionId();

      } catch (ResourceExistsException exists) {
        PutSecretValueRequest.Builder putBuilder = PutSecretValueRequest.builder().secretId(id);
        SecretPayload payload = toPayload(value);
        payload.apply(putBuilder);

        putResponse = client.putSecretValue(putBuilder.build());
        return putResponse.versionId();
      }

    } catch (SecretsManagerException e) {
      log.error("Failed to put secret: {}", key, e);
      throw new RuntimeException("Failed to store secret: " + key, e);
    }
  }

  @Override
  public String put(SecretKey key, Map<String, String> data) {
    try {
      String id = secretId(key);
      String json = serializeToJson(data);

      try {
        CreateSecretResponse created =
            client.createSecret(CreateSecretRequest.builder().name(id).secretString(json).build());
        return created.versionId();
      } catch (ResourceExistsException exists) {
        PutSecretValueResponse put =
            client.putSecretValue(
                PutSecretValueRequest.builder().secretId(id).secretString(json).build());
        return put.versionId();
      }

    } catch (SecretsManagerException e) {
      log.error("Failed to put secret map: {}", key, e);
      throw new RuntimeException("Failed to store secret map: " + key, e);
    }
  }

  @Override
  public boolean delete(SecretKey key) {
    try {
      client.deleteSecret(DeleteSecretRequest.builder().secretId(secretId(key)).build());
      return true;
    } catch (ResourceNotFoundException e) {
      return false;
    } catch (SecretsManagerException e) {
      log.error("Failed to delete secret: {}", key, e);
      return false;
    }
  }

  @Override
  public boolean exists(SecretKey key) {
    try {
      client.describeSecret(DescribeSecretRequest.builder().secretId(secretId(key)).build());
      return true;
    } catch (ResourceNotFoundException e) {
      return false;
    } catch (SecretsManagerException e) {
      log.error("Error checking secret existence: {}", key, e);
      return false;
    }
  }

  @Override
  public List<SecretKey> list(String prefix) {
    try {
      List<SecretKey> result = new ArrayList<>();

      String nextToken = null;
      do {
        ListSecretsResponse response =
            client.listSecrets(ListSecretsRequest.builder().nextToken(nextToken).build());

        for (SecretListEntry entry : response.secretList()) {
          String name = entry.name();
          if (prefix == null || prefix.isBlank() || name.startsWith(prefix)) {
            result.add(SecretKey.of(name));
          }
        }

        nextToken = response.nextToken();
      } while (nextToken != null && !nextToken.isBlank());

      return result;

    } catch (SecretsManagerException e) {
      log.error("Failed to list secrets with prefix: {}", prefix, e);
      return Collections.emptyList();
    }
  }

  private static String secretId(SecretKey key) {
    return key.value();
  }

  private static boolean isVersionStage(String version) {
    if (version == null || version.isBlank()) {
      return false;
    }

    // Default AWS rotation labels.
    return "AWSCURRENT".equals(version) || "AWSPREVIOUS".equals(version) || "AWSPENDING".equals(version);
  }

  private static SecretValue toSecretValue(GetSecretValueResponse response) {
    String version = response.versionId();
    Instant createdAt = response.createdDate() != null ? response.createdDate() : Instant.now();

    if (response.secretString() != null) {
      return SecretValue.of(response.secretString().getBytes(StandardCharsets.UTF_8), version, createdAt, null);
    }

    SdkBytes binary = response.secretBinary();
    if (binary != null) {
      return SecretValue.of(binary.asByteArray(), version, createdAt, null);
    }

    return SecretValue.of(new byte[0], version, createdAt, null);
  }

  private static SecretPayload toPayload(SecretValue value) {
    byte[] bytes = value.asBytes();

    String asUtf8 = new String(bytes, StandardCharsets.UTF_8);
    boolean looksLikeString = Arrays.equals(asUtf8.getBytes(StandardCharsets.UTF_8), bytes);

    if (looksLikeString) {
      return SecretPayload.string(asUtf8);
    }
    return SecretPayload.binary(SdkBytes.fromByteArray(bytes));
  }

  private static String serializeToJson(Map<String, String> data) {
    if (data == null || data.isEmpty()) {
      return "{}";
    }

    return data.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .map(e -> "\"" + escapeJson(e.getKey()) + "\":\"" + escapeJson(e.getValue()) + "\"")
        .collect(Collectors.joining(",", "{", "}"));
  }

  private static String escapeJson(String s) {
    if (s == null) {
      return "";
    }
    return s.replace("\\", "\\\\").replace("\"", "\\\"");
  }

  private sealed interface SecretPayload permits SecretPayload.StringPayload, SecretPayload.BinaryPayload {
    void apply(CreateSecretRequest.Builder builder);

    void apply(PutSecretValueRequest.Builder builder);

    static SecretPayload string(String value) {
      return new StringPayload(value);
    }

    static SecretPayload binary(SdkBytes bytes) {
      return new BinaryPayload(bytes);
    }

    record StringPayload(String value) implements SecretPayload {
      @Override
      public void apply(CreateSecretRequest.Builder builder) {
        builder.secretString(value);
      }

      @Override
      public void apply(PutSecretValueRequest.Builder builder) {
        builder.secretString(value);
      }
    }

    record BinaryPayload(SdkBytes bytes) implements SecretPayload {
      @Override
      public void apply(CreateSecretRequest.Builder builder) {
        builder.secretBinary(bytes);
      }

      @Override
      public void apply(PutSecretValueRequest.Builder builder) {
        builder.secretBinary(bytes);
      }
    }
  }
}
