package com.marcusprado02.commons.ports.queue;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a message received from a queue.
 *
 * @param <T> the type of message payload
 */
public final class ReceivedMessage<T> {

  private final String messageId;
  private final String receiptHandle;
  private final T payload;
  private final Map<String, String> attributes;
  private final int receiveCount;
  private final Instant sentTimestamp;
  private final String messageGroupId;

  private ReceivedMessage(Builder<T> builder) {
    this.messageId = Objects.requireNonNull(builder.messageId, "messageId cannot be null");
    this.receiptHandle =
        Objects.requireNonNull(builder.receiptHandle, "receiptHandle cannot be null");
    this.payload = Objects.requireNonNull(builder.payload, "payload cannot be null");
    this.attributes = new HashMap<>(builder.attributes);
    this.receiveCount = builder.receiveCount;
    this.sentTimestamp = builder.sentTimestamp;
    this.messageGroupId = builder.messageGroupId;
  }

  public String messageId() {
    return messageId;
  }

  public String receiptHandle() {
    return receiptHandle;
  }

  public T payload() {
    return payload;
  }

  public Map<String, String> attributes() {
    return new HashMap<>(attributes);
  }

  public int receiveCount() {
    return receiveCount;
  }

  public Instant sentTimestamp() {
    return sentTimestamp;
  }

  public String messageGroupId() {
    return messageGroupId;
  }

  public static <T> Builder<T> builder() {
    return new Builder<>();
  }

  public static final class Builder<T> {
    private String messageId;
    private String receiptHandle;
    private T payload;
    private final Map<String, String> attributes = new HashMap<>();
    private int receiveCount;
    private Instant sentTimestamp;
    private String messageGroupId;

    private Builder() {}

    public Builder<T> messageId(String messageId) {
      this.messageId = messageId;
      return this;
    }

    public Builder<T> receiptHandle(String receiptHandle) {
      this.receiptHandle = receiptHandle;
      return this;
    }

    public Builder<T> payload(T payload) {
      this.payload = payload;
      return this;
    }

    public Builder<T> attributes(Map<String, String> attributes) {
      this.attributes.clear();
      this.attributes.putAll(attributes);
      return this;
    }

    public Builder<T> receiveCount(int receiveCount) {
      this.receiveCount = receiveCount;
      return this;
    }

    public Builder<T> sentTimestamp(Instant sentTimestamp) {
      this.sentTimestamp = sentTimestamp;
      return this;
    }

    public Builder<T> messageGroupId(String messageGroupId) {
      this.messageGroupId = messageGroupId;
      return this;
    }

    public ReceivedMessage<T> build() {
      return new ReceivedMessage<>(this);
    }
  }
}
