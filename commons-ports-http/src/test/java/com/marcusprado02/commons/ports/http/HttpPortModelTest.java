package com.marcusprado02.commons.ports.http;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Unit tests for HTTP port domain model value objects. */
class HttpPortModelTest {

  // -----------------------------------------------------------------------
  // HttpMethod
  // -----------------------------------------------------------------------

  @Test
  void httpMethod_allValuesPresent() {
    assertEquals(7, HttpMethod.values().length);
    assertNotNull(HttpMethod.GET);
    assertNotNull(HttpMethod.POST);
    assertNotNull(HttpMethod.PUT);
    assertNotNull(HttpMethod.PATCH);
    assertNotNull(HttpMethod.DELETE);
    assertNotNull(HttpMethod.HEAD);
    assertNotNull(HttpMethod.OPTIONS);
  }

  // -----------------------------------------------------------------------
  // HttpAuth
  // -----------------------------------------------------------------------

  @Test
  void httpAuth_bearer_returnsCorrectHeader() {
    HttpAuth.Bearer bearer = new HttpAuth.Bearer("my-token");
    assertEquals("Bearer my-token", bearer.asAuthorizationHeaderValue());
  }

  @Test
  void httpAuth_bearer_nullTokenThrows() {
    assertThrows(NullPointerException.class, () -> new HttpAuth.Bearer(null));
  }

  @Test
  void httpAuth_basic_encodesCorrectly() {
    HttpAuth.Basic basic = new HttpAuth.Basic("user", "pass");
    // Base64("user:pass") = dXNlcjpwYXNz
    assertEquals("Basic dXNlcjpwYXNz", basic.asAuthorizationHeaderValue());
  }

  @Test
  void httpAuth_basic_nullUsernameThrows() {
    assertThrows(NullPointerException.class, () -> new HttpAuth.Basic(null, "pass"));
  }

  @Test
  void httpAuth_basic_nullPasswordThrows() {
    assertThrows(NullPointerException.class, () -> new HttpAuth.Basic("user", null));
  }

  // -----------------------------------------------------------------------
  // HttpBody
  // -----------------------------------------------------------------------

