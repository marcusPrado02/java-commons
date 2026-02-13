package com.marcusprado02.commons.ports.messaging;

public interface MessageSerializer<T> {

  byte[] serialize(T message);

  T deserialize(byte[] data, Class<T> type);
}
