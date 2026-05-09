package com.marcusprado02.commons.ports.messaging;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Test;

class MessagingModelTest {

  @Test
  void messageId_of_stores_value() {
    MessageId id = MessageId.of("abc-123");
    assertEquals("abc-123", id.value());
    assertEquals("abc-123", id.toString());
  }

  @Test
  void messageId_random_generates_unique_ids() {
    MessageId a = MessageId.random();
    MessageId b = MessageId.random();
    assertNotEquals(a, b);
    assertNotNull(a.value());
  }

  @Test
  void messageId_equals_same_value() {
    MessageId a = MessageId.of("x");
    MessageId b = MessageId.of("x");
    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
  }

  @Test
  void messageId_not_equals_different_value() {
    assertNotEquals(MessageId.of("a"), MessageId.of("b"));
  }

  @Test
  void messageId_not_equals_null_or_other_type() {
    MessageId id = MessageId.of("id");
    assertNotEquals(id, null);
    assertNotEquals(id, "id");
  }

  @Test
  void messageId_equals_same_instance() {
    MessageId id = MessageId.of("id");
    assertEquals(id, id);
  }

  @Test
  void messageId_rejects_null_value() {
    assertThrows(NullPointerException.class, () -> MessageId.of(null));
  }

  @Test
  void topicName_of_stores_value() {
    TopicName t = TopicName.of("events.orders");
    assertEquals("events.orders", t.value());
    assertEquals("events.orders", t.toString());
  }

  @Test
  void topicName_rejects_blank() {
    assertThrows(IllegalArgumentException.class, () -> TopicName.of("  "));
  }

  @Test
  void topicName_rejects_null() {
    assertThrows(NullPointerException.class, () -> TopicName.of(null));
  }

  @Test
  void topicName_equals_and_hashCode() {
    TopicName a = TopicName.of("t");
    TopicName b = TopicName.of("t");
    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
    assertEquals(a, a);
    assertNotEquals(a, null);
    assertNotEquals(a, TopicName.of("other"));
    assertNotEquals(a, "t");
  }

  @Test
  void messageHeaders_empty_is_empty() {
    MessageHeaders h = MessageHeaders.empty();
    assertTrue(h.isEmpty());
    assertTrue(h.asMap().isEmpty());
  }

  @Test
  void messageHeaders_of_map_stores_entries() {
    MessageHeaders h = MessageHeaders.of(Map.of("k", "v"));
    assertFalse(h.isEmpty());
    assertTrue(h.get("k").isPresent());
    assertEquals("v", h.get("k").get());
  }

  @Test
  void messageHeaders_get_missing_key_returns_empty() {
    assertTrue(MessageHeaders.empty().get("missing").isEmpty());
  }

  @Test
  void messageHeaders_builder_with_headers() {
    MessageHeaders h =
        MessageHeaders.builder()
            .header("a", "1")
            .correlationId("corr-99")
            .causationId("cause-1")
            .build();
    assertEquals("1", h.get("a").get());
    assertEquals("corr-99", h.get("correlationId").get());
  }

  @Test
  void messageHeaders_equals_and_hashCode() {
    MessageHeaders a = MessageHeaders.of(Map.of("k", "v"));
    MessageHeaders b = MessageHeaders.of(Map.of("k", "v"));
    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
    assertEquals(a, a);
    assertNotEquals(a, MessageHeaders.empty());
    assertNotEquals(a, null);
    assertNotEquals(a, "str");
  }

  @Test
  void messageHeaders_of_rejects_null() {
    assertThrows(NullPointerException.class, () -> MessageHeaders.of(null));
  }

  @Test
  void messageHeaders_builder_rejects_null_key() {
    assertThrows(NullPointerException.class, () -> MessageHeaders.builder().header(null, "v"));
  }
}
