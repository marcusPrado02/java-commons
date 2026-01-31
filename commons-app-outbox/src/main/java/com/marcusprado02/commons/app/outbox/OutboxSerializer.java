package com.marcusprado02.commons.app.outbox;

public interface OutboxSerializer {

  /** Serializes the event object for storage in the outbox. */
  String serialize(Object event);
}
