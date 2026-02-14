package com.marcusprado02.commons.spring.secrets;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.marcusprado02.commons.adapters.secrets.azure.AzureKeyVaultSecretStoreAdapter;
import com.marcusprado02.commons.ports.secrets.SecretStorePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for Azure Key Vault secrets adapter.
 *
 * <p>Activated when:
 *
 * <ul>
 *   <li>{@code commons.secrets.type=azure}
 *   <li>{@code AzureKeyVaultSecretStoreAdapter} is on classpath
 *   <li>{@code commons.secrets.azure.enabled=true} (default)
 * </ul>
 */
@AutoConfiguration
@ConditionalOnClass(AzureKeyVaultSecretStoreAdapter.class)
@ConditionalOnProperty(
    prefix = "commons.secrets.azure",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
@EnableConfigurationProperties(SecretsProperties.class)
public class AzureSecretsAutoConfiguration {

  private static final Logger log = LoggerFactory.getLogger(AzureSecretsAutoConfiguration.class);

  @Bean
  @ConditionalOnMissingBean
  public SecretClient secretClient(SecretsProperties properties) {
    String vaultUrl = properties.getAzure().getVaultUrl();

    if (vaultUrl == null || vaultUrl.isBlank()) {
      throw new IllegalStateException(
          "Azure Key Vault URL is required. Set 'commons.secrets.azure.vault-url' property.");
    }

    SecretClient client =
        new SecretClientBuilder()
            .vaultUrl(vaultUrl)
            .credential(new DefaultAzureCredentialBuilder().build())
            .buildClient();

    log.info("Configured Azure Key Vault: {}", vaultUrl);

    return client;
  }

  @Bean
  @ConditionalOnMissingBean(SecretStorePort.class)
  @ConditionalOnProperty(prefix = "commons.secrets", name = "type", havingValue = "azure")
  public SecretStorePort secretStorePort(SecretClient client) {
    AzureKeyVaultSecretStoreAdapter adapter = new AzureKeyVaultSecretStoreAdapter(client);

    log.info("Created Azure Key Vault secret store adapter");

    return adapter;
  }
}
