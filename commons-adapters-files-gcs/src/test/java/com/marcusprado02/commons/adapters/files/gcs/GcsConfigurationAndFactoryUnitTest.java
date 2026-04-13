package com.marcusprado02.commons.adapters.files.gcs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.auth.oauth2.GoogleCredentials;
import com.marcusprado02.commons.adapters.files.gcs.GcsConfiguration.AuthenticationType;
import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for GcsConfiguration and GcsClientFactory covering static factory and branch paths.
 */
class GcsConfigurationAndFactoryUnitTest {

  // ── GcsConfiguration static factories ───────────────────────────────────

  @Test
  void withServiceAccountShouldSetFieldsCorrectly() {
    var config = GcsConfiguration.withServiceAccount("my-project", "/path/key.json");

    assertThat(config.projectId()).isEqualTo("my-project");
    assertThat(config.authenticationType()).isEqualTo(AuthenticationType.SERVICE_ACCOUNT);
    assertThat(config.serviceAccountPath()).isEqualTo("/path/key.json");
    assertThat(config.customCredentials()).isNull();
    assertThat(config.endpoint()).isNull();
  }

  @Test
  void withApplicationDefaultShouldSetFieldsCorrectly() {
    var config = GcsConfiguration.withApplicationDefault("my-project");

    assertThat(config.projectId()).isEqualTo("my-project");
    assertThat(config.authenticationType()).isEqualTo(AuthenticationType.APPLICATION_DEFAULT);
    assertThat(config.serviceAccountPath()).isNull();
    assertThat(config.customCredentials()).isNull();
  }

  @Test
  void withCustomCredentialsShouldSetFieldsCorrectly() throws IOException {
    var creds = GoogleCredentials.create(null);
    var config = GcsConfiguration.withCustomCredentials("my-project", creds);

    assertThat(config.projectId()).isEqualTo("my-project");
    assertThat(config.authenticationType()).isEqualTo(AuthenticationType.CUSTOM);
    assertThat(config.customCredentials()).isSameAs(creds);
  }

  @Test
  void forTestingShouldCreateCustomCredentialsConfig() {
    var config = GcsConfiguration.forTesting("test-project", "http://localhost:4443");

    assertThat(config.projectId()).isEqualTo("test-project");
    assertThat(config.authenticationType()).isEqualTo(AuthenticationType.CUSTOM);
    assertThat(config.customCredentials()).isNotNull();
    assertThat(config.endpoint()).isEqualTo("http://localhost:4443");
  }

  // ── compact constructor null checks ──────────────────────────────────────

  @Test
  void constructorShouldThrowWhenProjectIdIsNull() {
    assertThatThrownBy(
            () ->
                new GcsConfiguration(
                    null, AuthenticationType.APPLICATION_DEFAULT, null, null, null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void constructorShouldThrowWhenAuthenticationTypeIsNull() {
    assertThatThrownBy(() -> new GcsConfiguration("proj", null, null, null, null))
        .isInstanceOf(NullPointerException.class);
  }

  // ── hasCustomEndpoint ─────────────────────────────────────────────────────

  @Test
  void hasCustomEndpointShouldReturnTrueWhenEndpointSet() {
    var config = GcsConfiguration.forTesting("proj", "http://localhost:4443");

    assertThat(config.hasCustomEndpoint()).isTrue();
  }

  @Test
  void hasCustomEndpointShouldReturnFalseWhenEndpointIsNull() {
    var config = GcsConfiguration.withApplicationDefault("proj");

    assertThat(config.hasCustomEndpoint()).isFalse();
  }

  // ── loadCredentials ───────────────────────────────────────────────────────

  @Test
  void loadCredentialsShouldReturnCustomCredentials() throws IOException {
    var creds = GoogleCredentials.create(null);
    var config = GcsConfiguration.withCustomCredentials("proj", creds);

    assertThat(config.loadCredentials()).isSameAs(creds);
  }

  @Test
  void loadCredentialsShouldThrowWhenCustomCredentialsIsNull() {
    var config = new GcsConfiguration("proj", AuthenticationType.CUSTOM, null, null, null);

    assertThatThrownBy(config::loadCredentials).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void loadCredentialsShouldThrowWhenServiceAccountPathIsNull() {
    var config = new GcsConfiguration("proj", AuthenticationType.SERVICE_ACCOUNT, null, null, null);

    assertThatThrownBy(config::loadCredentials).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void loadCredentialsShouldThrowWhenServiceAccountFileDoesNotExist() {
    var config = GcsConfiguration.withServiceAccount("proj", "/nonexistent/key.json");

    assertThatThrownBy(config::loadCredentials).isInstanceOf(IOException.class);
  }

  // ── Builder ───────────────────────────────────────────────────────────────

  @Test
  void builderShouldBuildApplicationDefaultConfig() {
    var config = GcsConfiguration.builder().projectId("my-project").applicationDefault().build();

    assertThat(config.projectId()).isEqualTo("my-project");
    assertThat(config.authenticationType()).isEqualTo(AuthenticationType.APPLICATION_DEFAULT);
  }

  @Test
  void builderShouldBuildServiceAccountConfig() {
    var config =
        GcsConfiguration.builder()
            .projectId("my-project")
            .serviceAccount("/path/to/key.json")
            .build();

    assertThat(config.authenticationType()).isEqualTo(AuthenticationType.SERVICE_ACCOUNT);
    assertThat(config.serviceAccountPath()).isEqualTo("/path/to/key.json");
  }

  @Test
  void builderShouldBuildCustomCredentialsConfig() throws IOException {
    var creds = GoogleCredentials.create(null);
    var config =
        GcsConfiguration.builder()
            .projectId("my-project")
            .customCredentials(creds)
            .endpoint("http://localhost:4443")
            .build();

    assertThat(config.authenticationType()).isEqualTo(AuthenticationType.CUSTOM);
    assertThat(config.customCredentials()).isSameAs(creds);
    assertThat(config.endpoint()).isEqualTo("http://localhost:4443");
  }

  // ── GcsClientFactory ─────────────────────────────────────────────────────

  @Test
  void createStorageShouldSucceedWithTestingConfig() throws IOException {
    var config = GcsConfiguration.forTesting("test-project", "http://localhost:4443");

    var storage = GcsClientFactory.createStorage(config);

    assertThat(storage).isNotNull();
  }

  @Test
  void createStorageShouldSucceedWithCustomEndpoint() throws IOException {
    var creds = GoogleCredentials.create(null);
    var config =
        new GcsConfiguration("proj", AuthenticationType.CUSTOM, null, creds, "http://fake:8080");

    var storage = GcsClientFactory.createStorage(config);

    assertThat(storage).isNotNull();
  }

  @Test
  void createStorageShouldThrowWhenConfigIsNull() {
    assertThatThrownBy(() -> GcsClientFactory.createStorage(null))
        .isInstanceOf(NullPointerException.class);
  }
}
