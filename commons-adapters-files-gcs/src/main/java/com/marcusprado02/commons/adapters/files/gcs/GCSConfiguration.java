package com.marcusprado02.commons.adapters.files.gcs;

import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Objects;

/**
 * Configuration for GCS Storage client. Supports multiple authentication methods: - Service Account
 * JSON file - Application Default Credentials (ADC) - Custom credentials
 */
public record GCSConfiguration(
    String projectId,
    AuthenticationType authenticationType,
    String serviceAccountPath,
    Credentials customCredentials,
    String endpoint) {

  public GCSConfiguration {
    Objects.requireNonNull(projectId, "projectId must not be null");
    Objects.requireNonNull(authenticationType, "authenticationType must not be null");
  }

  /** Authentication type for GCS. */
  public enum AuthenticationType {
    /** Service Account JSON file */
    SERVICE_ACCOUNT,
    /** Application Default Credentials (from environment) */
    APPLICATION_DEFAULT,
    /** Custom credentials provided programmatically */
    CUSTOM
  }

  /**
   * Create configuration with Service Account JSON file.
   *
   * @param projectId GCP project ID
   * @param serviceAccountPath path to service account JSON file
   * @return configuration
   */
  public static GCSConfiguration withServiceAccount(String projectId, String serviceAccountPath) {
    return new GCSConfiguration(
        projectId, AuthenticationType.SERVICE_ACCOUNT, serviceAccountPath, null, null);
  }

  /**
   * Create configuration with Application Default Credentials. This uses credentials from: -
   * GOOGLE_APPLICATION_CREDENTIALS environment variable - Compute Engine/App Engine/Cloud Run
   * service account - gcloud auth application-default login
   *
   * @param projectId GCP project ID
   * @return configuration
   */
  public static GCSConfiguration withApplicationDefault(String projectId) {
    return new GCSConfiguration(
        projectId, AuthenticationType.APPLICATION_DEFAULT, null, null, null);
  }

  /**
   * Create configuration with custom credentials.
   *
   * @param projectId GCP project ID
   * @param credentials custom credentials
   * @return configuration
   */
  public static GCSConfiguration withCustomCredentials(String projectId, Credentials credentials) {
    return new GCSConfiguration(projectId, AuthenticationType.CUSTOM, null, credentials, null);
  }

  /**
   * Create configuration for fake-gcs-server (testing).
   *
   * @param projectId project ID
   * @param endpoint fake-gcs-server endpoint
   * @return configuration
   */
  public static GCSConfiguration forTesting(String projectId, String endpoint) {
    try {
      // For testing, create no-op credentials
      var credentials = GoogleCredentials.create(null);
      return new GCSConfiguration(
          projectId, AuthenticationType.CUSTOM, null, credentials, endpoint);
    } catch (Exception e) {
      throw new RuntimeException("Failed to create testing configuration", e);
    }
  }

  /**
   * Load credentials based on authentication type.
   *
   * @return Google credentials
   * @throws IOException if credentials cannot be loaded
   */
  public Credentials loadCredentials() throws IOException {
    return switch (authenticationType) {
      case SERVICE_ACCOUNT -> {
        if (serviceAccountPath == null) {
          throw new IllegalStateException("Service account path must be provided");
        }
        try (var input = new FileInputStream(serviceAccountPath)) {
          yield ServiceAccountCredentials.fromStream(input);
        }
      }
      case APPLICATION_DEFAULT -> GoogleCredentials.getApplicationDefault();
      case CUSTOM -> {
        if (customCredentials == null) {
          throw new IllegalStateException("Custom credentials must be provided");
        }
        yield customCredentials;
      }
    };
  }

  /**
   * Check if this is a custom endpoint (e.g., for testing).
   *
   * @return true if custom endpoint is configured
   */
  public boolean hasCustomEndpoint() {
    return endpoint != null && !endpoint.isBlank();
  }

  /** Builder for GCSConfiguration. */
  public static class Builder {
    private String projectId;
    private AuthenticationType authenticationType = AuthenticationType.APPLICATION_DEFAULT;
    private String serviceAccountPath;
    private Credentials customCredentials;
    private String endpoint;

    public Builder projectId(String projectId) {
      this.projectId = projectId;
      return this;
    }

    public Builder serviceAccount(String path) {
      this.authenticationType = AuthenticationType.SERVICE_ACCOUNT;
      this.serviceAccountPath = path;
      return this;
    }

    public Builder applicationDefault() {
      this.authenticationType = AuthenticationType.APPLICATION_DEFAULT;
      return this;
    }

    public Builder customCredentials(Credentials credentials) {
      this.authenticationType = AuthenticationType.CUSTOM;
      this.customCredentials = credentials;
      return this;
    }

    public Builder endpoint(String endpoint) {
      this.endpoint = endpoint;
      return this;
    }

    public GCSConfiguration build() {
      return new GCSConfiguration(
          projectId, authenticationType, serviceAccountPath, customCredentials, endpoint);
    }
  }

  public static Builder builder() {
    return new Builder();
  }
}
