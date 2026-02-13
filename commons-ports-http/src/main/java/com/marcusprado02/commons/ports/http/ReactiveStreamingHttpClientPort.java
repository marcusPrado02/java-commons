package com.marcusprado02.commons.ports.http;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ReactiveStreamingHttpClientPort {

  Mono<HttpResponse<Flux<byte[]>>> executeStream(HttpRequest request);
}
