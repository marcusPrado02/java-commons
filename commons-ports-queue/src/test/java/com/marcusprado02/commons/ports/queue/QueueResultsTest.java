package com.marcusprado02.commons.ports.queue;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class QueueResultsTest {

  // --- ReceivedMessage ---

  @Test
  void receivedMessage_builder_creates_valid_message() {
    Instant now = Instant.now();
    ReceivedMessage<String> msg = ReceivedMessage.<String>builder()
        .messageId("msg-001")
        .receiptHandle("receipt-handle-xyz")
        .payload("Hello Queue")
        .receiveCount(1)
        .sentTimestamp(now)
        .messageGroupId("group-1")
        .attributes(Map.of("key", "value"))
        .build();

    assertEquals("msg-001", msg.messageId());
    assertEquals("receipt-handle-xyz", msg.receiptHandle());
    assertEquals("Hello Queue", msg.payload());
    assertEquals(1, msg.receiveCount());
    assertEquals(now, msg.sentTimestamp());
    assertEquals("group-1", msg.messageGroupId());
    assertEquals("value", msg.attributes().get("key"));
  }

  @Test
  void receivedMessage_null_message_id_throws() {
    assertThrows(NullPointerException.class, () ->
        ReceivedMessage.<String>builder()
            .messageId(null)
            .receiptHandle("handle")
            .payload("data")
            .build());
  }

  @Test
  void receivedMessage_null_receipt_handle_throws() {
    assertThrows(NullPointerException.class, () ->
        ReceivedMessage.<String>builder()
            .messageId("id")
            .receiptHandle(null)
            .payload("data")
            .build());
  }

  @Test
  void receivedMessage_null_payload_throws() {
    assertThrows(NullPointerException.class, () ->
        ReceivedMessage.<String>builder()
            .messageId("id")
            .receiptHandle("handle")
            .payload(null)
            .build());
  }

  @Test
  void receivedMessage_attributes_returns_defensive_copy() {
    ReceivedMessage<String> msg = ReceivedMessage.<String>builder()
        .messageId("id")
        .receiptHandle("h")
        .payload("p")
        .attributes(Map.of("k", "v"))
        .build();
    Map<String, String> attrs = msg.attributes();
    assertEquals("v", attrs.get("k"));
  }

  // --- BatchSendResult ---

  @Test
  void batchSendResult_no_failures() {
    SendMessageResult r1 = SendMessageResult.of("id-1");
    SendMessageResult r2 = SendMessageResult.of("id-2", "seq-2");
    BatchSendResult result = new BatchSendResult(List.of(r1, r2), List.of());
    assertEquals(2, result.successCount());
    assertEquals(0, result.failureCount());
    assertFalse(result.hasFailures());
  }

  @Test
  void batchSendResult_with_failures() {
    BatchSendResult.BatchFailure failure = new BatchSendResult.BatchFailure("id-3", "QUEUE_FULL", "Queue is full");
    BatchSendResult result = new BatchSendResult(List.of(), List.of(failure));
    assertEquals(0, result.successCount());
    assertEquals(1, result.failureCount());
    assertTrue(result.hasFailures());
    assertEquals("id-3", failure.id());
    assertEquals("QUEUE_FULL", failure.code());
    assertEquals("Queue is full", failure.message());
  }

  // --- BatchDeleteResult ---

  @Test
  void batchDeleteResult_no_failures() {
    BatchDeleteResult result = new BatchDeleteResult(List.of("id-1", "id-2"), List.of());
    assertEquals(2, result.successCount());
    assertEquals(0, result.failureCount());
    assertFalse(result.hasFailures());
  }

  @Test
  void batchDeleteResult_with_failures() {
    BatchDeleteResult.BatchFailure failure = new BatchDeleteResult.BatchFailure("id-3", "INVALID_HANDLE", "Receipt expired");
    BatchDeleteResult result = new BatchDeleteResult(List.of(), List.of(failure));
    assertEquals(0, result.successCount());
    assertEquals(1, result.failureCount());
    assertTrue(result.hasFailures());
  }

  // --- SendMessageResult ---

  @Test
  void sendMessageResult_of_id_only() {
    SendMessageResult r = SendMessageResult.of("msg-123");
    assertEquals("msg-123", r.messageId());
    assertNull(r.sequenceNumber());
  }

  @Test
  void sendMessageResult_of_id_and_sequence() {
    SendMessageResult r = SendMessageResult.of("msg-456", "seq-001");
    assertEquals("msg-456", r.messageId());
    assertEquals("seq-001", r.sequenceNumber());
  }
}
