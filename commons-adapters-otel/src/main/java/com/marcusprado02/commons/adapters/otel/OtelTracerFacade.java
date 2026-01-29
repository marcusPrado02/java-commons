package com.marcusprado02.commons.adapters.otel;

import com.marcusprado02.commons.app.observability.TracerFacade;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import java.util.Objects;
import java.util.function.Supplier;

public final class OtelTracerFacade implements TracerFacade {

  private final Tracer tracer;

  public OtelTracerFacade(String instrumentationName) {
    String name =
        (instrumentationName == null || instrumentationName.isBlank())
            ? "com.marcusprado02.commons"
            : instrumentationName.trim();
    this.tracer = GlobalOpenTelemetry.getTracer(name);
  }

  @Override
  public void inSpan(String spanName, Runnable action) {
    Objects.requireNonNull(action, "action must not be null");
    Span span = tracer.spanBuilder(normalize(spanName)).startSpan();
    try (Scope ignored = span.makeCurrent()) {
      action.run();
    } catch (RuntimeException ex) {
      span.recordException(ex);
      span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR);
      throw ex;
    } finally {
      span.end();
    }
  }

  @Override
  public <T> T inSpan(String spanName, Supplier<T> action) {
    Objects.requireNonNull(action, "action must not be null");
    Span span = tracer.spanBuilder(normalize(spanName)).startSpan();
    try (Scope ignored = span.makeCurrent()) {
      return action.get();
    } catch (RuntimeException ex) {
      span.recordException(ex);
      span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR);
      throw ex;
    } finally {
      span.end();
    }
  }

  private String normalize(String spanName) {
    return (spanName == null || spanName.isBlank()) ? "unnamed-span" : spanName.trim();
  }
}
