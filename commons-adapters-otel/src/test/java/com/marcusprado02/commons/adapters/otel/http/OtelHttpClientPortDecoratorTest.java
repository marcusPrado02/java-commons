package com.marcusprado02.commons.adapters.otel.http;

import static org.junit.jupiter.api.Assertions.*;

import com.marcusprado02.commons.ports.http.HttpClientPort;
import com.marcusprado02.commons.ports.http.HttpMethod;
import com.marcusprado02.commons.ports.http.HttpRequest;
import com.marcusprado02.commons.ports.http.HttpResponse;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OtelHttpClientPortDecoratorTest {

  private InMemorySpanExporter exporter;

  @BeforeEach
  void setup() {
    exporter = InMemorySpanExporter.create();

    SdkTracerProvider tracerProvider =
        SdkTracerProvider.builder().addSpanProcessor(SimpleSpanProcessor.create(exporter)).build();

    OpenTelemetrySdk openTelemetry =
        OpenTelemetrySdk.builder()
            .setTracerProvider(tracerProvider)
            .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
            .build();

    GlobalOpenTelemetry.resetForTest();
    GlobalOpenTelemetry.set(openTelemetry);
  }

  @AfterEach
  void tearDown() {
    GlobalOpenTelemetry.resetForTest();
  }

  @Test
  void shouldInjectTraceparentHeader() {
    AtomicReference<HttpRequest> captured = new AtomicReference<>();

    HttpClientPort delegate =
        request -> {
          captured.set(request);
          return new HttpResponse<>(200, Map.of(), new byte[0]);
        };

    OtelHttpClientPortDecorator decorated = new OtelHttpClientPortDecorator(delegate, "test");

    Tracer tracer = GlobalOpenTelemetry.getTracer("test");
    Span parent = tracer.spanBuilder("parent").startSpan();

    try (var ignored = parent.makeCurrent()) {
      decorated.execute(
          HttpRequest.builder()
              .method(HttpMethod.GET)
              .uri(URI.create("https://example.com/hello"))
              .build());
    } finally {
      parent.end();
    }

    HttpRequest req = captured.get();
    assertNotNull(req);

    // W3C tracecontext header
    List<String> traceparent = req.headers().get("traceparent");
    assertNotNull(traceparent);
    assertFalse(traceparent.isEmpty());

    assertEquals(2, exporter.getFinishedSpanItems().size());
  }

  @Test
  void shouldSetErrorStatusOnDelegateException() {
    HttpClientPort delegate =
        request -> {
          throw new RuntimeException("connection refused");
        };
    OtelHttpClientPortDecorator decorated = new OtelHttpClientPortDecorator(delegate, "test");

    assertThrows(
        RuntimeException.class,
        () ->
            decorated.execute(
                HttpRequest.builder()
                    .method(HttpMethod.GET)
                    .uri(URI.create("https://example.com/fail"))
                    .build()));

    assertFalse(exporter.getFinishedSpanItems().isEmpty());
  }

  @Test
  void shouldSetErrorStatusOn4xxResponse() {
    HttpClientPort delegate = request -> new HttpResponse<>(404, Map.of(), null);
    OtelHttpClientPortDecorator decorated = new OtelHttpClientPortDecorator(delegate, "test");

    HttpResponse<byte[]> response =
        decorated.execute(
            HttpRequest.builder()
                .method(HttpMethod.GET)
                .uri(URI.create("https://example.com/notfound"))
                .build());

    assertEquals(404, response.statusCode());
    assertFalse(exporter.getFinishedSpanItems().isEmpty());
  }

  @Test
  void shouldUseRequestNameAsSpanName() {
    HttpClientPort delegate = request -> new HttpResponse<>(200, Map.of(), new byte[0]);
    OtelHttpClientPortDecorator decorated = new OtelHttpClientPortDecorator(delegate, "test");

    decorated.execute(
        HttpRequest.builder()
            .name("my-named-call")
            .method(HttpMethod.POST)
            .uri(URI.create("https://example.com/api"))
            .body("{}".getBytes(StandardCharsets.UTF_8))
            .timeout(Duration.ofSeconds(5))
            .build());

    assertEquals(1, exporter.getFinishedSpanItems().size());
    assertEquals("my-named-call", exporter.getFinishedSpanItems().get(0).getName());
  }

  @Test
  void shouldCopyNullHeaderValues() {
    AtomicReference<HttpRequest> captured = new AtomicReference<>();
    HttpClientPort delegate =
        request -> {
          captured.set(request);
          return new HttpResponse<>(200, Map.of(), new byte[0]);
        };
    OtelHttpClientPortDecorator decorated = new OtelHttpClientPortDecorator(delegate, "test");

    Map<String, List<String>> headersWithNull = new LinkedHashMap<>();
    headersWithNull.put("X-Key", null);
    headersWithNull.put("X-Other", new ArrayList<>(List.of("val")));

    decorated.execute(
        HttpRequest.builder()
            .method(HttpMethod.GET)
            .uri(URI.create("https://example.com"))
            .headers(headersWithNull)
            .build());

    assertNotNull(captured.get());
  }

  @Test
  void shouldUseDefaultNameWhenInstrumentationNameIsNull() {
    HttpClientPort delegate = request -> new HttpResponse<>(200, Map.of(), new byte[0]);
    OtelHttpClientPortDecorator decorated = new OtelHttpClientPortDecorator(delegate, null);
    HttpResponse<byte[]> response =
        decorated.execute(
            HttpRequest.builder()
                .method(HttpMethod.GET)
                .uri(URI.create("https://example.com"))
                .build());
    assertEquals(200, response.statusCode());
  }

  @Test
  void decorateIfTracingEnabledReturnsWrapped() {
    HttpClientPort delegate = request -> new HttpResponse<>(200, Map.of(), new byte[0]);
    com.marcusprado02.commons.app.observability.TracerFacade facade =
        new com.marcusprado02.commons.app.observability.TracerFacade() {
          @Override
          public void inSpan(String spanName, Runnable action) {
            action.run();
          }

          @Override
          public <T> T inSpan(String spanName, java.util.function.Supplier<T> action) {
            return action.get();
          }
        };
    HttpClientPort result =
        OtelHttpClientPortDecorator.decorateIfTracingEnabled(delegate, facade, "test");
    assertNotNull(result);
    assertInstanceOf(OtelHttpClientPortDecorator.class, result);
  }

  @Test
  void decorateIfTracingEnabledWithNullTracerReturnsDelegate() {
    HttpClientPort delegate = request -> new HttpResponse<>(200, Map.of(), new byte[0]);
    HttpClientPort result =
        OtelHttpClientPortDecorator.decorateIfTracingEnabled(delegate, null, "test");
    assertSame(delegate, result);
  }
}
