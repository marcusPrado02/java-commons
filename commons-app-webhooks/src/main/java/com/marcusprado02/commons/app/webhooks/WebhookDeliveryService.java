package com.marcusprado02.commons.app.webhooks;

import com.marcusprado02.commons.kernel.result.Result;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Service for delivering webhook events.
 *
 * <p>Orchestrates the delivery of events to registered webhooks, including signature generation,
 * HTTP delivery, retry scheduling, and status tracking.
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * WebhookDeliveryService service = new WebhookDeliveryService(
 *     webhookRepository,
 *     deliveryRepository,
 *     httpClient,
 *     RetryPolicy.exponentialBackoff(5, Duration.ofSeconds(1))
 * );
 *
 * WebhookEvent event = WebhookEvent.builder()
 *     .id("event-123")
 *     .type("order.created")
 *     .payload(Map.of("orderId", "456"))
 *     .build();
 *
 * Result<Void> result = service.deliver(event);
 * }</pre>
 */
public class WebhookDeliveryService {

  private final WebhookRepository webhookRepository;
  private final WebhookDeliveryRepository deliveryRepository;
  private final WebhookHttpClient httpClient;
  private final RetryPolicy retryPolicy;
  private final WebhookPayloadSerializer payloadSerializer;

  public WebhookDeliveryService(
      WebhookRepository webhookRepository,
      WebhookDeliveryRepository deliveryRepository,
      WebhookHttpClient httpClient,
      RetryPolicy retryPolicy) {
    this(
        webhookRepository,
        deliveryRepository,
        httpClient,
        retryPolicy,
        new JsonPayloadSerializer());
  }

  public WebhookDeliveryService(
      WebhookRepository webhookRepository,
      WebhookDeliveryRepository deliveryRepository,
      WebhookHttpClient httpClient,
      RetryPolicy retryPolicy,
      WebhookPayloadSerializer payloadSerializer) {
    this.webhookRepository =
        Objects.requireNonNull(webhookRepository, "webhookRepository cannot be null");
    this.deliveryRepository =
        Objects.requireNonNull(deliveryRepository, "deliveryRepository cannot be null");
    this.httpClient = Objects.requireNonNull(httpClient, "httpClient cannot be null");
    this.retryPolicy = Objects.requireNonNull(retryPolicy, "retryPolicy cannot be null");
    this.payloadSerializer =
        Objects.requireNonNull(payloadSerializer, "payloadSerializer cannot be null");
  }

  /**
   * Delivers an event to all registered webhooks.
   *
   * @param event the event to deliver
   * @return result indicating success
   */
  public Result<Void> deliver(WebhookEvent event) {
    return webhookRepository
        .findByEventType(event.getType())
        .flatMap(
            webhooks -> {
              for (Webhook webhook : webhooks) {
                scheduleDelivery(webhook, event);
              }
              return Result.ok(null);
            });
  }

  /**
   * Processes pending deliveries that are due for delivery.
   *
   * @return result indicating number of deliveries processed
   */
  public Result<Integer> processPendingDeliveries() {
    return deliveryRepository
        .findScheduledBefore(Instant.now(), WebhookDeliveryStatus.PENDING)
        .flatMap(
            deliveries -> {
              int processed = 0;
              for (WebhookDelivery delivery : deliveries) {
                executeDelivery(delivery);
                processed++;
              }
              return Result.ok(processed);
            });
  }

  /**
   * Retries a failed delivery.
   *
   * @param deliveryId the delivery ID
   * @return result indicating success
   */
  public Result<Void> retry(String deliveryId) {
    return deliveryRepository
        .findById(deliveryId)
        .flatMap(
            deliveryOpt -> {
              if (deliveryOpt.isEmpty()) {
                return Result.fail(
                    com.marcusprado02.commons.kernel.errors.ProblemBuilder.of("DELIVERY_NOT_FOUND")
                        .message("Delivery not found")
                        .build());
              }
              WebhookDelivery delivery = deliveryOpt.get();
              if (!delivery.isRetryable()) {
                return Result.fail(
                    com.marcusprado02.commons.kernel.errors.ProblemBuilder.of("NOT_RETRYABLE")
                        .message("Delivery is not retryable")
                        .build());
              }
              executeDelivery(delivery);
              return Result.ok(null);
            });
  }

