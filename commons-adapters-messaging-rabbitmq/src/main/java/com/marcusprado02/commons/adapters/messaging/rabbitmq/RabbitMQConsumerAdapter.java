package com.marcusprado02.commons.adapters.messaging.rabbitmq;

import com.marcusprado02.commons.ports.messaging.ConsumerGroup;
import com.marcusprado02.commons.ports.messaging.MessageConsumerPort;
import com.marcusprado02.commons.ports.messaging.MessageEnvelope;
import com.marcusprado02.commons.ports.messaging.MessageHeaders;
import com.marcusprado02.commons.ports.messaging.MessageId;
import com.marcusprado02.commons.ports.messaging.MessageSerializer;
import com.marcusprado02.commons.ports.messaging.TopicName;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RabbitMQConsumerAdapter implements MessageConsumerPort, AutoCloseable {

  private static final Logger log = LoggerFactory.getLogger(RabbitMQConsumerAdapter.class);

  private final String host;
  private final int port;
  private final String username;
  private final String password;
  private final String virtualHost;
  private final Map<String, SubscriptionEntry<?>> subscriptions = new ConcurrentHashMap<>();
  private final Map<String, Connection> connections = new ConcurrentHashMap<>();

  private RabbitMQConsumerAdapter(
      String host, int port, String username, String password, String virtualHost) {
    this.host = host;
    this.port = port;
    this.username = username;
    this.password = password;
    this.virtualHost = virtualHost;
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public <T> void subscribe(
      TopicName topic,
      ConsumerGroup group,
      Class<T> messageType,
      MessageSerializer<T> serializer,
      Consumer<MessageEnvelope<T>> handler) {
    Objects.requireNonNull(topic, "topic must not be null");
    Objects.requireNonNull(group, "group must not be null");
    Objects.requireNonNull(messageType, "messageType must not be null");
    Objects.requireNonNull(serializer, "serializer must not be null");
    Objects.requireNonNull(handler, "handler must not be null");

    String key = subscriptionKey(topic, group);
    if (subscriptions.containsKey(key)) {
      throw new IllegalStateException("Already subscribed to " + topic + " with group " + group);
    }

    SubscriptionEntry<T> entry =
        new SubscriptionEntry<>(topic, group, messageType, serializer, handler);
    subscriptions.put(key, entry);
  }

  @Override
  public void unsubscribe(TopicName topic, ConsumerGroup group) {
    String key = subscriptionKey(topic, group);
    SubscriptionEntry<?> entry = subscriptions.remove(key);
    if (entry != null) {
      String connKey = connectionKey(topic, group);
      Connection conn = connections.remove(connKey);
      if (conn != null) {
        try {
          conn.close();
        } catch (IOException ex) {
          log.warn("Error closing connection for {}", key, ex);
        }
      }
    }
  }

  @Override
  public void start() {
    log.info("Starting RabbitMQ consumer adapter");
    subscriptions.values().forEach(this::startConsumer);
  }

  @Override
  public void stop() {
    log.info("Stopping RabbitMQ consumer adapter");
    connections
        .values()
        .forEach(
            conn -> {
              try {
                conn.close();
              } catch (IOException ex) {
                log.warn("Error closing connection", ex);
              }
            });
    connections.clear();
  }

  @Override
  public void close() {
    stop();
  }

  private <T> void startConsumer(SubscriptionEntry<T> entry) {
    try {
      ConnectionFactory factory = new ConnectionFactory();
      factory.setHost(host);
      factory.setPort(port);
      factory.setUsername(username);
      factory.setPassword(password);
      factory.setVirtualHost(virtualHost);
      factory.setAutomaticRecoveryEnabled(true);
      factory.setNetworkRecoveryInterval(5000);

      Connection connection = factory.newConnection();
      Channel channel = connection.createChannel();

      String queueName = entry.topic.value();
      channel.queueDeclare(queueName, true, false, false, null);

      channel.basicQos(1);

      com.rabbitmq.client.Consumer consumer =
          new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(
                String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
                throws IOException {
              try {
                MessageEnvelope<T> message = toEnvelope(body, properties, entry);
                entry.handler.accept(message);
                channel.basicAck(envelope.getDeliveryTag(), false);
              } catch (Exception ex) {
                log.error("Error processing message", ex);
                channel.basicNack(envelope.getDeliveryTag(), false, true);
              }
            }
          };

      channel.basicConsume(queueName, false, consumer);

      connections.put(connectionKey(entry.topic, entry.group), connection);
      log.info("Subscribed to {} with group {}", entry.topic, entry.group);
    } catch (IOException ex) {
      log.error("Failed to start consumer for topic {}", entry.topic, ex);
      throw new RuntimeException("Failed to start consumer", ex);
    } catch (TimeoutException ex) {
      log.error("Failed to start consumer for topic {} - timeout", entry.topic, ex);
      throw new RuntimeException("Failed to start consumer - timeout", ex);
    }
  }

  private <T> MessageEnvelope<T> toEnvelope(
      byte[] body, AMQP.BasicProperties properties, SubscriptionEntry<T> entry) {
    T payload = entry.serializer.deserialize(body, entry.messageType);

    MessageHeaders.Builder headersBuilder = MessageHeaders.builder();
    if (properties.getHeaders() != null) {
      properties
          .getHeaders()
          .forEach(
              (k, v) -> {
                if (v != null) {
                  headersBuilder.header(k, v.toString());
                }
              });
    }

    String messageIdValue =
        properties.getMessageId() != null
            ? properties.getMessageId()
            : extractStringHeader(properties, "messageId").orElse("unknown");
    MessageId messageId = MessageId.of(messageIdValue);

    long timestampMillis =
        properties.getTimestamp() != null
            ? properties.getTimestamp().getTime()
            : System.currentTimeMillis();
    Instant timestamp = Instant.ofEpochMilli(timestampMillis);

    String partitionKey = extractStringHeader(properties, "partitionKey").orElse(null);

    return MessageEnvelope.<T>builder()
        .id(messageId)
        .topic(entry.topic)
        .payload(payload)
        .headers(headersBuilder.build())
        .timestamp(timestamp)
        .partitionKey(partitionKey)
        .build();
  }

  private java.util.Optional<String> extractStringHeader(
      AMQP.BasicProperties properties, String key) {
    if (properties.getHeaders() == null) {
      return java.util.Optional.empty();
    }
    Object value = properties.getHeaders().get(key);
    if (value == null) {
      return java.util.Optional.empty();
    }
    if (value instanceof byte[]) {
      return java.util.Optional.of(new String((byte[]) value, StandardCharsets.UTF_8));
    }
    return java.util.Optional.of(value.toString());
  }

  private String subscriptionKey(TopicName topic, ConsumerGroup group) {
    return topic.value() + ":" + group.value();
  }

  private String connectionKey(TopicName topic, ConsumerGroup group) {
    return subscriptionKey(topic, group);
  }

  private static final class SubscriptionEntry<T> {
    private final TopicName topic;
    private final ConsumerGroup group;
    private final Class<T> messageType;
    private final MessageSerializer<T> serializer;
    private final Consumer<MessageEnvelope<T>> handler;

    private SubscriptionEntry(
        TopicName topic,
        ConsumerGroup group,
        Class<T> messageType,
        MessageSerializer<T> serializer,
        Consumer<MessageEnvelope<T>> handler) {
      this.topic = topic;
      this.group = group;
      this.messageType = messageType;
      this.serializer = serializer;
      this.handler = handler;
    }
  }

  public static final class Builder {
    private String host = "localhost";
    private int port = 5672;
    private String username = "guest";
    private String password = "guest";
    private String virtualHost = "/";

    private Builder() {}

    public Builder host(String host) {
      this.host = host;
      return this;
    }

    public Builder port(int port) {
      this.port = port;
      return this;
    }

    public Builder username(String username) {
      this.username = username;
      return this;
    }

    public Builder password(String password) {
      this.password = password;
      return this;
    }

    public Builder virtualHost(String virtualHost) {
      this.virtualHost = virtualHost;
      return this;
    }

    public RabbitMQConsumerAdapter build() {
      return new RabbitMQConsumerAdapter(host, port, username, password, virtualHost);
    }
  }
}
