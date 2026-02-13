package com.marcusprado02.commons.ports.http;

import java.io.ByteArrayInputStream;
import java.util.Objects;

public interface HttpClientPort {

  HttpResponse<byte[]> execute(HttpRequest request);

  default <T> HttpResponse<T> execute(HttpRequest request, HttpResponseBodyMapper<T> mapper) {
    Objects.requireNonNull(mapper, "mapper must not be null");
    HttpResponse<byte[]> response = execute(request);
    byte[] body = response.body().orElse(new byte[0]);
    return new HttpResponse<>(
        response.statusCode(), response.headers(), mapper.map(body, response));
  }

  default HttpStreamingResponse exchange(HttpRequest request) {
    HttpResponse<byte[]> response = execute(request);
    byte[] body = response.body().orElse(new byte[0]);
    return new HttpStreamingResponse(
        response.statusCode(), response.headers(), new ByteArrayInputStream(body));
  }
}
