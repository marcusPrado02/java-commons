package com.marcusprado02.commons.adapters.files.azureblob;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Factory for creating Azure Blob clients and adapters.
 */
public final class AzureBlobClientFactory {

  private static final Pattern ACCOUNT_NAME_PATTERN = Pattern.compile("AccountName=([^;]+)");

  private AzureBlobClientFactory() {
  }

  /**
   * Create a BlobServiceClient with the given configuration.
   */
  public static BlobServiceClient createClient(AzureBlobConfiguration config) {
    Objects.requireNonNull(config, "config must not be null");

    BlobServiceClientBuilder builder = new BlobServiceClientBuilder();

    switch (config.authenticationType()) {
      case CONNECTION_STRING -> {
        builder.connectionString(config.connectionString());
      }
      case SAS_TOKEN -> {
        Objects.requireNonNull(config.endpoint(), "endpoint required for SAS token authentication");
        Objects.requireNonNull(config.sasToken(), "sasToken required for SAS token authentication");
        builder.endpoint(config.endpoint()).sasToken(config.sasToken());
      }
      case MANAGED_IDENTITY -> {
        Objects.requireNonNull(config.endpoint(), "endpoint required for Managed Identity");
        Objects.requireNonNull(config.accountName(), "accountName required for Managed Identity");
        TokenCredential credential = new DefaultAzureCredentialBuilder().build();
        builder.endpoint(config.endpoint()).credential(credential);
      }
    }

    return builder.buildClient();
  }

  /**
   * Create a fully configured AzureBlobFileStoreAdapter.
   */
  public static AzureBlobFileStoreAdapter createAdapter(AzureBlobConfiguration config) {
    Objects.requireNonNull(config, "config must not be null");

    BlobServiceClient client = createClient(config);

    return new AzureBlobFileStoreAdapter(client, config);
  }

  /**
   * Extract account name from connection string.
   */
  static String extractAccountName(String connectionString) {
    Matcher matcher = ACCOUNT_NAME_PATTERN.matcher(connectionString);
    if (matcher.find()) {
      return matcher.group(1);
    }
    throw new IllegalArgumentException("Could not extract AccountName from connection string");
  }
}
