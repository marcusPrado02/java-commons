package com.marcusprado02.commons.adapters.email.smtp;

import java.util.Objects;
import java.util.Properties;

/**
 * SMTP server configuration.
 *
 * <p>Immutable configuration for connecting to an SMTP server.
 */
public record SmtpConfiguration(
    String host,
    int port,
    String username,
    String password,
    boolean useTls,
    boolean useStartTls,
    boolean requireAuth,
    int connectionTimeout,
    int writeTimeout) {

  public SmtpConfiguration {
    Objects.requireNonNull(host, "host must not be null");
    if (host.trim().isEmpty()) {
      throw new IllegalArgumentException("host must not be blank");
    }
    if (port <= 0 || port > 65535) {
      throw new IllegalArgumentException("port must be between 1 and 65535");
    }
    if (requireAuth) {
      Objects.requireNonNull(username, "username is required when authentication is enabled");
      Objects.requireNonNull(password, "password is required when authentication is enabled");
    }
    if (connectionTimeout < 0) {
      throw new IllegalArgumentException("connectionTimeout must not be negative");
    }
    if (writeTimeout < 0) {
      throw new IllegalArgumentException("writeTimeout must not be negative");
    }
  }

  /**
   * Creates a builder for SMTP configuration.
   *
   * @return a new builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates default configuration for local development (port 3025, no auth).
   *
   * @return default SMTP configuration
   */
  public static SmtpConfiguration defaults() {
    return builder().host("localhost").port(3025).requireAuth(false).build();
  }

  /**
   * Converts this configuration to Jakarta Mail Properties.
   *
   * @return Properties for JavaMail session
   */
  public Properties toProperties() {
    Properties props = new Properties();
    props.put("mail.smtp.host", host);
    props.put("mail.smtp.port", String.valueOf(port));
    props.put("mail.smtp.auth", String.valueOf(requireAuth));
    props.put("mail.smtp.connectiontimeout", String.valueOf(connectionTimeout));
    props.put("mail.smtp.writetimeout", String.valueOf(writeTimeout));

    if (useTls) {
      props.put("mail.smtp.ssl.enable", "true");
      props.put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");
    }

    if (useStartTls) {
      props.put("mail.smtp.starttls.enable", "true");
      props.put("mail.smtp.starttls.required", "true");
    }

    return props;
  }

  /** Builder for SmtpConfiguration. */
  public static final class Builder {
    private String host = "localhost";
    private int port = 25;
    private String username;
    private String password;
    private boolean useTls = false;
    private boolean useStartTls = false;
    private boolean requireAuth = true;
    private int connectionTimeout = 10000; // 10 seconds
    private int writeTimeout = 10000; // 10 seconds

    private Builder() {}

    public Builder host(String host) {
      this.host = host;
      return this;
    }

    public Builder port(int port) {
      this.port = port;
      return this;
    }

    public Builder username(String username) {
      this.username = username;
      return this;
    }

    public Builder password(String password) {
      this.password = password;
      return this;
    }

    public Builder useTls(boolean useTls) {
      this.useTls = useTls;
      return this;
    }

    public Builder useStartTls(boolean useStartTls) {
      this.useStartTls = useStartTls;
      return this;
    }

    public Builder requireAuth(boolean requireAuth) {
      this.requireAuth = requireAuth;
      return this;
    }

    public Builder connectionTimeout(int connectionTimeout) {
      this.connectionTimeout = connectionTimeout;
      return this;
    }

    public Builder writeTimeout(int writeTimeout) {
      this.writeTimeout = writeTimeout;
      return this;
    }

    public SmtpConfiguration build() {
      return new SmtpConfiguration(
          host,
          port,
          username,
          password,
          useTls,
          useStartTls,
          requireAuth,
          connectionTimeout,
          writeTimeout);
    }
  }
}
