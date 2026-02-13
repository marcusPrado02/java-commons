package com.marcusprado02.commons.ports.http;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public final class ReactiveHttpResponse {

  private final int statusCode;
  private final Map<String, List<String>> headers;
  private final Flux<byte[]> body;

  public ReactiveHttpResponse(int statusCode, Map<String, List<String>> headers, Flux<byte[]> body) {
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

  public Flux<byte[]> body() {
    return body;
  }

  public Mono<byte[]> aggregateBody() {
    return body.reduce(new byte[0], ReactiveHttpResponse::concat);
  }

  public Mono<String> aggregateBodyUtf8() {
    return aggregateBody().map(bytes -> new String(bytes, StandardCharsets.UTF_8));
  }

  private static byte[] concat(byte[] left, byte[] right) {
    byte[] out = new byte[left.length + right.length];
    System.arraycopy(left, 0, out, 0, left.length);
    System.arraycopy(right, 0, out, left.length, right.length);
    return out;
  }
}
