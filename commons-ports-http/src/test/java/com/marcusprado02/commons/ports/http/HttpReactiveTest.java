package com.marcusprado02.commons.ports.http;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class HttpReactiveTest {

  // --- HttpInterceptor default methods ---

  @Test
  void httpInterceptor_onRequest_returns_same_request() {
    HttpRequest request =
        HttpRequest.builder().method(HttpMethod.GET).url("http://example.com").build();
    HttpInterceptor interceptor = new HttpInterceptor() {};
    HttpRequest result = interceptor.onRequest(request);
    assertSame(request, result);
  }

  @Test
  void httpInterceptor_onRequest_null_throws() {
    HttpInterceptor interceptor = new HttpInterceptor() {};
    assertThrows(NullPointerException.class, () -> interceptor.onRequest(null));
  }

  @Test
  void httpInterceptor_onResponse_returns_same_response() {
    HttpRequest request =
        HttpRequest.builder().method(HttpMethod.GET).url("http://example.com").build();
    HttpResponse<byte[]> response = new HttpResponse<>(200, Map.of(), new byte[0]);
    HttpInterceptor interceptor = new HttpInterceptor() {};
    HttpResponse<byte[]> result = interceptor.onResponse(request, response);
    assertSame(response, result);
  }

  @Test
  void httpInterceptor_onResponse_null_request_throws() {
    HttpInterceptor interceptor = new HttpInterceptor() {};
    HttpResponse<byte[]> response = new HttpResponse<>(200, Map.of(), new byte[0]);
    assertThrows(NullPointerException.class, () -> interceptor.onResponse(null, response));
  }

  @Test
  void httpInterceptor_onResponse_null_response_throws() {
    HttpInterceptor interceptor = new HttpInterceptor() {};
    HttpRequest request =
        HttpRequest.builder().method(HttpMethod.GET).url("http://example.com").build();
    assertThrows(NullPointerException.class, () -> interceptor.onResponse(request, null));
  }

  // --- ReactiveHttpResponse ---

  @Test
  void reactiveResponse_stores_all_fields() {
    Flux<byte[]> body = Flux.just("hello".getBytes());
    Map<String, List<String>> headers = Map.of("Content-Type", List.of("text/plain"));
    ReactiveHttpResponse resp = new ReactiveHttpResponse(200, headers, body);
    assertEquals(200, resp.statusCode());
    assertEquals(headers, resp.headers());
    assertNotNull(resp.body());
  }

  @Test
  void reactiveResponse_null_headers_throws() {
    assertThrows(
        NullPointerException.class, () -> new ReactiveHttpResponse(200, null, Flux.empty()));
  }

  @Test
  void reactiveResponse_null_body_throws() {
    assertThrows(NullPointerException.class, () -> new ReactiveHttpResponse(200, Map.of(), null));
  }

  @Test
  void reactiveResponse_aggregateBody_concatenates_chunks() {
    Flux<byte[]> body = Flux.just("hel".getBytes(), "lo".getBytes());
    ReactiveHttpResponse resp = new ReactiveHttpResponse(200, Map.of(), body);
    byte[] result = resp.aggregateBody().block();
    assertNotNull(result);
    assertEquals("hello", new String(result));
  }

  @Test
  void reactiveResponse_aggregateBodyUtf8() {
    ReactiveHttpResponse resp =
        new ReactiveHttpResponse(200, Map.of(), Flux.just("world".getBytes()));
    assertEquals("world", resp.aggregateBodyUtf8().block());
  }

  // --- ReactiveHttpClientPort default methods ---

  @Test
  void reactiveClientPort_exchange_wraps_response() {
    HttpResponse<byte[]> rawResp = new HttpResponse<>(200, Map.of(), "data".getBytes());
    ReactiveHttpClientPort client = request -> Mono.just(rawResp);

    HttpRequest request =
        HttpRequest.builder().method(HttpMethod.GET).url("http://example.com").build();
    ReactiveHttpResponse result = client.exchange(request).block();

    assertNotNull(result);
    assertEquals(200, result.statusCode());
  }

  @Test
  void reactiveClientPort_exchange_empty_body() {
    HttpResponse<byte[]> rawResp = new HttpResponse<>(204, Map.of(), null);
    ReactiveHttpClientPort client = request -> Mono.just(rawResp);

    HttpRequest request =
        HttpRequest.builder().method(HttpMethod.GET).url("http://example.com").build();
    ReactiveHttpResponse result = client.exchange(request).block();

    assertNotNull(result);
    assertEquals(204, result.statusCode());
  }

  @Test
  void reactiveClientPort_executeSSE_null_request_throws() {
    ReactiveHttpClientPort client = request -> Mono.just(new HttpResponse<>(200, Map.of(), null));
    assertThrows(NullPointerException.class, () -> client.executeServerSentEvents(null));
  }

  @Test
  void reactiveClientPort_executeSSE_returns_events() {
    byte[] body = "event: msg\ndata: hello\n\nevent: msg\ndata: world\n\n".getBytes();
    HttpResponse<byte[]> rawResp = new HttpResponse<>(200, Map.of(), body);
    ReactiveHttpClientPort client = request -> Mono.just(rawResp);

    HttpRequest request =
        HttpRequest.builder().method(HttpMethod.GET).url("http://example.com").build();
    List<String> events = client.executeServerSentEvents(request).collectList().block();
    assertNotNull(events);
    assertFalse(events.isEmpty());
  }
}
