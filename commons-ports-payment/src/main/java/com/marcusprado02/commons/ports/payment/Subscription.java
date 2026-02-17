package com.marcusprado02.commons.ports.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Subscription record for recurring payments.
 *
 * @param id unique subscription identifier
 * @param customerId customer identifier
 * @param status subscription status (active, canceled, past_due, etc.)
 * @param priceId price/plan identifier
 * @param amount subscription amount per billing cycle
 * @param currency ISO 4217 currency code
 * @param interval billing interval (day, week, month, year)
 * @param intervalCount number of intervals between billing cycles
 * @param currentPeriodStart start of current billing period
 * @param currentPeriodEnd end of current billing period
 * @param canceledAt timestamp when subscription was canceled
 * @param endedAt timestamp when subscription ended
 * @param createdAt timestamp when subscription was created
 * @param metadata additional key-value metadata
 */
public record Subscription(
    String id,
    String customerId,
    String status,
    String priceId,
    BigDecimal amount,
    String currency,
    String interval,
    int intervalCount,
    Instant currentPeriodStart,
    Instant currentPeriodEnd,
    Optional<Instant> canceledAt,
    Optional<Instant> endedAt,
    Instant createdAt,
    Map<String, String> metadata) {

  /** Check if subscription is active. */
  public boolean isActive() {
    return "active".equals(status) || "trialing".equals(status);
  }

  /** Check if subscription is canceled. */
  public boolean isCanceled() {
    return canceledAt.isPresent();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String id;
    private String customerId;
    private String status = "active";
    private String priceId;
    private BigDecimal amount;
    private String currency;
    private String interval = "month";
    private int intervalCount = 1;
    private Instant currentPeriodStart = Instant.now();
    private Instant currentPeriodEnd = Instant.now().plusSeconds(30 * 24 * 60 * 60); // 30 days
    private Optional<Instant> canceledAt = Optional.empty();
    private Optional<Instant> endedAt = Optional.empty();
    private Instant createdAt = Instant.now();
    private Map<String, String> metadata = Map.of();

    public Builder id(String id) {
      this.id = id;
      return this;
    }

    public Builder customerId(String customerId) {
      this.customerId = customerId;
      return this;
    }

    public Builder status(String status) {
      this.status = status;
      return this;
    }

    public Builder priceId(String priceId) {
      this.priceId = priceId;
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

    public Builder interval(String interval) {
      this.interval = interval;
      return this;
    }

    public Builder intervalCount(int intervalCount) {
      this.intervalCount = intervalCount;
      return this;
    }

    public Builder currentPeriodStart(Instant currentPeriodStart) {
      this.currentPeriodStart = currentPeriodStart;
      return this;
    }

    public Builder currentPeriodEnd(Instant currentPeriodEnd) {
      this.currentPeriodEnd = currentPeriodEnd;
      return this;
    }

    public Builder canceledAt(Instant canceledAt) {
      this.canceledAt = Optional.ofNullable(canceledAt);
      return this;
    }

    public Builder endedAt(Instant endedAt) {
      this.endedAt = Optional.ofNullable(endedAt);
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

    public Subscription build() {
      return new Subscription(
          id,
          customerId,
          status,
          priceId,
          amount,
          currency,
          interval,
          intervalCount,
          currentPeriodStart,
          currentPeriodEnd,
          canceledAt,
          endedAt,
          createdAt,
          metadata);
    }
  }
}
