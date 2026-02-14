package com.marcusprado02.commons.adapters.sms.twilio;

import java.time.Duration;
import java.util.Objects;

/**
 * Configuration for Twilio SMS adapter.
 *
 * <p>Contains all necessary settings for connecting to Twilio SMS API.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * TwilioConfiguration config = TwilioConfiguration.builder()
 *     .accountSid("ACxxxx...")
 *     .authToken("your-auth-token")
 *     .fromPhoneNumber("+1234567890")
 *     .build();
 * }</pre>
 */
public record TwilioConfiguration(
    String accountSid,
    String authToken,
    String fromPhoneNumber,
    Duration requestTimeout,
    String webhookUrl,
    boolean deliveryReceiptsEnabled) {

  public TwilioConfiguration {
    Objects.requireNonNull(accountSid, "Account SID cannot be null");
    Objects.requireNonNull(authToken, "Auth token cannot be null");
    Objects.requireNonNull(fromPhoneNumber, "From phone number cannot be null");
    Objects.requireNonNull(requestTimeout, "Request timeout cannot be null");

    if (accountSid.isBlank()) {
      throw new IllegalArgumentException("Account SID cannot be blank");
    }

    if (authToken.isBlank()) {
      throw new IllegalArgumentException("Auth token cannot be blank");
    }

    if (fromPhoneNumber.isBlank()) {
      throw new IllegalArgumentException("From phone number cannot be blank");
    }

    if (!accountSid.startsWith("AC")) {
      throw new IllegalArgumentException("Invalid Twilio Account SID format: must start with 'AC'");
    }

    if (requestTimeout.isNegative() || requestTimeout.isZero()) {
      throw new IllegalArgumentException("Request timeout must be positive");
    }

    // Basic phone number validation
    if (!fromPhoneNumber.matches("^\\+[1-9]\\d{1,14}$")) {
      throw new IllegalArgumentException("Invalid from phone number format: " + fromPhoneNumber);
    }

    if (webhookUrl != null && !webhookUrl.isBlank() &&
        !webhookUrl.startsWith("http://") && !webhookUrl.startsWith("https://")) {
      throw new IllegalArgumentException("Webhook URL must be a valid HTTP/HTTPS URL");
    }
  }

  /**
   * Creates a builder for TwilioConfiguration.
   *
   * @return a new builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates a configuration for development with minimal settings.
   *
   * @param accountSid Twilio Account SID
   * @param authToken Twilio Auth Token
   * @param fromPhoneNumber verified Twilio phone number
   * @return development configuration
   */
  public static TwilioConfiguration forDevelopment(String accountSid, String authToken, String fromPhoneNumber) {
    return builder()
        .accountSid(accountSid)
        .authToken(authToken)
        .fromPhoneNumber(fromPhoneNumber)
        .requestTimeout(Duration.ofSeconds(30))
        .deliveryReceiptsEnabled(false)
        .build();
  }

  /**
   * Creates a configuration for production with enhanced settings.
   *
   * @param accountSid Twilio Account SID
   * @param authToken Twilio Auth Token
   * @param fromPhoneNumber verified Twilio phone number
   * @param webhookUrl webhook URL for delivery receipts
   * @return production configuration
   */
  public static TwilioConfiguration forProduction(String accountSid, String authToken,
                                                  String fromPhoneNumber, String webhookUrl) {
    return builder()
        .accountSid(accountSid)
        .authToken(authToken)
        .fromPhoneNumber(fromPhoneNumber)
        .requestTimeout(Duration.ofSeconds(15))
        .webhookUrl(webhookUrl)
        .deliveryReceiptsEnabled(true)
        .build();
  }

  /** Builder for TwilioConfiguration. */
  public static final class Builder {
    private String accountSid;
    private String authToken;
    private String fromPhoneNumber;
    private Duration requestTimeout = Duration.ofSeconds(10);
    private String webhookUrl;
    private boolean deliveryReceiptsEnabled = false;

    private Builder() {}

    /**
     * Sets the Twilio Account SID.
     *
     * @param accountSid Account SID starting with 'AC'
     * @return this builder
     */
    public Builder accountSid(String accountSid) {
      this.accountSid = accountSid;
      return this;
    }

    /**
     * Sets the Twilio Auth Token.
     *
     * @param authToken authentication token
     * @return this builder
     */
    public Builder authToken(String authToken) {
      this.authToken = authToken;
      return this;
    }

    /**
     * Sets the sender phone number.
     *
     * @param fromPhoneNumber verified Twilio phone number in E.164 format
     * @return this builder
     */
    public Builder fromPhoneNumber(String fromPhoneNumber) {
      this.fromPhoneNumber = fromPhoneNumber;
      return this;
    }

    /**
     * Sets the request timeout.
     *
     * @param requestTimeout timeout for HTTP requests
     * @return this builder
     */
    public Builder requestTimeout(Duration requestTimeout) {
      this.requestTimeout = requestTimeout;
      return this;
    }

    /**
     * Sets the webhook URL for status callbacks.
     *
     * @param webhookUrl HTTP/HTTPS URL for delivery receipts
     * @return this builder
     */
    public Builder webhookUrl(String webhookUrl) {
      this.webhookUrl = webhookUrl;
      return this;
    }

    /**
     * Enables or disables delivery receipt tracking.
     *
     * @param enabled true to enable delivery receipts
     * @return this builder
     */
    public Builder deliveryReceiptsEnabled(boolean enabled) {
      this.deliveryReceiptsEnabled = enabled;
      return this;
    }

    /**
     * Builds the TwilioConfiguration.
     *
     * @return TwilioConfiguration instance
     * @throws IllegalArgumentException if required fields are missing or invalid
     */
    public TwilioConfiguration build() {
      return new TwilioConfiguration(
          accountSid,
          authToken,
          fromPhoneNumber,
          requestTimeout,
          webhookUrl,
          deliveryReceiptsEnabled);
    }
  }
}
