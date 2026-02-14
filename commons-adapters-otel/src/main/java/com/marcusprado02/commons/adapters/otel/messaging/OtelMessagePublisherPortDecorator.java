package com.marcusprado02.commons.adapters.otel.messaging;

import com.marcusprado02.commons.ports.messaging.MessageEnvelope;
import com.marcusprado02.commons.ports.messaging.MessageHeaders;
import com.marcusprado02.commons.ports.messaging.MessagePublisherPort;
import com.marcusprado02.commons.ports.messaging.MessageSerializer;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import java.util.Map;
import java.util.Objects;

/**
 * OpenTelemetry decorator for {@link MessagePublisherPort}.
 *
 * <p>Creates a PRODUCER span and injects trace context into {@link MessageHeaders}.
 */
public final class OtelMessagePublisherPortDecorator implements MessagePublisherPort {

  private static final TextMapSetter<MessageHeaders.Builder> SETTER =
      (carrier, key, value) -> {
        if (carrier != null && key != null && value != null) {
          carrier.header(key, value);
        }
      };

  private final MessagePublisherPort delegate;
  private final Tracer tracer;
  private final TextMapPropagator propagator;

  public OtelMessagePublisherPortDecorator(
      MessagePublisherPort delegate, String instrumentationName) {
    this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");

    String name =
        (instrumentationName == null || instrumentationName.isBlank())
            ? "com.marcusprado02.commons"
            : instrumentationName.trim();

    this.tracer = GlobalOpenTelemetry.getTracer(name);
    this.propagator = GlobalOpenTelemetry.getPropagators().getTextMapPropagator();
  }

  @Override
  public <T> void publish(MessageEnvelope<T> message, MessageSerializer<T> serializer) {
    Objects.requireNonNull(message, "message must not be null");
    Objects.requireNonNull(serializer, "serializer must not be null");

    String spanName = "publish " + message.topic().value();

    Span span =
        tracer
            .spanBuilder(spanName)
            .setSpanKind(SpanKind.PRODUCER)
            .setAttribute("messaging.destination.name", message.topic().value())
            .setAttribute("messaging.message.id", message.id().value())
            .startSpan();

    try (Scope ignored = span.makeCurrent()) {
      MessageEnvelope<T> injected = inject(message);
      delegate.publish(injected, serializer);
    } catch (RuntimeException ex) {
      span.recordException(ex);
      span.setStatus(StatusCode.ERROR);
      throw ex;
    } finally {
      span.end();
    }
  }

  private <T> MessageEnvelope<T> inject(MessageEnvelope<T> message) {
    MessageHeaders.Builder headers = MessageHeaders.builder().headers(message.headers().asMap());

    propagator.inject(io.opentelemetry.context.Context.current(), headers, SETTER);

    return MessageEnvelope.<T>builder()
        .id(message.id())
        .topic(message.topic())
        .payload(message.payload())
        .headers(headers.build())
        .timestamp(message.timestamp())
        .partitionKey(message.partitionKey().orElse(null))
        .build();
  }

  /** Utility to merge headers, useful if you want to enforce propagation without spans. */
  public static MessageHeaders withTraceContext(MessageHeaders headers) {
    MessageHeaders.Builder builder =
        MessageHeaders.builder().headers(headers == null ? Map.of() : headers.asMap());
    GlobalOpenTelemetry.getPropagators()
        .getTextMapPropagator()
        .inject(io.opentelemetry.context.Context.current(), builder, SETTER);
    return builder.build();
  }
}
