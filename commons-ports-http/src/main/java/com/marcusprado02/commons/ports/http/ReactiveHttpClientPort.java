package com.marcusprado02.commons.ports.http;

import java.util.Arrays;
import java.util.Objects;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive (Project Reactor) HTTP client port for non-blocking HTTP communication. */
public interface ReactiveHttpClientPort {

  /**
   * Executes an HTTP request reactively and returns the raw byte-array response.
   *
   * @param request the HTTP request to send
   * @return a {@link Mono} of the HTTP response
   */
  Mono<HttpResponse<byte[]>> execute(HttpRequest request);

  /**
   * Executes an HTTP request and returns a streaming reactive response.
   *
   * @param request the HTTP request to send
   * @return a {@link Mono} of the reactive HTTP response
   */
  default Mono<ReactiveHttpResponse> exchange(HttpRequest request) {
    return execute(request)
        .map(
            response ->
                new ReactiveHttpResponse(
                    response.statusCode(),
                    response.headers(),
                    Flux.just(response.body().orElse(new byte[0]))));
  }

  /**
   * Executes an HTTP request and returns Server-Sent Events as a stream of raw event strings.
   *
   * @param request the HTTP request to send
   * @return a {@link Flux} of SSE event strings split on blank lines
   */
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
