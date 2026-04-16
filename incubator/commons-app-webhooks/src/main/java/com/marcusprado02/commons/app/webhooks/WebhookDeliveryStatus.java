package com.marcusprado02.commons.app.webhooks;

/**
 * Status of a webhook delivery attempt.
 *
 * <p>Represents the various states a webhook delivery can be in throughout its lifecycle.
 */
public enum WebhookDeliveryStatus {

  /** Delivery is pending and has not been attempted yet. */
  PENDING,

  /** Delivery is currently in progress. */
  IN_PROGRESS,

  /** Delivery succeeded (HTTP 2xx response). */
  SUCCEEDED,

  /** Delivery failed (HTTP error, timeout, or other error). */
  FAILED,

  /** Delivery exceeded maximum retry attempts and will not be retried. */
  EXHAUSTED,

  /** Delivery was cancelled manually. */
  CANCELLED
}
