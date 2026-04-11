package com.marcusprado02.commons.ports.http;

import java.io.ByteArrayInputStream;
import java.util.Objects;

/** Port for sending HTTP requests and receiving responses. */
public interface HttpClientPort {

  /**
   * Executes an HTTP request and returns the raw byte-array response.
   *
   * @param request the HTTP request to send
   * @return the HTTP response with a raw byte-array body
   */
  HttpResponse<byte[]> execute(HttpRequest request);

  /**
   * Executes an HTTP request and maps the body to type {@code T}.
   *
   * @param <T> the target body type
   * @param request the HTTP request to send
   * @param mapper the body mapper
   * @return the HTTP response with a mapped body
   */
  default <T> HttpResponse<T> execute(HttpRequest request, HttpResponseBodyMapper<T> mapper) {
    Objects.requireNonNull(mapper, "mapper must not be null");
    HttpResponse<byte[]> response = execute(request);
    byte[] body = response.body().orElse(new byte[0]);
    return new HttpResponse<>(
        response.statusCode(), response.headers(), mapper.map(body, response));
  }

  /**
   * Executes an HTTP request and returns a streaming response.
   *
   * @param request the HTTP request to send
   * @return a streaming HTTP response
   */
  default HttpStreamingResponse exchange(HttpRequest request) {
    HttpResponse<byte[]> response = execute(request);
    byte[] body = response.body().orElse(new byte[0]);
    return new HttpStreamingResponse(
        response.statusCode(), response.headers(), new ByteArrayInputStream(body));
  }
}
