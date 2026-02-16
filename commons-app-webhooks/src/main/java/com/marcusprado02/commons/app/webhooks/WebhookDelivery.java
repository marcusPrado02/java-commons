package com.marcusprado02.commons.app.webhooks;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents a webhook delivery attempt.
 *
 * <p>Tracks the delivery of a webhook event to a specific endpoint, including retry information,
 * HTTP response details, and timing metadata.
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * WebhookDelivery delivery = WebhookDelivery.builder()
 *     .id("delivery-789")
 *     .webhookId("webhook-123")
 *     .eventId("event-456")
 *     .status(WebhookDeliveryStatus.SUCCEEDED)
 *     .attemptNumber(1)
 *     .httpStatusCode(200)
 *     .build();
 * }</pre>
 */
public final class WebhookDelivery {

  private final String id;
  private final String webhookId;
  private final String eventId;
  private final WebhookDeliveryStatus status;
  private final int attemptNumber;
  private final Integer httpStatusCode;
  private final String responseBody;
  private final String errorMessage;
  private final Instant scheduledAt;
  private final Instant attemptedAt;
  private final Instant completedAt;
  private final Duration responseTime;
  private final Instant nextRetryAt;

  private WebhookDelivery(Builder builder) {
    this.id = Objects.requireNonNull(builder.id, "id cannot be null");
    this.webhookId = Objects.requireNonNull(builder.webhookId, "webhookId cannot be null");
    this.eventId = Objects.requireNonNull(builder.eventId, "eventId cannot be null");
    this.status = Objects.requireNonNull(builder.status, "status cannot be null");
    this.attemptNumber = builder.attemptNumber;
    this.httpStatusCode = builder.httpStatusCode;
    this.responseBody = builder.responseBody;
    this.errorMessage = builder.errorMessage;
    this.scheduledAt = Objects.requireNonNull(builder.scheduledAt, "scheduledAt cannot be null");
    this.attemptedAt = builder.attemptedAt;
    this.completedAt = builder.completedAt;
    this.responseTime = builder.responseTime;
    this.nextRetryAt = builder.nextRetryAt;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getId() {
    return id;
  }

  public String getWebhookId() {
    return webhookId;
  }

  public String getEventId() {
    return eventId;
  }

  public WebhookDeliveryStatus getStatus() {
    return status;
  }

  public int getAttemptNumber() {
    return attemptNumber;
  }

  public Optional<Integer> getHttpStatusCode() {
    return Optional.ofNullable(httpStatusCode);
  }

  public Optional<String> getResponseBody() {
    return Optional.ofNullable(responseBody);
  }

  public Optional<String> getErrorMessage() {
    return Optional.ofNullable(errorMessage);
  }

  public Instant getScheduledAt() {
    return scheduledAt;
  }

  public Optional<Instant> getAttemptedAt() {
    return Optional.ofNullable(attemptedAt);
  }

  public Optional<Instant> getCompletedAt() {
    return Optional.ofNullable(completedAt);
  }

  public Optional<Duration> getResponseTime() {
    return Optional.ofNullable(responseTime);
  }

  public Optional<Instant> getNextRetryAt() {
    return Optional.ofNullable(nextRetryAt);
  }

  /**
   * Checks if this delivery is in a terminal state (succeeded, exhausted, or cancelled).
   *
   * @return true if terminal
   */
  public boolean isTerminal() {
    return status == WebhookDeliveryStatus.SUCCEEDED
        || status == WebhookDeliveryStatus.EXHAUSTED
        || status == WebhookDeliveryStatus.CANCELLED;
  }

  /**
   * Checks if this delivery can be retried.
   *
   * @return true if retryable
   */
  public boolean isRetryable() {
    return status == WebhookDeliveryStatus.FAILED && nextRetryAt != null;
  }

  public static final class Builder {
    private String id;
    private String webhookId;
    private String eventId;
    private WebhookDeliveryStatus status;
    private int attemptNumber = 1;
    private Integer httpStatusCode;
    private String responseBody;
    private String errorMessage;
    private Instant scheduledAt = Instant.now();
    private Instant attemptedAt;
    private Instant completedAt;
    private Duration responseTime;
    private Instant nextRetryAt;

    private Builder() {}

    public Builder id(String id) {
      this.id = id;
      return this;
    }

    public Builder webhookId(String webhookId) {
      this.webhookId = webhookId;
      return this;
    }

    public Builder eventId(String eventId) {
      this.eventId = eventId;
      return this;
    }

    public Builder status(WebhookDeliveryStatus status) {
      this.status = status;
      return this;
    }

    public Builder attemptNumber(int attemptNumber) {
      this.attemptNumber = attemptNumber;
      return this;
    }

    public Builder httpStatusCode(Integer httpStatusCode) {
      this.httpStatusCode = httpStatusCode;
      return this;
    }

    public Builder responseBody(String responseBody) {
      this.responseBody = responseBody;
      return this;
    }

    public Builder errorMessage(String errorMessage) {
      this.errorMessage = errorMessage;
      return this;
    }

    public Builder scheduledAt(Instant scheduledAt) {
      this.scheduledAt = scheduledAt;
      return this;
    }

    public Builder attemptedAt(Instant attemptedAt) {
      this.attemptedAt = attemptedAt;
      return this;
    }

    public Builder completedAt(Instant completedAt) {
      this.completedAt = completedAt;
      return this;
    }

    public Builder responseTime(Duration responseTime) {
      this.responseTime = responseTime;
      return this;
    }

    public Builder nextRetryAt(Instant nextRetryAt) {
      this.nextRetryAt = nextRetryAt;
      return this;
    }

    public WebhookDelivery build() {
      return new WebhookDelivery(this);
    }
  }
}
