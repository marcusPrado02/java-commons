package com.marcusprado02.commons.ports.queue;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;

class QueueModelTest {

  @Test
  void queueMessage_builder_with_payload() {
    QueueMessage<String> msg = QueueMessage.<String>builder()
        .payload("hello")
        .build();

    assertEquals("hello", msg.payload());
    assertTrue(msg.delay().isEmpty());
    assertTrue(msg.messageGroupId().isEmpty());
    assertTrue(msg.deduplicationId().isEmpty());
  }

  @Test
  void queueMessage_builder_with_all_fields() {
    QueueMessage<String> msg = QueueMessage.<String>builder()
        .payload("data")
        .attribute("k", "v")
        .delay(Duration.ofSeconds(30))
        .messageGroupId("group-1")
        .deduplicationId("dedup-1")
        .build();

    assertEquals("data", msg.payload());
    assertEquals("v", msg.attributes().get("k"));
    assertTrue(msg.delay().isPresent());
    assertEquals(Duration.ofSeconds(30), msg.delay().get());
    assertEquals("group-1", msg.messageGroupId().get());
    assertEquals("dedup-1", msg.deduplicationId().get());
  }

  @Test
  void queueMessage_rejects_null_payload() {
    assertThrows(NullPointerException.class, () ->
        QueueMessage.builder().payload(null).build());
  }

  @Test
  void queueMessage_attributes_replaces_entries() {
    QueueMessage<String> msg = QueueMessage.<String>builder()
        .payload("p")
        .attribute("a", "1")
        .attributes(Map.of("b", "2"))
        .build();
    assertFalse(msg.attributes().containsKey("a"));
    assertEquals("2", msg.attributes().get("b"));
  }
}
