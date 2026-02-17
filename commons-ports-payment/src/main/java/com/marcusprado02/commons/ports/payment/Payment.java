package com.marcusprado02.commons.ports.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Payment transaction record containing all payment details.
 *
 * @param id unique payment identifier
 * @param amount payment amount in minor units (cents, pence, etc.)
 * @param currency ISO 4217 currency code (USD, EUR, GBP, etc.)
 * @param status current payment status
 * @param customerId customer identifier
 * @param paymentMethodId payment method identifier
 * @param description payment description
 * @param statementDescriptor descriptor shown on customer's statement
 * @param receiptEmail email address for receipt
 * @param createdAt timestamp when payment was created
 * @param updatedAt timestamp when payment was last updated
 * @param metadata additional key-value metadata
 * @param error error message if payment failed
 */
public record Payment(
    String id,
    BigDecimal amount,
    String currency,
    PaymentStatus status,
    String customerId,
    Optional<String> paymentMethodId,
    Optional<String> description,
    Optional<String> statementDescriptor,
    Optional<String> receiptEmail,
    Instant createdAt,
    Instant updatedAt,
    Map<String, String> metadata,
    Optional<String> error) {

  /** Check if payment is in a terminal state (succeeded, failed, canceled, refunded). */
  public boolean isTerminal() {
    return status == PaymentStatus.SUCCEEDED
        || status == PaymentStatus.FAILED
        || status == PaymentStatus.CANCELED
        || status == PaymentStatus.REFUNDED;
  }

  /** Check if payment succeeded. */
  public boolean isSucceeded() {
    return status == PaymentStatus.SUCCEEDED;
  }

  /** Check if payment failed. */
  public boolean isFailed() {
    return status == PaymentStatus.FAILED;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String id;
    private BigDecimal amount;
    private String currency;
    private PaymentStatus status = PaymentStatus.PENDING;
    private String customerId;
    private Optional<String> paymentMethodId = Optional.empty();
    private Optional<String> description = Optional.empty();
    private Optional<String> statementDescriptor = Optional.empty();
    private Optional<String> receiptEmail = Optional.empty();
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();
    private Map<String, String> metadata = Map.of();
    private Optional<String> error = Optional.empty();

    public Builder id(String id) {
      this.id = id;
      return this;
    }

    public Builder amount(BigDecimal amount) {
      this.amount = amount;
      return this;
    }

    public Builder currency(String currency) {
      this.currency = currency;
      return this;
    }

    public Builder status(PaymentStatus status) {
      this.status = status;
      return this;
    }

    public Builder customerId(String customerId) {
      this.customerId = customerId;
      return this;
    }

    public Builder paymentMethodId(String paymentMethodId) {
      this.paymentMethodId = Optional.ofNullable(paymentMethodId);
      return this;
    }

    public Builder description(String description) {
      this.description = Optional.ofNullable(description);
      return this;
    }

    public Builder statementDescriptor(String statementDescriptor) {
      this.statementDescriptor = Optional.ofNullable(statementDescriptor);
      return this;
    }

    public Builder receiptEmail(String receiptEmail) {
      this.receiptEmail = Optional.ofNullable(receiptEmail);
      return this;
    }

    public Builder createdAt(Instant createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Builder updatedAt(Instant updatedAt) {
      this.updatedAt = updatedAt;
      return this;
    }

    public Builder metadata(Map<String, String> metadata) {
      this.metadata = metadata;
      return this;
    }

    public Builder error(String error) {
      this.error = Optional.ofNullable(error);
      return this;
    }

    public Payment build() {
      return new Payment(
          id,
          amount,
          currency,
          status,
          customerId,
          paymentMethodId,
          description,
          statementDescriptor,
          receiptEmail,
          createdAt,
          updatedAt,
          metadata,
          error);
    }
  }
}
