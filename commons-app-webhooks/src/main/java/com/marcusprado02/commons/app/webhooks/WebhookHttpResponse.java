package com.marcusprado02.commons.app.webhooks;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * Represents an HTTP response from a webhook endpoint.
 *
 * <p>Contains the status code, headers, body, and response time.
 */
public final class WebhookHttpResponse {

  private final int statusCode;
  private final Map<String, String> headers;
  private final String body;
  private final Duration responseTime;

  private WebhookHttpResponse(Builder builder) {
    this.statusCode = builder.statusCode;
    this.headers = Map.copyOf(Objects.requireNonNull(builder.headers, "headers cannot be null"));
    this.body = builder.body;
    this.responseTime = Objects.requireNonNull(builder.responseTime, "responseTime cannot be null");
  }

  public static Builder builder() {
    return new Builder();
  }

  public int getStatusCode() {
    return statusCode;
  }

  public Map<String, String> getHeaders() {
    return headers;
  }

  public String getBody() {
    return body;
  }

  public Duration getResponseTime() {
    return responseTime;
  }

  /**
   * Checks if the response indicates success (2xx status code).
   *
   * @return true if successful
   */
  public boolean isSuccess() {
    return statusCode >= 200 && statusCode < 300;
  }

  public static final class Builder {
    private int statusCode;
    private Map<String, String> headers = Map.of();
    private String body;
    private Duration responseTime;

    private Builder() {}

    public Builder statusCode(int statusCode) {
      this.statusCode = statusCode;
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

    public Builder responseTime(Duration responseTime) {
      this.responseTime = responseTime;
      return this;
    }

    public WebhookHttpResponse build() {
      return new WebhookHttpResponse(this);
    }
  }
}
