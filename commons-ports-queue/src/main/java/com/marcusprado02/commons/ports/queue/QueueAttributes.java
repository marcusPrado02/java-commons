package com.marcusprado02.commons.ports.queue;

import java.time.Instant;

/** Queue attributes and statistics. */
public record QueueAttributes(
    int approximateNumberOfMessages,
    int approximateNumberOfMessagesNotVisible,
    int approximateNumberOfMessagesDelayed,
    Instant createdTimestamp,
    Instant lastModifiedTimestamp,
    boolean fifoQueue) {

  public int totalApproximateMessages() {
    return approximateNumberOfMessages
        + approximateNumberOfMessagesNotVisible
        + approximateNumberOfMessagesDelayed;
  }
}
