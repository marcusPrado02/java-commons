package com.marcusprado02.commons.adapters.messaging.rabbitmq;

import com.marcusprado02.commons.ports.messaging.MessageEnvelope;
import com.marcusprado02.commons.ports.messaging.MessagePublisherPort;
import com.marcusprado02.commons.ports.messaging.MessageSerializer;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RabbitMQPublisherAdapter implements MessagePublisherPort, AutoCloseable {

  private static final Logger log = LoggerFactory.getLogger(RabbitMQPublisherAdapter.class);

  private final Connection connection;
  private final Channel channel;
  private final String exchange;
  private final boolean confirmEnabled;

  private RabbitMQPublisherAdapter(
      Connection connection, Channel channel, String exchange, boolean confirmEnabled) {
    this.connection = Objects.requireNonNull(connection, "connection must not be null");
    this.channel = Objects.requireNonNull(channel, "channel must not be null");
    this.exchange = exchange == null ? "" : exchange;
    this.confirmEnabled = confirmEnabled;
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public <T> void publish(MessageEnvelope<T> message, MessageSerializer<T> serializer) {
    Objects.requireNonNull(message, "message must not be null");
    Objects.requireNonNull(serializer, "serializer must not be null");

    byte[] payload = serializer.serialize(message.payload());
    String routingKey = message.topic().value();

    AMQP.BasicProperties.Builder propsBuilder = new AMQP.BasicProperties.Builder();
    propsBuilder.contentType("application/json");
    propsBuilder.contentEncoding("UTF-8");
    propsBuilder.deliveryMode(2); // persistent
    propsBuilder.messageId(message.id().value());
    propsBuilder.timestamp(new java.util.Date(message.timestamp().toEpochMilli()));

    Map<String, Object> headers = new HashMap<>();
    message.headers().asMap().forEach((k, v) -> headers.put(k, v));
    headers.put("messageId", message.id().value());
    headers.put("timestamp", message.timestamp().toEpochMilli());
    message.partitionKey().ifPresent(key -> headers.put("partitionKey", key));
    propsBuilder.headers(headers);

    try {
      channel.basicPublish(exchange, routingKey, propsBuilder.build(), payload);

      if (confirmEnabled) {
        channel.waitForConfirmsOrDie(5000);
      }

      log.debug(
          "Published message {} to exchange {} with routing key {}",
          message.id(),
          exchange,
          routingKey);
    } catch (IOException | InterruptedException | TimeoutException ex) {
      log.error("Failed to publish message {} to exchange {}", message.id(), exchange, ex);
      throw new RuntimeException("Failed to publish message", ex);
    }
  }

  @Override
  public void close() {
    try {
      if (channel != null && channel.isOpen()) {
        channel.close();
      }
      if (connection != null && connection.isOpen()) {
        connection.close();
      }
    } catch (IOException | TimeoutException ex) {
      log.warn("Error closing RabbitMQ connection", ex);
    }
  }

  public static final class Builder {
    private String host = "localhost";
    private int port = 5672;
    private String username = "guest";
    private String password = "guest";
    private String virtualHost = "/";
    private String exchange = "";
    private boolean confirmEnabled = true;

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

    public Builder exchange(String exchange) {
      this.exchange = exchange;
      return this;
    }

    public Builder confirmEnabled(boolean confirmEnabled) {
      this.confirmEnabled = confirmEnabled;
      return this;
    }

    public RabbitMQPublisherAdapter build() {
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

        if (confirmEnabled) {
          channel.confirmSelect();
        }

        return new RabbitMQPublisherAdapter(connection, channel, exchange, confirmEnabled);
      } catch (IOException ex) {
        throw new RuntimeException("Failed to create RabbitMQ publisher", ex);
      } catch (TimeoutException ex) {
        throw new RuntimeException("Failed to create RabbitMQ publisher - timeout", ex);
      }
    }
  }
}
