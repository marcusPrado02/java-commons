package com.marcusprado02.commons.adapters.otel.http;

import com.marcusprado02.commons.app.observability.TracerFacade;
import com.marcusprado02.commons.ports.http.HttpClientPort;
import com.marcusprado02.commons.ports.http.HttpRequest;
import com.marcusprado02.commons.ports.http.HttpResponse;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * OpenTelemetry decorator for {@link HttpClientPort}.
 *
 * <p>Responsibilities:
 *
 * <ul>
 *   <li>Creates a CLIENT span around the HTTP call
 *   <li>Injects trace context into request headers (W3C tracecontext)
 * </ul>
 */
public final class OtelHttpClientPortDecorator implements HttpClientPort {

  private static final TextMapSetter<HttpRequest.Builder> SETTER =
      (carrier, key, value) -> {
        if (carrier != null && key != null && value != null) {
          carrier.header(key, value);
        }
      };

  private final HttpClientPort delegate;
  private final Tracer tracer;
  private final TextMapPropagator propagator;

  public OtelHttpClientPortDecorator(HttpClientPort delegate, String instrumentationName) {
    this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");

    String name =
        (instrumentationName == null || instrumentationName.isBlank())
            ? "com.marcusprado02.commons"
            : instrumentationName.trim();

    this.tracer = GlobalOpenTelemetry.getTracer(name);
    this.propagator = GlobalOpenTelemetry.getPropagators().getTextMapPropagator();
  }

  @Override
  public HttpResponse<byte[]> execute(HttpRequest request) {
    Objects.requireNonNull(request, "request must not be null");

    String spanName = request.name().orElse(request.method() + " " + request.uri().getHost());

    Span span =
        tracer
            .spanBuilder(spanName)
            .setSpanKind(SpanKind.CLIENT)
            .setAttribute("http.method", request.method().name())
            .setAttribute("url.full", request.uri().toString())
            .startSpan();

    try (Scope ignored = span.makeCurrent()) {
      HttpRequest injected = inject(request);
      HttpResponse<byte[]> response = delegate.execute(injected);

      span.setAttribute("http.status_code", (long) response.statusCode());
      if (response.statusCode() >= 400) {
        span.setStatus(StatusCode.ERROR);
      }

      return response;
    } catch (RuntimeException ex) {
      span.recordException(ex);
      span.setStatus(StatusCode.ERROR);
      throw ex;
    } finally {
      span.end();
    }
  }

  private HttpRequest inject(HttpRequest request) {
    HttpRequest.Builder builder = HttpRequest.builder();

    request.name().ifPresent(builder::name);
    builder.method(request.method());
    builder.uri(request.uri());

    builder.headers(copyHeaders(request.headers()));

    request.body().ifPresent(builder::body);
    request.timeout().ifPresent(builder::timeout);

    propagator.inject(io.opentelemetry.context.Context.current(), builder, SETTER);

    return builder.build();
  }

  private static Map<String, List<String>> copyHeaders(Map<String, List<String>> headers) {
    if (headers == null || headers.isEmpty()) {
      return Map.of();
    }

    var copy = new java.util.LinkedHashMap<String, List<String>>();
    headers.forEach(
        (k, values) -> copy.put(k, values == null ? List.of() : new ArrayList<>(values)));
    return copy;
  }

  /**
   * Convenience factory to wrap a client only when a tracer is provided.
   *
   * <p>This is useful for codebases already wired with {@link TracerFacade}.
   */
  public static HttpClientPort decorateIfTracingEnabled(
      HttpClientPort delegate, TracerFacade tracerFacade, String instrumentationName) {
    if (tracerFacade == null) {
      return delegate;
    }
    return new OtelHttpClientPortDecorator(delegate, instrumentationName);
  }
}
