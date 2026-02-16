/**
 * Webhook delivery system with retry policies, signature verification, and delivery tracking.
 *
 * <p>This module provides a complete webhook infrastructure for sending HTTP callbacks to
 * registered endpoints when events occur in your application.
 *
 * <h2>Core Concepts</h2>
 *
 * <ul>
 *   <li>{@link com.marcusprado02.commons.app.webhooks.Webhook}: A registered webhook endpoint with
 *       URL, events, and secret
 *   <li>{@link com.marcusprado02.commons.app.webhooks.WebhookEvent}: An event to be delivered to
 *       webhooks
 *   <li>{@link com.marcusprado02.commons.app.webhooks.WebhookDelivery}: A delivery attempt with
 *       status tracking
 *   <li>{@link com.marcusprado02.commons.app.webhooks.WebhookDeliveryService}: Orchestrates event
 *       delivery
 *   <li>{@link com.marcusprado02.commons.app.webhooks.RetryPolicy}: Configures retry behavior
 *   <li>{@link com.marcusprado02.commons.app.webhooks.WebhookSignature}: HMAC-SHA256 signature
 *       generation/verification
 * </ul>
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * // Register a webhook
 * Webhook webhook = Webhook.builder()
 *     .id("webhook-123")
 *     .url(URI.create("https://example.com/webhook"))
 *     .events(Set.of("order.created", "order.updated"))
 *     .secret("my-secret-key")
 *     .active(true)
 *     .build();
 *
 * webhookRepository.save(webhook);
 *
 * // Create and deliver an event
 * WebhookEvent event = WebhookEvent.builder()
 *     .id("event-456")
 *     .type("order.created")
 *     .payload(Map.of("orderId", "789", "amount", 100.0))
 *     .build();
 *
 * WebhookDeliveryService service = new WebhookDeliveryService(
 *     webhookRepository,
 *     deliveryRepository,
 *     httpClient,
 *     RetryPolicy.exponentialBackoff(5, Duration.ofSeconds(1))
 * );
 *
 * service.deliver(event);
 *
 * // Process pending deliveries (scheduled job)
 * service.processPendingDeliveries();
 *
 * // Verify signature on receiver side
 * String payload = request.getBody();
 * String signature = request.getHeader("X-Webhook-Signature");
 * boolean valid = WebhookSignature.verify(payload, secret, signature);
 * }</pre>
 *
 * <h2>Features</h2>
 *
 * <ul>
 *   <li><b>Event Subscription:</b> Webhooks subscribe to specific event types
 *   <li><b>Automatic Retries:</b> Failed deliveries are retried with configurable backoff
 *   <li><b>Signature Verification:</b> HMAC-SHA256 signatures prevent spoofing
 *   <li><b>Delivery Tracking:</b> Full audit trail of delivery attempts
 *   <li><b>Status Management:</b> Track pending, in-progress, succeeded, failed, exhausted states
 *   <li><b>Configurable Retry:</b> Exponential backoff, fixed delay, or no retry policies
 *   <li><b>Idempotency:</b> Events can include idempotency keys
 * </ul>
 *
 * <h2>Best Practices</h2>
 *
 * <ul>
 *   <li>Always verify signatures on the receiver side
 *   <li>Use exponential backoff for retries (prevents overwhelming failed endpoints)
 *   <li>Include idempotency keys in events for duplicate detection
 *   <li>Set reasonable retry limits (5-10 attempts)
 *   <li>Archive old delivery records to prevent database bloat
 *   <li>Monitor delivery success rates and alert on failures
 *   <li>Provide webhook management UI for users
 *   <li>Support webhook testing endpoints
 * </ul>
 */
package com.marcusprado02.commons.app.webhooks;
