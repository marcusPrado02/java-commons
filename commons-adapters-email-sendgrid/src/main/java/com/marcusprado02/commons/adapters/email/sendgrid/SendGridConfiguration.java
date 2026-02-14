package com.marcusprado02.commons.adapters.email.sendgrid;

import java.time.Duration;

/**
 * Configuration for SendGrid email adapter.
 *
 * <p>Required configuration:
 * <ul>
 *   <li>apiKey - SendGrid API key
 * </ul>
 *
 * <p>Optional configuration:
 * <ul>
 *   <li>requestTimeout - HTTP request timeout (default: 10 seconds)
 *   <li>defaultFromEmail - Default sender email (required if not specified per email)
 *   <li>defaultFromName - Default sender name
 *   <li>trackClicks - Enable click tracking (default: true)
 *   <li>trackOpens - Enable open tracking (default: true)
 *   <li>sandboxMode - Enable sandbox mode for testing (default: false)
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * SendGridConfiguration config = SendGridConfiguration.builder()
 *     .apiKey("SG.xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx")
 *     .defaultFromEmail("noreply@example.com")
 *     .defaultFromName("Example App")
 *     .trackClicks(true)
 *     .trackOpens(true)
 *     .sandboxMode(false)
 *     .build();
 * }</pre>
 */
public record SendGridConfiguration(
    String apiKey,
    Duration requestTimeout,
    String defaultFromEmail,
    String defaultFromName,
    boolean trackClicks,
    boolean trackOpens,
    boolean sandboxMode) {

  public SendGridConfiguration {
    if (apiKey == null || apiKey.isBlank()) {
      throw new IllegalArgumentException("SendGrid API key cannot be null or blank");
    }
    if (requestTimeout == null) {
      throw new IllegalArgumentException("Request timeout cannot be null");
    }
    if (requestTimeout.isNegative() || requestTimeout.isZero()) {
      throw new IllegalArgumentException("Request timeout must be positive");
    }
  }

  /**
   * Creates a new builder for SendGridConfiguration.
   *
   * @return a new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates a default configuration for local development and testing.
   * Uses sandbox mode to prevent actual email sending.
   *
   * @param apiKey the SendGrid API key
   * @return a configuration suitable for testing
   */
  public static SendGridConfiguration forTesting(String apiKey) {
    return builder()
        .apiKey(apiKey)
        .sandboxMode(true)
        .build();
  }

  /**
   * Creates a production configuration with tracking enabled.
   *
   * @param apiKey the SendGrid API key
   * @param fromEmail the default sender email address
   * @param fromName the default sender name
   * @return a configuration suitable for production
   */
  public static SendGridConfiguration forProduction(String apiKey, String fromEmail, String fromName) {
    return builder()
        .apiKey(apiKey)
        .defaultFromEmail(fromEmail)
        .defaultFromName(fromName)
        .trackClicks(true)
        .trackOpens(true)
        .sandboxMode(false)
        .build();
  }

  /**
   * Builder for SendGridConfiguration.
   */
  public static class Builder {
    private String apiKey;
    private Duration requestTimeout = Duration.ofSeconds(10);
    private String defaultFromEmail;
    private String defaultFromName;
    private boolean trackClicks = true;
    private boolean trackOpens = true;
    private boolean sandboxMode = false;

    private Builder() {}

    /**
     * Sets the SendGrid API key.
     *
     * @param apiKey the API key (required)
     * @return this builder
     */
    public Builder apiKey(String apiKey) {
      this.apiKey = apiKey;
      return this;
    }

    /**
     * Sets the HTTP request timeout.
     *
     * @param timeout the timeout duration
     * @return this builder
     */
    public Builder requestTimeout(Duration timeout) {
      this.requestTimeout = timeout;
      return this;
    }

    /**
     * Sets the default sender email address.
     *
     * @param email the sender email
     * @return this builder
     */
    public Builder defaultFromEmail(String email) {
      this.defaultFromEmail = email;
      return this;
    }

    /**
     * Sets the default sender name.
     *
     * @param name the sender name
     * @return this builder
     */
    public Builder defaultFromName(String name) {
      this.defaultFromName = name;
      return this;
    }

    /**
     * Enables or disables click tracking.
     *
     * @param enabled true to enable click tracking
     * @return this builder
     */
    public Builder trackClicks(boolean enabled) {
      this.trackClicks = enabled;
      return this;
    }

    /**
     * Enables or disables open tracking.
     *
     * @param enabled true to enable open tracking
     * @return this builder
     */
    public Builder trackOpens(boolean enabled) {
      this.trackOpens = enabled;
      return this;
    }

    /**
     * Enables or disables sandbox mode.
     * In sandbox mode, emails are validated but not sent.
     *
     * @param enabled true to enable sandbox mode
     * @return this builder
     */
    public Builder sandboxMode(boolean enabled) {
      this.sandboxMode = enabled;
      return this;
    }

    /**
     * Builds the SendGridConfiguration.
     *
     * @return the configuration
     * @throws IllegalArgumentException if required fields are missing
     */
    public SendGridConfiguration build() {
      return new SendGridConfiguration(
          apiKey,
          requestTimeout,
          defaultFromEmail,
          defaultFromName,
          trackClicks,
          trackOpens,
          sandboxMode);
    }
  }
}
