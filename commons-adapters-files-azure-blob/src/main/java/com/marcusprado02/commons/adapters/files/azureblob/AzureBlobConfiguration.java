package com.marcusprado02.commons.adapters.files.azureblob;

import java.util.Objects;

/**
 * Configuration for AzureBlobFileStoreAdapter.
 */
public record AzureBlobConfiguration(
    String endpoint,
    String accountName,
    AuthenticationType authenticationType,
    String connectionString,
    String sasToken,
    boolean useManagedIdentity,
    int maxRetries,
    long timeoutSeconds
) {

  public AzureBlobConfiguration {
    if (authenticationType == null) {
      throw new IllegalArgumentException("authenticationType must not be null");
    }
    if (maxRetries < 0) {
      throw new IllegalArgumentException("maxRetries must be non-negative");
    }
    if (timeoutSeconds <= 0) {
      throw new IllegalArgumentException("timeoutSeconds must be positive");
    }
  }

  /**
   * Authentication type for Azure Blob Storage.
   */
  public enum AuthenticationType {
    CONNECTION_STRING,
    SAS_TOKEN,
    MANAGED_IDENTITY
  }

  /**
   * Default configuration with connection string.
   */
  public static AzureBlobConfiguration withConnectionString(String connectionString) {
    return new AzureBlobConfiguration(
        null,
        null,
        AuthenticationType.CONNECTION_STRING,
        connectionString,
        null,
        false,
        3,
        30
    );
  }

  /**
   * Configuration with SAS token.
   */
  public static AzureBlobConfiguration withSasToken(String endpoint, String sasToken) {
    return new AzureBlobConfiguration(
        endpoint,
        null,
        AuthenticationType.SAS_TOKEN,
        null,
        sasToken,
        false,
        3,
        30
    );
  }

  /**
   * Configuration with Managed Identity.
   */
  public static AzureBlobConfiguration withManagedIdentity(String endpoint, String accountName) {
    return new AzureBlobConfiguration(
        endpoint,
        accountName,
        AuthenticationType.MANAGED_IDENTITY,
        null,
        null,
        true,
        3,
        30
    );
  }

  /**
   * Configuration for Azurite (local emulator).
   */
  public static AzureBlobConfiguration azurite(String connectionString) {
    return new AzureBlobConfiguration(
        null,
        null,
        AuthenticationType.CONNECTION_STRING,
        connectionString,
        null,
        false,
        3,
        30
    );
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String endpoint;
    private String accountName;
    private AuthenticationType authenticationType = AuthenticationType.CONNECTION_STRING;
    private String connectionString;
    private String sasToken;
    private boolean useManagedIdentity = false;
    private int maxRetries = 3;
    private long timeoutSeconds = 30;

    public Builder endpoint(String endpoint) {
      this.endpoint = endpoint;
      return this;
    }

    public Builder accountName(String accountName) {
      this.accountName = accountName;
      return this;
    }

    public Builder authenticationType(AuthenticationType authenticationType) {
      this.authenticationType = authenticationType;
      return this;
    }

    public Builder connectionString(String connectionString) {
      this.connectionString = connectionString;
      this.authenticationType = AuthenticationType.CONNECTION_STRING;
      return this;
    }

    public Builder sasToken(String sasToken) {
      this.sasToken = sasToken;
      this.authenticationType = AuthenticationType.SAS_TOKEN;
      return this;
    }

    public Builder useManagedIdentity(boolean useManagedIdentity) {
      this.useManagedIdentity = useManagedIdentity;
      if (useManagedIdentity) {
        this.authenticationType = AuthenticationType.MANAGED_IDENTITY;
      }
      return this;
    }

    public Builder maxRetries(int maxRetries) {
      this.maxRetries = maxRetries;
      return this;
    }

    public Builder timeoutSeconds(long timeoutSeconds) {
      this.timeoutSeconds = timeoutSeconds;
      return this;
    }

    public AzureBlobConfiguration build() {
      return new AzureBlobConfiguration(
          endpoint,
          accountName,
          authenticationType,
          connectionString,
          sasToken,
          useManagedIdentity,
          maxRetries,
          timeoutSeconds
      );
    }
  }
}
