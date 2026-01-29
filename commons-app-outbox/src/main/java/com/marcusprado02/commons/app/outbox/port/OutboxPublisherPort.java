package com.marcusprado02.commons.app.outbox.port;

import com.marcusprado02.commons.app.outbox.model.OutboxMessage;

public interface OutboxPublisherPort {

  void publish(OutboxMessage message);
}
