package com.marcusprado02.commons.adapters.messaging.azureservicebus;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.marcusprado02.commons.ports.messaging.MessageEnvelope;
import com.marcusprado02.commons.ports.messaging.MessagePublisherPort;
import com.marcusprado02.commons.ports.messaging.MessageSerializer;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AzureServiceBusPublisherAdapter implements MessagePublisherPort, AutoCloseable {

  private static final Logger log = LoggerFactory.getLogger(AzureServiceBusPublisherAdapter.class);

  private final Map<String, ServiceBusSenderClient> senders = new HashMap<>();
  private final ServiceBusClientBuilder clientBuilder;

  private AzureServiceBusPublisherAdapter(ServiceBusClientBuilder clientBuilder) {
    this.clientBuilder = Objects.requireNonNull(clientBuilder, "clientBuilder must not be null");
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public <T> void publish(MessageEnvelope<T> message, MessageSerializer<T> serializer) {
    Objects.requireNonNull(message, "message must not be null");
    Objects.requireNonNull(serializer, "serializer must not be null");

    String queueName = message.topic().value();
    ServiceBusSenderClient sender = getOrCreateSender(queueName);

    byte[] payload = serializer.serialize(message.payload());

    ServiceBusMessage sbMessage = new ServiceBusMessage(payload);
    sbMessage.setContentType("application/json");
    sbMessage.setMessageId(message.id().value());
    sbMessage.setTimeToLive(Duration.ofHours(24));

    // Add custom headers
    Map<String, Object> applicationProperties = sbMessage.getApplicationProperties();
    message.headers().asMap().forEach(applicationProperties::put);
    applicationProperties.put("messageId", message.id().value());
    applicationProperties.put("timestamp", message.timestamp().toEpochMilli());
    message.partitionKey().ifPresent(sbMessage::setPartitionKey);

    // Session support
    message.partitionKey().ifPresent(sbMessage::setSessionId);

    try {
      sender.sendMessage(sbMessage);
      log.debug(
          "Published message {} to queue {} with session {}",
          message.id(),
          queueName,
          sbMessage.getSessionId());
    } catch (Exception ex) {
      log.error("Failed to publish message {} to queue {}", message.id(), queueName, ex);
      throw new RuntimeException("Failed to publish message", ex);
    }
  }

  private ServiceBusSenderClient getOrCreateSender(String queueName) {
    return senders.computeIfAbsent(
        queueName, key -> clientBuilder.sender().queueName(key).buildClient());
  }

  @Override
  public void close() {
    senders
        .values()
        .forEach(
            sender -> {
              try {
                sender.close();
              } catch (Exception ex) {
                log.warn("Error closing sender", ex);
              }
            });
    senders.clear();
  }

  public static final class Builder {
    private String connectionString;
    private String fullyQualifiedNamespace;
    private boolean useManagedIdentity = false;

    private Builder() {}

    public Builder connectionString(String connectionString) {
      this.connectionString = connectionString;
      return this;
    }

    public Builder fullyQualifiedNamespace(String fullyQualifiedNamespace) {
      this.fullyQualifiedNamespace = fullyQualifiedNamespace;
      this.useManagedIdentity = true;
      return this;
    }

    public AzureServiceBusPublisherAdapter build() {
      ServiceBusClientBuilder clientBuilder = new ServiceBusClientBuilder();

      if (useManagedIdentity && fullyQualifiedNamespace != null) {
        clientBuilder
            .fullyQualifiedNamespace(fullyQualifiedNamespace)
            .credential(new com.azure.identity.DefaultAzureCredentialBuilder().build());
      } else if (connectionString != null) {
        clientBuilder.connectionString(connectionString);
      } else {
        throw new IllegalArgumentException(
            "Either connectionString or fullyQualifiedNamespace must be provided");
      }

      return new AzureServiceBusPublisherAdapter(clientBuilder);
    }
  }
}
