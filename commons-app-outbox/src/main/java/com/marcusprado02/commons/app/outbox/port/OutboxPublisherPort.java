package com.marcusprado02.commons.app.outbox.port;

import com.marcusprado02.commons.app.outbox.model.OutboxMessage;

/** OutboxPublisherPort contract. */
public interface OutboxPublisherPort {

  void publish(OutboxMessage message);
}
