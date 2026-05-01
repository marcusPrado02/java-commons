package com.marcusprado02.commons.adapters.http.webclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.marcusprado02.commons.ports.http.HttpBody;
import com.marcusprado02.commons.ports.http.HttpMethod;
import com.marcusprado02.commons.ports.http.HttpRequest;
import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

class WebClientBranchTest {

  private DisposableServer server;

  @AfterEach
  void tearDown() {
    if (server != null) {
      server.disposeNow();
    }
  }

  private void startServer(RouterFunction<ServerResponse> routes) {
    HttpHandler handler = RouterFunctions.toHttpHandler(routes);
    server = HttpServer.create().port(0).handle(new ReactorHttpHandlerAdapter(handler)).bindNow();
  }

  private String baseUrl() {
    return "http://localhost:" + server.port();
  }

  // --- Builder.interceptor(null): false branch of "if (interceptor != null)" ---

  @Test
  void builder_nullInterceptor_silentlyIgnored() {
    WebClientHttpClientAdapter adapter =
        WebClientHttpClientAdapter.builder()
            .webClient(WebClient.builder().build())
            .interceptor(null)
            .build();
    assertNotNull(adapter);
  }

  // --- execute(): Duration.ZERO timeout → !isZero() false → timeout not applied ---

  @Test
  void execute_zeroTimeout_timeoutSkipped() {
    startServer(
        RouterFunctions.route(
            org.springframework.web.reactive.function.server.RequestPredicates.GET("/ok"),
            req -> ServerResponse.ok().bodyValue("ok")));

    WebClientHttpClientAdapter adapter = WebClientHttpClientAdapter.builder().build();
    HttpRequest request =
        HttpRequest.builder()
            .method(HttpMethod.GET)
            .uri(URI.create(baseUrl() + "/ok"))
            .timeout(Duration.ZERO)
            .build();

    var response = adapter.execute(request).block(Duration.ofSeconds(2));
    assertNotNull(response);
    assertEquals(200, response.statusCode());
  }

  // --- exchange(): positive timeout → timeout.isPresent() true, !isNeg && !isZero true ---

  @Test
  void exchange_withTimeout_appliesTimeout() {
    startServer(
        RouterFunctions.route(
            org.springframework.web.reactive.function.server.RequestPredicates.GET("/ex"),
            req -> ServerResponse.ok().bodyValue("ok")));

    WebClientHttpClientAdapter adapter = WebClientHttpClientAdapter.builder().build();
    HttpRequest request =
        HttpRequest.builder()
            .method(HttpMethod.GET)
            .uri(URI.create(baseUrl() + "/ex"))
            .timeout(Duration.ofSeconds(5))
            .build();

    var response = adapter.exchange(request).block(Duration.ofSeconds(2));
    assertNotNull(response);
    assertEquals(200, response.statusCode());
  }

  // --- applyInferredContentType: Content-Type already set → early return ---

  @Test
  void execute_withExplicitContentType_doesNotOverride() {
    startServer(
        RouterFunctions.route(
            org.springframework.web.reactive.function.server.RequestPredicates.POST("/ct"),
            req -> ServerResponse.ok().bodyValue("ok")));

    WebClientHttpClientAdapter adapter = WebClientHttpClientAdapter.builder().build();
    HttpRequest request =
        HttpRequest.builder()
            .method(HttpMethod.POST)
            .uri(URI.create(baseUrl() + "/ct"))
            .header("Content-Type", "application/json")
            .body(new HttpBody.Bytes("{\"a\":1}".getBytes(), "application/json"))
            .build();

    var response = adapter.execute(request).block(Duration.ofSeconds(2));
    assertNotNull(response);
    assertEquals(200, response.statusCode());
  }

  // --- buildBodyInserter Multipart: null filename → "filename != null" false branch ---

  @Test
  void execute_multipartWithNullFilename_omitsFilename() {
    startServer(
        RouterFunctions.route(
            org.springframework.web.reactive.function.server.RequestPredicates.POST("/up"),
            req -> ServerResponse.ok().bodyValue("ok")));

    WebClientHttpClientAdapter adapter = WebClientHttpClientAdapter.builder().build();
    HttpBody.Multipart.Part part = new HttpBody.Multipart.Part("file", "content".getBytes());
    HttpRequest request =
        HttpRequest.builder()
            .method(HttpMethod.POST)
            .uri(URI.create(baseUrl() + "/up"))
            .body(new HttpBody.Multipart(List.of(part)))
            .build();

    var response = adapter.execute(request).block(Duration.ofSeconds(2));
    assertNotNull(response);
    assertEquals(200, response.statusCode());
  }

  // --- applyContextHeaders: provider returns empty map → isEmpty() true → return request ---

  @Test
  void execute_contextProviderReturnsEmpty_requestUnchanged() {
    startServer(
        RouterFunctions.route(
            org.springframework.web.reactive.function.server.RequestPredicates.GET("/empty"),
            req -> ServerResponse.ok().bodyValue("ok")));

    WebClientHttpClientAdapter adapter =
        WebClientHttpClientAdapter.builder().contextHeadersProvider(ctx -> Map.of()).build();
    HttpRequest request =
        HttpRequest.builder().method(HttpMethod.GET).uri(URI.create(baseUrl() + "/empty")).build();

    var response = adapter.execute(request).block(Duration.ofSeconds(2));
    assertNotNull(response);
    assertEquals(200, response.statusCode());
  }

  // --- applyContextHeaders forEach: null value → key!=null && value!=null false branch ---

  @Test
  void execute_contextProviderReturnsNullValue_headerSkipped() {
    startServer(
        RouterFunctions.route(
            org.springframework.web.reactive.function.server.RequestPredicates.GET("/nullval"),
            req -> ServerResponse.ok().bodyValue("ok")));

    Map<String, String> headersWithNullValue = new HashMap<>();
    headersWithNullValue.put("X-Skip", null);
    headersWithNullValue.put("X-Keep", "val");

    WebClientHttpClientAdapter adapter =
        WebClientHttpClientAdapter.builder()
            .contextHeadersProvider(ctx -> headersWithNullValue)
            .build();
    HttpRequest request =
        HttpRequest.builder()
            .method(HttpMethod.GET)
            .uri(URI.create(baseUrl() + "/nullval"))
            .build();

    var response = adapter.execute(request).block(Duration.ofSeconds(2));
    assertNotNull(response);
    assertEquals(200, response.statusCode());
  }
}
