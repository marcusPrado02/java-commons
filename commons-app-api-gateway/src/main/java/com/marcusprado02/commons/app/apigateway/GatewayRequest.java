package com.marcusprado02.commons.app.apigateway;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Represents an HTTP request in the API Gateway.
 *
 * <p>This immutable record encapsulates all information about an incoming HTTP request, including
 * method, path, headers, query parameters, and body.
 *
 * @param method the HTTP method (GET, POST, PUT, DELETE, etc.)
 * @param path the request path (e.g., "/api/users/123")
 * @param headers the request headers
 * @param queryParams the query parameters
 * @param body the request body (optional)
 * @param attributes contextual attributes for the request
 */
public record GatewayRequest(
    String method,
    String path,
    Map<String, String> headers,
    Map<String, String> queryParams,
    Optional<String> body,
    Map<String, Object> attributes) {

  /**
   * Creates a new GatewayRequest with defensive copies of mutable collections.
   *
   * @param method the HTTP method
   * @param path the request path
   * @param headers the request headers
   * @param queryParams the query parameters
   * @param body the request body
   * @param attributes contextual attributes
   */
  public GatewayRequest {
    headers = Map.copyOf(headers);
    queryParams = Map.copyOf(queryParams);
    attributes = new HashMap<>(attributes);
  }

  /**
   * Creates a builder for GatewayRequest.
   *
   * @return a new builder instance
   */
  public static Builder builder() {
    return new Builder();
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
   * Gets a query parameter value by name.
   *
   * @param name the parameter name
   * @return the parameter value, or empty if not found
   */
  public Optional<String> getQueryParam(String name) {
    return Optional.ofNullable(queryParams.get(name));
  }

  /**
   * Gets an attribute by key.
   *
   * @param key the attribute key
   * @param <T> the attribute type
   * @return the attribute value, or empty if not found
   */
  @SuppressWarnings("unchecked")
  public <T> Optional<T> getAttribute(String key) {
    return Optional.ofNullable((T) attributes.get(key));
  }

  /**
   * Creates a new request with an additional attribute.
   *
   * @param key the attribute key
   * @param value the attribute value
   * @return a new GatewayRequest with the added attribute
   */
  public GatewayRequest withAttribute(String key, Object value) {
    Map<String, Object> newAttributes = new HashMap<>(attributes);
    newAttributes.put(key, value);
    return new GatewayRequest(method, path, headers, queryParams, body, newAttributes);
  }

  /** Builder for GatewayRequest. */
  public static class Builder {
    private String method = "GET";
    private String path = "/";
    private Map<String, String> headers = new HashMap<>();
    private Map<String, String> queryParams = new HashMap<>();
    private Optional<String> body = Optional.empty();
    private Map<String, Object> attributes = new HashMap<>();

    public Builder method(String method) {
      this.method = method;
      return this;
    }

    public Builder path(String path) {
      this.path = path;
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

    public Builder queryParam(String name, String value) {
      this.queryParams.put(name, value);
      return this;
    }

    public Builder queryParams(Map<String, String> queryParams) {
      this.queryParams.putAll(queryParams);
      return this;
    }

    public Builder body(String body) {
      this.body = Optional.ofNullable(body);
      return this;
    }

    public Builder attribute(String key, Object value) {
      this.attributes.put(key, value);
      return this;
    }

    public Builder attributes(Map<String, Object> attributes) {
      this.attributes.putAll(attributes);
      return this;
    }

    public GatewayRequest build() {
      return new GatewayRequest(method, path, headers, queryParams, body, attributes);
    }
  }
}
