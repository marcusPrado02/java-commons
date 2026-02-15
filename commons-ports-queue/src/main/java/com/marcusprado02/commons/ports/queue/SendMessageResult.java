package com.marcusprado02.commons.ports.queue;

/** Result of sending a message to a queue. */
public record SendMessageResult(String messageId, String sequenceNumber) {

  public static SendMessageResult of(String messageId) {
    return new SendMessageResult(messageId, null);
  }

  public static SendMessageResult of(String messageId, String sequenceNumber) {
    return new SendMessageResult(messageId, sequenceNumber);
  }
}