  @Test
  void httpBody_bytes_defaultsToOctetStream() {
    HttpBody.Bytes body = new HttpBody.Bytes("hello".getBytes(StandardCharsets.UTF_8));
    assertEquals("application/octet-stream", body.contentType());
    assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8), body.value());
  }

  @Test
  void httpBody_bytes_customContentType() {
    HttpBody.Bytes body =
        new HttpBody.Bytes("{}".getBytes(StandardCharsets.UTF_8), "application/json");
    assertEquals("application/json", body.contentType());
  }

  @Test
  void httpBody_bytes_blankContentTypeFallsBackToOctetStream() {
    HttpBody.Bytes body = new HttpBody.Bytes("x".getBytes(StandardCharsets.UTF_8), "  ");
    assertEquals("application/octet-stream", body.contentType());
  }

  @Test
  void httpBody_formUrlEncoded_contentType() {
    HttpBody.FormUrlEncoded form = new HttpBody.FormUrlEncoded(Map.of("key", List.of("value")));
    assertEquals("application/x-www-form-urlencoded", form.contentType());
    assertNotNull(form.fields());
  }

  @Test
  void httpBody_multipart_contentType() {
    HttpBody.Multipart.Part part =
        new HttpBody.Multipart.Part(
            "file", "test.txt", "text/plain", "content".getBytes(StandardCharsets.UTF_8));
    HttpBody.Multipart multipart = new HttpBody.Multipart(List.of(part));
    assertEquals("multipart/form-data", multipart.contentType());
    assertEquals(1, multipart.parts().size());
  }

  // -----------------------------------------------------------------------
  // HttpRequest
  // -----------------------------------------------------------------------

  @Test
  void httpRequest_builder_minimalRequest() {
    HttpRequest request =
        HttpRequest.builder().method(HttpMethod.GET).uri(URI.create("http://example.com")).build();

    assertEquals(HttpMethod.GET, request.method());
    assertEquals(URI.create("http://example.com"), request.uri());
    assertFalse(request.name().isPresent());
    assertFalse(request.body().isPresent());
    assertFalse(request.timeout().isPresent());
    assertTrue(request.headers().isEmpty());
  }

  @Test
  void httpRequest_builder_withAllFields() {
    HttpRequest request =
        HttpRequest.builder()
            .name("test-request")
            .method(HttpMethod.POST)
            .uri(URI.create("http://example.com/api"))
            .header("Content-Type", "application/json")
            .body(
                new HttpBody.Bytes(
                    "{\"k\":\"v\"}".getBytes(StandardCharsets.UTF_8), "application/json"))
            .timeout(Duration.ofSeconds(30))
            .build();

    assertTrue(request.name().isPresent());
    assertEquals("test-request", request.name().get());
    assertEquals(HttpMethod.POST, request.method());
    assertTrue(request.body().isPresent());
    assertTrue(request.timeout().isPresent());
    assertEquals(Duration.ofSeconds(30), request.timeout().get());
    assertTrue(request.headers().containsKey("Content-Type"));
  }

  @Test
  void httpRequest_builder_authAddsHeader() {
    HttpRequest request =
        HttpRequest.builder()
            .method(HttpMethod.GET)
            .uri(URI.create("http://example.com"))
            .auth(new HttpAuth.Bearer("token"))
            .build();

    assertTrue(request.headers().containsKey("Authorization"));
    assertEquals("Bearer token", request.headers().get("Authorization").get(0));
  }

  @Test
  void httpRequest_builder_missingMethodThrows() {
    assertThrows(
        NullPointerException.class,
        () -> HttpRequest.builder().uri(URI.create("http://example.com")).build());
  }

  @Test
  void httpRequest_builder_missingUriThrows() {
    assertThrows(
        NullPointerException.class, () -> HttpRequest.builder().method(HttpMethod.GET).build());
  }

  // -----------------------------------------------------------------------
  // HttpResponse
  // -----------------------------------------------------------------------

  @Test
  void httpResponse_isSuccessful_for2xx() {
    HttpResponse<byte[]> r200 = new HttpResponse<>(200, Map.of(), new byte[0]);
    HttpResponse<byte[]> r299 = new HttpResponse<>(299, Map.of(), new byte[0]);
    assertTrue(r200.isSuccessful());
    assertTrue(r299.isSuccessful());
  }

  @Test
  void httpResponse_isNotSuccessful_for4xxAnd5xx() {
    HttpResponse<byte[]> r404 = new HttpResponse<>(404, Map.of(), null);
    HttpResponse<byte[]> r500 = new HttpResponse<>(500, Map.of(), null);
    assertFalse(r404.isSuccessful());
    assertFalse(r500.isSuccessful());
  }

  @Test
  void httpResponse_body_emptyWhenNull() {
    HttpResponse<byte[]> response = new HttpResponse<>(200, Map.of(), null);
    assertFalse(response.body().isPresent());
  }

  @Test
  void httpResponse_body_presentWhenNotNull() {
    HttpResponse<byte[]> response =
        new HttpResponse<>(200, Map.of(), "data".getBytes(StandardCharsets.UTF_8));
    assertTrue(response.body().isPresent());
  }

  @Test
  void httpResponse_headers_accessible() {
    HttpResponse<byte[]> response =
        new HttpResponse<>(200, Map.of("X-Custom", List.of("val")), null);
    assertTrue(response.headers().containsKey("X-Custom"));
  }

  // -----------------------------------------------------------------------
  // HttpStreamingResponse
  // -----------------------------------------------------------------------

  @Test
  void httpStreamingResponse_accessors() throws Exception {
    byte[] data = "stream data".getBytes(StandardCharsets.UTF_8);
    try (HttpStreamingResponse sr =
        new HttpStreamingResponse(200, Map.of(), new ByteArrayInputStream(data))) {
      assertEquals(200, sr.statusCode());
      assertNotNull(sr.body());
      assertNotNull(sr.headers());
    }
  }

  @Test
  void httpStreamingResponse_nullBodyThrows() {
    assertThrows(NullPointerException.class, () -> new HttpStreamingResponse(200, Map.of(), null));
  }

  // -----------------------------------------------------------------------
  // HttpResponseBodyMapper
  // -----------------------------------------------------------------------

  @Test
  void httpResponseBodyMapper_lambdaWorks() {
    HttpResponseBodyMapper<String> mapper =
        (bytes, resp) -> new String(bytes, StandardCharsets.UTF_8);
    HttpResponse<byte[]> resp =
        new HttpResponse<>(200, Map.of(), "hello".getBytes(StandardCharsets.UTF_8));
    assertEquals("hello", mapper.map(resp.body().orElse(new byte[0]), resp));
  }

  // -----------------------------------------------------------------------
  // HttpRequest builder edge cases
  // -----------------------------------------------------------------------

  @Test
  void httpRequest_builder_bodyByteArray() {
    HttpRequest request =
        HttpRequest.builder()
            .method(HttpMethod.PUT)
            .uri(URI.create("http://example.com"))
            .body("data".getBytes(StandardCharsets.UTF_8))
            .build();
    assertTrue(request.body().isPresent());
  }

  @Test
  void httpRequest_builder_bodyByteArrayNull() {
    HttpRequest request =
        HttpRequest.builder()
            .method(HttpMethod.DELETE)
            .uri(URI.create("http://example.com"))
            .body((byte[]) null)
            .build();
    assertFalse(request.body().isPresent());
  }

  @Test
  void httpRequest_builder_headersMapOverwrite() {
    HttpRequest request =
        HttpRequest.builder()
            .method(HttpMethod.GET)
            .uri(URI.create("http://example.com"))
            .header("X-Old", "old")
            .headers(Map.of("X-New", List.of("new")))
            .build();
    assertFalse(request.headers().containsKey("X-Old"));
    assertTrue(request.headers().containsKey("X-New"));
  }

  @Test
  void httpRequest_builder_headersMapWithNullValues() {
    Map<String, List<String>> headersWithNull = new java.util.HashMap<>();
    headersWithNull.put("X-Key", null);
    HttpRequest request =
        HttpRequest.builder()
            .method(HttpMethod.GET)
            .uri(URI.create("http://example.com"))
            .headers(headersWithNull)
            .build();
    assertTrue(request.headers().containsKey("X-Key"));
    assertTrue(request.headers().get("X-Key").isEmpty());
  }

  @Test
  void httpRequest_builder_formUrlEncoded() {
    HttpRequest request =
        HttpRequest.builder()
            .method(HttpMethod.POST)
            .uri(URI.create("http://example.com"))
            .formUrlEncoded(Map.of("field", List.of("value")))
            .build();
    assertTrue(request.body().isPresent());
    assertEquals("application/x-www-form-urlencoded", request.body().get().contentType());
  }

  @Test
  void httpRequest_builder_multipart() {
    HttpBody.Multipart.Part part =
        new HttpBody.Multipart.Part(
            "file", "f.txt", "text/plain", "content".getBytes(StandardCharsets.UTF_8));
    HttpRequest request =
        HttpRequest.builder()
            .method(HttpMethod.POST)
            .uri(URI.create("http://example.com"))
            .multipart(List.of(part))
            .build();
    assertTrue(request.body().isPresent());
    assertEquals("multipart/form-data", request.body().get().contentType());
  }

  // -----------------------------------------------------------------------
  // HttpClientPort default methods
  // -----------------------------------------------------------------------

  @Test
  void httpClientPort_executeWithMapper_convertsBody() {
    HttpClientPort adapter =
        request -> new HttpResponse<>(200, Map.of(), "mapped".getBytes(StandardCharsets.UTF_8));
    HttpRequest req =
        HttpRequest.builder().method(HttpMethod.GET).uri(URI.create("http://x.com")).build();
    HttpResponse<String> result =
        adapter.execute(req, (bytes, r) -> new String(bytes, StandardCharsets.UTF_8));
    assertEquals("mapped", result.body().orElseThrow());
  }

  @Test
  void httpClientPort_exchange_returnsStreamingResponse() throws Exception {
    HttpClientPort adapter =
        request -> new HttpResponse<>(200, Map.of(), "streamed".getBytes(StandardCharsets.UTF_8));
    HttpRequest req =
        HttpRequest.builder().method(HttpMethod.GET).uri(URI.create("http://x.com")).build();
    try (HttpStreamingResponse sr = adapter.exchange(req)) {
      assertEquals(200, sr.statusCode());
      assertNotNull(sr.body());
    }
  }

  @Test
  void httpClientPort_exchange_emptyBodyWhenNull() throws Exception {
    HttpClientPort adapter = request -> new HttpResponse<>(204, Map.of(), null);
    HttpRequest req =
        HttpRequest.builder().method(HttpMethod.GET).uri(URI.create("http://x.com")).build();
    try (HttpStreamingResponse sr = adapter.exchange(req)) {
      assertEquals(204, sr.statusCode());
    }
  }

  @Test
  void httpClientPort_executeWithMapper_nullMapperThrows() {
    HttpClientPort adapter = request -> new HttpResponse<>(200, Map.of(), new byte[0]);
    HttpRequest req =
        HttpRequest.builder().method(HttpMethod.GET).uri(URI.create("http://x.com")).build();
    assertThrows(NullPointerException.class, () -> adapter.execute(req, null));
  }
}
