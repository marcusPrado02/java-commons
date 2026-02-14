package com.marcusprado02.commons.spring.secrets;

import com.marcusprado02.commons.adapters.secrets.vault.VaultSecretStoreAdapter;
import com.marcusprado02.commons.ports.secrets.SecretStorePort;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultToken;

/**
 * Auto-configuration for HashiCorp Vault secrets adapter.
 *
 * <p>Activated when:
 *
 * <ul>
 *   <li>{@code commons.secrets.type=vault} (default)
 *   <li>{@code VaultSecretStoreAdapter} is on classpath
 *   <li>{@code commons.secrets.vault.enabled=true} (default)
 * </ul>
 */
@AutoConfiguration
@ConditionalOnClass(VaultSecretStoreAdapter.class)
@ConditionalOnProperty(
    prefix = "commons.secrets.vault",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
@EnableConfigurationProperties(SecretsProperties.class)
public class VaultSecretsAutoConfiguration {

  private static final Logger log = LoggerFactory.getLogger(VaultSecretsAutoConfiguration.class);

  @Bean
  @ConditionalOnMissingBean
  public VaultTemplate vaultTemplate(SecretsProperties properties) {
    SecretsProperties.Vault vault = properties.getVault();

    VaultEndpoint endpoint = VaultEndpoint.from(URI.create(vault.getUri()));
    VaultToken token = VaultToken.of(vault.getToken());
    TokenAuthentication authentication = new TokenAuthentication(token);

    VaultTemplate template = new VaultTemplate(endpoint, authentication);

    log.info(
        "Configured Vault connection: {} (KV path: {}, version: {})",
        vault.getUri(),
        vault.getKvPath(),
        vault.getKvVersion());

    return template;
  }

  @Bean
  @ConditionalOnMissingBean(SecretStorePort.class)
  @ConditionalOnProperty(
      prefix = "commons.secrets",
      name = "type",
      havingValue = "vault",
      matchIfMissing = true)
  public SecretStorePort secretStorePort(
      VaultTemplate vaultTemplate, SecretsProperties properties) {
    String kvPath = properties.getVault().getKvPath();

    VaultSecretStoreAdapter adapter = new VaultSecretStoreAdapter(vaultTemplate, kvPath);

    log.info("Created Vault secret store adapter (KV path: {})", kvPath);

    return adapter;
  }
}
