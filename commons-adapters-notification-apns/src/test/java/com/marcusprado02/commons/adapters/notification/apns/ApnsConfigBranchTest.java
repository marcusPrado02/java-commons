package com.marcusprado02.commons.adapters.notification.apns;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eatthepath.pushy.apns.ApnsClientBuilder;
import com.eatthepath.pushy.apns.auth.ApnsSigningKey;
import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ApnsConfigBranchTest {

  @TempDir File tempDir;

  // ── Builder.build() validation branches ──────────────────────────────────

  @Test
  void build_nullTopic_throwsNpe() {
    ApnsSigningKey mockKey = mock(ApnsSigningKey.class);
    assertThatThrownBy(
            () ->
                ApnsConfiguration.builder()
                    .signingKey(mockKey)
                    .teamId("T1")
                    .keyId("K1")
                    // no .topic()
                    .build())
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void build_noCredentials_throwsIllegalState() {
    assertThatThrownBy(() -> ApnsConfiguration.builder().topic("com.example.app").build())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("credentials must be provided");
  }

  @Test
  void build_bothSigningKeyAndP12_throwsIllegalState() throws Exception {
    ApnsSigningKey mockKey = mock(ApnsSigningKey.class);
    File cert = new File(tempDir, "cert.p12");
    cert.createNewFile();

    assertThatThrownBy(
            () ->
                ApnsConfiguration.builder()
                    .signingKey(mockKey)
                    .teamId("T1")
                    .keyId("K1")
                    .topic("com.example.app")
                    .p12Certificate(cert)
                    .build())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Cannot use both");
  }

  @Test
  void build_tokenBased_nullTeamId_throwsNpe() {
    ApnsSigningKey mockKey = mock(ApnsSigningKey.class);
    assertThatThrownBy(
            () ->
                ApnsConfiguration.builder()
                    .signingKey(mockKey)
                    .keyId("K1")
                    .topic("com.example.app")
                    .build())
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void build_tokenBased_nullKeyId_throwsNpe() {
    ApnsSigningKey mockKey = mock(ApnsSigningKey.class);
    assertThatThrownBy(
            () ->
                ApnsConfiguration.builder()
                    .signingKey(mockKey)
                    .teamId("T1")
                    .topic("com.example.app")
                    .build())
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void build_p12FileNotExist_throwsIllegalState() {
    File nonExistent = new File(tempDir, "no.p12");
    assertThatThrownBy(
            () ->
                ApnsConfiguration.builder()
                    .p12Certificate(nonExistent)
                    .p12Password("pass")
                    .topic("com.example.app")
                    .build())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("does not exist");
  }

  @Test
  void build_validTokenBased_succeeds() {
    ApnsSigningKey mockKey = mock(ApnsSigningKey.class);
    ApnsConfiguration config =
        ApnsConfiguration.builder()
            .signingKey(mockKey)
            .teamId("T1")
            .keyId("K1")
            .topic("com.example.app")
            .production(true)
            .build();

    assertThat(config).isNotNull();
    assertThat(config.isTokenBased()).isTrue();
    assertThat(config.isCertificateBased()).isFalse();
    assertThat(config.isProduction()).isTrue();
    assertThat(config.getTopic()).isEqualTo("com.example.app");
    assertThat(config.getTeamId()).isEqualTo("T1");
    assertThat(config.getKeyId()).isEqualTo("K1");
  }

  @Test
  void build_validCertBased_succeeds() throws Exception {
    File cert = new File(tempDir, "cert.p12");
    cert.createNewFile();

    ApnsConfiguration config =
        ApnsConfiguration.builder()
            .p12Certificate(cert)
            .p12Password("pass123")
            .topic("com.example.app")
            .production(false)
            .build();

    assertThat(config).isNotNull();
    assertThat(config.isTokenBased()).isFalse();
    assertThat(config.isCertificateBased()).isTrue();
    assertThat(config.isProduction()).isFalse();
    assertThat(config.getP12Password()).isEqualTo("pass123");
  }

  // ── Builder setters that return 'this' ──────────────────────────────────

  @Test
  void builder_p12Path_setsFileFromPath() throws Exception {
    File cert = new File(tempDir, "cert.p12");
    cert.createNewFile();

    ApnsConfiguration config =
        ApnsConfiguration.builder()
            .p12Path(cert.getAbsolutePath())
            .topic("com.example.app")
            .build();

    assertThat(config.isCertificateBased()).isTrue();
  }

  // ── configureClientBuilder branches ──────────────────────────────────────

  @Test
  @SuppressWarnings("unchecked")
  void configureClientBuilder_tokenBased_production_usesProductionHost() throws IOException {
    ApnsSigningKey mockKey = mock(ApnsSigningKey.class);
    ApnsClientBuilder mockBuilder = mock(ApnsClientBuilder.class);
    when(mockBuilder.setApnsServer(any(String.class))).thenReturn(mockBuilder);
    when(mockBuilder.setSigningKey(any())).thenReturn(mockBuilder);

    ApnsConfiguration config =
        ApnsConfiguration.builder()
            .signingKey(mockKey)
            .teamId("T1")
            .keyId("K1")
            .topic("com.example.app")
            .production(true)
            .build();

    ApnsClientBuilder result = config.configureClientBuilder(mockBuilder);
    assertThat(result).isNotNull();
  }

  @Test
  @SuppressWarnings("unchecked")
  void configureClientBuilder_tokenBased_sandbox_usesDevelopmentHost() throws IOException {
    ApnsSigningKey mockKey = mock(ApnsSigningKey.class);
    ApnsClientBuilder mockBuilder = mock(ApnsClientBuilder.class);
    when(mockBuilder.setApnsServer(any(String.class))).thenReturn(mockBuilder);
    when(mockBuilder.setSigningKey(any())).thenReturn(mockBuilder);

    ApnsConfiguration config =
        ApnsConfiguration.builder()
            .signingKey(mockKey)
            .teamId("T1")
            .keyId("K1")
            .topic("com.example.app")
            .production(false)
            .build();

    ApnsClientBuilder result = config.configureClientBuilder(mockBuilder);
    assertThat(result).isNotNull();
  }

  @Test
  @SuppressWarnings("unchecked")
  void configureClientBuilder_certBased_production_usesProductionHost() throws Exception {
    File cert = new File(tempDir, "cert.p12");
    cert.createNewFile();

    ApnsClientBuilder mockBuilder = mock(ApnsClientBuilder.class);
    when(mockBuilder.setApnsServer(any(String.class))).thenReturn(mockBuilder);
    when(mockBuilder.setClientCredentials(any(File.class), any())).thenReturn(mockBuilder);

    ApnsConfiguration config =
        ApnsConfiguration.builder()
            .p12Certificate(cert)
            .p12Password("pass")
            .topic("com.example.app")
            .production(true)
            .build();

    ApnsClientBuilder result = config.configureClientBuilder(mockBuilder);
    assertThat(result).isNotNull();
  }

  @Test
  @SuppressWarnings("unchecked")
  void configureClientBuilder_certBased_sandbox_usesDevelopmentHost() throws Exception {
    File cert = new File(tempDir, "cert.p12");
    cert.createNewFile();

    ApnsClientBuilder mockBuilder = mock(ApnsClientBuilder.class);
    when(mockBuilder.setApnsServer(any(String.class))).thenReturn(mockBuilder);
    when(mockBuilder.setClientCredentials(any(File.class), any())).thenReturn(mockBuilder);

    ApnsConfiguration config =
        ApnsConfiguration.builder()
            .p12Certificate(cert)
            .p12Password("pass")
            .topic("com.example.app")
            .production(false)
            .build();

    ApnsClientBuilder result = config.configureClientBuilder(mockBuilder);
    assertThat(result).isNotNull();
  }
}
