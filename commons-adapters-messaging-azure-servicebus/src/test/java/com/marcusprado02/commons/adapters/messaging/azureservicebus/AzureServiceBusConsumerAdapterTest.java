package com.marcusprado02.commons.adapters.messaging.azureservicebus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AzureServiceBusConsumerAdapterTest {

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
        AzureServiceBusConsumerAdapter.builder()
            .connectionString(
                "Endpoint=sb://test.servicebus.windows.net/;SharedAccessKeyName=test;SharedAccessKey=test")
            .build();

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
}
