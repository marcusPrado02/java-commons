package com.marcusprado02.commons.ports.http;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

public interface ReactiveHttpClientPort {

  Mono<HttpResponse<byte[]>> execute(HttpRequest request);

  default Mono<ReactiveHttpResponse> exchange(HttpRequest request) {
    return execute(request)
        .map(
            response ->
                new ReactiveHttpResponse(
                    response.statusCode(), response.headers(), Flux.just(response.body().orElse(new byte[0]))));
  }

  default Flux<String> executeServerSentEvents(HttpRequest request) {
    Objects.requireNonNull(request, "request must not be null");
    // Minimal support: aggregate and split by blank line; consumers can parse SSE fields if needed.
    return exchange(request)
        .flatMapMany(
            response ->
                response
                    .aggregateBodyUtf8()
                    .flatMapMany(
                        body ->
                            Flux.fromIterable(
                                Arrays.stream(body.split("\\n\\n"))
                                    .map(String::trim)
                                    .filter(s -> !s.isBlank())
                                    .toList())));
  }
}
