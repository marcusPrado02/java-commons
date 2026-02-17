package com.marcusprado02.commons.ports.payment;

/**
 * Payment status enum representing different states of a payment transaction.
 *
 * <p>The payment lifecycle typically follows this flow: PENDING → PROCESSING → SUCCEEDED or PENDING
 * → PROCESSING → FAILED with CANCELED and REFUNDED as alternative end states.
 */
public enum PaymentStatus {
  /** Payment has been created but not yet processed. */
  PENDING,

  /** Payment is currently being processed by the payment provider. */
  PROCESSING,

  /** Payment has been successfully completed. */
  SUCCEEDED,

  /** Payment failed due to insufficient funds, declined card, or other errors. */
  FAILED,

  /** Payment was canceled before completion. */
  CANCELED,

  /** Payment was refunded after being completed. */
  REFUNDED,

  /** Payment requires additional action (e.g., 3D Secure authentication). */
  REQUIRES_ACTION
}
