package com.marcusprado02.commons.app.outbox;

public interface OutboxProcessor {

  /** Process pending messages. */
  void processAll();
}
