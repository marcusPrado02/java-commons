package com.marcusprado02.commons.adapters.messaging.azureservicebus;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.marcusprado02.commons.ports.messaging.ConsumerGroup;
import com.marcusprado02.commons.ports.messaging.MessageConsumerPort;
import com.marcusprado02.commons.ports.messaging.MessageEnvelope;
import com.marcusprado02.commons.ports.messaging.MessageHeaders;
import com.marcusprado02.commons.ports.messaging.MessageId;
import com.marcusprado02.commons.ports.messaging.MessageSerializer;
import com.marcusprado02.commons.ports.messaging.TopicName;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AzureServiceBusConsumerAdapter implements MessageConsumerPort, AutoCloseable {

  private static final Logger log = LoggerFactory.getLogger(AzureServiceBusConsumerAdapter.class);

  private final Map<SubscriptionKey, ServiceBusProcessorClient> processors = new HashMap<>();
  private final ServiceBusClientBuilder clientBuilder;

  private AzureServiceBusConsumerAdapter(ServiceBusClientBuilder clientBuilder) {
    this.clientBuilder = Objects.requireNonNull(clientBuilder, "clientBuilder must not be null");
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public <T> void subscribe(
      TopicName topic,
      ConsumerGroup group,
      Class<T> type,
      MessageSerializer<T> serializer,
      Consumer<MessageEnvelope<T>> handler) {
    Objects.requireNonNull(topic, "topic must not be null");
    Objects.requireNonNull(type, "type must not be null");
    Objects.requireNonNull(serializer, "serializer must not be null");
    Objects.requireNonNull(handler, "handler must not be null");

    SubscriptionKey key = new SubscriptionKey(topic, group);

    if (processors.containsKey(key)) {
      log.warn("Already subscribed to queue {} with subscription {}", topic, group);
      return;
    }

    ServiceBusProcessorClient processor =
        clientBuilder
            .processor()
            .queueName(topic.value())
            .processMessage(context -> processMessage(context, type, serializer, handler))
            .processError(this::processError)
            .maxConcurrentCalls(1)
            .buildProcessorClient();

    processors.put(key, processor);
    log.info("Subscribed to queue {} with subscription {}", topic, group);
  }

  private <T> void processMessage(
      ServiceBusReceivedMessageContext context,
      Class<T> type,
      MessageSerializer<T> serializer,
      Consumer<MessageEnvelope<T>> handler) {
    ServiceBusReceivedMessage message = context.getMessage();
    try {
      byte[] body = message.getBody().toBytes();
      T payload = serializer.deserialize(body, type);

      MessageHeaders.Builder headersBuilder = MessageHeaders.builder();
      message.getApplicationProperties().forEach((k, v) -> headersBuilder.header(k, v.toString()));

      MessageEnvelope.Builder<T> envelopeBuilder =
          MessageEnvelope.<T>builder()
              .id(MessageId.of(message.getMessageId()))
              .payload(payload)
              .headers(headersBuilder.build())
              .timestamp(
                  Optional.ofNullable(message.getEnqueuedTime())
                      .map(offsetDateTime -> offsetDateTime.toInstant())
                      .orElse(Instant.now()));

      if (message.getPartitionKey() != null) {
        envelopeBuilder.partitionKey(message.getPartitionKey());
      }

      MessageEnvelope<T> envelope = envelopeBuilder.build();

      handler.accept(envelope);
      context.complete();
      log.debug("Processed message {}", message.getMessageId());
    } catch (Exception ex) {
      log.error("Error processing message {}", message.getMessageId(), ex);
      context.abandon(); // Requeue the message
    }
  }

  private void processError(ServiceBusErrorContext context) {
    log.error(
        "Error processing messages from queue {}: {}",
        context.getEntityPath(),
        context.getException().getMessage(),
        context.getException());
  }

  @Override
  public void start() {
    processors
        .values()
        .forEach(
            processor -> {
              try {
                processor.start();
                log.info("Started processor");
              } catch (Exception ex) {
                log.error("Failed to start processor", ex);
                throw new RuntimeException("Failed to start processor", ex);
              }
            });
  }

  @Override
  public void stop() {
    processors
        .values()
        .forEach(
            processor -> {
              try {
                processor.stop();
                log.info("Stopped processor");
              } catch (Exception ex) {
                log.warn("Error stopping processor", ex);
              }
            });
  }

  @Override
  public void unsubscribe(TopicName topic, ConsumerGroup group) {
    Objects.requireNonNull(topic, "topic must not be null");
    SubscriptionKey key = new SubscriptionKey(topic, group);
    ServiceBusProcessorClient processor = processors.remove(key);
    if (processor != null) {
      try {
        processor.close();
        log.info("Unsubscribed from queue {} with subscription {}", topic, group);
      } catch (Exception ex) {
        log.error("Error closing processor for queue {}", topic, ex);
      }
    }
  }

  @Override
  public void close() {
    stop();
    processors
        .values()
        .forEach(
            processor -> {
              try {
                processor.close();
              } catch (Exception ex) {
                log.warn("Error closing processor", ex);
              }
            });
    processors.clear();
  }

  private record SubscriptionKey(TopicName topic, ConsumerGroup group) {}

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

    public AzureServiceBusConsumerAdapter build() {
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

      return new AzureServiceBusConsumerAdapter(clientBuilder);
    }
  }
}
