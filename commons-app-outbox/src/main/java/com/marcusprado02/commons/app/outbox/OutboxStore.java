package com.marcusprado02.commons.app.outbox;

import com.marcusprado02.commons.app.outbox.model.OutboxMessage;

public interface OutboxStore {

  /**
   * Appends an OutboxMessage into the outbox storage. This method **must** be called as part of a
   * transaction.
   */
  void append(OutboxMessage message);
}
