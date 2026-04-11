package com.marcusprado02.commons.adapters.messaging.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marcusprado02.commons.ports.messaging.ConsumerGroup;
import com.marcusprado02.commons.ports.messaging.MessageEnvelope;
import com.marcusprado02.commons.ports.messaging.MessageHeaders;
import com.marcusprado02.commons.ports.messaging.TopicName;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class KafkaAdapterTest {

  @Container
  private static final KafkaContainer kafka =
      new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

  private KafkaPublisherAdapter publisher;
  private KafkaConsumerAdapter consumer;
  private JacksonMessageSerializer<TestMessage> serializer;

  @BeforeEach
  void setUp() {
    String bootstrapServers = kafka.getBootstrapServers();

    publisher = KafkaPublisherAdapter.builder().bootstrapServers(bootstrapServers).build();

    consumer = KafkaConsumerAdapter.builder().bootstrapServers(bootstrapServers).build();

    serializer = new JacksonMessageSerializer<>();
  }

  @AfterEach
  void tearDown() {
    if (publisher != null) {
      publisher.close();
    }
    if (consumer != null) {
      consumer.close();
    }
  }

  @Test
  void publishes_and_consumes_message() {
    TopicName topic = TopicName.of("test-topic");
    ConsumerGroup group = ConsumerGroup.of("test-group");

    List<MessageEnvelope<TestMessage>> received = new ArrayList<>();

    consumer.subscribe(topic, group, TestMessage.class, serializer, received::add);
    consumer.start();

    TestMessage payload = new TestMessage("Hello, Kafka!");
    MessageHeaders headers =
        MessageHeaders.builder().correlationId("corr-123").causationId("cause-456").build();

    MessageEnvelope<TestMessage> envelope =
        MessageEnvelope.<TestMessage>builder()
            .topic(topic)
            .payload(payload)
            .headers(headers)
            .build();

    publisher.publish(envelope, serializer);

    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () -> {
              assertThat(received).hasSize(1);
              MessageEnvelope<TestMessage> msg = received.get(0);
              assertThat(msg.payload().message()).isEqualTo("Hello, Kafka!");
              assertThat(msg.headers().get("correlationId")).hasValue("corr-123");
              assertThat(msg.headers().get("causationId")).hasValue("cause-456");
            });
  }

  @Test
  void publishes_multiple_messages() {
    TopicName topic = TopicName.of("test-batch");
    ConsumerGroup group = ConsumerGroup.of("batch-group");

    List<MessageEnvelope<TestMessage>> received = new ArrayList<>();

    consumer.subscribe(topic, group, TestMessage.class, serializer, received::add);
    consumer.start();

    for (int i = 0; i < 5; i++) {
      TestMessage payload = new TestMessage("Message " + i);
      MessageEnvelope<TestMessage> envelope =
          MessageEnvelope.<TestMessage>builder().topic(topic).payload(payload).build();
      publisher.publish(envelope, serializer);
    }

    await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(received).hasSize(5));
  }

  @Test
  void supports_partition_key() {
    TopicName topic = TopicName.of("test-partition");
    ConsumerGroup group = ConsumerGroup.of("partition-group");

    List<MessageEnvelope<TestMessage>> received = new ArrayList<>();

    consumer.subscribe(topic, group, TestMessage.class, serializer, received::add);
    consumer.start();

    TestMessage payload = new TestMessage("Partitioned message");
    MessageEnvelope<TestMessage> envelope =
        MessageEnvelope.<TestMessage>builder()
            .topic(topic)
            .payload(payload)
            .partitionKey("user-123")
            .build();

    publisher.publish(envelope, serializer);

    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () -> {
              assertThat(received).hasSize(1);
              assertThat(received.get(0).partitionKey()).hasValue("user-123");
            });
  }

  @Test
  void subscribe_after_start_also_starts_consumer() {
    TopicName topic = TopicName.of("test-subscribe-after-start");
    ConsumerGroup group = ConsumerGroup.of("sub-after-start-group");

    KafkaConsumerAdapter lateConsumer =
        KafkaConsumerAdapter.builder()
            .bootstrapServers(kafka.getBootstrapServers())
            .pollTimeout(Duration.ofMillis(100))
            .build();

    try {
      lateConsumer.start();
      List<MessageEnvelope<TestMessage>> received = new ArrayList<>();
      lateConsumer.subscribe(topic, group, TestMessage.class, serializer, received::add);

      MessageEnvelope<TestMessage> envelope =
          MessageEnvelope.<TestMessage>builder()
              .topic(topic)
              .payload(new TestMessage("late"))
              .build();
      publisher.publish(envelope, serializer);

      await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(received).hasSize(1));
    } finally {
      lateConsumer.close();
    }
  }

  @Test
  void unsubscribe_removes_subscription() {
    TopicName topic = TopicName.of("test-unsub");
    ConsumerGroup group = ConsumerGroup.of("unsub-group");

    consumer.subscribe(topic, group, TestMessage.class, serializer, msg -> {});
    consumer.unsubscribe(topic, group);
    // No exception expected
  }

  @Test
  void stop_shuts_down_running_consumer() {
    KafkaConsumerAdapter stoppable =
        KafkaConsumerAdapter.builder().bootstrapServers(kafka.getBootstrapServers()).build();

    stoppable.start();
    stoppable.stop();
    // Second stop is a no-op
    stoppable.stop();
  }

  @Test
  void duplicate_subscribe_throws() {
    TopicName topic = TopicName.of("test-dup");
    ConsumerGroup group = ConsumerGroup.of("dup-group");

    consumer.subscribe(topic, group, TestMessage.class, serializer, msg -> {});
    assertThatThrownBy(
            () -> consumer.subscribe(topic, group, TestMessage.class, serializer, msg -> {}))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void serializer_with_custom_object_mapper() {
    JacksonMessageSerializer<TestMessage> customSerializer =
        new JacksonMessageSerializer<>(new ObjectMapper());
    byte[] bytes = customSerializer.serialize(new TestMessage("hello"));
    TestMessage result = customSerializer.deserialize(bytes, TestMessage.class);
    assertThat(result.message()).isEqualTo("hello");
  }

  @Test
  void publisher_builder_with_property() {
    // Verify builder property() and transactionalId() produce a valid builder
    // (we can't build without a real broker for transactional, so just test non-transactional)
    KafkaPublisherAdapter pub =
        KafkaPublisherAdapter.builder()
            .bootstrapServers(kafka.getBootstrapServers())
            .property("max.block.ms", 1000)
            .build();
    pub.close();
  }

  @Test
  void beginTransaction_throws_when_not_transactional() {
    KafkaPublisherAdapter pub =
        KafkaPublisherAdapter.builder().bootstrapServers(kafka.getBootstrapServers()).build();
    try {
      assertThatThrownBy(pub::beginTransaction).isInstanceOf(IllegalStateException.class);
    } finally {
      pub.close();
    }
  }

  @Test
  void commitTransaction_throws_when_not_transactional() {
    KafkaPublisherAdapter pub =
        KafkaPublisherAdapter.builder().bootstrapServers(kafka.getBootstrapServers()).build();
    try {
      assertThatThrownBy(pub::commitTransaction).isInstanceOf(IllegalStateException.class);
    } finally {
      pub.close();
    }
  }

  @Test
  void abortTransaction_throws_when_not_transactional() {
    KafkaPublisherAdapter pub =
        KafkaPublisherAdapter.builder().bootstrapServers(kafka.getBootstrapServers()).build();
    try {
      assertThatThrownBy(pub::abortTransaction).isInstanceOf(IllegalStateException.class);
    } finally {
      pub.close();
    }
  }

  @Test
  void unsubscribe_nonexistent_subscription_is_noop() {
    TopicName topic = TopicName.of("never-subscribed");
    ConsumerGroup group = ConsumerGroup.of("never-group");
    // Should not throw even if no subscription exists
    consumer.unsubscribe(topic, group);
  }

  public record TestMessage(String message) {}
}
