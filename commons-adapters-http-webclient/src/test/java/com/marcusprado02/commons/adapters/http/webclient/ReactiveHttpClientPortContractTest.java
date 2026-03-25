package com.marcusprado02.commons.adapters.http.webclient;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.marcusprado02.commons.ports.http.HttpAuth;
import com.marcusprado02.commons.ports.http.HttpBody;
import com.marcusprado02.commons.ports.http.HttpMethod;
import com.marcusprado02.commons.ports.http.HttpRequest;
import com.marcusprado02.commons.ports.http.HttpResponse;
import com.marcusprado02.commons.ports.http.ReactiveHttpClientPort;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Contract test for {@link ReactiveHttpClientPort}.
 *
 * <p>Verifies that {@link WebClientHttpClientAdapter} honours the reactive port contract:
 * every HTTP interaction must return the correct status, body, and headers as a {@code Mono} —
 * never throwing for 4xx/5xx responses, always completing with a populated {@link HttpResponse}.
 */
class ReactiveHttpClientPortContractTest {

  private WireMockServer server;
  private ReactiveHttpClientPort adapter;

  @BeforeEach
  void setUp() {
    server = new WireMockServer(WireMockConfiguration.options().dynamicPort());
    server.start();
    adapter = WebClientHttpClientAdapter.builder().build();
  }

  @AfterEach
  void tearDown() {
    server.stop();
  }

  private URI uri(String path) {
    return URI.create(server.baseUrl() + path);
  }

  private HttpResponse<byte[]> execute(HttpRequest request) {
    HttpResponse<byte[]> response = adapter.execute(request).block(Duration.ofSeconds(5));
    assertNotNull(response, "Mono must not complete empty");
    return response;
  }

  // -----------------------------------------------------------------------
  // GET
  // -----------------------------------------------------------------------

