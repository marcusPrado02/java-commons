package com.marcusprado02.commons.adapters.search.opensearch;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Configuration for OpenSearch client connection.
 *
 * <p>Supports both single-node and cluster configurations with authentication options.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Development
 * OpenSearchConfiguration config = OpenSearchConfiguration.forDevelopment();
 *
 * // Production with authentication
 * OpenSearchConfiguration config = OpenSearchConfiguration.builder()
 *     .addUrl("https://opensearch-1.example.com:9200")
 *     .addUrl("https://opensearch-2.example.com:9200")
 *     .username("admin")
 *     .password("admin")
 *     .enableSsl(true)
 *     .build();
 *
 * // Production  with API key
 * OpenSearchConfiguration config = OpenSearchConfiguration.withApiKey(
 *     "https://opensearch.example.com:9200",
 *     "your-api-key-id",
 *     "your-api-key-secret"
 * );
 * }</pre>
 */
public record OpenSearchConfiguration(
    List<String> urls,
    String username,
    String password,
    String apiKeyId,
    String apiKeySecret,
    Duration connectionTimeout,
    Duration socketTimeout,
    int maxConnections,
    boolean enableSsl,
    boolean verifySslCertificates) {

  public OpenSearchConfiguration {
    Objects.requireNonNull(urls, "URLs cannot be null");
    if (urls.isEmpty()) {
      throw new IllegalArgumentException("At least one URL must be provided");
    }

    urls = List.copyOf(urls); // Defensive copy

    Objects.requireNonNull(connectionTimeout, "Connection timeout cannot be null");
    Objects.requireNonNull(socketTimeout, "Socket timeout cannot be null");

    if (maxConnections < 1 || maxConnections > 1000) {
      throw new IllegalArgumentException("Max connections must be between 1 and 1000");
    }

    if (connectionTimeout.isNegative() || connectionTimeout.isZero()) {
      throw new IllegalArgumentException("Connection timeout must be positive");
    }

    if (socketTimeout.isNegative() || socketTimeout.isZero()) {
      throw new IllegalArgumentException("Socket timeout must be positive");
    }

    // Validate authentication: either username/password OR apiKey OR none
    boolean hasBasicAuth = username != null && password != null;
    boolean hasApiKey = apiKeyId != null && apiKeySecret != null;

    if (hasBasicAuth && hasApiKey) {
      throw new IllegalArgumentException("Cannot use both basic auth and API key");
    }
  }

  /**
   * Checks if basic authentication is configured.
   *
   * @return true if username and password are set
   */
  public boolean hasBasicAuth() {
    return username != null && password != null;
  }

  /**
   * Checks if API key authentication is configured.
   *
   * @return true if API key ID and secret are set
   */
  public boolean hasApiKey() {
    return apiKeyId != null && apiKeySecret != null;
  }

  /**
   * Creates a builder for OpenSearchConfiguration.
   *
   * @return new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates a development configuration with default settings.
   *
   * <p>Defaults:
   *
   * <ul>
   *   <li>URL: http://localhost:9200
   *   <li>No authentication
   *   <li>Connection timeout: 5 seconds
   *   <li>Socket timeout: 30 seconds
   *   <li>Max connections: 10
   *   <li>SSL disabled
   * </ul>
   *
   * @return development configuration
   */
  public static OpenSearchConfiguration forDevelopment() {
    return builder().addUrl("http://localhost:9200").build();
  }

  /**
   * Creates a production configuration with recommended settings.
   *
   * <p>Defaults:
   *
   * <ul>
   *   <li>URL: must be provided
   *   <li>Authentication: must be configured separately
   *   <li>Connection timeout: 10 seconds
   *   <li>Socket timeout: 60 seconds
   *   <li>Max connections: 50
   *   <li>SSL enabled
   * </ul>
   *
   * @return builder for production configuration
   */
  public static Builder forProduction() {
    return builder()
        .connectionTimeout(Duration.ofSeconds(10))
        .socketTimeout(Duration.ofSeconds(60))
        .maxConnections(50)
        .enableSsl(true);
  }

  /**
   * Creates a configuration with API key authentication.
   *
   * @param url OpenSearch URL
   * @param apiKeyId API key ID
   * @param apiKeySecret API key secret
   * @return configuration with API key
   */
  public static OpenSearchConfiguration withApiKey(
      String url, String apiKeyId, String apiKeySecret) {
    return builder().addUrl(url).apiKeyId(apiKeyId).apiKeySecret(apiKeySecret).build();
  }

  /** Builder for OpenSearchConfiguration. */
  public static class Builder {
    private final List<String> urls = new ArrayList<>();
    private String username;
    private String password;
    private String apiKeyId;
    private String apiKeySecret;
    private Duration connectionTimeout = Duration.ofSeconds(5);
    private Duration socketTimeout = Duration.ofSeconds(30);
    private int maxConnections = 10;
    private boolean enableSsl = false;
    private boolean verifySslCertificates = true;

    /**
     * Adds an OpenSearch URL.
     *
     * @param url OpenSearch URL (e.g., "http://localhost:9200")
     * @return this builder
     */
    public Builder addUrl(String url) {
      this.urls.add(Objects.requireNonNull(url, "URL cannot be null"));
      return this;
    }

    /**
     * Sets the username for basic authentication.
     *
     * @param username username
     * @return this builder
     */
    public Builder username(String username) {
      this.username = username;
      return this;
    }

    /**
     * Sets the password for basic authentication.
     *
     * @param password password
     * @return this builder
     */
    public Builder password(String password) {
      this.password = password;
      return this;
    }

    /**
     * Sets the API key ID for API key authentication.
     *
     * @param apiKeyId API key ID
     * @return this builder
     */
    public Builder apiKeyId(String apiKeyId) {
      this.apiKeyId = apiKeyId;
      return this;
    }

    /**
     * Sets the API key secret for API key authentication.
     *
     * @param apiKeySecret API key secret
     * @return this builder
     */
    public Builder apiKeySecret(String apiKeySecret) {
      this.apiKeySecret = apiKeySecret;
      return this;
    }

    /**
     * Sets the connection timeout.
     *
     * @param connectionTimeout connection timeout
     * @return this builder
     */
    public Builder connectionTimeout(Duration connectionTimeout) {
      this.connectionTimeout = connectionTimeout;
      return this;
    }

    /**
     * Sets the socket timeout.
     *
     * @param socketTimeout socket timeout
     * @return this builder
     */
    public Builder socketTimeout(Duration socketTimeout) {
      this.socketTimeout = socketTimeout;
      return this;
    }

    /**
     * Sets the maximum number of connections.
     *
     * @param maxConnections max connections (1-1000)
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
     * @param verifySslCertificates true to verify SSL certificates
     * @return this builder
     */
    public Builder verifySslCertificates(boolean verifySslCertificates) {
      this.verifySslCertificates = verifySslCertificates;
      return this;
    }

    /**
     * Builds the configuration.
     *
     * @return OpenSearchConfiguration instance
     */
    public OpenSearchConfiguration build() {
      return new OpenSearchConfiguration(
          urls,
          username,
          password,
          apiKeyId,
          apiKeySecret,
          connectionTimeout,
          socketTimeout,
          maxConnections,
          enableSsl,
          verifySslCertificates);
    }
  }
}
