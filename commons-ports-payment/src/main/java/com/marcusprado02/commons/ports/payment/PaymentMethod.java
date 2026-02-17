package com.marcusprado02.commons.ports.payment;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Payment method information (credit card, bank account, etc.).
 *
 * @param id unique payment method identifier
 * @param type payment method type (card, bank_account, etc.)
 * @param customerId customer ID associated with this payment method
 * @param last4 last 4 digits of card/account number
 * @param brand card brand (visa, mastercard, amex, etc.) or bank name
 * @param expiryMonth card expiry month (1-12) for card payments
 * @param expiryYear card expiry year for card payments
 * @param isDefault whether this is the customer's default payment method
 * @param createdAt timestamp when payment method was created
 * @param metadata additional provider-specific metadata
 */
public record PaymentMethod(
    String id,
    String type,
    String customerId,
    String last4,
    Optional<String> brand,
    Optional<Integer> expiryMonth,
    Optional<Integer> expiryYear,
    boolean isDefault,
    Instant createdAt,
    Map<String, String> metadata) {

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String id;
    private String type;
    private String customerId;
    private String last4;
    private Optional<String> brand = Optional.empty();
    private Optional<Integer> expiryMonth = Optional.empty();
    private Optional<Integer> expiryYear = Optional.empty();
    private boolean isDefault = false;
    private Instant createdAt = Instant.now();
    private Map<String, String> metadata = Map.of();

    public Builder id(String id) {
      this.id = id;
      return this;
    }

    public Builder type(String type) {
      this.type = type;
      return this;
    }

    public Builder customerId(String customerId) {
      this.customerId = customerId;
      return this;
    }

    public Builder last4(String last4) {
      this.last4 = last4;
      return this;
    }

    public Builder brand(String brand) {
      this.brand = Optional.ofNullable(brand);
      return this;
    }

    public Builder expiryMonth(Integer month) {
      this.expiryMonth = Optional.ofNullable(month);
      return this;
    }

    public Builder expiryYear(Integer year) {
      this.expiryYear = Optional.ofNullable(year);
      return this;
    }

    public Builder isDefault(boolean isDefault) {
      this.isDefault = isDefault;
      return this;
    }

    public Builder createdAt(Instant createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Builder metadata(Map<String, String> metadata) {
      this.metadata = metadata;
      return this;
    }

    public PaymentMethod build() {
      return new PaymentMethod(
          id,
          type,
          customerId,
          last4,
          brand,
          expiryMonth,
          expiryYear,
          isDefault,
          createdAt,
          metadata);
    }
  }
}
