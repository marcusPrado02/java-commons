package com.marcusprado02.commons.adapters.messaging.azureservicebus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azure.core.util.BinaryData;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.marcusprado02.commons.ports.messaging.ConsumerGroup;
import com.marcusprado02.commons.ports.messaging.TopicName;
import java.time.Instant;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;

@SuppressWarnings({"unchecked", "rawtypes"})
class AzureServiceBusConsumerBranchTest {

  private static final String CONN_STR =
      "Endpoint=sb://test.servicebus.windows.net/;SharedAccessKeyName=test;SharedAccessKey=test";

  record TestPayload(String id, String content, Instant timestamp) {}

  private ServiceBusClientBuilder.ServiceBusProcessorClientBuilder setupProcBuilder(
      ServiceBusProcessorClient mockProcessor) {
    var b = mock(ServiceBusClientBuilder.ServiceBusProcessorClientBuilder.class);
    when(b.queueName(any())).thenReturn(b);
    when(b.processMessage(any())).thenReturn(b);
    when(b.processError(any())).thenReturn(b);
    when(b.maxConcurrentCalls(anyInt())).thenReturn(b);
    when(b.buildProcessorClient()).thenReturn(mockProcessor);
    return b;
  }

  // --- processMessage: success path (complete called) ---

  @Test
  void processMessage_success_completesContext() {
    ServiceBusProcessorClient mockProcessor = mock(ServiceBusProcessorClient.class);
    var mockProcBuilder = setupProcBuilder(mockProcessor);

    ArgumentCaptor<Consumer> msgCaptor = ArgumentCaptor.forClass(Consumer.class);

    try (MockedConstruction<ServiceBusClientBuilder> ignored =
        mockConstruction(
            ServiceBusClientBuilder.class,
            (mock, ctx) -> when(mock.processor()).thenReturn(mockProcBuilder))) {

      AzureServiceBusConsumerAdapter adapter =
          AzureServiceBusConsumerAdapter.builder().connectionString(CONN_STR).build();

      adapter.subscribe(
          TopicName.of("q"),
          ConsumerGroup.of("g"),
          TestPayload.class,
          new JacksonMessageSerializer<>(),
          env -> {});

      verify(mockProcBuilder).processMessage(msgCaptor.capture());
      Consumer<ServiceBusReceivedMessageContext> processMsg = msgCaptor.getValue();

      ServiceBusReceivedMessageContext mockCtx = mock(ServiceBusReceivedMessageContext.class);
      ServiceBusReceivedMessage mockMsg = mock(ServiceBusReceivedMessage.class);
      when(mockCtx.getMessage()).thenReturn(mockMsg);
      when(mockMsg.getBody())
          .thenReturn(
              BinaryData.fromString(
                  "{\"id\":\"1\",\"content\":\"x\",\"timestamp\":\"2026-01-01T00:00:00Z\"}"));
      when(mockMsg.getApplicationProperties()).thenReturn(Map.of("k", "v"));
      when(mockMsg.getMessageId()).thenReturn("msg-1");
      when(mockMsg.getEnqueuedTime()).thenReturn(null);
      when(mockMsg.getPartitionKey()).thenReturn("pk-1");

      processMsg.accept(mockCtx);

      verify(mockCtx).complete();
    }
  }

  // --- processMessage: exception → abandon called ---

