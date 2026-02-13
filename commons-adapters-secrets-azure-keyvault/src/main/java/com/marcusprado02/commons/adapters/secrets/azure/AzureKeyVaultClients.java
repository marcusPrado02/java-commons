package com.marcusprado02.commons.adapters.secrets.azure;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.certificates.CertificateClient;
import com.azure.security.keyvault.certificates.CertificateClientBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import java.util.Objects;

public final class AzureKeyVaultClients {

  private AzureKeyVaultClients() {}

  public static SecretClient secretClient(String vaultUrl) {
    Objects.requireNonNull(vaultUrl, "vaultUrl cannot be null");

    return new SecretClientBuilder()
        .vaultUrl(vaultUrl)
        .credential(new DefaultAzureCredentialBuilder().build())
        .buildClient();
  }

  public static CertificateClient certificateClient(String vaultUrl) {
    Objects.requireNonNull(vaultUrl, "vaultUrl cannot be null");

    return new CertificateClientBuilder()
        .vaultUrl(vaultUrl)
        .credential(new DefaultAzureCredentialBuilder().build())
        .buildClient();
  }
}
