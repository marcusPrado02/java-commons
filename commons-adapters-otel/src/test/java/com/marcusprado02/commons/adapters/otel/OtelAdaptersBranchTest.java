package com.marcusprado02.commons.adapters.otel;

import static org.junit.jupiter.api.Assertions.*;

import com.marcusprado02.commons.adapters.otel.baggage.BaggageBinder;
import com.marcusprado02.commons.adapters.otel.messaging.OtelMessagePublisherPortDecorator;
import com.marcusprado02.commons.adapters.otel.metrics.OtelBusinessMetrics;
import com.marcusprado02.commons.kernel.ddd.audit.ActorId;
import com.marcusprado02.commons.kernel.ddd.tenant.TenantId;
import com.marcusprado02.commons.platform.context.RequestContextSnapshot;
import com.marcusprado02.commons.ports.messaging.MessageEnvelope;
import com.marcusprado02.commons.ports.messaging.MessageHeaders;
import com.marcusprado02.commons.ports.messaging.MessageSerializer;
import com.marcusprado02.commons.ports.messaging.TopicName;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OtelAdaptersBranchTest {

  private InMemorySpanExporter exporter;

  @BeforeEach
  void setup() {
    exporter = InMemorySpanExporter.create();
    SdkTracerProvider tracerProvider =
        SdkTracerProvider.builder().addSpanProcessor(SimpleSpanProcessor.create(exporter)).build();
    OpenTelemetrySdk openTelemetry =
        OpenTelemetrySdk.builder()
            .setTracerProvider(tracerProvider)
            .setMeterProvider(SdkMeterProvider.builder().build())
            .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
            .build();
    GlobalOpenTelemetry.resetForTest();
    GlobalOpenTelemetry.set(openTelemetry);
  }

  @AfterEach
  void tearDown() {
    GlobalOpenTelemetry.resetForTest();
  }

  // --- OtelTracerFacade constructor blank instrumentation name ---

  @Test
  void tracerFacade_blank_instrumentation_name_uses_default() {
    OtelTracerFacade facade = new OtelTracerFacade("   ");
    facade.inSpan("span", () -> {});
    assertEquals(1, exporter.getFinishedSpanItems().size());
  }

  // --- OtelBusinessMetrics constructor blank name ---

  @Test
  void otelBusinessMetrics_blank_name_uses_default() {
    OtelBusinessMetrics metrics = new OtelBusinessMetrics("   ");
    assertNotNull(metrics.counter("c", "desc", "1"));
  }

  // --- OtelBusinessMetrics non-null description/unit branches ---

  @Test
  void otelBusinessMetrics_upDownCounter_null_description_and_unit() {
    OtelBusinessMetrics metrics = new OtelBusinessMetrics("test");
    assertNotNull(metrics.upDownCounter("uc", null, null));
  }

  @Test
  void otelBusinessMetrics_histogram_non_null_description_and_unit() {
    OtelBusinessMetrics metrics = new OtelBusinessMetrics("test");
    assertNotNull(metrics.histogram("h", "desc", "ms"));
  }

  @Test
  void otelBusinessMetrics_histogram_null_description_and_unit() {
    OtelBusinessMetrics metrics = new OtelBusinessMetrics("test");
    assertNotNull(metrics.histogram("h2", null, null));
  }

  @Test
  void otelBusinessMetrics_long_gauge_non_null_description_and_unit() {
    OtelBusinessMetrics metrics = new OtelBusinessMetrics("test");
    assertNotNull(metrics.gauge("g.long", "desc", "count", () -> 1L));
  }

  @Test
  void otelBusinessMetrics_double_gauge_non_null_description_and_unit() {
    OtelBusinessMetrics metrics = new OtelBusinessMetrics("test");
    assertNotNull(metrics.gauge("g.double", "desc", "ms", () -> 1.0));
  }

  @Test
  void otelBusinessMetrics_counter_non_null_description_and_unit() {
    OtelBusinessMetrics metrics = new OtelBusinessMetrics("test");
    assertNotNull(metrics.counter("cnt", "desc", "1"));
  }

  // --- OtelMessagePublisherPortDecorator exception branch ---

  @Test
  void publisher_exception_in_delegate_records_on_span_and_rethrows() {
    com.marcusprado02.commons.ports.messaging.MessagePublisherPort failing =
        new com.marcusprado02.commons.ports.messaging.MessagePublisherPort() {
          @Override
          public <T> void publish(MessageEnvelope<T> message, MessageSerializer<T> serializer) {
            throw new RuntimeException("publish failed");
          }
        };

    OtelMessagePublisherPortDecorator decorator =
        new OtelMessagePublisherPortDecorator(failing, "test");

    MessageSerializer<String> serializer =
        new MessageSerializer<>() {
          @Override
          public byte[] serialize(String message) {
            return message.getBytes(java.nio.charset.StandardCharsets.UTF_8);
          }

          @Override
          public String deserialize(byte[] data, Class<String> type) {
            return new String(data, java.nio.charset.StandardCharsets.UTF_8);
          }
        };

    assertThrows(
        RuntimeException.class,
        () ->
            decorator.publish(
                MessageEnvelope.<String>builder()
                    .topic(TopicName.of("orders"))
                    .payload("p")
                    .build(),
                serializer));

    assertFalse(exporter.getFinishedSpanItems().isEmpty());
  }

  // --- OtelMessagePublisherPortDecorator.withTraceContext null headers ---

  @Test
  void withTraceContext_null_headers_uses_empty_map() {
    MessageHeaders result = OtelMessagePublisherPortDecorator.withTraceContext(null);
    assertNotNull(result);
  }

  // --- BaggageBinder blank tenantId/actorId values (via putIfNotBlank blank branch) ---

  @Test
  void baggageBinder_null_correlationId_and_causationId() {
    RequestContextSnapshot ctx = new RequestContextSnapshot(null, null, null, null);
    assertNotNull(BaggageBinder.bind(ctx));
  }

  @Test
  void baggageBinder_with_all_non_blank_values() {
    RequestContextSnapshot ctx =
        new RequestContextSnapshot(
            "corr-x", "caus-x", TenantId.of("tenant-1"), ActorId.of("actor-1"));
    assertNotNull(BaggageBinder.bind(ctx));
  }
}
