package com.marcusprado02.commons.spring.secrets;

import static org.assertj.core.api.Assertions.assertThat;

import com.marcusprado02.commons.adapters.secrets.vault.VaultSecretStoreAdapter;
import com.marcusprado02.commons.ports.secrets.SecretKey;
import com.marcusprado02.commons.ports.secrets.SecretStorePort;
import com.marcusprado02.commons.ports.secrets.SecretValue;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.vault.core.VaultTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class VaultSecretsAutoConfigurationTest {

  @Container
  private static final GenericContainer<?> vault =
      new GenericContainer<>(DockerImageName.parse("hashicorp/vault:1.15"))
          .withExposedPorts(8200)
          .withEnv("VAULT_DEV_ROOT_TOKEN_ID", "test-token")
          .withEnv("VAULT_DEV_LISTEN_ADDRESS", "0.0.0.0:8200");

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(VaultSecretsAutoConfiguration.class));

  @Test
  void shouldAutoConfigureVaultSecrets() {
    contextRunner
        .withPropertyValues(
            "commons.secrets.type=vault",
            "commons.secrets.vault.uri=http://"
                + vault.getHost()
                + ":"
                + vault.getFirstMappedPort(),
            "commons.secrets.vault.token=test-token")
        .run(
            context -> {
              assertThat(context).hasSingleBean(VaultTemplate.class);
              assertThat(context).hasSingleBean(SecretStorePort.class);

              SecretStorePort secretStore = context.getBean(SecretStorePort.class);
              assertThat(secretStore).isInstanceOf(VaultSecretStoreAdapter.class);

              // Test secret operations
              SecretKey key = SecretKey.of("test-key");
              SecretValue value = SecretValue.of("test-value");
              secretStore.put(key, value);
              assertThat(secretStore.get(key).map(SecretValue::asString)).hasValue("test-value");
            });
  }

  @Test
  void shouldRespectKvPath() {
    contextRunner
        .withPropertyValues(
            "commons.secrets.type=vault",
            "commons.secrets.vault.uri=http://"
                + vault.getHost()
                + ":"
                + vault.getFirstMappedPort(),
            "commons.secrets.vault.token=test-token",
            "commons.secrets.vault.kv-path=secret",
            "commons.secrets.vault.kv-version=2")
        .run(
            context -> {
              SecretStorePort secretStore = context.getBean(SecretStorePort.class);

              SecretKey key = SecretKey.of("key1");
              SecretValue value = SecretValue.of("value1");
              secretStore.put(key, value);
              assertThat(secretStore.get(key).map(SecretValue::asString)).hasValue("value1");
            });
  }

  @Test
  void shouldNotAutoConfigureWhenDisabled() {
    contextRunner
        .withPropertyValues(
            "commons.secrets.vault.enabled=false",
            "commons.secrets.vault.uri=http://"
                + vault.getHost()
                + ":"
                + vault.getFirstMappedPort(),
            "commons.secrets.vault.token=test-token")
        .run(context -> assertThat(context).doesNotHaveBean(SecretStorePort.class));
  }

  @Test
  void shouldNotAutoConfigureWhenTypeIsNotVault() {
    contextRunner
        .withPropertyValues(
            "commons.secrets.type=aws",
            "commons.secrets.vault.uri=http://"
                + vault.getHost()
                + ":"
                + vault.getFirstMappedPort(),
            "commons.secrets.vault.token=test-token")
        .run(context -> assertThat(context).doesNotHaveBean(SecretStorePort.class));
  }
}
