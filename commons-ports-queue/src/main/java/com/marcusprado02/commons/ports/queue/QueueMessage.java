package com.marcusprado02.commons.ports.queue;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents a message to be sent to a queue.
 *
 * @param <T> the type of message payload
 */
public final class QueueMessage<T> {

  private final T payload;
  private final Map<String, String> attributes;
  private final Duration delay;
  private final String messageGroupId; // For FIFO queues
  private final String deduplicationId; // For FIFO queues

  private QueueMessage(Builder<T> builder) {
    this.payload = Objects.requireNonNull(builder.payload, "payload cannot be null");
    this.attributes = new HashMap<>(builder.attributes);
    this.delay = builder.delay;
    this.messageGroupId = builder.messageGroupId;
    this.deduplicationId = builder.deduplicationId;
  }

  public T payload() {
    return payload;
  }

  public Map<String, String> attributes() {
    return new HashMap<>(attributes);
  }

  public Optional<Duration> delay() {
    return Optional.ofNullable(delay);
  }

  public Optional<String> messageGroupId() {
    return Optional.ofNullable(messageGroupId);
  }

  public Optional<String> deduplicationId() {
    return Optional.ofNullable(deduplicationId);
  }

  public static <T> Builder<T> builder() {
    return new Builder<>();
  }

  public static final class Builder<T> {
    private T payload;
    private final Map<String, String> attributes = new HashMap<>();
    private Duration delay;
    private String messageGroupId;
    private String deduplicationId;

    private Builder() {}

    public Builder<T> payload(T payload) {
      this.payload = payload;
      return this;
    }

    public Builder<T> attribute(String key, String value) {
      this.attributes.put(key, value);
      return this;
    }

    public Builder<T> attributes(Map<String, String> attributes) {
      this.attributes.clear();
      this.attributes.putAll(attributes);
      return this;
    }

    public Builder<T> delay(Duration delay) {
      this.delay = delay;
      return this;
    }

    public Builder<T> messageGroupId(String messageGroupId) {
      this.messageGroupId = messageGroupId;
      return this;
    }

    public Builder<T> deduplicationId(String deduplicationId) {
      this.deduplicationId = deduplicationId;
      return this;
    }

    public QueueMessage<T> build() {
      return new QueueMessage<>(this);
    }
  }
}