  @Test
  void get_returns_200_with_body() {
    server.stubFor(
        get(urlEqualTo("/hello"))
            .willReturn(aResponse().withStatus(200).withBody("hello")));

    HttpRequest request =
        HttpRequest.builder().method(HttpMethod.GET).uri(uri("/hello")).build();
    HttpResponse<byte[]> response = execute(request);

    assertEquals(200, response.statusCode());
    assertTrue(response.isSuccessful());
    assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8), response.body().orElseThrow());
  }

  @Test
  void get_passes_through_404_as_mono_value_not_error() {
    server.stubFor(
        get(urlEqualTo("/missing"))
            .willReturn(aResponse().withStatus(404).withBody("not found")));

    HttpRequest request =
        HttpRequest.builder().method(HttpMethod.GET).uri(uri("/missing")).build();
    HttpResponse<byte[]> response = execute(request);

    assertEquals(404, response.statusCode());
    assertFalse(response.isSuccessful());
  }

  @Test
  void get_passes_through_500_as_mono_value_not_error() {
    server.stubFor(
        get(urlEqualTo("/error"))
            .willReturn(aResponse().withStatus(500).withBody("server error")));

    HttpRequest request =
        HttpRequest.builder().method(HttpMethod.GET).uri(uri("/error")).build();
    HttpResponse<byte[]> response = execute(request);

    assertEquals(500, response.statusCode());
    assertFalse(response.isSuccessful());
  }

  @Test
  void get_returns_empty_body_on_204() {
    server.stubFor(
        get(urlEqualTo("/no-content")).willReturn(aResponse().withStatus(204)));

    HttpRequest request =
        HttpRequest.builder().method(HttpMethod.GET).uri(uri("/no-content")).build();
    HttpResponse<byte[]> response = execute(request);

    assertEquals(204, response.statusCode());
    assertTrue(response.body().isPresent());
    assertEquals(0, response.body().get().length);
  }

  // -----------------------------------------------------------------------
  // Headers
  // -----------------------------------------------------------------------

  @Test
  void sends_custom_request_headers() {
    server.stubFor(
        get(urlEqualTo("/headers"))
            .withHeader("X-Api-Key", equalTo("secret"))
            .willReturn(aResponse().withStatus(200).withBody("ok")));

    HttpRequest request =
        HttpRequest.builder()
            .method(HttpMethod.GET)
            .uri(uri("/headers"))
            .header("X-Api-Key", "secret")
            .build();

    assertEquals(200, execute(request).statusCode());
  }

  @Test
  void sends_bearer_auth_header() {
    server.stubFor(
        get(urlEqualTo("/secure"))
            .withHeader("Authorization", equalTo("Bearer tk"))
            .willReturn(aResponse().withStatus(200)));

    HttpRequest request =
        HttpRequest.builder()
            .method(HttpMethod.GET)
            .uri(uri("/secure"))
            .auth(new HttpAuth.Bearer("tk"))
            .build();

    assertEquals(200, execute(request).statusCode());
  }

  @Test
  void response_headers_are_accessible() {
    server.stubFor(
        get(urlEqualTo("/hdr"))
            .willReturn(
                aResponse().withStatus(200).withHeader("X-Trace", "abc").withBody("ok")));

    HttpRequest request = HttpRequest.builder().method(HttpMethod.GET).uri(uri("/hdr")).build();
    HttpResponse<byte[]> response = execute(request);

    assertTrue(response.headers().containsKey("X-Trace"));
    assertTrue(response.headers().get("X-Trace").contains("abc"));
  }

  // -----------------------------------------------------------------------
  // POST / PUT / DELETE
  // -----------------------------------------------------------------------

  @Test
  void post_sends_json_body_and_returns_201() {
    String json = "{\"name\":\"Alice\"}";
    server.stubFor(
        post(urlEqualTo("/users"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withRequestBody(equalTo(json))
            .willReturn(aResponse().withStatus(201).withBody("{\"id\":\"1\"}")));

    HttpRequest request =
        HttpRequest.builder()
            .method(HttpMethod.POST)
            .uri(uri("/users"))
            .header("Content-Type", "application/json")
            .body(new HttpBody.Bytes(json.getBytes(StandardCharsets.UTF_8), "application/json"))
            .build();

    assertEquals(201, execute(request).statusCode());
  }

  @Test
  void put_sends_body() {
    String json = "{\"name\":\"Bob\"}";
    server.stubFor(
        put(urlEqualTo("/users/1"))
            .withRequestBody(equalTo(json))
            .willReturn(aResponse().withStatus(200)));

    HttpRequest request =
        HttpRequest.builder()
            .method(HttpMethod.PUT)
            .uri(uri("/users/1"))
            .header("Content-Type", "application/json")
            .body(new HttpBody.Bytes(json.getBytes(StandardCharsets.UTF_8), "application/json"))
            .build();

    assertEquals(200, execute(request).statusCode());
  }

  @Test
  void delete_returns_204() {
    server.stubFor(
        delete(urlEqualTo("/users/1")).willReturn(aResponse().withStatus(204)));

    HttpRequest request =
        HttpRequest.builder().method(HttpMethod.DELETE).uri(uri("/users/1")).build();
    assertEquals(204, execute(request).statusCode());
  }

  // -----------------------------------------------------------------------
  // Reactive semantics
  // -----------------------------------------------------------------------

  @Test
  void mono_is_resubscribable() {
    server.stubFor(
        get(urlEqualTo("/ping"))
            .willReturn(aResponse().withStatus(200).withBody("pong")));

    HttpRequest request = HttpRequest.builder().method(HttpMethod.GET).uri(uri("/ping")).build();

    // Each subscription should trigger an independent HTTP call
    var mono = adapter.execute(request);
    HttpResponse<byte[]> first = mono.block(Duration.ofSeconds(5));
    HttpResponse<byte[]> second = mono.block(Duration.ofSeconds(5));

    assertNotNull(first);
    assertNotNull(second);
    assertEquals(200, first.statusCode());
    assertEquals(200, second.statusCode());
  }
}
