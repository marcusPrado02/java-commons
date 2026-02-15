package com.marcusprado02.commons.adapters.search.elasticsearch;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * Configuration for Elasticsearch Java client.
 *
 * <p>Supports both single-node and cluster configurations with authentication options.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * ElasticsearchConfiguration config = ElasticsearchConfiguration.builder()
 *     .serverUrl("https://localhost:9200")
 *     .username("elastic")
 *     .password("changeme")
 *     .build();
 * }</pre>
 */
public record ElasticsearchConfiguration(
    List<String> serverUrls,
    String username,
    String password,
    String apiKey,
    Duration connectionTimeout,
    Duration socketTimeout,
    int maxConnections,
    boolean enableSsl,
    boolean verifySslCertificates
) {

  public ElasticsearchConfiguration {
    Objects.requireNonNull(serverUrls, "Server URLs cannot be null");
    Objects.requireNonNull(connectionTimeout, "Connection timeout cannot be null");
    Objects.requireNonNull(socketTimeout, "Socket timeout cannot be null");

    if (serverUrls.isEmpty()) {
      throw new IllegalArgumentException("At least one server URL must be provided");
    }

    for (String url : serverUrls) {
      if (url == null || url.isBlank()) {
        throw new IllegalArgumentException("Server URL cannot be blank");
      }
      if (!url.startsWith("http://") && !url.startsWith("https://")) {
        throw new IllegalArgumentException("Server URL must start with http:// or https://");
      }
    }

    // Validate authentication - either username/password or apiKey
    boolean hasBasicAuth = username != null && !username.isBlank() &&
                           password != null && !password.isBlank();
    boolean hasApiKey = apiKey != null && !apiKey.isBlank();

    if (!hasBasicAuth && !hasApiKey) {
      throw new IllegalArgumentException(
          "Either username/password or apiKey must be provided for authentication");
    }

    if (connectionTimeout.isNegative() || connectionTimeout.isZero()) {
      throw new IllegalArgumentException("Connection timeout must be positive");
    }

    if (socketTimeout.isNegative() || socketTimeout.isZero()) {
      throw new IllegalArgumentException("Socket timeout must be positive");
    }

    if (maxConnections < 1) {
      throw new IllegalArgumentException("Max connections must be at least 1");
    }

    if (maxConnections > 1000) {
      throw new IllegalArgumentException("Max connections cannot exceed 1000");
    }

    serverUrls = List.copyOf(serverUrls);
  }

  /**
   * Creates a builder for ElasticsearchConfiguration.
   *
   * @return new builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates development configuration for local Elasticsearch.
   *
   * @param serverUrl server URL (e.g., http://localhost:9200)
   * @param username username
   * @param password password
   * @return development configuration
   */
  public static ElasticsearchConfiguration forDevelopment(
      String serverUrl, String username, String password) {
    return builder()
        .serverUrl(serverUrl)
        .username(username)
        .password(password)
        .connectionTimeout(Duration.ofSeconds(30))
        .socketTimeout(Duration.ofSeconds(60))
        .maxConnections(10)
        .enableSsl(false)
        .verifySslCertificates(false)
        .build();
  }

  /**
   * Creates production configuration with SSL.
   *
   * @param serverUrls cluster server URLs
   * @param username username
   * @param password password
   * @return production configuration
   */
  public static ElasticsearchConfiguration forProduction(
      List<String> serverUrls, String username, String password) {
    return builder()
        .serverUrls(serverUrls)
        .username(username)
        .password(password)
        .connectionTimeout(Duration.ofSeconds(10))
        .socketTimeout(Duration.ofSeconds(30))
        .maxConnections(50)
        .enableSsl(true)
        .verifySslCertificates(true)
        .build();
  }

  /**
   * Creates configuration using API key authentication.
   *
   * @param serverUrl server URL
   * @param apiKey API key
   * @return configuration with API key auth
   */
  public static ElasticsearchConfiguration withApiKey(String serverUrl, String apiKey) {
    return builder()
        .serverUrl(serverUrl)
        .apiKey(apiKey)
        .connectionTimeout(Duration.ofSeconds(15))
        .socketTimeout(Duration.ofSeconds(30))
        .maxConnections(20)
        .enableSsl(true)
        .verifySslCertificates(true)
        .build();
  }

  /** Builder for ElasticsearchConfiguration. */
  public static final class Builder {
    private List<String> serverUrls = List.of();
    private String username;
    private String password;
    private String apiKey;
    private Duration connectionTimeout = Duration.ofSeconds(15);
    private Duration socketTimeout = Duration.ofSeconds(30);
    private int maxConnections = 20;
    private boolean enableSsl = true;
    private boolean verifySslCertificates = true;

    private Builder() {}

    /**
     * Sets a single server URL.
     *
     * @param serverUrl Elasticsearch server URL
     * @return this builder
     */
    public Builder serverUrl(String serverUrl) {
      this.serverUrls = List.of(serverUrl);
      return this;
    }

    /**
     * Sets multiple server URLs for cluster mode.
     *
     * @param serverUrls list of server URLs
     * @return this builder
     */
    public Builder serverUrls(List<String> serverUrls) {
      this.serverUrls = serverUrls;
      return this;
    }

    /**
     * Sets username for basic authentication.
     *
     * @param username username
     * @return this builder
     */
    public Builder username(String username) {
      this.username = username;
      return this;
    }

    /**
     * Sets password for basic authentication.
     *
     * @param password password
     * @return this builder
     */
    public Builder password(String password) {
      this.password = password;
      return this;
    }

    /**
     * Sets API key for authentication.
     *
     * @param apiKey API key
     * @return this builder
     */
    public Builder apiKey(String apiKey) {
      this.apiKey = apiKey;
      return this;
    }

    /**
     * Sets connection timeout.
     *
     * @param connectionTimeout timeout for establishing connections
     * @return this builder
     */
    public Builder connectionTimeout(Duration connectionTimeout) {
      this.connectionTimeout = connectionTimeout;
      return this;
    }

    /**
     * Sets socket timeout.
     *
     * @param socketTimeout timeout for socket operations
     * @return this builder
     */
    public Builder socketTimeout(Duration socketTimeout) {
      this.socketTimeout = socketTimeout;
      return this;
    }

    /**
     * Sets maximum number of connections.
     *
     * @param maxConnections max concurrent connections
     * @return this builder
     */
    public Builder maxConnections(int maxConnections) {
      this.maxConnections = maxConnections;
      return this;
    }

    /**
     * Enables or disables SSL.
     *
     * @param enableSsl true to enable SSL
     * @return this builder
     */
    public Builder enableSsl(boolean enableSsl) {
      this.enableSsl = enableSsl;
      return this;
    }

    /**
     * Enables or disables SSL certificate verification.
     *
     * @param verifySslCertificates true to verify certificates
     * @return this builder
     */
    public Builder verifySslCertificates(boolean verifySslCertificates) {
      this.verifySslCertificates = verifySslCertificates;
      return this;
    }

    /**
     * Builds the ElasticsearchConfiguration.
     *
     * @return ElasticsearchConfiguration instance
     * @throws IllegalArgumentException if configuration is invalid
     */
    public ElasticsearchConfiguration build() {
      return new ElasticsearchConfiguration(
          serverUrls,
          username,
          password,
          apiKey,
          connectionTimeout,
          socketTimeout,
          maxConnections,
          enableSsl,
          verifySslCertificates
      );
    }
  }
}
