package com.marcusprado02.commons.ports.sms;

import java.util.Objects;

/**
 * SMS message envelope.
 *
 * <p>Represents a complete SMS message with sender, recipient, and content.
 */
public record SMS(PhoneNumber from, PhoneNumber to, String message, SMSOptions options) {

  public SMS {
    Objects.requireNonNull(from, "from phone number must not be null");
    Objects.requireNonNull(to, "to phone number must not be null");
    Objects.requireNonNull(message, "message must not be null");
    Objects.requireNonNull(options, "options must not be null");

    if (message.isBlank()) {
      throw new IllegalArgumentException("message cannot be blank");
    }

    // SMS message length validation (standard SMS is 160 characters for GSM, 70 for Unicode)
    if (message.length() > 1600) { // Allow up to 10 concatenated SMS messages
      throw new IllegalArgumentException(
          "message too long: " + message.length() + " characters (max 1600)");
    }
  }

  /**
   * Creates a new builder for constructing SMS instances.
   *
   * @return a new Builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates a simple SMS with default options.
   *
   * @param from sender phone number
   * @param to recipient phone number
   * @param message message content
   * @return SMS instance
   */
  public static SMS of(String from, String to, String message) {
    return builder().from(from).to(to).message(message).build();
  }

  /**
   * Creates a simple SMS with PhoneNumber objects.
   *
   * @param from sender phone number
   * @param to recipient phone number
   * @param message message content
   * @return SMS instance
   */
  public static SMS of(PhoneNumber from, PhoneNumber to, String message) {
    return builder().from(from).to(to).message(message).build();
  }

  /** Builder for SMS instances. */
  public static final class Builder {
    private PhoneNumber from;
    private PhoneNumber to;
    private String message;
    private SMSOptions options = SMSOptions.defaults();

    private Builder() {}

    /**
     * Sets the sender phone number.
     *
     * @param from sender phone number string
     * @return this builder
     */
    public Builder from(String from) {
      this.from = PhoneNumber.of(from);
      return this;
    }

    /**
     * Sets the sender phone number.
     *
     * @param from sender phone number
     * @return this builder
     */
    public Builder from(PhoneNumber from) {
      this.from = from;
      return this;
    }

    /**
     * Sets the recipient phone number.
     *
     * @param to recipient phone number string
     * @return this builder
     */
    public Builder to(String to) {
      this.to = PhoneNumber.of(to);
      return this;
    }

    /**
     * Sets the recipient phone number.
     *
     * @param to recipient phone number
     * @return this builder
     */
    public Builder to(PhoneNumber to) {
      this.to = to;
      return this;
    }

    /**
     * Sets the message content.
     *
     * @param message message text
     * @return this builder
     */
    public Builder message(String message) {
      this.message = message;
      return this;
    }

    /**
     * Sets SMS options.
     *
     * @param options SMS options
     * @return this builder
     */
    public Builder options(SMSOptions options) {
      this.options = options;
      return this;
    }

    /**
     * Enables delivery receipt tracking.
     *
     * @return this builder
     */
    public Builder withDeliveryReceipt() {
      this.options = SMSOptions.builder(options).deliveryReceipt(true).build();
      return this;
    }

    /**
     * Sets message validity period.
     *
     * @param validityPeriodMinutes validity period in minutes
     * @return this builder
     */
    public Builder validityPeriod(int validityPeriodMinutes) {
      this.options =
          SMSOptions.builder(options).validityPeriodMinutes(validityPeriodMinutes).build();
      return this;
    }

    /**
     * Builds the SMS instance.
     *
     * @return SMS instance
     */
    public SMS build() {
      return new SMS(from, to, message, options);
    }
  }
}
