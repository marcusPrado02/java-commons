package com.marcusprado02.commons.adapters.messaging.azureservicebus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.marcusprado02.commons.ports.messaging.MessageEnvelope;
import com.marcusprado02.commons.ports.messaging.MessageHeaders;
import com.marcusprado02.commons.ports.messaging.MessageId;
import com.marcusprado02.commons.ports.messaging.MessageSerializer;
import com.marcusprado02.commons.ports.messaging.TopicName;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AzureServiceBusPublisherAdapterTest {

  private ServiceBusClientBuilder mockClientBuilder;
  private ServiceBusClientBuilder.ServiceBusSenderClientBuilder mockSenderBuilder;
  private ServiceBusSenderClient mockSender;
  private MessageSerializer<TestMessage> serializer;
  private AzureServiceBusPublisherAdapter publisher;

  @BeforeEach
  void setUp() {
    mockClientBuilder = mock(ServiceBusClientBuilder.class);
    mockSenderBuilder = mock(ServiceBusClientBuilder.ServiceBusSenderClientBuilder.class);
    mockSender = mock(ServiceBusSenderClient.class);

    when(mockClientBuilder.sender()).thenReturn(mockSenderBuilder);
    when(mockSenderBuilder.queueName(any())).thenReturn(mockSenderBuilder);
    when(mockSenderBuilder.buildClient()).thenReturn(mockSender);

    serializer = new JacksonMessageSerializer<>();
  }

  @Test
  void shouldPublishMessage() {
    // This test would require actual Azure Service Bus or more sophisticated mocking
    // For now, we just verify the builder works
    AzureServiceBusPublisherAdapter adapter =
        AzureServiceBusPublisherAdapter.builder()
            .connectionString(
                "Endpoint=sb://test.servicebus.windows.net/;SharedAccessKeyName=test;SharedAccessKey=test")
            .build();

    assertThat(adapter).isNotNull();
    adapter.close();
  }

  @Test
  void shouldFailWithoutConnectionStringOrNamespace() {
    assertThatThrownBy(() -> AzureServiceBusPublisherAdapter.builder().build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Either connectionString or fullyQualifiedNamespace must be provided");
  }

  @Test
  void shouldBuildWithConnectionString() {
    AzureServiceBusPublisherAdapter adapter =
        AzureServiceBusPublisherAdapter.builder()
            .connectionString(
                "Endpoint=sb://test.servicebus.windows.net/;SharedAccessKeyName=test;SharedAccessKey=test")
            .build();

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

  record TestMessage(String id, String content, Instant timestamp) {}
}
