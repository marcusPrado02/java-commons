package com.marcusprado02.commons.adapters.http.webclient;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.marcusprado02.commons.ports.http.HttpInterceptor;
import com.marcusprado02.commons.ports.http.HttpMethod;
import com.marcusprado02.commons.ports.http.HttpRequest;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

class WebClientHttpClientAdapterTest {

  private DisposableServer server;

  @AfterEach
  void tearDown() {
    if (server != null) {
      server.disposeNow();
    }
  }

  @Test
  void executes_get_and_returns_body() {
    startServer(
        RouterFunctions.route(
            org.springframework.web.reactive.function.server.RequestPredicates.GET("/hello"),
            req -> ServerResponse.ok().contentType(MediaType.TEXT_PLAIN).bodyValue("ok")));

    WebTestClient.bindToServer()
        .baseUrl(baseUrl())
        .build()
        .get()
        .uri("/hello")
        .exchange()
        .expectStatus()
        .isOk();

    WebClientHttpClientAdapter adapter = WebClientHttpClientAdapter.builder().build();

    HttpRequest request =
        HttpRequest.builder().method(HttpMethod.GET).uri(URI.create(baseUrl() + "/hello")).build();

    var response = adapter.execute(request).block(Duration.ofSeconds(2));
    assertNotNull(response);
    assertEquals(200, response.statusCode());
    assertArrayEquals("ok".getBytes(StandardCharsets.UTF_8), response.body().orElseThrow());
  }

  @Test
  void applies_request_interceptor_header() {
    startServer(
        RouterFunctions.route(
            org.springframework.web.reactive.function.server.RequestPredicates.GET("/secure"),
            req -> {
              String header = req.headers().firstHeader("X-Test");
              if (!"123".equals(header)) {
                return ServerResponse.status(400).build();
              }
              return ServerResponse.ok().bodyValue("ok");
            }));

    HttpInterceptor interceptor =
        new HttpInterceptor() {
          @Override
          public HttpRequest onRequest(HttpRequest request) {
            HttpRequest.Builder builder = HttpRequest.builder();
            request.name().ifPresent(builder::name);
            builder.method(request.method());
            builder.uri(request.uri());
            builder.headers(request.headers());
            request.body().ifPresent(builder::body);
            request.timeout().ifPresent(builder::timeout);
            builder.header("X-Test", "123");
            return builder.build();
          }
        };

    WebClientHttpClientAdapter adapter =
        WebClientHttpClientAdapter.builder().interceptor(interceptor).build();

    HttpRequest request =
        HttpRequest.builder().method(HttpMethod.GET).uri(URI.create(baseUrl() + "/secure")).build();

    var response = adapter.execute(request).block(Duration.ofSeconds(2));
    assertNotNull(response);
    assertEquals(200, response.statusCode());
  }

  @Test
  void can_add_headers_from_reactor_context() {
    startServer(
        RouterFunctions.route(
            org.springframework.web.reactive.function.server.RequestPredicates.GET("/ctx"),
            req -> {
              String cid = req.headers().firstHeader("X-Correlation-Id");
              if (!"abc".equals(cid)) {
                return ServerResponse.status(400).build();
              }
              return ServerResponse.ok().bodyValue("ok");
            }));

    WebClientHttpClientAdapter adapter =
        WebClientHttpClientAdapter.builder()
            .contextHeadersProvider(
                ctx ->
                    ctx.hasKey("correlationId")
                        ? Map.of("X-Correlation-Id", Objects.toString(ctx.get("correlationId"), ""))
                        : Map.of())
            .build();

    HttpRequest request =
        HttpRequest.builder().method(HttpMethod.GET).uri(URI.create(baseUrl() + "/ctx")).build();

    var response =
        adapter
            .execute(request)
            .contextWrite(context -> context.put("correlationId", "abc"))
            .block(Duration.ofSeconds(2));

    assertNotNull(response);
    assertEquals(200, response.statusCode());
  }

  private void startServer(RouterFunction<ServerResponse> routes) {
    HttpHandler httpHandler = RouterFunctions.toHttpHandler(routes);
    server =
        HttpServer.create().port(0).handle(new ReactorHttpHandlerAdapter(httpHandler)).bindNow();
  }

  private String baseUrl() {
    return "http://localhost:" + server.port();
  }
}
