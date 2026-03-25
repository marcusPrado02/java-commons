package com.marcusprado02.commons.ports.payment;

import com.marcusprado02.commons.kernel.result.Result;

/**
 * Port for receiving and verifying payment provider webhooks.
 *
 * <p>Implementations are responsible for:
 * <ul>
 *   <li>Verifying the webhook signature to ensure authenticity</li>
 *   <li>Parsing the raw payload into a {@link WebhookEvent}</li>
 * </ul>
 *
 * <p>Usage example:
 * <pre>{@code
 * Result<WebhookEvent> result = webhookService.parseAndVerify(rawBody, signatureHeader);
 * result.peek(event -> {
 *     switch (event.type()) {
 *         case "payment_intent.succeeded" -> handleSuccess(event);
 *         case "payment_intent.payment_failed" -> handleFailure(event);
 *     }
 * });
 * }</pre>
 */
public interface PaymentWebhookService {

  /**
   * Verifies the webhook signature and parses the raw payload into a {@link WebhookEvent}.
   *
   * @param rawPayload raw request body bytes exactly as received (must not be modified)
   * @param signatureHeader value of the provider-specific signature header
   * @return parsed event on success, or a failure describing the verification error
   */
  Result<WebhookEvent> parseAndVerify(byte[] rawPayload, String signatureHeader);

  /**
   * Checks whether this service can handle the given event type.
   *
   * @param eventType event type string (e.g. "payment_intent.succeeded")
   * @return true if the event type is recognized
   */
  boolean supports(String eventType);
}
