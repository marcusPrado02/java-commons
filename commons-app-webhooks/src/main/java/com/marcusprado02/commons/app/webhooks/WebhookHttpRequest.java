package com.marcusprado02.commons.app.webhooks;

import java.net.URI;
import java.util.Map;
import java.util.Objects;

/**
 * Represents an HTTP request to deliver a webhook.
 *
 * <p>Contains the URL, headers, and payload for the webhook delivery.
 */
public final class WebhookHttpRequest {

  private final URI url;
  private final Map<String, String> headers;
  private final String body;

  private WebhookHttpRequest(Builder builder) {
    this.url = Objects.requireNonNull(builder.url, "url cannot be null");
    this.headers = Map.copyOf(Objects.requireNonNull(builder.headers, "headers cannot be null"));
    this.body = Objects.requireNonNull(builder.body, "body cannot be null");
  }

  public static Builder builder() {
    return new Builder();
  }

  public URI getUrl() {
    return url;
  }

  public Map<String, String> getHeaders() {
    return headers;
  }

  public String getBody() {
    return body;
  }

  public static final class Builder {
    private URI url;
    private Map<String, String> headers = Map.of();
    private String body;

    private Builder() {}

    public Builder url(URI url) {
      this.url = url;
      return this;
    }

    public Builder headers(Map<String, String> headers) {
      this.headers = headers;
      return this;
    }

    public Builder body(String body) {
      this.body = body;
      return this;
    }

    public WebhookHttpRequest build() {
      return new WebhookHttpRequest(this);
    }
  }
}
