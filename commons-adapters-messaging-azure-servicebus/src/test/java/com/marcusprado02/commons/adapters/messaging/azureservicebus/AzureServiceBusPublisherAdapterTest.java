package com.marcusprado02.commons.adapters.messaging.azureservicebus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.marcusprado02.commons.ports.messaging.MessageEnvelope;
import com.marcusprado02.commons.ports.messaging.TopicName;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

class AzureServiceBusPublisherAdapterTest {

  private static final String CONN_STR =
      "Endpoint=sb://test.servicebus.windows.net/;SharedAccessKeyName=test;SharedAccessKey=test";

  @Test
  void shouldFailWithoutConnectionStringOrNamespace() {
    assertThatThrownBy(() -> AzureServiceBusPublisherAdapter.builder().build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(
            "Either connectionString or fullyQualifiedNamespace must be provided");
  }

  @Test
  void shouldBuildWithConnectionString() {
    AzureServiceBusPublisherAdapter adapter =
        AzureServiceBusPublisherAdapter.builder().connectionString(CONN_STR).build();
    assertThat(adapter).isNotNull();
    adapter.close();
  }

  @Test
  void shouldBuildWithManagedIdentity() {
    AzureServiceBusPublisherAdapter adapter =
        AzureServiceBusPublisherAdapter.builder()
            .fullyQualifiedNamespace("test.servicebus.windows.net")
            .build();
    assertThat(adapter).isNotNull();
    adapter.close();
  }

  @Test
  void publish_sends_message_via_sender() {
    var mockSenderBuilder = mock(ServiceBusClientBuilder.ServiceBusSenderClientBuilder.class);
    ServiceBusSenderClient mockSender = mock(ServiceBusSenderClient.class);

    when(mockSenderBuilder.queueName(any())).thenReturn(mockSenderBuilder);
    when(mockSenderBuilder.buildClient()).thenReturn(mockSender);

    try (MockedConstruction<ServiceBusClientBuilder> mocked =
        mockConstruction(
            ServiceBusClientBuilder.class,
            (mock, ctx) -> when(mock.sender()).thenReturn(mockSenderBuilder))) {

      AzureServiceBusPublisherAdapter publisher =
          AzureServiceBusPublisherAdapter.builder().connectionString(CONN_STR).build();

      var serializer = new JacksonMessageSerializer<TestMessage>();
      MessageEnvelope<TestMessage> envelope =
          MessageEnvelope.<TestMessage>builder()
              .topic(TopicName.of("test-queue"))
              .payload(new TestMessage("1", "hello", Instant.now()))
              .build();

      publisher.publish(envelope, serializer);
      verify(mockSender).sendMessage(any());

      publisher.close();
      verify(mockSender).close();
    }
  }

  @Test
  void publish_with_partition_key_sets_session() {
    var mockSenderBuilder = mock(ServiceBusClientBuilder.ServiceBusSenderClientBuilder.class);
    ServiceBusSenderClient mockSender = mock(ServiceBusSenderClient.class);

    when(mockSenderBuilder.queueName(any())).thenReturn(mockSenderBuilder);
    when(mockSenderBuilder.buildClient()).thenReturn(mockSender);

    try (MockedConstruction<ServiceBusClientBuilder> mocked =
        mockConstruction(
            ServiceBusClientBuilder.class,
            (mock, ctx) -> when(mock.sender()).thenReturn(mockSenderBuilder))) {

      AzureServiceBusPublisherAdapter publisher =
          AzureServiceBusPublisherAdapter.builder().connectionString(CONN_STR).build();

      var serializer = new JacksonMessageSerializer<TestMessage>();
      MessageEnvelope<TestMessage> envelope =
          MessageEnvelope.<TestMessage>builder()
              .topic(TopicName.of("test-queue"))
              .payload(new TestMessage("2", "partitioned", Instant.now()))
              .partitionKey("session-key")
              .build();

      publisher.publish(envelope, serializer);
      verify(mockSender).sendMessage(any());

      publisher.close();
    }
  }

  @Test
  void close_with_no_senders_is_noop() {
    AzureServiceBusPublisherAdapter publisher =
        AzureServiceBusPublisherAdapter.builder().connectionString(CONN_STR).build();
    publisher.close();
  }

  record TestMessage(String id, String content, Instant timestamp) {}
}
