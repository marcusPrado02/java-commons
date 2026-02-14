package com.marcusprado02.commons.testkit.contracts;

import static org.assertj.core.api.Assertions.assertThat;

import com.marcusprado02.commons.ports.http.*;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Base contract test for {@link HttpClientPort} implementations.
 *
 * <p>Extend this class to verify that your HTTP client implementation correctly follows the
 * HttpClientPort contract.
 *
 * <p>Example:
 *
 * <pre>{@code
 * class OkHttpClientContractTest extends HttpClientPortContract {
 *   @Override
 *   protected HttpClientPort createHttpClient() {
 *     return new OkHttpClientAdapter();
 *   }
 *
 *   @Override
 *   protected String getTestServerUrl() {
 *     return "https://httpbin.org";
 *   }
 * }
 * }</pre>
 */
public abstract class HttpClientPortContract {

  protected HttpClientPort httpClient;

  /**
   * Create the HTTP client instance to be tested.
   *
   * @return HTTP client implementation
   */
  protected abstract HttpClientPort createHttpClient();

  /**
   * Get the base URL of a test server (e.g., httpbin.org or WireMock server).
   *
   * @return test server base URL
   */
  protected abstract String getTestServerUrl();

  @BeforeEach
  void setUp() {
    httpClient = createHttpClient();
  }

  @Test
  @DisplayName("Should execute GET request successfully")
  void shouldExecuteGetRequest() {
    // Given
    HttpRequest request =
        HttpRequest.builder()
            .method(HttpMethod.GET)
            .uri(java.net.URI.create(getTestServerUrl() + "/get"))
            .build();

    // When
    HttpResponse<byte[]> response = httpClient.execute(request);

    // Then
    assertThat(response).isNotNull();
    assertThat(response.statusCode()).isBetween(200, 299);
    assertThat(response.body()).isPresent();
  }

  @Test
  @DisplayName("Should execute POST request with body")
  void shouldExecutePostRequest() {
    // Given
    String requestBody = "{\"test\":\"value\"}";
    HttpRequest request =
        HttpRequest.builder()
            .method(HttpMethod.POST)
            .uri(java.net.URI.create(getTestServerUrl() + "/post"))
            .body(new HttpBody.Bytes(requestBody.getBytes(), "application/json"))
            .build();

    // When
    HttpResponse<byte[]> response = httpClient.execute(request);

    // Then
    assertThat(response).isNotNull();
    assertThat(response.statusCode()).isBetween(200, 299);
  }

  @Test
  @DisplayName("Should include custom headers in request")
  void shouldIncludeCustomHeaders() {
    // Given
    HttpRequest request =
        HttpRequest.builder()
            .method(HttpMethod.GET)
            .uri(java.net.URI.create(getTestServerUrl() + "/headers"))
            .headers(Map.of("X-Custom-Header", List.of("test-value")))
            .build();

    // When
    HttpResponse<byte[]> response = httpClient.execute(request);

    // Then
    assertThat(response).isNotNull();
    assertThat(response.statusCode()).isBetween(200, 299);
  }

  @Test
  @DisplayName("Should handle 4xx error responses")
  void shouldHandle4xxErrors() {
    // Given
    HttpRequest request =
        HttpRequest.builder()
            .method(HttpMethod.GET)
            .uri(java.net.URI.create(getTestServerUrl() + "/status/404"))
            .build();

    // When
    HttpResponse<byte[]> response = httpClient.execute(request);

    // Then
    assertThat(response).isNotNull();
    assertThat(response.statusCode()).isEqualTo(404);
  }

  @Test
  @DisplayName("Should execute request with mapper")
  void shouldExecuteWithMapper() {
    // Given
    HttpRequest request =
        HttpRequest.builder()
            .method(HttpMethod.GET)
            .uri(java.net.URI.create(getTestServerUrl() + "/get"))
            .build();

    HttpResponseBodyMapper<String> mapper = (bytes, resp) -> new String(bytes);

    // When
    HttpResponse<String> response = httpClient.execute(request, mapper);

    // Then
    assertThat(response).isNotNull();
    assertThat(response.statusCode()).isBetween(200, 299);
    assertThat(response.body()).isPresent();
    assertThat(response.body().get()).isNotEmpty();
  }

  @Test
  @DisplayName("Should preserve response headers")
  void shouldPreserveResponseHeaders() {
    // Given
    HttpRequest request =
        HttpRequest.builder()
            .method(HttpMethod.GET)
            .uri(java.net.URI.create(getTestServerUrl() + "/response-headers?X-Test=value"))
            .build();

    // When
    HttpResponse<byte[]> response = httpClient.execute(request);

    // Then
    assertThat(response).isNotNull();
    assertThat(response.headers()).isNotNull();
  }
}
