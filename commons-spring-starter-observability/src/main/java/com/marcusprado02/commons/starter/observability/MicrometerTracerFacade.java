package com.marcusprado02.commons.starter.observability;

import com.marcusprado02.commons.app.observability.TracerFacade;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.Tracer.SpanInScope;
import java.util.Objects;
import java.util.function.Supplier;

final class MicrometerTracerFacade implements TracerFacade {

  private final Tracer tracer;

  MicrometerTracerFacade(Tracer tracer) {
    this.tracer = Objects.requireNonNull(tracer, "tracer must not be null");
  }

  @Override
  public void inSpan(String spanName, Runnable action) {
    Objects.requireNonNull(action, "action must not be null");

    Span span = tracer.nextSpan().name(safeSpanName(spanName)).start();
    try (SpanInScope ignored = tracer.withSpan(span)) {
      action.run();
    } catch (RuntimeException ex) {
      span.error(ex);
      throw ex;
    } finally {
      span.end();
    }
  }

  @Override
  public <T> T inSpan(String spanName, Supplier<T> action) {
    Objects.requireNonNull(action, "action must not be null");

    Span span = tracer.nextSpan().name(safeSpanName(spanName)).start();
    try (SpanInScope ignored = tracer.withSpan(span)) {
      return action.get();
    } catch (RuntimeException ex) {
      span.error(ex);
      throw ex;
    } finally {
      span.end();
    }
  }

  private static String safeSpanName(String spanName) {
    return (spanName == null || spanName.isBlank()) ? "operation" : spanName.trim();
  }
}
