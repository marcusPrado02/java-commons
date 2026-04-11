package com.marcusprado02.commons.adapters.payment.stripe;

import com.marcusprado02.commons.kernel.errors.ErrorCategory;
import com.marcusprado02.commons.kernel.errors.ErrorCode;
import com.marcusprado02.commons.kernel.errors.Problem;
import com.marcusprado02.commons.kernel.errors.Severity;
import com.marcusprado02.commons.kernel.result.Result;
import com.marcusprado02.commons.ports.payment.PaymentWebhookService;
import com.marcusprado02.commons.ports.payment.WebhookEvent;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;
import java.time.Instant;
import java.util.HashMap;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stripe implementation of {@link PaymentWebhookService}.
 *
 * <p>Verifies the {@code Stripe-Signature} header using the HMAC-SHA256 scheme provided by the
 * Stripe SDK, then converts the raw {@link Event} to a provider-agnostic {@link WebhookEvent}.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * var webhookService = StripeWebhookService.create("whsec_...");
 *
 * // In your HTTP handler:
 * byte[] body  = request.getBodyBytes();
 * String sigHeader = request.getHeader("Stripe-Signature");
 * Result<WebhookEvent> event = webhookService.parseAndVerify(body, sigHeader);
 * }</pre>
 */
public final class StripeWebhookService implements PaymentWebhookService {

  private static final Logger logger = LoggerFactory.getLogger(StripeWebhookService.class);

  /** Stripe event types recognised by this service. */
  private static final Set<String> SUPPORTED_EVENTS =
      Set.of(
          "payment_intent.created",
          "payment_intent.succeeded",
          "payment_intent.payment_failed",
          "payment_intent.canceled",
          "payment_intent.requires_action",
          "charge.succeeded",
          "charge.failed",
          "charge.refunded",
          "customer.subscription.created",
          "customer.subscription.updated",
          "customer.subscription.deleted",
          "invoice.payment_succeeded",
          "invoice.payment_failed");

  private final String webhookSecret;

  private StripeWebhookService(String webhookSecret) {
    this.webhookSecret = webhookSecret;
  }

  /**
   * Creates a new {@link StripeWebhookService}.
   *
   * @param webhookSecret Stripe endpoint secret (starts with {@code whsec_})
   * @return configured service instance
   */
  public static StripeWebhookService create(String webhookSecret) {
    if (webhookSecret == null || webhookSecret.isBlank()) {
      throw new IllegalArgumentException("webhookSecret must not be blank");
    }
    return new StripeWebhookService(webhookSecret);
  }

  @Override
  public Result<WebhookEvent> parseAndVerify(byte[] rawPayload, String signatureHeader) {
    if (rawPayload == null || rawPayload.length == 0) {
      return Result.fail(
          Problem.of(
              ErrorCode.of("WEBHOOK.EMPTY_PAYLOAD"),
              ErrorCategory.BUSINESS,
              Severity.WARNING,
              "Webhook payload must not be empty"));
    }

    if (signatureHeader == null || signatureHeader.isBlank()) {
      return Result.fail(
          Problem.of(
              ErrorCode.of("WEBHOOK.MISSING_SIGNATURE"),
              ErrorCategory.BUSINESS,
              Severity.WARNING,
              "Stripe-Signature header is missing"));
    }

    Event event;
    try {
      event = Webhook.constructEvent(new String(rawPayload), signatureHeader, webhookSecret);
    } catch (SignatureVerificationException e) {
      logger.warn("Stripe webhook signature verification failed: {}", e.getMessage());
      return Result.fail(
          Problem.of(
              ErrorCode.of("WEBHOOK.INVALID_SIGNATURE"),
              ErrorCategory.BUSINESS,
              Severity.WARNING,
              "Webhook signature verification failed: " + e.getMessage()));
    } catch (Exception e) {
      logger.error("Failed to parse Stripe webhook: {}", e.getMessage(), e);
      return Result.fail(
          Problem.of(
              ErrorCode.of("WEBHOOK.PARSE_FAILED"),
              ErrorCategory.TECHNICAL,
              Severity.ERROR,
              "Failed to parse webhook payload: " + e.getMessage()));
    }

    var webhookEvent = mapToWebhookEvent(event);
    logger.info("Received Stripe webhook event: {} ({})", event.getType(), event.getId());
    return Result.ok(webhookEvent);
  }

  @Override
  public boolean supports(String eventType) {
    return SUPPORTED_EVENTS.contains(eventType);
  }

  // -------------------------------------------------------------------------
  // Mapping
  // -------------------------------------------------------------------------

  private WebhookEvent mapToWebhookEvent(Event event) {
    var data = new HashMap<String, String>();
    data.put("livemode", String.valueOf(event.getLivemode()));
    data.put("apiVersion", event.getApiVersion() != null ? event.getApiVersion() : "");

    String paymentId = extractPaymentId(event);

    return new WebhookEvent(
        event.getId(), event.getType(), paymentId, data, Instant.ofEpochSecond(event.getCreated()));
  }

  private String extractPaymentId(Event event) {
    try {
      StripeObject stripeObject = event.getDataObjectDeserializer().getObject().orElse(null);
      if (stripeObject instanceof PaymentIntent pi) {
        return pi.getId();
      }
    } catch (Exception e) {
      logger.debug("Could not extract payment ID from event {}: {}", event.getId(), e.getMessage());
    }
    return null;
  }
}