  @Test
  void processMessage_handlerThrows_abandonsContext() {
    ServiceBusProcessorClient mockProcessor = mock(ServiceBusProcessorClient.class);
    var mockProcBuilder = setupProcBuilder(mockProcessor);

    ArgumentCaptor<Consumer> msgCaptor = ArgumentCaptor.forClass(Consumer.class);

    try (MockedConstruction<ServiceBusClientBuilder> ignored =
        mockConstruction(
            ServiceBusClientBuilder.class,
            (mock, ctx) -> when(mock.processor()).thenReturn(mockProcBuilder))) {

      AzureServiceBusConsumerAdapter adapter =
          AzureServiceBusConsumerAdapter.builder().connectionString(CONN_STR).build();

      adapter.subscribe(
          TopicName.of("q"),
          ConsumerGroup.of("g"),
          TestPayload.class,
          new JacksonMessageSerializer<>(),
          env -> {
            throw new RuntimeException("handler failed");
          });

      verify(mockProcBuilder).processMessage(msgCaptor.capture());
      Consumer<ServiceBusReceivedMessageContext> processMsg = msgCaptor.getValue();

      ServiceBusReceivedMessageContext mockCtx = mock(ServiceBusReceivedMessageContext.class);
      ServiceBusReceivedMessage mockMsg = mock(ServiceBusReceivedMessage.class);
      when(mockCtx.getMessage()).thenReturn(mockMsg);
      when(mockMsg.getBody())
          .thenReturn(
              BinaryData.fromString(
                  "{\"id\":\"2\",\"content\":\"y\",\"timestamp\":\"2026-01-01T00:00:00Z\"}"));
      when(mockMsg.getApplicationProperties()).thenReturn(Map.of());
      when(mockMsg.getMessageId()).thenReturn("msg-2");
      when(mockMsg.getEnqueuedTime()).thenReturn(null);
      when(mockMsg.getPartitionKey()).thenReturn(null);

      processMsg.accept(mockCtx);

      verify(mockCtx).abandon();
    }
  }

  // --- processError: logs error with entity path and exception ---

  @Test
  void processError_logsError() {
    ServiceBusProcessorClient mockProcessor = mock(ServiceBusProcessorClient.class);
    var mockProcBuilder = setupProcBuilder(mockProcessor);

    ArgumentCaptor<Consumer> errCaptor = ArgumentCaptor.forClass(Consumer.class);

    try (MockedConstruction<ServiceBusClientBuilder> ignored =
        mockConstruction(
            ServiceBusClientBuilder.class,
            (mock, ctx) -> when(mock.processor()).thenReturn(mockProcBuilder))) {

      AzureServiceBusConsumerAdapter adapter =
          AzureServiceBusConsumerAdapter.builder().connectionString(CONN_STR).build();

      adapter.subscribe(
          TopicName.of("q"),
          ConsumerGroup.of("g"),
          TestPayload.class,
          new JacksonMessageSerializer<>(),
          env -> {});

      verify(mockProcBuilder).processError(errCaptor.capture());
      Consumer<ServiceBusErrorContext> processErr = errCaptor.getValue();

      ServiceBusErrorContext mockErrCtx = mock(ServiceBusErrorContext.class);
      when(mockErrCtx.getEntityPath()).thenReturn("test-queue");
      when(mockErrCtx.getException()).thenReturn(new RuntimeException("bus error"));

      processErr.accept(mockErrCtx);
    }
  }

  // --- start(): processor.start() throws → RuntimeException rethrown ---

  @Test
  void start_processorThrows_rethrowsRuntimeException() {
    ServiceBusProcessorClient mockProcessor = mock(ServiceBusProcessorClient.class);
    var mockProcBuilder = setupProcBuilder(mockProcessor);
    doThrow(new RuntimeException("start failed")).when(mockProcessor).start();

    try (MockedConstruction<ServiceBusClientBuilder> ignored =
        mockConstruction(
            ServiceBusClientBuilder.class,
            (mock, ctx) -> when(mock.processor()).thenReturn(mockProcBuilder))) {

      AzureServiceBusConsumerAdapter adapter =
          AzureServiceBusConsumerAdapter.builder().connectionString(CONN_STR).build();

      adapter.subscribe(
          TopicName.of("q"),
          ConsumerGroup.of("g"),
          TestPayload.class,
          new JacksonMessageSerializer<>(),
          env -> {});

      assertThatThrownBy(adapter::start).isInstanceOf(RuntimeException.class);
    }
  }

  // --- stop(): processor.stop() throws → exception swallowed ---

