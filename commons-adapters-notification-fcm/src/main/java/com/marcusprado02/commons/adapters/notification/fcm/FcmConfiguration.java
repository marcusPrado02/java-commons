package com.marcusprado02.commons.adapters.notification.fcm;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * Configuration for Firebase Cloud Messaging adapter.
 *
 * <p>Provides builder pattern for FCM configuration with service account credentials.
 *
 * <p>Example:
 *
 * <pre>{@code
 * FcmConfiguration config = FcmConfiguration.builder()
 *     .credentialsPath("/path/to/service-account.json")
 *     .build();
 *
 * // Or with InputStream
 * FcmConfiguration config = FcmConfiguration.builder()
 *     .credentials(inputStream)
 *     .build();
 * }</pre>
 */
public class FcmConfiguration {

  private final GoogleCredentials credentials;
  private final String projectId;
  private final boolean validateTokens;

  private FcmConfiguration(
      GoogleCredentials credentials, String projectId, boolean validateTokens) {
    this.credentials = credentials;
    this.projectId = projectId;
    this.validateTokens = validateTokens;
  }

  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates FCM configuration from service account JSON file path.
   *
   * @param credentialsPath path to service account JSON file
   * @return configuration
   * @throws IOException if credentials file cannot be read
   */
  public static FcmConfiguration fromPath(String credentialsPath) throws IOException {
    return builder().credentialsPath(credentialsPath).build();
  }

  /**
   * Creates FCM configuration from service account JSON input stream.
   *
   * @param credentialsStream input stream with service account JSON
   * @return configuration
   * @throws IOException if credentials cannot be read
   */
  public static FcmConfiguration fromStream(InputStream credentialsStream) throws IOException {
    return builder().credentials(credentialsStream).build();
  }

  public GoogleCredentials getCredentials() {
    return credentials;
  }

  public String getProjectId() {
    return projectId;
  }

  public boolean isValidateTokens() {
    return validateTokens;
  }

  public FirebaseApp initializeApp() {
    FirebaseOptions.Builder optionsBuilder = FirebaseOptions.builder().setCredentials(credentials);

    if (projectId != null) {
      optionsBuilder.setProjectId(projectId);
    }

    return FirebaseApp.initializeApp(optionsBuilder.build());
  }

  public static class Builder {
    private GoogleCredentials credentials;
    private String projectId;
    private boolean validateTokens = true;

    public Builder credentialsPath(String path) throws IOException {
      Objects.requireNonNull(path, "credentialsPath cannot be null");
      try (FileInputStream serviceAccount = new FileInputStream(path)) {
        this.credentials = GoogleCredentials.fromStream(serviceAccount);
      }
      return this;
    }

    public Builder credentials(InputStream credentialsStream) throws IOException {
      Objects.requireNonNull(credentialsStream, "credentialsStream cannot be null");
      this.credentials = GoogleCredentials.fromStream(credentialsStream);
      return this;
    }

    public Builder credentials(GoogleCredentials credentials) {
      this.credentials = Objects.requireNonNull(credentials, "credentials cannot be null");
      return this;
    }

    public Builder projectId(String projectId) {
      this.projectId = projectId;
      return this;
    }

    public Builder validateTokens(boolean validateTokens) {
      this.validateTokens = validateTokens;
      return this;
    }

    public FcmConfiguration build() {
      Objects.requireNonNull(credentials, "credentials is required");
      return new FcmConfiguration(credentials, projectId, validateTokens);
    }
  }
}
