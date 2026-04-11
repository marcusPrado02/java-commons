package com.marcusprado02.commons.ports.messaging;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Immutable collection of key-value headers attached to a message. */
public final class MessageHeaders {

  private final Map<String, String> headers;

  private MessageHeaders(Map<String, String> headers) {
    this.headers = Collections.unmodifiableMap(new LinkedHashMap<>(headers));
  }

  public static MessageHeaders empty() {
    return new MessageHeaders(Map.of());
  }

  public static MessageHeaders of(Map<String, String> headers) {
    Objects.requireNonNull(headers, "headers must not be null");
    return new MessageHeaders(headers);
  }

  public static Builder builder() {
    return new Builder();
  }

  public Optional<String> get(String key) {
    return Optional.ofNullable(headers.get(key));
  }

  public Map<String, String> asMap() {
    return headers;
  }

  public boolean isEmpty() {
    return headers.isEmpty();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof MessageHeaders other)) {
      return false;
    }
    return headers.equals(other.headers);
  }

  @Override
  public int hashCode() {
    return Objects.hash(headers);
  }

  @Override
  public String toString() {
    return headers.toString();
  }

  /** Builder for {@link MessageHeaders}. */
  public static final class Builder {
    private final Map<String, String> headers = new LinkedHashMap<>();

    private Builder() {}

    /**
     * Adds a header entry.
     *
     * @param key header key
     * @param value header value
     * @return this builder
     */
    public Builder header(String key, String value) {
      Objects.requireNonNull(key, "key must not be null");
      Objects.requireNonNull(value, "value must not be null");
      headers.put(key, value);
      return this;
    }

    /**
     * Adds all entries from the given map.
     *
     * @param headers headers to add
     * @return this builder
     */
    public Builder headers(Map<String, String> headers) {
      Objects.requireNonNull(headers, "headers must not be null");
      this.headers.putAll(headers);
      return this;
    }

    public Builder correlationId(String correlationId) {
      return header("correlationId", correlationId);
    }

    public Builder causationId(String causationId) {
      return header("causationId", causationId);
    }

    public MessageHeaders build() {
      return new MessageHeaders(headers);
    }
  }
}
