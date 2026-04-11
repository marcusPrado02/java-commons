package com.marcusprado02.commons.ports.http;

import java.util.Objects;

/** Intercepts outgoing HTTP requests and incoming responses for cross-cutting concerns. */
public interface HttpInterceptor {

  /**
   * Intercepts and optionally modifies an outgoing HTTP request.
   *
   * @param request the original request
   * @return the (possibly modified) request to send
   */
  default HttpRequest onRequest(HttpRequest request) {
    Objects.requireNonNull(request, "request must not be null");
    return request;
  }

  /**
   * Intercepts and optionally modifies an incoming HTTP response.
   *
   * @param request the original request
   * @param response the received response
   * @return the (possibly modified) response
   */
  default HttpResponse<byte[]> onResponse(HttpRequest request, HttpResponse<byte[]> response) {
    Objects.requireNonNull(request, "request must not be null");
    Objects.requireNonNull(response, "response must not be null");
    return response;
  }
}
