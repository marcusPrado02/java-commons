package com.marcusprado02.commons.adapters.messaging.rabbitmq;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marcusprado02.commons.ports.messaging.MessageEnvelope;
import com.marcusprado02.commons.ports.messaging.MessageHeaders;
import com.marcusprado02.commons.ports.messaging.TopicName;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;

class RabbitMqBranchTest {

  private static RabbitMqPublisherAdapter buildPublisher(
      Connection connection, Channel channel, String exchange, boolean confirmEnabled)
      throws Exception {
    Constructor<RabbitMqPublisherAdapter> ctor =
        RabbitMqPublisherAdapter.class.getDeclaredConstructor(
            Connection.class, Channel.class, String.class, boolean.class);
    ctor.setAccessible(true);
    return ctor.newInstance(connection, channel, exchange, confirmEnabled);
  }

  // --- Publisher close(): channel not open → skip channel.close() ---

  @Test
  void publisherClose_channelNotOpen_skipsChannelClose() throws Exception {
    Connection connection = mock(Connection.class);
    Channel channel = mock(Channel.class);
    when(channel.isOpen()).thenReturn(false);
    when(connection.isOpen()).thenReturn(true);

    RabbitMqPublisherAdapter adapter = buildPublisher(connection, channel, "", false);
    adapter.close();

    verify(channel, never()).close();
    verify(connection).close();
  }

  // --- Publisher close(): connection not open → skip connection.close() ---

  @Test
  void publisherClose_connectionNotOpen_skipsConnectionClose() throws Exception {
    Connection connection = mock(Connection.class);
    Channel channel = mock(Channel.class);
    when(channel.isOpen()).thenReturn(true);
    when(connection.isOpen()).thenReturn(false);

    RabbitMqPublisherAdapter adapter = buildPublisher(connection, channel, "", false);
    adapter.close();

    verify(channel).close();
    verify(connection, never()).close();
  }

  // --- Publisher close(): IOException thrown → swallowed ---

  @Test
  void publisherClose_ioException_noThrow() throws Exception {
    Connection connection = mock(Connection.class);
    Channel channel = mock(Channel.class);
    when(channel.isOpen()).thenReturn(true);
    doThrow(new IOException("fail")).when(channel).close();
    when(connection.isOpen()).thenReturn(true);

    RabbitMqPublisherAdapter adapter = buildPublisher(connection, channel, "", false);
    adapter.close();
  }

  // --- Publisher close(): TimeoutException thrown → swallowed ---

  @Test
  void publisherClose_timeoutException_noThrow() throws Exception {
    Connection connection = mock(Connection.class);
    Channel channel = mock(Channel.class);
    when(channel.isOpen()).thenReturn(true);
    doThrow(new TimeoutException("timeout")).when(channel).close();
    when(connection.isOpen()).thenReturn(true);

    RabbitMqPublisherAdapter adapter = buildPublisher(connection, channel, "", false);
    adapter.close();
  }

  // --- Publisher close(): channel == null → skip channel.close() ---

  @Test
  void publisherClose_nullChannel_skipsChannelClose() throws Exception {
    Connection connection = mock(Connection.class);
    Channel channel = mock(Channel.class);
    when(connection.isOpen()).thenReturn(true);

    RabbitMqPublisherAdapter adapter = buildPublisher(connection, channel, "", false);

    Field channelField = RabbitMqPublisherAdapter.class.getDeclaredField("channel");
    channelField.setAccessible(true);
    channelField.set(adapter, null);

    adapter.close();
    verify(connection).close();
  }

  // --- Publisher close(): connection == null → skip connection.close() ---

  @Test
  void publisherClose_nullConnection_skipsConnectionClose() throws Exception {
    Connection connection = mock(Connection.class);
    Channel channel = mock(Channel.class);
    when(channel.isOpen()).thenReturn(true);

    RabbitMqPublisherAdapter adapter = buildPublisher(connection, channel, "", false);

    Field connectionField = RabbitMqPublisherAdapter.class.getDeclaredField("connection");
    connectionField.setAccessible(true);
    connectionField.set(adapter, null);

    adapter.close();
    verify(channel).close();
  }

  // --- Publisher publish(): IOException thrown → RuntimeException ---

  @Test
  void publish_ioException_throwsRuntimeException() throws Exception {
    Connection connection = mock(Connection.class);
    Channel channel = mock(Channel.class);
    when(connection.isOpen()).thenReturn(true);
    when(channel.isOpen()).thenReturn(true);
    doThrow(new IOException("publish failed"))
        .when(channel)
        .basicPublish(anyString(), anyString(), any(), any(byte[].class));

    RabbitMqPublisherAdapter adapter = buildPublisher(connection, channel, "", false);

    MessageEnvelope<String> envelope =
        MessageEnvelope.<String>builder()
            .topic(TopicName.of("test"))
            .payload("hello")
            .headers(MessageHeaders.builder().build())
            .build();

    assertThatThrownBy(() -> adapter.publish(envelope, new JacksonMessageSerializer<>()))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Failed to publish message");
  }

  // --- Consumer stop(): IOException thrown → swallowed ---

  @Test
  @SuppressWarnings("unchecked")
  void consumerStop_ioException_noThrow() throws Exception {
    RabbitMqConsumerAdapter consumer = RabbitMqConsumerAdapter.builder().build();

    Field connectionsField = RabbitMqConsumerAdapter.class.getDeclaredField("connections");
    connectionsField.setAccessible(true);
    Map<String, Connection> connections = (Map<String, Connection>) connectionsField.get(consumer);

    Connection mockConn = mock(Connection.class);
    doThrow(new IOException("fail")).when(mockConn).close();
    connections.put("test-key", mockConn);

    consumer.stop();
  }
}
