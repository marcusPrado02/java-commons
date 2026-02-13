package com.marcusprado02.commons.ports.messaging;

import java.util.List;

public interface MessagePublisherPort {

  <T> void publish(MessageEnvelope<T> message, MessageSerializer<T> serializer);

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
