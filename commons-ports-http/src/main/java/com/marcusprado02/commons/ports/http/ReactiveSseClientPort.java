package com.marcusprado02.commons.ports.http;

import reactor.core.publisher.Flux;

public interface ReactiveSseClientPort {

  Flux<SseEvent> subscribe(HttpRequest request);
}
