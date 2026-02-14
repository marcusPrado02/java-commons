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
}
