package com.marcusprado02.commons.app.outbox;

public interface OutboundPublisher {

  /**
   * Publish a serialized payload with headers to the outside world.
   *
   * @param topic the destination topic or channel
   * @param body the binary payload
   * @param headers the context headers
   */
  void publish(String topic, byte[] body, java.util.Map<String, String> headers);
}
