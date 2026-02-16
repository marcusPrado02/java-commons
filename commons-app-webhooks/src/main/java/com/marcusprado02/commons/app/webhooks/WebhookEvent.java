package com.marcusprado02.commons.app.webhooks;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Represents an event to be delivered via webhook.
 *
 * <p>Webhook events contain the event type, payload data, and metadata needed for delivery and
 * tracking.
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * WebhookEvent event = WebhookEvent.builder()
 *     .id("event-456")
 *     .type("order.created")
 *     .payload(Map.of("orderId", "123", "amount", 100.0))
 *     .build();
 * }</pre>
 */
public final class WebhookEvent {

  private final String id;
  private final String type;
  private final Map<String, Object> payload;
  private final Instant occurredAt;
  private final String idempotencyKey;

  private WebhookEvent(Builder builder) {
    this.id = Objects.requireNonNull(builder.id, "id cannot be null");
    this.type = Objects.requireNonNull(builder.type, "type cannot be null");
    this.payload = Map.copyOf(Objects.requireNonNull(builder.payload, "payload cannot be null"));
    this.occurredAt = Objects.requireNonNull(builder.occurredAt, "occurredAt cannot be null");
    this.idempotencyKey = builder.idempotencyKey;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getId() {
    return id;
  }

  public String getType() {
    return type;
  }

  public Map<String, Object> getPayload() {
    return payload;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }

  public String getIdempotencyKey() {
    return idempotencyKey;
  }

  public static final class Builder {
    private String id;
    private String type;
    private Map<String, Object> payload;
    private Instant occurredAt = Instant.now();
    private String idempotencyKey;

    private Builder() {}

    public Builder id(String id) {
      this.id = id;
      return this;
    }

    public Builder type(String type) {
      this.type = type;
      return this;
    }

    public Builder payload(Map<String, Object> payload) {
      this.payload = payload;
      return this;
    }

    public Builder occurredAt(Instant occurredAt) {
      this.occurredAt = occurredAt;
      return this;
    }

    public Builder idempotencyKey(String idempotencyKey) {
      this.idempotencyKey = idempotencyKey;
      return this;
    }

    public WebhookEvent build() {
      return new WebhookEvent(this);
    }
  }
}
