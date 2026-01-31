package com.marcusprado02.commons.app.outbox;

import com.marcusprado02.commons.app.outbox.model.OutboxMessage;
import com.marcusprado02.commons.app.outbox.model.OutboxStatus;
import java.util.List;
import java.util.Objects;

public final class DefaultOutboxProcessor implements OutboxProcessor {

  private final OutboxStore store;
  private final OutboundPublisher outbound;
  private final int maxAttempts;

  public DefaultOutboxProcessor(OutboxStore store, OutboundPublisher outbound, int maxAttempts) {
    this.store = Objects.requireNonNull(store);
    this.outbound = Objects.requireNonNull(outbound);
    this.maxAttempts = maxAttempts;
  }

  @Override
  public void processAll() {

    // suprema UX: batch fetch
    List<OutboxMessage> pending = store.fetchByStatus(OutboxStatus.PENDING);

    for (OutboxMessage msg : pending) {
      try {
        // marcar processing
        store.updateStatus(msg.id(), OutboxStatus.PROCESSING);

        // publicar para fora
        outbound.publish(msg.topic(), msg.payload().body(), msg.headers());

        // marca como SENT
        store.updateStatus(msg.id(), OutboxStatus.PUBLISHED);
      } catch (Exception ex) {
        int nextAttempts = msg.attempts() + 1;
        if (nextAttempts >= maxAttempts) {
          store.updateStatus(msg.id(), OutboxStatus.FAILED);
        } else {
          store.updateStatus(msg.id(), OutboxStatus.PENDING, nextAttempts);
        }
      }
    }
  }
}
