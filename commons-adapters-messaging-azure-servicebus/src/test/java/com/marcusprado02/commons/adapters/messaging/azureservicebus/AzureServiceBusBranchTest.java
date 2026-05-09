package com.marcusprado02.commons.adapters.messaging.azureservicebus;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.marcusprado02.commons.ports.messaging.MessageEnvelope;
import com.marcusprado02.commons.ports.messaging.TopicName;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

class AzureServiceBusBranchTest {

  private static final String CONN_STR =
      "Endpoint=sb://test.servicebus.windows.net/;SharedAccessKeyName=test;SharedAccessKey=test";

  record TestMessage(String id, String content, Instant timestamp) {}

  // --- Publisher close(): sender.close() throws exception (catch branch) ---

  @Test
  void publisherClose_senderThrowsOnClose_noExceptionPropagated() {
    var mockSenderBuilder = mock(ServiceBusClientBuilder.ServiceBusSenderClientBuilder.class);
    ServiceBusSenderClient mockSender = mock(ServiceBusSenderClient.class);
    when(mockSenderBuilder.queueName(any())).thenReturn(mockSenderBuilder);
    when(mockSenderBuilder.buildClient()).thenReturn(mockSender);
    doThrow(new RuntimeException("close failed")).when(mockSender).close();

    try (MockedConstruction<ServiceBusClientBuilder> mocked =
        mockConstruction(
            ServiceBusClientBuilder.class,
            (mock, ctx) -> when(mock.sender()).thenReturn(mockSenderBuilder))) {

      AzureServiceBusPublisherAdapter publisher =
          AzureServiceBusPublisherAdapter.builder().connectionString(CONN_STR).build();

      var serializer = new JacksonMessageSerializer<TestMessage>();
      MessageEnvelope<TestMessage> envelope =
          MessageEnvelope.<TestMessage>builder()
              .topic(TopicName.of("q"))
              .payload(new TestMessage("1", "x", Instant.now()))
              .build();
      publisher.publish(envelope, serializer);

      publisher.close();
    }
  }

  // --- Publisher publish(): sender.sendMessage() throws exception ---

  @Test
  void publish_senderThrows_rethrowsRuntimeException() {
    var mockSenderBuilder = mock(ServiceBusClientBuilder.ServiceBusSenderClientBuilder.class);
    ServiceBusSenderClient mockSender = mock(ServiceBusSenderClient.class);
    when(mockSenderBuilder.queueName(any())).thenReturn(mockSenderBuilder);
    when(mockSenderBuilder.buildClient()).thenReturn(mockSender);
    doThrow(new RuntimeException("send failed")).when(mockSender).sendMessage(any());

    try (MockedConstruction<ServiceBusClientBuilder> mocked =
        mockConstruction(
            ServiceBusClientBuilder.class,
            (mock, ctx) -> when(mock.sender()).thenReturn(mockSenderBuilder))) {

      AzureServiceBusPublisherAdapter publisher =
          AzureServiceBusPublisherAdapter.builder().connectionString(CONN_STR).build();

      var serializer = new JacksonMessageSerializer<TestMessage>();
      MessageEnvelope<TestMessage> envelope =
          MessageEnvelope.<TestMessage>builder()
              .topic(TopicName.of("q"))
              .payload(new TestMessage("2", "y", Instant.now()))
              .build();

      assertThatThrownBy(() -> publisher.publish(envelope, serializer))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("Failed to publish message");
    }
  }
}
