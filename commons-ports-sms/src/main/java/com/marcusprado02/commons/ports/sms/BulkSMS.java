package com.marcusprado02.commons.ports.sms;

import java.util.List;
import java.util.Objects;

/**
 * Bulk SMS message envelope for sending the same message to multiple recipients.
 *
 * <p>Optimized for sending identical messages to many recipients efficiently.
 */
public record BulkSMS(PhoneNumber from, List<PhoneNumber> to, String message, SMSOptions options) {

  public BulkSMS {
    Objects.requireNonNull(from, "from phone number must not be null");
    Objects.requireNonNull(to, "to phone numbers must not be null");
    Objects.requireNonNull(message, "message must not be null");
    Objects.requireNonNull(options, "options must not be null");

    if (to.isEmpty()) {
      throw new IllegalArgumentException("at least one recipient is required");
    }

    if (message.isBlank()) {
      throw new IllegalArgumentException("message cannot be blank");
    }

    // Validate message length
    if (message.length() > 1600) {
      throw new IllegalArgumentException(
          "message too long: " + message.length() + " characters (max 1600)");
    }

    // Limit number of recipients to prevent abuse
    if (to.size() > 1000) {
      throw new IllegalArgumentException("too many recipients: " + to.size() + " (max 1000)");
    }

    // Make defensive copy
    to = List.copyOf(to);
  }

  /**
   * Creates a new builder for constructing BulkSMS instances.
   *
   * @return a new Builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates a simple bulk SMS with default options.
   *
   * @param from sender phone number
   * @param recipients list of recipient phone numbers
   * @param message message content
   * @return BulkSMS instance
   */
  public static BulkSMS of(String from, List<String> recipients, String message) {
    return builder().from(from).toAll(recipients).message(message).build();
  }

  /** Builder for BulkSMS instances. */
  public static final class Builder {
    private PhoneNumber from;
    private java.util.List<PhoneNumber> to = new java.util.ArrayList<>();
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
     * Adds a recipient phone number.
     *
     * @param to recipient phone number string
     * @return this builder
     */
    public Builder to(String to) {
      this.to.add(PhoneNumber.of(to));
      return this;
    }

    /**
     * Adds a recipient phone number.
     *
     * @param to recipient phone number
     * @return this builder
     */
    public Builder to(PhoneNumber to) {
      this.to.add(to);
      return this;
    }

    /**
     * Adds multiple recipients from string list.
     *
     * @param phoneNumbers list of phone number strings
     * @return this builder
     */
    public Builder toAll(List<String> phoneNumbers) {
      phoneNumbers.forEach(this::to);
      return this;
    }

    /**
     * Adds multiple recipients from PhoneNumber list.
     *
     * @param phoneNumbers list of PhoneNumber objects
     * @return this builder
     */
    public Builder toAllPhones(List<PhoneNumber> phoneNumbers) {
      this.to.addAll(phoneNumbers);
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
     * Enables delivery receipt tracking for all messages.
     *
     * @return this builder
     */
    public Builder withDeliveryReceipt() {
      this.options = SMSOptions.builder(options).deliveryReceipt(true).build();
      return this;
    }

    /**
     * Sets message validity period for all messages.
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
     * Builds the BulkSMS instance.
     *
     * @return BulkSMS instance
     */
    public BulkSMS build() {
      return new BulkSMS(from, to, message, options);
    }
  }
}
