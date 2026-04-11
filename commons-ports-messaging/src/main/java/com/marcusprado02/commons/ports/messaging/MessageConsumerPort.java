package com.marcusprado02.commons.ports.messaging;

import java.util.function.Consumer;

/** Port for subscribing to and consuming messages from topics. */
public interface MessageConsumerPort {

  <T> void subscribe(
      TopicName topic,
      ConsumerGroup group,
      Class<T> messageType,
      MessageSerializer<T> serializer,
      Consumer<MessageEnvelope<T>> handler);

  void unsubscribe(TopicName topic, ConsumerGroup group);

  void start();

  void stop();
}
