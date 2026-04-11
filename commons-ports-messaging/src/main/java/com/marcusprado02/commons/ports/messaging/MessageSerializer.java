package com.marcusprado02.commons.ports.messaging;

/** Serializes and deserializes message payloads to and from bytes. */
public interface MessageSerializer<T> {

  byte[] serialize(T message);

  T deserialize(byte[] data, Class<T> type);
}
