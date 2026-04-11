package com.marcusprado02.commons.adapters.secrets.azure;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.certificates.CertificateClient;
import com.azure.security.keyvault.certificates.CertificateClientBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import java.util.Objects;

/** AzureKeyVaultClients implementation. */
public final class AzureKeyVaultClients {

  private AzureKeyVaultClients() {}

  /** Executes the secretClient operation. */
  public static SecretClient secretClient(String vaultUrl) {
    Objects.requireNonNull(vaultUrl, "vaultUrl cannot be null");

    return new SecretClientBuilder()
        .vaultUrl(vaultUrl)
        .credential(new DefaultAzureCredentialBuilder().build())
        .buildClient();
  }

  /** Executes the certificateClient operation. */
  public static CertificateClient certificateClient(String vaultUrl) {
    Objects.requireNonNull(vaultUrl, "vaultUrl cannot be null");

    return new CertificateClientBuilder()
        .vaultUrl(vaultUrl)
        .credential(new DefaultAzureCredentialBuilder().build())
        .buildClient();
  }
}
