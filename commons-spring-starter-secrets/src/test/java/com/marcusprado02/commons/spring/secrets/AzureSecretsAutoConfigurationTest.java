package com.marcusprado02.commons.spring.secrets;

import static org.assertj.core.api.Assertions.assertThat;

import com.marcusprado02.commons.ports.secrets.SecretStorePort;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Tests for AzureSecretsAutoConfiguration conditional logic.
 *
 * <p>These tests cover the Spring conditional conditions and the vault URL validation without
 * requiring a real Azure Key Vault endpoint.
 */
class AzureSecretsAutoConfigurationTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(AzureSecretsAutoConfiguration.class));

  @Test
  void shouldFailWhenVaultUrlIsNull() {
    // Auto-configuration activated (type=azure) but no vault URL configured
    contextRunner
        .withPropertyValues("commons.secrets.type=azure")
        .run(context -> assertThat(context).hasFailed());
  }

  @Test
  void shouldFailWhenVaultUrlIsBlank() {
    contextRunner
        .withPropertyValues("commons.secrets.type=azure", "commons.secrets.azure.vault-url= ")
        .run(context -> assertThat(context).hasFailed());
  }

  @Test
  void shouldNotAutoConfigureWhenDisabled() {
    contextRunner
        .withPropertyValues(
            "commons.secrets.type=azure",
            "commons.secrets.azure.enabled=false",
            "commons.secrets.azure.vault-url=https://example.vault.azure.net")
        .run(context -> assertThat(context).doesNotHaveBean(SecretStorePort.class));
  }

  @Test
  void shouldNotAutoConfigureWhenTypeIsNotAzure() {
    contextRunner
        .withPropertyValues(
            "commons.secrets.type=vault",
            "commons.secrets.azure.vault-url=https://example.vault.azure.net")
        .run(context -> assertThat(context).doesNotHaveBean(SecretStorePort.class));
  }
}
