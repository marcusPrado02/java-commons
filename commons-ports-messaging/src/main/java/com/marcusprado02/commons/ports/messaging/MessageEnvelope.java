package com.marcusprado02.commons.ports.messaging;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class MessageEnvelope<T> {

  private final MessageId id;
  private final TopicName topic;
  private final T payload;
  private final MessageHeaders headers;
  private final Instant timestamp;
  private final String partitionKey;

  private MessageEnvelope(
      MessageId id,
      TopicName topic,
      T payload,
      MessageHeaders headers,
      Instant timestamp,
      String partitionKey) {
    this.id = Objects.requireNonNull(id, "id must not be null");
    this.topic = Objects.requireNonNull(topic, "topic must not be null");
    this.payload = Objects.requireNonNull(payload, "payload must not be null");
    this.headers = Objects.requireNonNull(headers, "headers must not be null");
    this.timestamp = Objects.requireNonNull(timestamp, "timestamp must not be null");
    this.partitionKey = partitionKey;
  }

  public static <T> Builder<T> builder() {
    return new Builder<>();
  }

  public MessageId id() {
    return id;
  }

  public TopicName topic() {
    return topic;
  }

  public T payload() {
    return payload;
  }

  public MessageHeaders headers() {
    return headers;
  }

  public Instant timestamp() {
    return timestamp;
  }

  public Optional<String> partitionKey() {
    return Optional.ofNullable(partitionKey);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof MessageEnvelope<?> other)) {
      return false;
    }
    return id.equals(other.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "MessageEnvelope{id=" + id + ", topic=" + topic + "}";
  }

  public static final class Builder<T> {
    private MessageId id;
    private TopicName topic;
    private T payload;
    private MessageHeaders headers = MessageHeaders.empty();
    private Instant timestamp;
    private String partitionKey;

    private Builder() {}

    public Builder<T> id(MessageId id) {
      this.id = id;
      return this;
    }

    public Builder<T> topic(TopicName topic) {
      this.topic = topic;
      return this;
    }

    public Builder<T> payload(T payload) {
      this.payload = payload;
      return this;
    }

    public Builder<T> headers(MessageHeaders headers) {
      this.headers = headers;
      return this;
    }

    public Builder<T> timestamp(Instant timestamp) {
      this.timestamp = timestamp;
      return this;
    }

    public Builder<T> partitionKey(String partitionKey) {
      this.partitionKey = partitionKey;
      return this;
    }

    public MessageEnvelope<T> build() {
      MessageId safeId = (id == null) ? MessageId.random() : id;
      Instant safeTimestamp = (timestamp == null) ? Instant.now() : timestamp;
      return new MessageEnvelope<>(safeId, topic, payload, headers, safeTimestamp, partitionKey);
    }
  }
}