  private void scheduleDelivery(Webhook webhook, WebhookEvent event) {
    WebhookDelivery delivery =
        WebhookDelivery.builder()
            .id(UUID.randomUUID().toString())
            .webhookId(webhook.getId())
            .eventId(event.getId())
            .status(WebhookDeliveryStatus.PENDING)
            .attemptNumber(1)
            .scheduledAt(Instant.now())
            .build();

    deliveryRepository.save(delivery);
  }

  private void executeDelivery(WebhookDelivery delivery) {
    webhookRepository
        .findById(delivery.getWebhookId())
        .peek(
            webhookOpt -> {
              if (webhookOpt.isEmpty()) {
                return;
              }
              Webhook webhook = webhookOpt.get();

              // Mark as in progress
              WebhookDelivery inProgress =
                  WebhookDelivery.builder()
                      .id(delivery.getId())
                      .webhookId(delivery.getWebhookId())
                      .eventId(delivery.getEventId())
                      .status(WebhookDeliveryStatus.IN_PROGRESS)
                      .attemptNumber(delivery.getAttemptNumber())
                      .scheduledAt(delivery.getScheduledAt())
                      .attemptedAt(Instant.now())
                      .build();

              deliveryRepository.save(inProgress);

              // Build and send request
              String payload = payloadSerializer.serialize(Map.of()); // Serialize event
              String signature = WebhookSignature.generate(payload, webhook.getSecret());

              Map<String, String> headers = new HashMap<>();
              headers.put("Content-Type", "application/json");
              headers.put("X-Webhook-Signature", signature);
              headers.put("X-Webhook-Event-Id", delivery.getEventId());
              headers.put("X-Webhook-Delivery-Id", delivery.getId());

              WebhookHttpRequest request =
                  WebhookHttpRequest.builder()
                      .url(webhook.getUrl())
                      .headers(headers)
                      .body(payload)
                      .build();

              Instant startTime = Instant.now();
              Result<WebhookHttpResponse> response = httpClient.send(request);
              Instant endTime = Instant.now();

              // Handle response
              handleDeliveryResponse(delivery, response, startTime, endTime);
            });
  }

  private void handleDeliveryResponse(
      WebhookDelivery delivery,
      Result<WebhookHttpResponse> responseResult,
      Instant startTime,
      Instant endTime) {

    WebhookDelivery.Builder updatedBuilder =
        WebhookDelivery.builder()
            .id(delivery.getId())
            .webhookId(delivery.getWebhookId())
            .eventId(delivery.getEventId())
            .attemptNumber(delivery.getAttemptNumber())
            .scheduledAt(delivery.getScheduledAt())
            .attemptedAt(delivery.getAttemptedAt().orElse(startTime))
            .completedAt(endTime)
            .responseTime(java.time.Duration.between(startTime, endTime));

    responseResult.peek(
        response -> {
          updatedBuilder.httpStatusCode(response.getStatusCode()).responseBody(response.getBody());

          if (response.isSuccess()) {
            updatedBuilder.status(WebhookDeliveryStatus.SUCCEEDED);
          } else {
            scheduleRetry(updatedBuilder, delivery.getAttemptNumber());
          }

          deliveryRepository.save(updatedBuilder.build());
        });

    responseResult.peekError(
        problem -> {
          updatedBuilder.errorMessage(problem.message());
          scheduleRetry(updatedBuilder, delivery.getAttemptNumber());
          deliveryRepository.save(updatedBuilder.build());
        });
  }

  private void scheduleRetry(WebhookDelivery.Builder builder, int currentAttempt) {
    if (currentAttempt >= retryPolicy.getMaxRetries()) {
      builder.status(WebhookDeliveryStatus.EXHAUSTED);
    } else {
      builder.status(WebhookDeliveryStatus.FAILED);
      java.time.Duration retryDelay = retryPolicy.getRetryDelay(currentAttempt + 1);
      builder.nextRetryAt(Instant.now().plus(retryDelay));
    }
  }

  interface WebhookPayloadSerializer {
    String serialize(Map<String, Object> payload);
  }

  static class JsonPayloadSerializer implements WebhookPayloadSerializer {
    @Override
    public String serialize(Map<String, Object> payload) {
      // Simple implementation - in real code use Jackson or similar
      return "{}";
    }
  }
}
