package com.marcusprado02.commons.spring.secrets;

import static org.assertj.core.api.Assertions.assertThat;

import com.marcusprado02.commons.ports.secrets.SecretKey;
import com.marcusprado02.commons.ports.secrets.SecretStorePort;
import com.marcusprado02.commons.ports.secrets.SecretValue;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

class SecretsHealthIndicatorTest {

  @Test
  void shouldReturnUpWhenSecretStoreIsHealthy() {
    // Arrange
    SecretStorePort secretStore = new InMemorySecretStore();
    SecretsHealthIndicator indicator = new SecretsHealthIndicator(secretStore);

    // Act
    Health health = indicator.health();

    // Assert
    assertThat(health.getStatus()).isEqualTo(Status.UP);
    assertThat(health.getDetails()).containsKey("type");
    assertThat(health.getDetails()).containsKey("status");
  }

  @Test
  void shouldReturnDownWhenSecretStoreThrowsException() {
    // Arrange
    SecretStorePort secretStore = new FailingSecretStore();
    SecretsHealthIndicator indicator = new SecretsHealthIndicator(secretStore);

    // Act
    Health health = indicator.health();

    // Assert
    assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    assertThat(health.getDetails()).containsKey("error");
  }

  private static class InMemorySecretStore implements SecretStorePort {
    private final java.util.Map<SecretKey, SecretValue> secrets =
        new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    public String put(SecretKey key, SecretValue value) {
      secrets.put(key, value);
      return "v1";
    }

    @Override
    public java.util.Optional<SecretValue> get(SecretKey key) {
      return java.util.Optional.ofNullable(secrets.get(key));
    }

    @Override
    public java.util.Optional<SecretValue> get(SecretKey key, String version) {
      return get(key);
    }

    @Override
    public String putMap(SecretKey key, java.util.Map<String, String> values) {
      return put(key, SecretValue.of(values.toString()));
    }

    @Override
    public java.util.Map<String, String> getMap(SecretKey key) {
      return java.util.Map.of();
    }

    @Override
    public void delete(SecretKey key) {
      secrets.remove(key);
    }

    @Override
    public void delete(SecretKey key, String version) {
      delete(key);
    }

    @Override
    public boolean exists(SecretKey key) {
      return secrets.containsKey(key);
    }

    @Override
    public java.util.List<SecretKey> list(String prefix) {
      return secrets.keySet().stream()
          .filter(k -> k.value().startsWith(prefix))
          .collect(java.util.stream.Collectors.toList());
    }
  }

  private static class FailingSecretStore implements SecretStorePort {
    @Override
    public String put(SecretKey key, SecretValue value) {
      throw new RuntimeException("Secret store connection failed");
    }

    @Override
    public java.util.Optional<SecretValue> get(SecretKey key) {
      throw new RuntimeException("Secret store connection failed");
    }

    @Override
    public java.util.Optional<SecretValue> get(SecretKey key, String version) {
      throw new RuntimeException("Secret store connection failed");
    }

    @Override
    public String putMap(SecretKey key, java.util.Map<String, String> values) {
      throw new RuntimeException("Secret store connection failed");
    }

    @Override
    public java.util.Map<String, String> getMap(SecretKey key) {
      throw new RuntimeException("Secret store connection failed");
    }

    @Override
    public void delete(SecretKey key) {
      throw new RuntimeException("Secret store connection failed");
    }

    @Override
    public void delete(SecretKey key, String version) {
      throw new RuntimeException("Secret store connection failed");
    }

    @Override
    public boolean exists(SecretKey key) {
      throw new RuntimeException("Secret store connection failed");
    }

    @Override
    public java.util.List<SecretKey> list(String prefix) {
      throw new RuntimeException("Secret store connection failed");
    }
  }
}
