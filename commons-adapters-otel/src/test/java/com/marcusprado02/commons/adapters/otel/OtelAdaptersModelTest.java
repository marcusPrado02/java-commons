package com.marcusprado02.commons.adapters.otel;

import static org.junit.jupiter.api.Assertions.*;

import com.marcusprado02.commons.adapters.otel.baggage.BaggageBinder;
import com.marcusprado02.commons.adapters.otel.metrics.OtelBusinessMetrics;
import com.marcusprado02.commons.kernel.ddd.audit.ActorId;
import com.marcusprado02.commons.kernel.ddd.tenant.TenantId;
import com.marcusprado02.commons.platform.context.RequestContextSnapshot;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OtelAdaptersModelTest {

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

  // ── OtelTracerFacade ─────────────────────────────────────────────────────

  @Test
  void tracerFacadeInSpanRunnableSucceeds() {
    OtelTracerFacade facade = new OtelTracerFacade("test");
    AtomicBoolean ran = new AtomicBoolean(false);
    facade.inSpan("my-span", () -> ran.set(true));
    assertTrue(ran.get());
    assertEquals(1, exporter.getFinishedSpanItems().size());
  }

  @Test
  void tracerFacadeInSpanRunnableRecordsExceptionAndRethrows() {
    OtelTracerFacade facade = new OtelTracerFacade("test");
    assertThrows(
        RuntimeException.class,
        () ->
            facade.inSpan(
                "err-span",
                () -> {
                  throw new RuntimeException("boom");
                }));
    assertEquals(1, exporter.getFinishedSpanItems().size());
  }

  @Test
  void tracerFacadeInSpanSupplierReturnsValue() {
    OtelTracerFacade facade = new OtelTracerFacade("test");
    String result = facade.inSpan("supply-span", () -> "hello");
    assertEquals("hello", result);
    assertEquals(1, exporter.getFinishedSpanItems().size());
  }

  @Test
  void tracerFacadeInSpanSupplierRecordsExceptionAndRethrows() {
    OtelTracerFacade facade = new OtelTracerFacade("test");
    assertThrows(
        RuntimeException.class,
        () ->
            facade.<String>inSpan(
                "err-supply",
                () -> {
                  throw new RuntimeException("supply-fail");
                }));
    assertEquals(1, exporter.getFinishedSpanItems().size());
  }

  @Test
  void tracerFacadeNullInstrumentationNameUsesDefault() {
    OtelTracerFacade facade = new OtelTracerFacade(null);
    facade.inSpan("null-name-span", () -> {});
    assertEquals(1, exporter.getFinishedSpanItems().size());
  }

  @Test
  void tracerFacadeBlankSpanNameUsesDefault() {
    OtelTracerFacade facade = new OtelTracerFacade("test");
    facade.inSpan("   ", () -> {});
    assertEquals(1, exporter.getFinishedSpanItems().size());
    assertEquals("unnamed-span", exporter.getFinishedSpanItems().get(0).getName());
  }

  // ── BaggageBinder ────────────────────────────────────────────────────────

  @Test
  void baggageBinderNullContextReturnsCurrent() {
    Baggage result = BaggageBinder.bind(null);
    assertNotNull(result);
  }

  @Test
  void baggageBinderWithFullContext() {
    RequestContextSnapshot ctx =
        new RequestContextSnapshot(
            "corr-1", "caus-1", TenantId.of("tenant-x"), ActorId.of("actor-y"));
    Baggage result = BaggageBinder.bind(ctx);
    assertNotNull(result);
  }

  @Test
  void baggageBinderWithNullTenantAndActor() {
    RequestContextSnapshot ctx = new RequestContextSnapshot("corr-2", "caus-2", null, null);
    Baggage result = BaggageBinder.bind(ctx);
    assertNotNull(result);
  }

  @Test
  void baggageBinderWithBlankCorrelation() {
    RequestContextSnapshot ctx =
        new RequestContextSnapshot("   ", null, TenantId.of("t1"), ActorId.of("a1"));
    Baggage result = BaggageBinder.bind(ctx);
    assertNotNull(result);
  }

  // ── OtelBusinessMetrics ──────────────────────────────────────────────────

  @Test
  void otelBusinessMetricsCounterWithNullParams() {
    OtelBusinessMetrics metrics = new OtelBusinessMetrics(null);
    assertNotNull(metrics.counter("my.counter", null, null));
  }

  @Test
  void otelBusinessMetricsUpDownCounter() {
    OtelBusinessMetrics metrics = new OtelBusinessMetrics("test");
    assertNotNull(metrics.upDownCounter("my.updown", "desc", "1"));
  }

  @Test
  void otelBusinessMetricsHistogram() {
    OtelBusinessMetrics metrics = new OtelBusinessMetrics("test");
    assertNotNull(metrics.histogram("my.histogram", null, null));
  }

  @Test
  void otelBusinessMetricsLongGauge() {
    OtelBusinessMetrics metrics = new OtelBusinessMetrics("test");
    assertNotNull(metrics.gauge("my.gauge.long", null, null, () -> 42L));
  }

  @Test
  void otelBusinessMetricsDoubleGauge() {
    OtelBusinessMetrics metrics = new OtelBusinessMetrics("test");
    assertNotNull(metrics.gauge("my.gauge.double", null, null, () -> 3.14));
  }
}
