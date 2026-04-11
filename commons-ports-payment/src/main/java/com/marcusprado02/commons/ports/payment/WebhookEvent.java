package com.marcusprado02.commons.ports.payment;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a payment webhook event received from a payment provider.
 *
 * @param id unique event identifier assigned by the provider
 * @param type event type (e.g. "payment_intent.succeeded", "charge.refunded")
 * @param paymentId related payment identifier, if applicable
 * @param data provider-specific event data as key-value pairs
 * @param occurredAt timestamp when the event occurred
 */
public record WebhookEvent(
    String id, String type, String paymentId, Map<String, String> data, Instant occurredAt) {

  /** Validates webhook event fields and creates defensive copies. */
  public WebhookEvent {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(occurredAt, "occurredAt");
    data = data == null ? Map.of() : Map.copyOf(data);
  }
}
