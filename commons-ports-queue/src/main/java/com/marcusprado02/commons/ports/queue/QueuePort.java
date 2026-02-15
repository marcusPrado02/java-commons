package com.marcusprado02.commons.ports.queue;

import com.marcusprado02.commons.kernel.result.Result;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Port interface for queue operations.
 *
 * <p>Supports sending, receiving, deleting messages, and managing queue attributes. Implementations
 * can adapt to various queue backends (AWS SQS, Azure Storage Queue, RabbitMQ, etc.).
 *
 * @param <T> the type of message payload
 */
public interface QueuePort<T> {

  /**
   * Sends a message to the queue.
   *
   * @param message the message to send
   * @return result containing the message ID if successful
   */
  Result<SendMessageResult> send(QueueMessage<T> message);

  /**
   * Sends multiple messages to the queue in a batch operation.
   *
   * @param messages the messages to send
   * @return result containing batch send results
   */
  Result<BatchSendResult> sendBatch(List<QueueMessage<T>> messages);

  /**
   * Receives messages from the queue.
   *
   * @param maxMessages maximum number of messages to receive (1-10)
   * @param visibilityTimeout duration messages should be invisible to other consumers
   * @return result containing list of received messages
   */
  Result<List<ReceivedMessage<T>>> receive(int maxMessages, Duration visibilityTimeout);

  /**
   * Receives a single message from the queue.
   *
   * @param visibilityTimeout duration the message should be invisible to other consumers
   * @return result containing the received message if available
   */
  default Result<Optional<ReceivedMessage<T>>> receiveOne(Duration visibilityTimeout) {
    return receive(1, visibilityTimeout)
        .map(messages -> messages.isEmpty() ? Optional.empty() : Optional.of(messages.get(0)));
  }

  /**
   * Deletes a message from the queue.
   *
   * @param receiptHandle the receipt handle received when the message was retrieved
   * @return result indicating success or failure
   */
  Result<Void> delete(String receiptHandle);

  /**
   * Deletes multiple messages from the queue in a batch operation.
   *
   * @param receiptHandles the receipt handles of messages to delete
   * @return result containing batch delete results
   */
  Result<BatchDeleteResult> deleteBatch(List<String> receiptHandles);

  /**
   * Changes the visibility timeout of a message.
   *
   * @param receiptHandle the receipt handle of the message
   * @param visibilityTimeout new visibility timeout
   * @return result indicating success or failure
   */
  Result<Void> changeVisibility(String receiptHandle, Duration visibilityTimeout);

  /**
   * Purges all messages from the queue.
   *
   * @return result indicating success or failure
   */
  Result<Void> purge();

  /**
   * Gets the approximate number of messages in the queue.
   *
   * @return result containing queue attributes
   */
  Result<QueueAttributes> getAttributes();
}
