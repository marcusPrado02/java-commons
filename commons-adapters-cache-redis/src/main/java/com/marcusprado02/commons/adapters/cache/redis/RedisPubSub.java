package com.marcusprado02.commons.adapters.cache.redis;

import java.util.Objects;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Redis Pub/Sub support for real-time messaging.
 *
 * <p>Provides publish/subscribe messaging patterns using Redis channels. Useful for event
 * broadcasting, real-time notifications, and inter-service communication.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Create Pub/Sub instance
 * RedisPubSub<String, OrderEvent> orderPubSub = new RedisPubSub<>(
 *     redisTemplate,
 *     messageListenerContainer,
 *     "orders.events"
 * );
 *
 * // Subscribe to channel
 * orderPubSub.subscribe(event -> {
 *     System.out.println("Received order event: " + event);
 *     processOrder(event);
 * });
 *
 * // Publish message
 * OrderEvent event = new OrderEvent("order-123", "CREATED");
 * orderPubSub.publish(event);
 * }</pre>
 *
 * @param <K> Key type (channel name)
 * @param <V> Message value type
 */
public class RedisPubSub<K, V> {

  private static final Logger logger = LoggerFactory.getLogger(RedisPubSub.class);

  private final RedisTemplate<K, V> redisTemplate;
  private final RedisMessageListenerContainer listenerContainer;
  private final String channelName;

  public RedisPubSub(
      RedisTemplate<K, V> redisTemplate,
      RedisMessageListenerContainer listenerContainer,
      String channelName) {
    this.redisTemplate = Objects.requireNonNull(redisTemplate, "redisTemplate must not be null");
    this.listenerContainer =
        Objects.requireNonNull(listenerContainer, "listenerContainer must not be null");
    this.channelName = Objects.requireNonNull(channelName, "channelName must not be null");
  }

  /**
   * Publish message to channel.
   *
   * @param message Message to publish
   */
  public void publish(V message) {
    Objects.requireNonNull(message, "message must not be null");
    try {
      redisTemplate.convertAndSend(channelName, message);
      logger.debug("Published message to channel {}", channelName);
    } catch (Exception e) {
      logger.error("Error publishing to channel {}", channelName, e);
    }
  }

  /**
   * Subscribe to channel with message handler.
   *
   * <p>The handler will be called for each message received on the channel.
   *
   * @param messageHandler Consumer to handle received messages
   */
  public void subscribe(Consumer<V> messageHandler) {
    Objects.requireNonNull(messageHandler, "messageHandler must not be null");

    MessageListener listener =
        new MessageListener() {
          @Override
          @SuppressWarnings("unchecked")
          public void onMessage(Message message, byte[] pattern) {
            try {
              V value = (V) redisTemplate.getValueSerializer().deserialize(message.getBody());
              if (value != null) {
                messageHandler.accept(value);
              }
            } catch (ClassCastException e) {
              logger.error("Error deserializing message from channel {}", channelName, e);
            } catch (Exception e) {
              logger.error("Error handling message from channel {}", channelName, e);
            }
          }
        };

    listenerContainer.addMessageListener(listener, new ChannelTopic(channelName));
    logger.info("Subscribed to channel {}", channelName);
  }

  /**
   * Unsubscribe from channel.
   *
   * <p>Note: This stops the listener container, unsubscribing from all channels. For per-channel
   * unsubscription, manage listeners separately.
   */
  public void unsubscribe() {
    try {
      listenerContainer.stop();
      logger.info("Unsubscribed from channel {}", channelName);
    } catch (Exception e) {
      logger.error("Error unsubscribing from channel {}", channelName, e);
    }
  }

  /**
   * Get the channel name.
   *
   * @return Channel name
   */
  public String getChannelName() {
    return channelName;
  }
}
