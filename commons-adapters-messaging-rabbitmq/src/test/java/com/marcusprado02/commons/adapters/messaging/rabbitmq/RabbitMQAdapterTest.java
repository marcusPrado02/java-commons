package com.marcusprado02.commons.adapters.messaging.rabbitmq;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

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
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class RabbitMQAdapterTest {

  @Container
  private static final RabbitMQContainer rabbitMQ =
      new RabbitMQContainer(DockerImageName.parse("rabbitmq:3.13-alpine"));

  private RabbitMQPublisherAdapter publisher;
  private RabbitMQConsumerAdapter consumer;
  private JacksonMessageSerializer<TestMessage> serializer;

  @BeforeEach
  void setUp() {
    String host = rabbitMQ.getHost();
    int port = rabbitMQ.getAmqpPort();

    publisher =
        RabbitMQPublisherAdapter.builder()
            .host(host)
            .port(port)
            .username("guest")
            .password("guest")
            .build();

    consumer =
        RabbitMQConsumerAdapter.builder()
            .host(host)
            .port(port)
            .username("guest")
            .password("guest")
            .build();

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

    // Give consumer time to setup
    await().atMost(Duration.ofSeconds(2)).pollDelay(Duration.ofMillis(500)).until(() -> true);

    TestMessage payload = new TestMessage("Hello, RabbitMQ!");
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
              assertThat(msg.payload().message()).isEqualTo("Hello, RabbitMQ!");
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

    await().atMost(Duration.ofSeconds(2)).pollDelay(Duration.ofMillis(500)).until(() -> true);

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

    await().atMost(Duration.ofSeconds(2)).pollDelay(Duration.ofMillis(500)).until(() -> true);

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

  public record TestMessage(String message) {}
}
