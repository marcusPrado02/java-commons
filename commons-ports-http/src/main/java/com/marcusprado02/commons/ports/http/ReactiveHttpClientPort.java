package com.marcusprado02.commons.ports.http;

import reactor.core.publisher.Mono;

public interface ReactiveHttpClientPort {

  Mono<HttpResponse<byte[]>> execute(HttpRequest request);
}
