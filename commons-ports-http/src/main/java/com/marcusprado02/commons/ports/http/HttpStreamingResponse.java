package com.marcusprado02.commons.ports.http;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** HTTP response with a streaming {@link java.io.InputStream} body; must be closed after use. */
public final class HttpStreamingResponse implements AutoCloseable {

  private final int statusCode;
  private final Map<String, List<String>> headers;
  private final InputStream body;

  /**
   * Creates a streaming HTTP response.
   *
   * @param statusCode HTTP status code
   * @param headers response headers
   * @param body response body stream
   */
  public HttpStreamingResponse(
      int statusCode, Map<String, List<String>> headers, InputStream body) {
    this.statusCode = statusCode;
    this.headers = Objects.requireNonNull(headers, "headers must not be null");
    this.body = Objects.requireNonNull(body, "body must not be null");
  }

  public int statusCode() {
    return statusCode;
  }

  public Map<String, List<String>> headers() {
    return headers;
  }

  public InputStream body() {
    return body;
  }

  @Override
  public void close() throws Exception {
    body.close();
  }
}
