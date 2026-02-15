package com.marcusprado02.commons.adapters.sms.sns;

import software.amazon.awssdk.regions.Region;
import java.time.Duration;
import java.util.Objects;

/**
 * Configuration for AWS SNS SMS adapter.
 *
 * <p>Contains all necessary settings for connecting to Amazon SNS for SMS operations.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * SnsConfiguration config = SnsConfiguration.builder()
 *     .region(Region.US_EAST_1)
 *     .accessKeyId("AKIA...")
 *     .secretAccessKey("secret...")
 *     .build();
 * }</pre>
 */
public record SnsConfiguration(
    Region region,
    String accessKeyId,
    String secretAccessKey,
    String sessionToken,
    Duration requestTimeout,
    String defaultSenderId,
    double maxPriceUSD,
    SmsType smsType,
    boolean deliveryStatusLogging) {

  public SnsConfiguration {
    Objects.requireNonNull(region, "Region cannot be null");
    Objects.requireNonNull(requestTimeout, "Request timeout cannot be null");
    Objects.requireNonNull(smsType, "SMS type cannot be null");

    // Validate credentials - either access key or session token should be provided
    if ((accessKeyId == null || accessKeyId.isBlank()) && (sessionToken == null || sessionToken.isBlank())) {
      throw new IllegalArgumentException("Either access key ID or session token must be provided");
    }

    if (accessKeyId != null && !accessKeyId.isBlank() && (secretAccessKey == null || secretAccessKey.isBlank())) {
      throw new IllegalArgumentException("Secret access key is required when access key ID is provided");
    }

    if (requestTimeout.isNegative() || requestTimeout.isZero()) {
      throw new IllegalArgumentException("Request timeout must be positive");
    }

    if (maxPriceUSD <= 0) {
      throw new IllegalArgumentException("Max price must be positive");
    }

    if (maxPriceUSD > 10.0) {
      throw new IllegalArgumentException("Max price cannot exceed $10.00 USD for safety");
    }
  }

  /**
   * Creates a builder for SnsConfiguration.
   *
   * @return a new builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates a configuration for development with minimal settings.
   *
   * @param region AWS region
   * @param accessKeyId AWS access key ID
   * @param secretAccessKey AWS secret access key
   * @return development configuration
   */
  public static SnsConfiguration forDevelopment(Region region, String accessKeyId, String secretAccessKey) {
    return builder()
        .region(region)
        .accessKeyId(accessKeyId)
        .secretAccessKey(secretAccessKey)
        .requestTimeout(Duration.ofSeconds(30))
        .maxPriceUSD(0.1) // Low limit for dev
        .smsType(SmsType.TRANSACTIONAL)
        .deliveryStatusLogging(true)
        .build();
  }

  /**
   * Creates a configuration for production with enhanced settings.
   *
   * @param region AWS region
   * @param accessKeyId AWS access key ID
   * @param secretAccessKey AWS secret access key
   * @param senderId default sender ID for messages
   * @return production configuration
   */
  public static SnsConfiguration forProduction(Region region, String accessKeyId,
                                               String secretAccessKey, String senderId) {
    return builder()
        .region(region)
        .accessKeyId(accessKeyId)
        .secretAccessKey(secretAccessKey)
        .requestTimeout(Duration.ofSeconds(15))
        .defaultSenderId(senderId)
        .maxPriceUSD(1.0) // Reasonable limit for prod
        .smsType(SmsType.TRANSACTIONAL)
        .deliveryStatusLogging(true)
        .build();
  }

  /**
   * Creates configuration using IAM roles (no explicit credentials).
   *
   * @param region AWS region
   * @return IAM role-based configuration
   */
  public static SnsConfiguration withIamRole(Region region) {
    return builder()
        .region(region)
        .requestTimeout(Duration.ofSeconds(20))
        .maxPriceUSD(0.5)
        .smsType(SmsType.TRANSACTIONAL)
        .deliveryStatusLogging(true)
        .build();
  }

  /** SMS message types supported by AWS SNS. */
  public enum SmsType {
    /**
     * Promotional messages (marketing, ads).
     * Lower delivery priority, may be filtered by carriers.
     */
    PROMOTIONAL,

    /**
     * Transactional messages (OTP, notifications).
     * Higher delivery priority and reliability.
     */
    TRANSACTIONAL
  }

  /** Builder for SnsConfiguration. */
  public static final class Builder {
    private Region region;
    private String accessKeyId;
    private String secretAccessKey;
    private String sessionToken;
    private Duration requestTimeout = Duration.ofSeconds(15);
    private String defaultSenderId;
    private double maxPriceUSD = 0.5; // $0.50 USD default limit
    private SmsType smsType = SmsType.TRANSACTIONAL;
    private boolean deliveryStatusLogging = false;

    private Builder() {}

    /**
     * Sets the AWS region.
     *
     * @param region AWS region for SNS service
     * @return this builder
     */
    public Builder region(Region region) {
      this.region = region;
      return this;
    }

    /**
     * Sets the AWS access key ID.
     *
     * @param accessKeyId AWS access key ID
     * @return this builder
     */
    public Builder accessKeyId(String accessKeyId) {
      this.accessKeyId = accessKeyId;
      return this;
    }

    /**
     * Sets the AWS secret access key.
     *
     * @param secretAccessKey AWS secret access key
     * @return this builder
     */
    public Builder secretAccessKey(String secretAccessKey) {
      this.secretAccessKey = secretAccessKey;
      return this;
    }

    /**
     * Sets the AWS session token (for temporary credentials).
     *
     * @param sessionToken AWS session token
     * @return this builder
     */
    public Builder sessionToken(String sessionToken) {
      this.sessionToken = sessionToken;
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
     * Sets the default sender ID for SMS messages.
     *
     * @param senderId sender ID (up to 11 alphanumeric characters)
     * @return this builder
     */
    public Builder defaultSenderId(String senderId) {
      this.defaultSenderId = senderId;
      return this;
    }

    /**
     * Sets the maximum price per SMS in USD (safety limit).
     *
     * @param maxPriceUSD maximum price per SMS (default: $0.50)
     * @return this builder
     */
    public Builder maxPriceUSD(double maxPriceUSD) {
      this.maxPriceUSD = maxPriceUSD;
      return this;
    }

    /**
     * Sets the SMS message type.
     *
     * @param smsType message type (PROMOTIONAL or TRANSACTIONAL)
     * @return this builder
     */
    public Builder smsType(SmsType smsType) {
      this.smsType = smsType;
      return this;
    }

    /**
     * Enables or disables delivery status logging.
     *
     * @param enabled true to enable delivery status logging
     * @return this builder
     */
    public Builder deliveryStatusLogging(boolean enabled) {
      this.deliveryStatusLogging = enabled;
      return this;
    }

    /**
     * Builds the SnsConfiguration.
     *
     * @return SnsConfiguration instance
     * @throws IllegalArgumentException if required fields are missing or invalid
     */
    public SnsConfiguration build() {
      return new SnsConfiguration(
          region,
          accessKeyId,
          secretAccessKey,
          sessionToken,
          requestTimeout,
          defaultSenderId,
          maxPriceUSD,
          smsType,
          deliveryStatusLogging);
    }
  }
}
