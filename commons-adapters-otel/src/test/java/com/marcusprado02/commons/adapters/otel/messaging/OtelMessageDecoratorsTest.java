package com.marcusprado02.commons.adapters.otel.messaging;

import static org.junit.jupiter.api.Assertions.*;

import com.marcusprado02.commons.ports.messaging.*;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OtelMessageDecoratorsTest {

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
  void publisherShouldInjectTraceparentIntoHeaders() {
    AtomicReference<MessageEnvelope<String>> captured = new AtomicReference<>();

    MessagePublisherPort delegate =
        new MessagePublisherPort() {
          @Override
          public <T> void publish(MessageEnvelope<T> message, MessageSerializer<T> serializer) {
            @SuppressWarnings("unchecked")
            MessageEnvelope<String> m = (MessageEnvelope<String>) message;
            captured.set(m);
          }
        };

    MessagePublisherPort publisher = new OtelMessagePublisherPortDecorator(delegate, "test");

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

    Tracer tracer = GlobalOpenTelemetry.getTracer("test");
    Span parent = tracer.spanBuilder("parent").startSpan();

    try (var ignored = parent.makeCurrent()) {
      publisher.publish(
          MessageEnvelope.<String>builder().topic(TopicName.of("orders")).payload("p").build(),
          serializer);
    } finally {
      parent.end();
    }

    MessageEnvelope<String> envelope = captured.get();
    assertNotNull(envelope);
    assertTrue(envelope.headers().get("traceparent").isPresent());
  }

  @Test
  void consumerShouldExtractParentContextAndCreateConsumerSpan() {
    AtomicReference<Consumer<MessageEnvelope<?>>> handlerRef = new AtomicReference<>();

    MessageConsumerPort delegate =
        new MessageConsumerPort() {
          @Override
          public <T> void subscribe(
              TopicName topic,
              ConsumerGroup group,
              Class<T> messageType,
              MessageSerializer<T> serializer,
              Consumer<MessageEnvelope<T>> handler) {
            handlerRef.set((Consumer<MessageEnvelope<?>>) (Consumer<?>) handler);
          }

          @Override
          public void unsubscribe(TopicName topic, ConsumerGroup group) {}

          @Override
          public void start() {}

          @Override
          public void stop() {}
        };

    MessageConsumerPort consumer = new OtelMessageConsumerPortDecorator(delegate, "test");

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

    consumer.subscribe(
        TopicName.of("orders"),
        ConsumerGroup.of("g1"),
        String.class,
      serializer,
        msg -> assertTrue(Span.current().getSpanContext().isValid()));

    @SuppressWarnings("unchecked")
    Consumer<MessageEnvelope<String>> wrappedHandler =
      (Consumer<MessageEnvelope<String>>) (Consumer<?>) handlerRef.get();
    assertNotNull(wrappedHandler);

    // Create a parent span and inject into headers
    Tracer tracer = GlobalOpenTelemetry.getTracer("test");
    Span parent = tracer.spanBuilder("parent").startSpan();

    MessageHeaders headers;
    try (var ignored = parent.makeCurrent()) {
      headers = OtelMessagePublisherPortDecorator.withTraceContext(MessageHeaders.of(Map.of()));
    } finally {
      parent.end();
    }

    MessageEnvelope<String> incoming =
        MessageEnvelope.<String>builder()
            .topic(TopicName.of("orders"))
            .payload("p")
            .headers(headers)
            .build();

    wrappedHandler.accept(incoming);

    // consumer span should share trace id with injected context
    assertFalse(exporter.getFinishedSpanItems().isEmpty());
    String traceId = exporter.getFinishedSpanItems().get(0).getTraceId();
    assertNotNull(traceId);
    assertFalse(traceId.isBlank());
  }
}
