package com.marcusprado02.commons.ports.queue;

import static org.junit.jupiter.api.Assertions.*;

import com.marcusprado02.commons.kernel.result.Result;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class QueuePortBranchTest {

  // --- QueuePort.receiveOne() default method ---

  private static QueuePort<String> portReturning(List<ReceivedMessage<String>> messages) {
    return new QueuePort<>() {
      @Override
      public Result<SendMessageResult> send(QueueMessage<String> message) {
        throw new UnsupportedOperationException();
      }

      @Override
      public Result<BatchSendResult> sendBatch(List<QueueMessage<String>> messages) {
        throw new UnsupportedOperationException();
      }

      @Override
      public Result<List<ReceivedMessage<String>>> receive(
          int maxMessages, Duration visibilityTimeout) {
        return Result.ok(messages);
      }

      @Override
      public Result<Void> delete(String receiptHandle) {
        throw new UnsupportedOperationException();
      }

      @Override
      public Result<BatchDeleteResult> deleteBatch(List<String> receiptHandles) {
        throw new UnsupportedOperationException();
      }

      @Override
      public Result<Void> changeVisibility(String receiptHandle, Duration visibilityTimeout) {
        throw new UnsupportedOperationException();
      }

      @Override
      public Result<Void> purge() {
        throw new UnsupportedOperationException();
      }

      @Override
      public Result<QueueAttributes> getAttributes() {
        throw new UnsupportedOperationException();
      }
    };
  }

  @Test
  void receiveOne_empty_queue_returns_empty_optional() {
    QueuePort<String> port = portReturning(List.of());
    Result<Optional<ReceivedMessage<String>>> result = port.receiveOne(Duration.ofSeconds(30));
    assertTrue(result.isOk());
    assertFalse(result.getOrNull().isPresent());
  }

  @Test
  void receiveOne_non_empty_queue_returns_first_message() {
    ReceivedMessage<String> msg =
        ReceivedMessage.<String>builder()
            .messageId("id-1")
            .receiptHandle("handle-1")
            .payload("hello")
            .build();
    QueuePort<String> port = portReturning(List.of(msg));
    Result<Optional<ReceivedMessage<String>>> result = port.receiveOne(Duration.ofSeconds(30));
    assertTrue(result.isOk());
    assertTrue(result.getOrNull().isPresent());
    assertEquals("id-1", result.getOrNull().get().messageId());
  }

  // --- QueueAttributes ---

  @Test
  void queueAttributes_total_messages_sum() {
    Instant now = Instant.now();
    QueueAttributes attrs = new QueueAttributes(5, 3, 2, now, now, false);
    assertEquals(10, attrs.totalApproximateMessages());
    assertFalse(attrs.fifoQueue());
  }

  @Test
  void queueAttributes_fifo_queue() {
    Instant now = Instant.now();
    QueueAttributes attrs = new QueueAttributes(0, 0, 0, now, now, true);
    assertTrue(attrs.fifoQueue());
  }

  // --- QueueMessage equals/hashCode ---

  @Test
  void queueMessage_equals_and_hashCode() {
    QueueMessage<String> m1 = QueueMessage.<String>builder().payload("x").build();
    QueueMessage<String> m2 = QueueMessage.<String>builder().payload("x").build();
    assertEquals(m1, m1);
    assertNotEquals(m1, null);
    assertNotEquals(m1, "x");
  }

  // --- ReceivedMessage equals/hashCode ---

  @Test
  void receivedMessage_equals_and_hashCode() {
    ReceivedMessage<String> m =
        ReceivedMessage.<String>builder().messageId("id").receiptHandle("h").payload("p").build();
    assertEquals(m, m);
    assertNotEquals(m, null);
  }
}
