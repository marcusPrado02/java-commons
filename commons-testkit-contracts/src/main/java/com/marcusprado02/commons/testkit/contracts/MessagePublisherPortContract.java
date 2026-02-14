package com.marcusprado02.commons.testkit.contracts;

import com.marcusprado02.commons.ports.messaging.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Base contract test for {@link MessagePublisherPort} implementations.
 *
 * <p>Extend this class to verify that your messaging implementation correctly follows the
 * MessagePublisherPort contract.
 *
 * <p>Example:
 *
 * <pre>{@code
 * class KafkaPublisherContractTest extends MessagePublisherPortContract<String> {
 *   @Override
 *   protected MessagePublisherPort createPublisher() {
 *     return new KafkaMessagePublisher(kafkaTemplate);
 *   }
 *
 *   @Override
 *   protected MessageSerializer<String> createSerializer() {
 *     return new JacksonMessageSerializer<>(String.class);
 *   }
 *
 *   @Override
 *   protected TopicName getTestTopic() {
 *     return TopicName.of("test-topic");
 *   }
 *
 *   @Override
 *   protected String createTestPayload() {
 *     return "test message";
 *   }
 * }
 * }</pre>
 *
 * @param <T> Message payload type
 */
public abstract class MessagePublisherPortContract<T> {

  protected MessagePublisherPort publisher;

  /**
   * Create the message publisher instance to be tested.
   *
   * @return message publisher implementation
   */
  protected abstract MessagePublisherPort createPublisher();

  /**
   * Create a serializer for test messages.
   *
   * @return message serializer
   */
  protected abstract MessageSerializer<T> createSerializer();

  /**
   * Get the topic name for testing.
   *
   * @return topic name
   */
  protected abstract TopicName getTestTopic();

  /**
   * Create a test payload.
   *
   * @return test payload
   */
  protected abstract T createTestPayload();

  /**
   * Consume messages from topic for verification (optional, override if available).
   *
   * @param topic topic to consume from
   * @param timeout max wait time
   * @return list of consumed messages
   */
  protected List<T> consumeMessages(TopicName topic, Duration timeout) {
    // Override this method if you can consume messages for verification
    return new ArrayList<>();
  }

  @BeforeEach
  void setUp() {
    publisher = createPublisher();
  }

  @Test
  @DisplayName("Should publish message successfully")
  void shouldPublishMessage() {
    // Given
    T payload = createTestPayload();
    MessageEnvelope<T> envelope =
        MessageEnvelope.<T>builder().topic(getTestTopic()).payload(payload).build();

    MessageSerializer<T> serializer = createSerializer();

    // When/Then - should not throw exception
    publisher.publish(envelope, serializer);
  }

  @Test
  @DisplayName("Should publish message with simple API")
  void shouldPublishWithSimpleApi() {
    // Given
    T payload = createTestPayload();
    TopicName topic = getTestTopic();
    MessageSerializer<T> serializer = createSerializer();

    // When/Then - should not throw exception
    publisher.publish(topic, payload, serializer);
  }

  @Test
  @DisplayName("Should publish batch of messages")
  void shouldPublishBatch() {
    // Given
    List<MessageEnvelope<T>> messages =
        List.of(
            MessageEnvelope.<T>builder().topic(getTestTopic()).payload(createTestPayload()).build(),
            MessageEnvelope.<T>builder()
                .topic(getTestTopic())
                .payload(createTestPayload())
                .build());
    MessageSerializer<T> serializer = createSerializer();

    // When/Then - should not throw exception
    publisher.publishBatch(messages, serializer);
  }

  @Test
  @DisplayName("Should publish message with headers")
  void shouldPublishWithHeaders() {
    // Given
    T payload = createTestPayload();
    MessageHeaders headers = MessageHeaders.builder().header("X-Test-Header", "test-value").build();

    MessageEnvelope<T> envelope =
        MessageEnvelope.<T>builder()
            .topic(getTestTopic())
            .payload(payload)
            .headers(headers)
            .build();

    MessageSerializer<T> serializer = createSerializer();

    // When/Then - should not throw exception
    publisher.publish(envelope, serializer);
  }

  @Test
  @DisplayName("Should publish message with message ID")
  void shouldPublishWithMessageId() {
    // Given
    T payload = createTestPayload();
    MessageId messageId = MessageId.random();

    MessageEnvelope<T> envelope =
        MessageEnvelope.<T>builder().topic(getTestTopic()).payload(payload).id(messageId).build();

    MessageSerializer<T> serializer = createSerializer();

    // When/Then - should not throw exception
    publisher.publish(envelope, serializer);
  }
}
