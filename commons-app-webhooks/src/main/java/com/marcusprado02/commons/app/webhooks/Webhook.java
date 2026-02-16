package com.marcusprado02.commons.app.webhooks;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a registered webhook endpoint.
 *
 * <p>A webhook is a user-defined HTTP callback that is triggered when specific events occur. This
 * class contains all the metadata needed to identify and deliver events to the webhook endpoint.
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * Webhook webhook = Webhook.builder()
 *     .id("webhook-123")
 *     .url(URI.create("https://example.com/webhook"))
 *     .events(Set.of("order.created", "order.updated"))
 *     .secret("my-secret-key")
 *     .active(true)
 *     .build();
 * }</pre>
 */
public final class Webhook {

  private final String id;
  private final URI url;
  private final Set<String> events;
  private final String secret;
  private final boolean active;
  private final Instant createdAt;
  private final Instant updatedAt;
  private final String description;

  private Webhook(Builder builder) {
    this.id = Objects.requireNonNull(builder.id, "id cannot be null");
    this.url = Objects.requireNonNull(builder.url, "url cannot be null");
    this.events = Set.copyOf(Objects.requireNonNull(builder.events, "events cannot be null"));
    this.secret = Objects.requireNonNull(builder.secret, "secret cannot be null");
    this.active = builder.active;
    this.createdAt = Objects.requireNonNull(builder.createdAt, "createdAt cannot be null");
    this.updatedAt = builder.updatedAt;
    this.description = builder.description;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getId() {
    return id;
  }

  public URI getUrl() {
    return url;
  }

  public Set<String> getEvents() {
    return events;
  }

  public String getSecret() {
    return secret;
  }

  public boolean isActive() {
    return active;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public String getDescription() {
    return description;
  }

  /**
   * Checks if this webhook is subscribed to the specified event.
   *
   * @param event the event name
   * @return true if subscribed
   */
  public boolean isSubscribedTo(String event) {
    return events.contains(event) || events.contains("*");
  }

  public static final class Builder {
    private String id;
    private URI url;
    private Set<String> events;
    private String secret;
    private boolean active = true;
    private Instant createdAt = Instant.now();
    private Instant updatedAt;
    private String description;

    private Builder() {}

    public Builder id(String id) {
      this.id = id;
      return this;
    }

    public Builder url(URI url) {
      this.url = url;
      return this;
    }

    public Builder events(Set<String> events) {
      this.events = events;
      return this;
    }

    public Builder secret(String secret) {
      this.secret = secret;
      return this;
    }

    public Builder active(boolean active) {
      this.active = active;
      return this;
    }

    public Builder createdAt(Instant createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Builder updatedAt(Instant updatedAt) {
      this.updatedAt = updatedAt;
      return this;
    }

    public Builder description(String description) {
      this.description = description;
      return this;
    }

    public Webhook build() {
      return new Webhook(this);
    }
  }
}
