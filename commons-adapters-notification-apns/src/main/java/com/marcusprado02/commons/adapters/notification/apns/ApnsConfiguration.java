package com.marcusprado02.commons.adapters.notification.apns;

import com.eatthepath.pushy.apns.ApnsClientBuilder;
import com.eatthepath.pushy.apns.auth.ApnsSigningKey;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/**
 * Configuration for Apple Push Notification Service (APNS) adapter.
 *
 * <p>Supports both token-based (p8) and certificate-based (p12) authentication.
 *
 * <p><b>Token-based Authentication (Recommended):</b>
 *
 * <pre>{@code
 * ApnsConfiguration config = ApnsConfiguration.builder()
 *     .p8KeyPath("/path/to/AuthKey_KEYID.p8")
 *     .teamId("TEAM123456")
 *     .keyId("KEY123456")
 *     .topic("com.example.app")
 *     .production(true)
 *     .build();
 * }</pre>
 *
 * <p><b>Certificate-based Authentication:</b>
 *
 * <pre>{@code
 * ApnsConfiguration config = ApnsConfiguration.builder()
 *     .p12Path("/path/to/certificate.p12")
 *     .p12Password("password")
 *     .topic("com.example.app")
 *     .production(false)
 *     .build();
 * }</pre>
 */
public class ApnsConfiguration {

  private final ApnsSigningKey signingKey;
  private final String teamId;
  private final String keyId;
  private final File p12Certificate;
  private final String p12Password;
  private final String topic;
  private final boolean production;

  private ApnsConfiguration(
      ApnsSigningKey signingKey,
      String teamId,
      String keyId,
      File p12Certificate,
      String p12Password,
      String topic,
      boolean production) {
    this.signingKey = signingKey;
    this.teamId = teamId;
    this.keyId = keyId;
    this.p12Certificate = p12Certificate;
    this.p12Password = p12Password;
    this.topic = topic;
    this.production = production;
  }

  public static Builder builder() {
    return new Builder();
  }

  public ApnsSigningKey getSigningKey() {
    return signingKey;
  }

  public String getTeamId() {
    return teamId;
  }

  public String getKeyId() {
    return keyId;
  }

  public File getP12Certificate() {
    return p12Certificate;
  }

  public String getP12Password() {
    return p12Password;
  }

  public String getTopic() {
    return topic;
  }

  public boolean isProduction() {
    return production;
  }

  public boolean isTokenBased() {
    return signingKey != null;
  }

  public boolean isCertificateBased() {
    return p12Certificate != null;
  }

  public ApnsClientBuilder configureClientBuilder(ApnsClientBuilder builder) throws IOException {
    if (isTokenBased()) {
      return builder
          .setApnsServer(
              production
                  ? ApnsClientBuilder.PRODUCTION_APNS_HOST
                  : ApnsClientBuilder.DEVELOPMENT_APNS_HOST)
          .setSigningKey(signingKey);
    } else if (isCertificateBased()) {
      return builder
          .setApnsServer(
              production
                  ? ApnsClientBuilder.PRODUCTION_APNS_HOST
                  : ApnsClientBuilder.DEVELOPMENT_APNS_HOST)
          .setClientCredentials(p12Certificate, p12Password);
    } else {
      throw new IllegalStateException(
          "Invalid configuration: must provide either token-based (p8) or certificate-based (p12) credentials");
    }
  }

  public static class Builder {
    private ApnsSigningKey signingKey;
    private String teamId;
    private String keyId;
    private File p12Certificate;
    private String p12Password;
    private String topic;
    private boolean production = false;

    /** Token-based authentication (p8) - recommended. */
    public Builder p8KeyPath(String path)
        throws IOException, NoSuchAlgorithmException, InvalidKeyException {
      Objects.requireNonNull(path, "p8KeyPath cannot be null");
      this.signingKey = ApnsSigningKey.loadFromPkcs8File(new File(path), teamId, keyId);
      return this;
    }

    /** Token-based authentication (p8) - recommended. */
    public Builder p8KeyStream(InputStream stream)
        throws IOException, NoSuchAlgorithmException, InvalidKeyException {
      Objects.requireNonNull(stream, "p8KeyStream cannot be null");
      this.signingKey = ApnsSigningKey.loadFromInputStream(stream, teamId, keyId);
      return this;
    }

    /** Token-based authentication (p8) - recommended. */
    public Builder signingKey(ApnsSigningKey signingKey) {
      this.signingKey = Objects.requireNonNull(signingKey, "signingKey cannot be null");
      return this;
    }

    /** Apple Team ID (required for token-based auth). */
    public Builder teamId(String teamId) {
      this.teamId = Objects.requireNonNull(teamId, "teamId cannot be null");
      return this;
    }

    /** Key ID from Apple Developer Portal (required for token-based auth). */
    public Builder keyId(String keyId) {
      this.keyId = Objects.requireNonNull(keyId, "keyId cannot be null");
      return this;
    }

    /** Certificate-based authentication (p12). */
    public Builder p12Path(String path) {
      Objects.requireNonNull(path, "p12Path cannot be null");
      this.p12Certificate = new File(path);
      return this;
    }

    /** Certificate-based authentication (p12). */
    public Builder p12Certificate(File certificate) {
      this.p12Certificate = Objects.requireNonNull(certificate, "certificate cannot be null");
      return this;
    }

    /** Password for p12 certificate. */
    public Builder p12Password(String password) {
      this.p12Password = password;
      return this;
    }

    /** App bundle ID (e.g., com.example.app). */
    public Builder topic(String topic) {
      this.topic = Objects.requireNonNull(topic, "topic cannot be null");
      return this;
    }

    /** Use production APNS servers (default: false = sandbox). */
    public Builder production(boolean production) {
      this.production = production;
      return this;
    }

    public ApnsConfiguration build() {
      Objects.requireNonNull(topic, "topic is required");

      if (signingKey == null && p12Certificate == null) {
        throw new IllegalStateException(
            "Either token-based (p8) or certificate-based (p12) credentials must be provided");
      }

      if (signingKey != null && p12Certificate != null) {
        throw new IllegalStateException(
            "Cannot use both token-based and certificate-based authentication");
      }

      if (signingKey != null) {
        Objects.requireNonNull(teamId, "teamId is required for token-based authentication");
        Objects.requireNonNull(keyId, "keyId is required for token-based authentication");
      }

      if (p12Certificate != null && !p12Certificate.exists()) {
        throw new IllegalStateException("P12 certificate file does not exist: " + p12Certificate);
      }

      return new ApnsConfiguration(
          signingKey, teamId, keyId, p12Certificate, p12Password, topic, production);
    }
  }
}
