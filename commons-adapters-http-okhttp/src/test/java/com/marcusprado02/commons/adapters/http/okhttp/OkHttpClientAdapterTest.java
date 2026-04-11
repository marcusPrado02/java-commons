package com.marcusprado02.commons.adapters.http.okhttp;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.marcusprado02.commons.app.observability.TracerFacade;
import com.marcusprado02.commons.app.resilience.ResilienceExecutor;
import com.marcusprado02.commons.app.resilience.ResiliencePolicySet;
import com.marcusprado02.commons.ports.http.HttpAuth;
import com.marcusprado02.commons.ports.http.HttpBody;
import com.marcusprado02.commons.ports.http.HttpInterceptor;
import com.marcusprado02.commons.ports.http.HttpMethod;
import com.marcusprado02.commons.ports.http.HttpRequest;
import com.marcusprado02.commons.ports.http.HttpResponse;
import com.marcusprado02.commons.ports.http.HttpStreamingResponse;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
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

  @Test
  void post_with_bytes_body_sends_correct_content() {
    WireMockServer server = new WireMockServer(WireMockConfiguration.options().dynamicPort());
    server.start();
    try {
      server.stubFor(
          WireMock.post("/data")
              .willReturn(WireMock.aResponse().withStatus(201).withBody("created")));

      OkHttpClientAdapter adapter = OkHttpClientAdapter.builder().build();
      byte[] payload = "{\"key\":\"value\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
      HttpRequest request =
          HttpRequest.builder()
              .method(HttpMethod.POST)
              .uri(URI.create(server.baseUrl() + "/data"))
              .body(new HttpBody.Bytes(payload, "application/json"))
              .build();

      var response = adapter.execute(request);
      assertEquals(201, response.statusCode());
    } finally {
      server.stop();
    }
  }

  @Test
  void post_with_form_body_sends_encoded_fields() {
    WireMockServer server = new WireMockServer(WireMockConfiguration.options().dynamicPort());
    server.start();
    try {
      server.stubFor(
          WireMock.post("/form").willReturn(WireMock.aResponse().withStatus(200).withBody("ok")));

      OkHttpClientAdapter adapter = OkHttpClientAdapter.builder().build();
      HttpRequest request =
          HttpRequest.builder()
              .method(HttpMethod.POST)
              .uri(URI.create(server.baseUrl() + "/form"))
              .body(new HttpBody.FormUrlEncoded(Map.of("field", List.of("value"))))
              .build();

      var response = adapter.execute(request);
      assertEquals(200, response.statusCode());
    } finally {
      server.stop();
    }
  }

  @Test
  void post_with_multipart_body_with_filename() {
    WireMockServer server = new WireMockServer(WireMockConfiguration.options().dynamicPort());
    server.start();
    try {
      server.stubFor(
          WireMock.post("/upload").willReturn(WireMock.aResponse().withStatus(200).withBody("ok")));

      OkHttpClientAdapter adapter = OkHttpClientAdapter.builder().build();
      byte[] fileBytes = "file-content".getBytes(java.nio.charset.StandardCharsets.UTF_8);
      HttpBody.Multipart.Part part =
          new HttpBody.Multipart.Part("file", "test.txt", "application/octet-stream", fileBytes);
      HttpRequest request =
          HttpRequest.builder()
              .method(HttpMethod.POST)
              .uri(URI.create(server.baseUrl() + "/upload"))
              .body(new HttpBody.Multipart(List.of(part)))
              .build();

      var response = adapter.execute(request);
      assertEquals(200, response.statusCode());
    } finally {
      server.stop();
    }
  }

  @Test
  void post_with_multipart_body_without_filename() {
    WireMockServer server = new WireMockServer(WireMockConfiguration.options().dynamicPort());
    server.start();
    try {
      server.stubFor(
          WireMock.post("/upload").willReturn(WireMock.aResponse().withStatus(200).withBody("ok")));

      OkHttpClientAdapter adapter = OkHttpClientAdapter.builder().build();
      byte[] fileBytes = "data".getBytes(java.nio.charset.StandardCharsets.UTF_8);
      HttpBody.Multipart.Part part =
          new HttpBody.Multipart.Part("field", null, "application/octet-stream", fileBytes);
      HttpRequest request =
          HttpRequest.builder()
              .method(HttpMethod.POST)
              .uri(URI.create(server.baseUrl() + "/upload"))
              .body(new HttpBody.Multipart(List.of(part)))
              .build();

      var response = adapter.execute(request);
      assertEquals(200, response.statusCode());
    } finally {
      server.stop();
    }
  }

  @Test
  void exchange_returns_streaming_response() throws Exception {
    WireMockServer server = new WireMockServer(WireMockConfiguration.options().dynamicPort());
    server.start();
    try {
      server.stubFor(
          WireMock.get("/stream")
              .willReturn(WireMock.aResponse().withStatus(200).withBody("streamed-data")));

      OkHttpClientAdapter adapter = OkHttpClientAdapter.builder().build();
      HttpRequest request =
          HttpRequest.builder()
              .method(HttpMethod.GET)
              .uri(URI.create(server.baseUrl() + "/stream"))
              .build();

      try (HttpStreamingResponse response = adapter.exchange(request)) {
        assertEquals(200, response.statusCode());
        assertNotNull(response.body());
        byte[] bytes = response.body().readAllBytes();
        assertTrue(bytes.length > 0);
      }
    } finally {
      server.stop();
    }
  }

  @Test
  void builder_with_all_timeouts_creates_adapter() {
    OkHttpClientAdapter adapter =
        OkHttpClientAdapter.builder()
            .connectTimeout(Duration.ofSeconds(5))
            .readTimeout(Duration.ofSeconds(5))
            .writeTimeout(Duration.ofSeconds(5))
            .callTimeout(Duration.ofSeconds(10))
            .build();
    assertNotNull(adapter);
  }

  @Test
  void per_request_timeout_overrides_client_timeout() {
    WireMockServer server = new WireMockServer(WireMockConfiguration.options().dynamicPort());
    server.start();
    try {
      server.stubFor(
          WireMock.get("/fast").willReturn(WireMock.aResponse().withStatus(200).withBody("ok")));

      OkHttpClientAdapter adapter = OkHttpClientAdapter.builder().build();
      HttpRequest request =
          HttpRequest.builder()
              .method(HttpMethod.GET)
              .uri(URI.create(server.baseUrl() + "/fast"))
              .timeout(Duration.ofSeconds(5))
              .build();

      var response = adapter.execute(request);
      assertEquals(200, response.statusCode());
    } finally {
      server.stop();
    }
  }

  @Test
  void interceptors_are_applied_to_request_and_response() {
    WireMockServer server = new WireMockServer(WireMockConfiguration.options().dynamicPort());
    server.start();
    try {
      server.stubFor(
          WireMock.get("/intercepted")
              .willReturn(WireMock.aResponse().withStatus(200).withBody("ok")));

      AtomicInteger requestCount = new AtomicInteger();
      AtomicInteger responseCount = new AtomicInteger();
      HttpInterceptor interceptor =
          new HttpInterceptor() {
            @Override
            public HttpRequest onRequest(HttpRequest req) {
              requestCount.incrementAndGet();
              return req;
            }

            @Override
            public HttpResponse<byte[]> onResponse(HttpRequest req, HttpResponse<byte[]> resp) {
              responseCount.incrementAndGet();
              return resp;
            }
          };

      OkHttpClientAdapter adapter = OkHttpClientAdapter.builder().interceptor(interceptor).build();
      HttpRequest request =
          HttpRequest.builder()
              .method(HttpMethod.GET)
              .uri(URI.create(server.baseUrl() + "/intercepted"))
              .build();

      adapter.execute(request);
      assertEquals(1, requestCount.get());
      assertEquals(1, responseCount.get());
    } finally {
      server.stop();
    }
  }
}
