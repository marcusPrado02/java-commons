package com.marcusprado02.commons.ports.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Refund record for reversed payments.
 *
 * @param id unique refund identifier
 * @param paymentId original payment identifier
 * @param amount refund amount (can be partial or full)
 * @param currency ISO 4217 currency code
 * @param status refund status (pending, succeeded, failed, canceled)
 * @param reason refund reason (duplicate, fraudulent, requested_by_customer)
 * @param createdAt timestamp when refund was created
 * @param metadata additional key-value metadata
 * @param error error message if refund failed
 */
public record Refund(
    String id,
    String paymentId,
    BigDecimal amount,
    String currency,
    String status,
    Optional<String> reason,
    Instant createdAt,
    Map<String, String> metadata,
    Optional<String> error) {

  /** Check if refund succeeded. */
  public boolean isSucceeded() {
    return "succeeded".equals(status);
  }

  /** Check if refund failed. */
  public boolean isFailed() {
    return "failed".equals(status);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String id;
    private String paymentId;
    private BigDecimal amount;
    private String currency;
    private String status = "pending";
    private Optional<String> reason = Optional.empty();
    private Instant createdAt = Instant.now();
    private Map<String, String> metadata = Map.of();
    private Optional<String> error = Optional.empty();

    public Builder id(String id) {
      this.id = id;
      return this;
    }

    public Builder paymentId(String paymentId) {
      this.paymentId = paymentId;
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

    public Builder status(String status) {
      this.status = status;
      return this;
    }

    public Builder reason(String reason) {
      this.reason = Optional.ofNullable(reason);
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

    public Builder error(String error) {
      this.error = Optional.ofNullable(error);
      return this;
    }

    public Refund build() {
      return new Refund(
          id, paymentId, amount, currency, status, reason, createdAt, metadata, error);
    }
  }
}
