package com.marcusprado02.commons.app.apigateway;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Represents an HTTP response from the API Gateway.
 *
 * <p>This immutable record encapsulates all information about an HTTP response, including status
 * code, headers, and body.
 *
 * @param statusCode the HTTP status code (200, 404, 500, etc.)
 * @param headers the response headers
 * @param body the response body (optional)
 */
public record GatewayResponse(int statusCode, Map<String, String> headers, Optional<String> body) {

  /**
   * Creates a new GatewayResponse with defensive copies of mutable collections.
   *
   * @param statusCode the HTTP status code
   * @param headers the response headers
   * @param body the response body
   */
  public GatewayResponse {
    headers = Map.copyOf(headers);
  }

  /**
   * Creates a builder for GatewayResponse.
   *
   * @return a new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates a 200 OK response.
   *
   * @param body the response body
   * @return a new GatewayResponse
   */
  public static GatewayResponse ok(String body) {
    return builder().statusCode(200).body(body).build();
  }

  /**
   * Creates a 200 OK response without body.
   *
   * @return a new GatewayResponse
   */
  public static GatewayResponse ok() {
    return builder().statusCode(200).build();
  }

  /**
   * Creates a 201 Created response.
   *
   * @param body the response body
   * @return a new GatewayResponse
   */
  public static GatewayResponse created(String body) {
    return builder().statusCode(201).body(body).build();
  }

  /**
   * Creates a 204 No Content response.
   *
   * @return a new GatewayResponse
   */
  public static GatewayResponse noContent() {
    return builder().statusCode(204).build();
  }

  /**
   * Creates a 400 Bad Request response.
   *
   * @param message the error message
   * @return a new GatewayResponse
   */
  public static GatewayResponse badRequest(String message) {
    return builder().statusCode(400).body(message).build();
  }

  /**
   * Creates a 401 Unauthorized response.
   *
   * @param message the error message
   * @return a new GatewayResponse
   */
  public static GatewayResponse unauthorized(String message) {
    return builder().statusCode(401).body(message).build();
  }

  /**
   * Creates a 403 Forbidden response.
   *
   * @param message the error message
   * @return a new GatewayResponse
   */
  public static GatewayResponse forbidden(String message) {
    return builder().statusCode(403).body(message).build();
  }

  /**
   * Creates a 404 Not Found response.
   *
   * @param message the error message
   * @return a new GatewayResponse
   */
  public static GatewayResponse notFound(String message) {
    return builder().statusCode(404).body(message).build();
  }

  /**
   * Creates a 429 Too Many Requests response.
   *
   * @param message the error message
   * @return a new GatewayResponse
   */
  public static GatewayResponse tooManyRequests(String message) {
    return builder().statusCode(429).body(message).build();
  }

  /**
   * Creates a 500 Internal Server Error response.
   *
   * @param message the error message
   * @return a new GatewayResponse
   */
  public static GatewayResponse serverError(String message) {
    return builder().statusCode(500).body(message).build();
  }

  /**
   * Creates a 502 Bad Gateway response.
   *
   * @param message the error message
   * @return a new GatewayResponse
   */
  public static GatewayResponse badGateway(String message) {
    return builder().statusCode(502).body(message).build();
  }

  /**
   * Creates a 503 Service Unavailable response.
   *
   * @param message the error message
   * @return a new GatewayResponse
   */
  public static GatewayResponse serviceUnavailable(String message) {
    return builder().statusCode(503).body(message).build();
  }

  /**
   * Creates a 504 Gateway Timeout response.
   *
   * @param message the error message
   * @return a new GatewayResponse
   */
  public static GatewayResponse gatewayTimeout(String message) {
    return builder().statusCode(504).body(message).build();
  }

  /**
   * Gets a header value by name (case-insensitive).
   *
   * @param name the header name
   * @return the header value, or empty if not found
   */
  public Optional<String> getHeader(String name) {
    return headers.entrySet().stream()
        .filter(e -> e.getKey().equalsIgnoreCase(name))
        .map(Map.Entry::getValue)
        .findFirst();
  }

  /**
   * Creates a new response with an additional header.
   *
   * @param name the header name
   * @param value the header value
   * @return a new GatewayResponse with the added header
   */
  public GatewayResponse withHeader(String name, String value) {
    Map<String, String> newHeaders = new HashMap<>(headers);
    newHeaders.put(name, value);
    return new GatewayResponse(statusCode, newHeaders, body);
  }

  /** Builder for GatewayResponse. */
  public static class Builder {
    private int statusCode = 200;
    private Map<String, String> headers = new HashMap<>();
    private Optional<String> body = Optional.empty();

    public Builder statusCode(int statusCode) {
      this.statusCode = statusCode;
      return this;
    }

    public Builder header(String name, String value) {
      this.headers.put(name, value);
      return this;
    }

    public Builder headers(Map<String, String> headers) {
      this.headers.putAll(headers);
      return this;
    }

    public Builder body(String body) {
      this.body = Optional.ofNullable(body);
      return this;
    }

    public Builder contentType(String contentType) {
      this.headers.put("Content-Type", contentType);
      return this;
    }

    public GatewayResponse build() {
      return new GatewayResponse(statusCode, headers, body);
    }
  }
}
