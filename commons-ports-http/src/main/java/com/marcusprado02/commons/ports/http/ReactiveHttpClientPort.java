package com.marcusprado02.commons.ports.http;

import reactor.core.publisher.Mono;

public interface ReactiveHttpClientPort {

  Mono<HttpResponse<byte[]>> execute(HttpRequest request);

  default <T> Mono<HttpResponse<T>> execute(HttpRequest request, HttpBodyMapper<T> mapper) {
    return execute(request).map(raw -> new HttpResponse<>(raw.statusCode(), raw.headers(), mapper.map(raw)));
  }
}
