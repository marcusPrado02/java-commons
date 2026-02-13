package com.marcusprado02.commons.adapters.http.okhttp;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.marcusprado02.commons.app.observability.TracerFacade;
import com.marcusprado02.commons.app.resilience.ResilienceExecutor;
import com.marcusprado02.commons.app.resilience.ResiliencePolicySet;
import com.marcusprado02.commons.ports.http.HttpAuth;
import com.marcusprado02.commons.ports.http.HttpMethod;
import com.marcusprado02.commons.ports.http.HttpRequest;
import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class OkHttpClientAdapterTest {

  @Test
  void executes_get_request_and_returns_body() {
    WireMockServer server = new WireMockServer(WireMockConfiguration.options().dynamicPort());
    server.start();
    try {
      server.stubFor(
          com.github.tomakehurst.wiremock.client.WireMock.get("/hello")
              .willReturn(
                  com.github.tomakehurst.wiremock.client.WireMock.aResponse()
                      .withStatus(200)
                      .withBody("ok")));

      OkHttpClientAdapter adapter = OkHttpClientAdapter.builder().build();

      HttpRequest request =
          HttpRequest.builder()
              .method(HttpMethod.GET)
              .uri(URI.create(server.baseUrl() + "/hello"))
              .build();

      var response = adapter.execute(request);

      assertTrue(response.isSuccessful());
      assertEquals(200, response.statusCode());
      assertArrayEquals(
          "ok".getBytes(java.nio.charset.StandardCharsets.UTF_8), response.body().orElseThrow());
    } finally {
      server.stop();
    }
  }

  @Test
  void supports_bearer_auth_via_request_builder() {
    WireMockServer server = new WireMockServer(WireMockConfiguration.options().dynamicPort());
    server.start();
    try {
      server.stubFor(
          com.github.tomakehurst.wiremock.client.WireMock.get("/secure")
              .withHeader(
                  "Authorization",
                  com.github.tomakehurst.wiremock.client.WireMock.equalTo("Bearer abc"))
              .willReturn(
                  com.github.tomakehurst.wiremock.client.WireMock.aResponse()
                      .withStatus(200)
                      .withBody("ok")));

      OkHttpClientAdapter adapter = OkHttpClientAdapter.builder().build();

      HttpRequest request =
          HttpRequest.builder()
              .method(HttpMethod.GET)
              .uri(URI.create(server.baseUrl() + "/secure"))
              .auth(new HttpAuth.Bearer("abc"))
              .build();

      var response = adapter.execute(request);
      assertEquals(200, response.statusCode());
    } finally {
      server.stop();
    }
  }

  @Test
  void integrates_with_tracer_and_resilience_executor() {
    WireMockServer server = new WireMockServer(WireMockConfiguration.options().dynamicPort());
    server.start();
    try {
      server.stubFor(
          com.github.tomakehurst.wiremock.client.WireMock.get("/hello")
              .willReturn(
                  com.github.tomakehurst.wiremock.client.WireMock.aResponse()
                      .withStatus(200)
                      .withBody("ok")));

      AtomicInteger spans = new AtomicInteger();
      AtomicInteger resilience = new AtomicInteger();

      TracerFacade tracer =
          new TracerFacade() {
            @Override
            public void inSpan(String spanName, Runnable action) {
              spans.incrementAndGet();
              action.run();
            }

            @Override
            public <T> T inSpan(String spanName, Supplier<T> action) {
              spans.incrementAndGet();
              return action.get();
            }
          };

      ResilienceExecutor executor =
          new ResilienceExecutor() {
            @Override
            public void run(String name, ResiliencePolicySet policies, Runnable action) {
              resilience.incrementAndGet();
              action.run();
            }

            @Override
            public <T> T supply(String name, ResiliencePolicySet policies, Supplier<T> action) {
              resilience.incrementAndGet();
              return action.get();
            }
          };

      OkHttpClientAdapter adapter =
          OkHttpClientAdapter.builder()
              .tracerFacade(tracer)
              .resilienceExecutor(executor)
               .resiliencePolicies(new ResiliencePolicySet(null, null, null, null, null, null))
              .build();

      HttpRequest request =
          HttpRequest.builder()
              .name("test-request")
              .method(HttpMethod.GET)
              .uri(URI.create(server.baseUrl() + "/hello"))
              .build();

      adapter.execute(request);

      assertEquals(1, spans.get());
      assertEquals(1, resilience.get());
    } finally {
      server.stop();
    }
  }
}
