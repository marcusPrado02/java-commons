package com.marcusprado02.commons.ports.sms;

/**
 * Configuration options for SMS sending.
 *
 * <p>Provides various options that can be set when sending SMS messages.
 */
public record SMSOptions(
    boolean deliveryReceipt,
    int validityPeriodMinutes,
    SMSPriority priority,
    String webhookUrl) {

  public SMSOptions {
    if (validityPeriodMinutes < 0) {
      throw new IllegalArgumentException("validity period must be non-negative");
    }
    if (validityPeriodMinutes > 10080) { // 7 days max
      throw new IllegalArgumentException("validity period cannot exceed 10080 minutes (7 days)");
    }
  }

  /**
   * Creates default SMS options.
   *
   * @return default options
   */
  public static SMSOptions defaults() {
    return new SMSOptions(false, 1440, SMSPriority.NORMAL, null); // 24 hours validity
  }

  /**
   * Creates a builder from existing options.
   *
   * @param existing existing options to copy
   * @return builder with existing values
   */
  public static Builder builder(SMSOptions existing) {
    return new Builder()
        .deliveryReceipt(existing.deliveryReceipt)
        .validityPeriodMinutes(existing.validityPeriodMinutes)
        .priority(existing.priority)
        .webhookUrl(existing.webhookUrl);
  }

  /**
   * Creates a new builder.
   *
   * @return new builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /** SMS priority levels. */
  public enum SMSPriority {
    LOW,
    NORMAL,
    HIGH,
    URGENT
  }

  /** Builder for SMSOptions. */
  public static final class Builder {
    private boolean deliveryReceipt = false;
    private int validityPeriodMinutes = 1440; // 24 hours
    private SMSPriority priority = SMSPriority.NORMAL;
    private String webhookUrl;

    private Builder() {}

    /**
     * Enables or disables delivery receipt.
     *
     * @param deliveryReceipt true to enable delivery receipt
     * @return this builder
     */
    public Builder deliveryReceipt(boolean deliveryReceipt) {
      this.deliveryReceipt = deliveryReceipt;
      return this;
    }

    /**
     * Sets message validity period.
     *
     * @param minutes validity period in minutes (max 10080 = 7 days)
     * @return this builder
     */
    public Builder validityPeriodMinutes(int minutes) {
      this.validityPeriodMinutes = minutes;
      return this;
    }

    /**
     * Sets message priority.
     *
     * @param priority SMS priority
     * @return this builder
     */
    public Builder priority(SMSPriority priority) {
      this.priority = priority;
      return this;
    }

    /**
     * Sets webhook URL for status callbacks.
     *
     * @param webhookUrl webhook URL
     * @return this builder
     */
    public Builder webhookUrl(String webhookUrl) {
      this.webhookUrl = webhookUrl;
      return this;
    }

    /**
     * Builds SMSOptions instance.
     *
     * @return SMSOptions instance
     */
    public SMSOptions build() {
      return new SMSOptions(deliveryReceipt, validityPeriodMinutes, priority, webhookUrl);
    }
  }
}
