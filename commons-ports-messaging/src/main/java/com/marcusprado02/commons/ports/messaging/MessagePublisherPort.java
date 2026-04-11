package com.marcusprado02.commons.ports.messaging;

import java.util.List;

/** Port for publishing messages to topics. */
public interface MessagePublisherPort {

  <T> void publish(MessageEnvelope<T> message, MessageSerializer<T> serializer);

  /**
   * Publishes a message built from topic and payload with default envelope.
   *
   * @param <T> payload type
   * @param topic the topic name
   * @param payload the message payload
   * @param serializer the message serializer
   */
  default <T> void publish(TopicName topic, T payload, MessageSerializer<T> serializer) {
    MessageEnvelope<T> envelope =
        MessageEnvelope.<T>builder().topic(topic).payload(payload).build();
    publish(envelope, serializer);
  }

  default <T> void publishBatch(
      List<MessageEnvelope<T>> messages, MessageSerializer<T> serializer) {
    messages.forEach(msg -> publish(msg, serializer));
  }
}
