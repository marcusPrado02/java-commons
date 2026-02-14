package com.marcusprado02.commons.adapters.otel.messaging;

import com.marcusprado02.commons.ports.messaging.ConsumerGroup;
import com.marcusprado02.commons.ports.messaging.MessageConsumerPort;
import com.marcusprado02.commons.ports.messaging.MessageEnvelope;
import com.marcusprado02.commons.ports.messaging.MessageSerializer;
import com.marcusprado02.commons.ports.messaging.TopicName;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * OpenTelemetry decorator for {@link MessageConsumerPort}.
 *
 * <p>Extracts trace context from {@link com.marcusprado02.commons.ports.messaging.MessageHeaders}
 * and creates a CONSUMER span for the handler.
 */
public final class OtelMessageConsumerPortDecorator implements MessageConsumerPort {

  private static final TextMapGetter<MessageEnvelope<?>> GETTER =
      new TextMapGetter<>() {
        @Override
        public Iterable<String> keys(MessageEnvelope<?> carrier) {
          if (carrier == null || carrier.headers() == null) {
            return java.util.List.of();
          }
          return carrier.headers().asMap().keySet();
        }

        @Override
        public String get(MessageEnvelope<?> carrier, String key) {
          if (carrier == null || carrier.headers() == null) {
            return null;
          }
          return carrier.headers().get(key).orElse(null);
        }
      };

  private final MessageConsumerPort delegate;
  private final Tracer tracer;
  private final TextMapPropagator propagator;

  public OtelMessageConsumerPortDecorator(
      MessageConsumerPort delegate, String instrumentationName) {
    this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");

    String name =
        (instrumentationName == null || instrumentationName.isBlank())
            ? "com.marcusprado02.commons"
            : instrumentationName.trim();

    this.tracer = GlobalOpenTelemetry.getTracer(name);
    this.propagator = GlobalOpenTelemetry.getPropagators().getTextMapPropagator();
  }

  @Override
  public <T> void subscribe(
      TopicName topic,
      ConsumerGroup group,
      Class<T> messageType,
      MessageSerializer<T> serializer,
      Consumer<MessageEnvelope<T>> handler) {

    delegate.subscribe(
        topic,
        group,
        messageType,
        serializer,
        envelope -> {
          Context extracted = propagator.extract(Context.current(), envelope, GETTER);

          String spanName = "consume " + envelope.topic().value();
          Span span =
              tracer
                  .spanBuilder(spanName)
                  .setParent(extracted)
                  .setSpanKind(SpanKind.CONSUMER)
                  .setAttribute("messaging.destination.name", envelope.topic().value())
                  .setAttribute("messaging.message.id", envelope.id().value())
                  .startSpan();

          try (Scope ignored = span.makeCurrent()) {
            handler.accept(envelope);
          } catch (RuntimeException ex) {
            span.recordException(ex);
            span.setStatus(StatusCode.ERROR);
            throw ex;
          } finally {
            span.end();
          }
        });
  }

  @Override
  public void unsubscribe(TopicName topic, ConsumerGroup group) {
    delegate.unsubscribe(topic, group);
  }

  @Override
  public void start() {
    delegate.start();
  }

  @Override
  public void stop() {
    delegate.stop();
  }
}