  @Test
  void stop_processorThrows_noExceptionPropagated() {
    ServiceBusProcessorClient mockProcessor = mock(ServiceBusProcessorClient.class);
    var mockProcBuilder = setupProcBuilder(mockProcessor);
    doThrow(new RuntimeException("stop failed")).when(mockProcessor).stop();

    try (MockedConstruction<ServiceBusClientBuilder> ignored =
        mockConstruction(
            ServiceBusClientBuilder.class,
            (mock, ctx) -> when(mock.processor()).thenReturn(mockProcBuilder))) {

      AzureServiceBusConsumerAdapter adapter =
          AzureServiceBusConsumerAdapter.builder().connectionString(CONN_STR).build();

      adapter.subscribe(
          TopicName.of("q"),
          ConsumerGroup.of("g"),
          TestPayload.class,
          new JacksonMessageSerializer<>(),
          env -> {});

      adapter.stop();
    }
  }

  // --- close(): processor.close() throws → exception swallowed ---

  @Test
  void close_processorThrows_noExceptionPropagated() {
    ServiceBusProcessorClient mockProcessor = mock(ServiceBusProcessorClient.class);
    var mockProcBuilder = setupProcBuilder(mockProcessor);
    doThrow(new RuntimeException("close failed")).when(mockProcessor).close();

    try (MockedConstruction<ServiceBusClientBuilder> ignored =
        mockConstruction(
            ServiceBusClientBuilder.class,
            (mock, ctx) -> when(mock.processor()).thenReturn(mockProcBuilder))) {

      AzureServiceBusConsumerAdapter adapter =
          AzureServiceBusConsumerAdapter.builder().connectionString(CONN_STR).build();

      adapter.subscribe(
          TopicName.of("q"),
          ConsumerGroup.of("g"),
          TestPayload.class,
          new JacksonMessageSerializer<>(),
          env -> {});

      adapter.close();
    }
  }

  // --- unsubscribe(): processor.close() throws → exception swallowed ---

  @Test
  void unsubscribe_processorCloseThrows_noExceptionPropagated() {
    ServiceBusProcessorClient mockProcessor = mock(ServiceBusProcessorClient.class);
    var mockProcBuilder = setupProcBuilder(mockProcessor);
    doThrow(new RuntimeException("unsubscribe-close failed")).when(mockProcessor).close();

    try (MockedConstruction<ServiceBusClientBuilder> ignored =
        mockConstruction(
            ServiceBusClientBuilder.class,
            (mock, ctx) -> when(mock.processor()).thenReturn(mockProcBuilder))) {

      AzureServiceBusConsumerAdapter adapter =
          AzureServiceBusConsumerAdapter.builder().connectionString(CONN_STR).build();

      TopicName topic = TopicName.of("q");
      ConsumerGroup group = ConsumerGroup.of("g");

      adapter.subscribe(
          topic, group, TestPayload.class, new JacksonMessageSerializer<>(), env -> {});
      adapter.unsubscribe(topic, group);
    }
  }

  // --- JacksonMessageSerializer: deserialize failure ---

  @Test
  void jacksonSerializer_deserializeInvalidJson_throws() {
    var serializer = new JacksonMessageSerializer<TestPayload>();
    assertThatThrownBy(() -> serializer.deserialize("not-json".getBytes(), TestPayload.class))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Failed to deserialize message");
  }

  // --- JacksonMessageSerializer: serialize failure via bad ObjectMapper ---

  @Test
  void jacksonSerializer_serializeFailure_throws() {
    com.fasterxml.jackson.databind.ObjectMapper badMapper =
        mock(com.fasterxml.jackson.databind.ObjectMapper.class);
    try {
      when(badMapper.writeValueAsString(any()))
          .thenThrow(new com.fasterxml.jackson.core.JsonProcessingException("bad") {});
    } catch (Exception ignored) {
    }
    var serializer = new JacksonMessageSerializer<TestPayload>(badMapper);
    assertThatThrownBy(() -> serializer.serialize(new TestPayload("1", "x", Instant.now())))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Failed to serialize message");

    assertThat(serializer).isNotNull();
  }
}
