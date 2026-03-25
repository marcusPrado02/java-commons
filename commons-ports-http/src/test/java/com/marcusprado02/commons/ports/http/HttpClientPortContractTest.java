package com.marcusprado02.commons.ports.http;

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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Abstract contract test for {@link HttpClientPort}.
 *
 * <p>Every concrete {@code HttpClientPort} adapter must extend this class and implement
 * {@link #createAdapter()} to obtain a fresh adapter instance. All tests defined here assert the
 * behavioural contract that <em>all</em> adapters must honour, regardless of the underlying HTTP
 * library.
 *
 * <pre>
 * class OkHttpClientAdapterContractTest extends HttpClientPortContractTest {
 *   {@literal @}Override
 *   protected HttpClientPort createAdapter() {
 *     return OkHttpClientAdapter.builder().build();
 *   }
 * }
 * </pre>
 */
public abstract class HttpClientPortContractTest {

  protected WireMockServer server;

  /**
   * Factory method: subclasses return a fully configured adapter under test.
   * The adapter must not apply any interceptors or special configuration unless
   * the individual contract scenario requires it.
   */
  protected abstract HttpClientPort createAdapter();

  @BeforeEach
  void startServer() {
    server = new WireMockServer(WireMockConfiguration.options().dynamicPort());
    server.start();
  }

  @AfterEach
  void stopServer() {
    server.stop();
  }

  protected URI uri(String path) {
    return URI.create(server.baseUrl() + path);
  }

  // -----------------------------------------------------------------------
  // GET
  // -----------------------------------------------------------------------

  @Test
  void get_returns_200_with_body() {
    server.stubFor(
        get(urlEqualTo("/hello"))
            .willReturn(
                aResponse().withStatus(200).withBody("hello")));

    HttpRequest request = HttpRequest.builder().method(HttpMethod.GET).uri(uri("/hello")).build();
    HttpResponse<byte[]> response = createAdapter().execute(request);

    assertEquals(200, response.statusCode());
    assertTrue(response.isSuccessful());
    assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8), response.body().orElseThrow());
  }

  @Test
  void get_passes_through_404_without_throwing() {
    server.stubFor(
        get(urlEqualTo("/missing"))
            .willReturn(aResponse().withStatus(404).withBody("not found")));

    HttpRequest request =
        HttpRequest.builder().method(HttpMethod.GET).uri(uri("/missing")).build();
    HttpResponse<byte[]> response = createAdapter().execute(request);

    assertEquals(404, response.statusCode());
    assertFalse(response.isSuccessful());
  }

  @Test
  void get_passes_through_500_without_throwing() {
    server.stubFor(
        get(urlEqualTo("/error"))
            .willReturn(aResponse().withStatus(500).withBody("server error")));

    HttpRequest request = HttpRequest.builder().method(HttpMethod.GET).uri(uri("/error")).build();
    HttpResponse<byte[]> response = createAdapter().execute(request);

    assertEquals(500, response.statusCode());
    assertFalse(response.isSuccessful());
  }

  @Test
  void get_returns_empty_body_on_204() {
    server.stubFor(
        get(urlEqualTo("/no-content")).willReturn(aResponse().withStatus(204)));

    HttpRequest request =
        HttpRequest.builder().method(HttpMethod.GET).uri(uri("/no-content")).build();
    HttpResponse<byte[]> response = createAdapter().execute(request);

    assertEquals(204, response.statusCode());
    // body must be present but empty (never null)
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

    HttpResponse<byte[]> response = createAdapter().execute(request);
    assertEquals(200, response.statusCode());
  }

  @Test
  void sends_bearer_auth_header() {
    server.stubFor(
        get(urlEqualTo("/secure"))
            .withHeader("Authorization", equalTo("Bearer my-token"))
            .willReturn(aResponse().withStatus(200).withBody("ok")));

    HttpRequest request =
        HttpRequest.builder()
            .method(HttpMethod.GET)
            .uri(uri("/secure"))
            .auth(new HttpAuth.Bearer("my-token"))
            .build();

    HttpResponse<byte[]> response = createAdapter().execute(request);
    assertEquals(200, response.statusCode());
  }

  @Test
  void sends_basic_auth_header() {
    // Basic base64("user:pass") = dXNlcjpwYXNz
    server.stubFor(
        get(urlEqualTo("/basic"))
            .withHeader("Authorization", equalTo("Basic dXNlcjpwYXNz"))
            .willReturn(aResponse().withStatus(200)));

    HttpRequest request =
        HttpRequest.builder()
            .method(HttpMethod.GET)
            .uri(uri("/basic"))
            .auth(new HttpAuth.Basic("user", "pass"))
            .build();

    HttpResponse<byte[]> response = createAdapter().execute(request);
    assertEquals(200, response.statusCode());
  }

  @Test
  void response_headers_are_accessible() {
    server.stubFor(
        get(urlEqualTo("/with-headers"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("X-Custom", "value1")
                    .withBody("ok")));

    HttpRequest request =
        HttpRequest.builder().method(HttpMethod.GET).uri(uri("/with-headers")).build();
    HttpResponse<byte[]> response = createAdapter().execute(request);

    assertEquals(200, response.statusCode());
    assertTrue(response.headers().containsKey("X-Custom"));
    assertTrue(response.headers().get("X-Custom").contains("value1"));
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

    HttpResponse<byte[]> response = createAdapter().execute(request);
    assertEquals(201, response.statusCode());
  }

  @Test
  void put_sends_body() {
    String json = "{\"name\":\"Bob\"}";
    server.stubFor(
        put(urlEqualTo("/users/1"))
            .withRequestBody(equalTo(json))
            .willReturn(aResponse().withStatus(200).withBody("{\"id\":\"1\"}")));

    HttpRequest request =
        HttpRequest.builder()
            .method(HttpMethod.PUT)
            .uri(uri("/users/1"))
            .header("Content-Type", "application/json")
            .body(new HttpBody.Bytes(json.getBytes(StandardCharsets.UTF_8), "application/json"))
            .build();

    HttpResponse<byte[]> response = createAdapter().execute(request);
    assertEquals(200, response.statusCode());
  }

  @Test
  void delete_returns_204() {
    server.stubFor(
        delete(urlEqualTo("/users/1")).willReturn(aResponse().withStatus(204)));

    HttpRequest request =
        HttpRequest.builder().method(HttpMethod.DELETE).uri(uri("/users/1")).build();
    HttpResponse<byte[]> response = createAdapter().execute(request);

    assertEquals(204, response.statusCode());
  }

  // -----------------------------------------------------------------------
  // Body mapper
  // -----------------------------------------------------------------------

  @Test
  void body_mapper_converts_bytes_to_string() {
    server.stubFor(
        get(urlEqualTo("/text"))
            .willReturn(aResponse().withStatus(200).withBody("hello mapper")));

    HttpRequest request = HttpRequest.builder().method(HttpMethod.GET).uri(uri("/text")).build();

    HttpResponseBodyMapper<String> mapper =
        (bytes, resp) -> new String(bytes, StandardCharsets.UTF_8);

    HttpResponse<String> response = createAdapter().execute(request, mapper);

    assertEquals(200, response.statusCode());
    assertEquals("hello mapper", response.body().orElseThrow());
  }
}
