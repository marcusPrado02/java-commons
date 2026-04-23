package com.marcusprado02.commons.ports.messaging;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class MessagingExtendedTest {

  // --- ConsumerGroup ---

  @Test
  void consumerGroup_of_stores_value() {
    ConsumerGroup g = ConsumerGroup.of("my-group");
    assertEquals("my-group", g.value());
    assertEquals("my-group", g.toString());
  }

  @Test
  void consumerGroup_null_throws() {
    assertThrows(NullPointerException.class, () -> ConsumerGroup.of(null));
  }

  @Test
  void consumerGroup_blank_throws() {
    assertThrows(IllegalArgumentException.class, () -> ConsumerGroup.of("   "));
  }

  @Test
  void consumerGroup_equals_same_value() {
    ConsumerGroup a = ConsumerGroup.of("g");
    ConsumerGroup b = ConsumerGroup.of("g");
    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
    assertEquals(a, a);
    assertNotEquals(a, null);
    assertNotEquals(a, ConsumerGroup.of("other"));
    assertNotEquals(a, "g");
  }

  // --- MessageEnvelope ---

  @Test
  void messageEnvelope_builder_full() {
    Instant now = Instant.now();
    MessageId id = MessageId.of("msg-1");
    TopicName topic = TopicName.of("events.orders");
    MessageHeaders headers = MessageHeaders.of(java.util.Map.of("k", "v"));

    MessageEnvelope<String> env =
        MessageEnvelope.<String>builder()
            .id(id)
            .topic(topic)
            .payload("hello")
            .headers(headers)
            .timestamp(now)
            .partitionKey("pk-1")
            .build();

    assertEquals(id, env.id());
    assertEquals(topic, env.topic());
    assertEquals("hello", env.payload());
    assertEquals(headers, env.headers());
    assertEquals(now, env.timestamp());
    assertTrue(env.partitionKey().isPresent());
    assertEquals("pk-1", env.partitionKey().get());
  }

  @Test
  void messageEnvelope_builder_null_id_generates_random() {
    TopicName topic = TopicName.of("t");
    MessageEnvelope<String> env =
        MessageEnvelope.<String>builder().topic(topic).payload("p").build();
    assertNotNull(env.id());
  }

  @Test
  void messageEnvelope_builder_null_timestamp_defaults_to_now() {
    Instant before = Instant.now();
    TopicName topic = TopicName.of("t");
    MessageEnvelope<String> env =
        MessageEnvelope.<String>builder().topic(topic).payload("p").build();
    assertFalse(env.timestamp().isBefore(before));
  }

  @Test
  void messageEnvelope_null_partition_key_returns_empty() {
    TopicName topic = TopicName.of("t");
    MessageEnvelope<String> env =
        MessageEnvelope.<String>builder().topic(topic).payload("p").build();
    assertFalse(env.partitionKey().isPresent());
  }

  @Test
  void messageEnvelope_null_topic_throws() {
    assertThrows(
        NullPointerException.class,
        () -> MessageEnvelope.<String>builder().topic(null).payload("p").build());
  }

  @Test
  void messageEnvelope_null_payload_throws() {
    assertThrows(
        NullPointerException.class,
        () -> MessageEnvelope.<String>builder().topic(TopicName.of("t")).payload(null).build());
  }

  @Test
  void messageEnvelope_equals_by_id() {
    MessageId id = MessageId.of("id-1");
    TopicName topic = TopicName.of("t");
    MessageEnvelope<String> e1 =
        MessageEnvelope.<String>builder().id(id).topic(topic).payload("a").build();
    MessageEnvelope<String> e2 =
        MessageEnvelope.<String>builder().id(id).topic(topic).payload("b").build();
    assertEquals(e1, e2);
    assertEquals(e1.hashCode(), e2.hashCode());
    assertEquals(e1, e1);
    assertNotEquals(e1, null);
    assertNotEquals(
        e1,
        MessageEnvelope.<String>builder().id(MessageId.random()).topic(topic).payload("a").build());
    assertEquals("MessageEnvelope{id=id-1, topic=t}", e1.toString());
  }

  @Test
  void messageEnvelope_not_equals_different_class() {
    MessageId id = MessageId.of("id-1");
    TopicName topic = TopicName.of("t");
    MessageEnvelope<String> e1 =
        MessageEnvelope.<String>builder().id(id).topic(topic).payload("a").build();
    assertNotEquals(e1, "not an envelope");
  }

  // --- MessagePublisherPort default methods ---

  @Test
  void publisherPort_default_publish_builds_envelope() {
    List<MessageEnvelope<?>> captured = new ArrayList<>();
    MessagePublisherPort publisher = (envelope, serializer) -> captured.add(envelope);

    TopicName topic = TopicName.of("test.topic");
    publisher.publish(topic, "payload", msg -> msg.getBytes());

    assertEquals(1, captured.size());
    assertEquals(topic, captured.get(0).topic());
  }

  @Test
  void publisherPort_default_publishBatch_sends_all() {
    List<MessageEnvelope<?>> captured = new ArrayList<>();
    MessagePublisherPort publisher = (envelope, serializer) -> captured.add(envelope);

    TopicName topic = TopicName.of("batch.topic");
    MessageEnvelope<String> e1 =
        MessageEnvelope.<String>builder().topic(topic).payload("a").build();
    MessageEnvelope<String> e2 =
        MessageEnvelope.<String>builder().topic(topic).payload("b").build();

    publisher.publishBatch(List.of(e1, e2), msg -> msg.getBytes());

    assertEquals(2, captured.size());
  }

  // --- MessageHeaders.builder null checks ---

  @Test
  void messageHeaders_builder_headers_map_null_throws() {
    assertThrows(NullPointerException.class, () -> MessageHeaders.builder().headers(null));
  }

  @Test
  void messageHeaders_builder_headers_map_merges() {
    MessageHeaders h =
        MessageHeaders.builder().headers(java.util.Map.of("a", "1", "b", "2")).build();
    assertEquals("1", h.get("a").get());
  }
}
