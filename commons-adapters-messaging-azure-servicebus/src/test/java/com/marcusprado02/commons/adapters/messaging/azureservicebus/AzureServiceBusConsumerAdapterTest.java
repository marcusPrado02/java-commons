package com.marcusprado02.commons.adapters.messaging.azureservicebus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.marcusprado02.commons.ports.messaging.ConsumerGroup;
import com.marcusprado02.commons.ports.messaging.MessageEnvelope;
import com.marcusprado02.commons.ports.messaging.TopicName;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

class AzureServiceBusConsumerAdapterTest {

  private static final String CONN_STR =
      "Endpoint=sb://test.servicebus.windows.net/;SharedAccessKeyName=test;SharedAccessKey=test";

  @Test
  void shouldFailWithoutConnectionStringOrNamespace() {
    assertThatThrownBy(() -> AzureServiceBusConsumerAdapter.builder().build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(
            "Either connectionString or fullyQualifiedNamespace must be provided");
  }

  @Test
  void shouldBuildWithConnectionString() {
    AzureServiceBusConsumerAdapter adapter =
        AzureServiceBusConsumerAdapter.builder().connectionString(CONN_STR).build();
    assertThat(adapter).isNotNull();
    adapter.close();
  }

  @Test
  void shouldBuildWithManagedIdentity() {
    AzureServiceBusConsumerAdapter adapter =
        AzureServiceBusConsumerAdapter.builder()
            .fullyQualifiedNamespace("test.servicebus.windows.net")
            .build();
    assertThat(adapter).isNotNull();
    adapter.close();
  }

  @Test
  void subscribe_start_stop_and_close_with_processor() {
    var mockProcessorBuilder = mock(ServiceBusClientBuilder.ServiceBusProcessorClientBuilder.class);
    ServiceBusProcessorClient mockProcessor = mock(ServiceBusProcessorClient.class);

    when(mockProcessorBuilder.queueName(any())).thenReturn(mockProcessorBuilder);
    when(mockProcessorBuilder.processMessage(any())).thenReturn(mockProcessorBuilder);
    when(mockProcessorBuilder.processError(any())).thenReturn(mockProcessorBuilder);
    when(mockProcessorBuilder.maxConcurrentCalls(anyInt())).thenReturn(mockProcessorBuilder);
    when(mockProcessorBuilder.buildProcessorClient()).thenReturn(mockProcessor);

    try (MockedConstruction<ServiceBusClientBuilder> mocked =
        mockConstruction(
            ServiceBusClientBuilder.class,
            (mock, ctx) -> when(mock.processor()).thenReturn(mockProcessorBuilder))) {

      AzureServiceBusConsumerAdapter adapter =
          AzureServiceBusConsumerAdapter.builder().connectionString(CONN_STR).build();

      var serializer = new JacksonMessageSerializer<TestMessage>();
      TopicName topic = TopicName.of("test-queue");
      ConsumerGroup group = ConsumerGroup.of("test-group");

      adapter.subscribe(topic, group, TestMessage.class, serializer, msg -> {});
      adapter.start();
      adapter.close(); // close() calls stop() internally

      verify(mockProcessor).start();
      verify(mockProcessor).stop();
    }
  }

  @Test
  void duplicate_subscribe_logs_warning_and_ignores() {
    var mockProcessorBuilder = mock(ServiceBusClientBuilder.ServiceBusProcessorClientBuilder.class);
    ServiceBusProcessorClient mockProcessor = mock(ServiceBusProcessorClient.class);

    when(mockProcessorBuilder.queueName(any())).thenReturn(mockProcessorBuilder);
    when(mockProcessorBuilder.processMessage(any())).thenReturn(mockProcessorBuilder);
    when(mockProcessorBuilder.processError(any())).thenReturn(mockProcessorBuilder);
    when(mockProcessorBuilder.maxConcurrentCalls(anyInt())).thenReturn(mockProcessorBuilder);
    when(mockProcessorBuilder.buildProcessorClient()).thenReturn(mockProcessor);

    try (MockedConstruction<ServiceBusClientBuilder> mocked =
        mockConstruction(
            ServiceBusClientBuilder.class,
            (mock, ctx) -> when(mock.processor()).thenReturn(mockProcessorBuilder))) {

      AzureServiceBusConsumerAdapter adapter =
          AzureServiceBusConsumerAdapter.builder().connectionString(CONN_STR).build();

      var serializer = new JacksonMessageSerializer<TestMessage>();
      TopicName topic = TopicName.of("dup-queue");
      ConsumerGroup group = ConsumerGroup.of("dup-group");

      adapter.subscribe(topic, group, TestMessage.class, serializer, msg -> {});
      // Second subscribe: should log warning and return (no exception)
      adapter.subscribe(topic, group, TestMessage.class, serializer, msg -> {});
      adapter.close();
    }
  }

  @Test
  void unsubscribe_existing_processor_closes_it() {
    var mockProcessorBuilder = mock(ServiceBusClientBuilder.ServiceBusProcessorClientBuilder.class);
    ServiceBusProcessorClient mockProcessor = mock(ServiceBusProcessorClient.class);

    when(mockProcessorBuilder.queueName(any())).thenReturn(mockProcessorBuilder);
    when(mockProcessorBuilder.processMessage(any())).thenReturn(mockProcessorBuilder);
    when(mockProcessorBuilder.processError(any())).thenReturn(mockProcessorBuilder);
    when(mockProcessorBuilder.maxConcurrentCalls(anyInt())).thenReturn(mockProcessorBuilder);
    when(mockProcessorBuilder.buildProcessorClient()).thenReturn(mockProcessor);

    try (MockedConstruction<ServiceBusClientBuilder> mocked =
        mockConstruction(
            ServiceBusClientBuilder.class,
            (mock, ctx) -> when(mock.processor()).thenReturn(mockProcessorBuilder))) {

      AzureServiceBusConsumerAdapter adapter =
          AzureServiceBusConsumerAdapter.builder().connectionString(CONN_STR).build();

      var serializer = new JacksonMessageSerializer<TestMessage>();
      TopicName topic = TopicName.of("unsub-queue");
      ConsumerGroup group = ConsumerGroup.of("unsub-group");

      adapter.subscribe(topic, group, TestMessage.class, serializer, msg -> {});
      adapter.unsubscribe(topic, group);

      verify(mockProcessor).close();
    }
  }

  @Test
  void unsubscribe_nonexistent_is_noop() {
    AzureServiceBusConsumerAdapter adapter =
        AzureServiceBusConsumerAdapter.builder().connectionString(CONN_STR).build();
    // Should not throw
    adapter.unsubscribe(TopicName.of("missing"), ConsumerGroup.of("missing-group"));
    adapter.close();
  }

  @Test
  void start_and_stop_with_no_subscriptions_is_noop() {
    AzureServiceBusConsumerAdapter adapter =
        AzureServiceBusConsumerAdapter.builder().connectionString(CONN_STR).build();
    adapter.start();
    adapter.stop();
    adapter.close();
  }

  @Test
  void subscribe_and_close_with_processor() {
    var mockProcessorBuilder = mock(ServiceBusClientBuilder.ServiceBusProcessorClientBuilder.class);
    ServiceBusProcessorClient mockProcessor = mock(ServiceBusProcessorClient.class);

    when(mockProcessorBuilder.queueName(any())).thenReturn(mockProcessorBuilder);
    when(mockProcessorBuilder.processMessage(any())).thenReturn(mockProcessorBuilder);
    when(mockProcessorBuilder.processError(any())).thenReturn(mockProcessorBuilder);
    when(mockProcessorBuilder.maxConcurrentCalls(anyInt())).thenReturn(mockProcessorBuilder);
    when(mockProcessorBuilder.buildProcessorClient()).thenReturn(mockProcessor);

    try (MockedConstruction<ServiceBusClientBuilder> mocked =
        mockConstruction(
            ServiceBusClientBuilder.class,
            (mock, ctx) -> when(mock.processor()).thenReturn(mockProcessorBuilder))) {

      AzureServiceBusConsumerAdapter adapter =
          AzureServiceBusConsumerAdapter.builder().connectionString(CONN_STR).build();

      var serializer = new JacksonMessageSerializer<TestMessage>();
      TopicName topic = TopicName.of("handler-queue");
      ConsumerGroup group = ConsumerGroup.of("handler-group");

      List<MessageEnvelope<TestMessage>> received = new ArrayList<>();
      adapter.subscribe(topic, group, TestMessage.class, serializer, received::add);
      adapter.close();

      verify(mockProcessor).close();
    }
  }

  record TestMessage(String message) {}
}
