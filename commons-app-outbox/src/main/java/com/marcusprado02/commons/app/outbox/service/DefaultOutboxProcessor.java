package com.marcusprado02.commons.app.outbox.service;

import com.marcusprado02.commons.app.outbox.model.OutboxMessage;
import com.marcusprado02.commons.app.outbox.model.OutboxStatus;
import com.marcusprado02.commons.app.outbox.port.OutboxPublisherPort;
import com.marcusprado02.commons.app.outbox.port.OutboxRepositoryPort;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public final class DefaultOutboxProcessor implements OutboxProcessor {

  private final OutboxRepositoryPort repository;
  private final OutboxPublisherPort publisher;

  public DefaultOutboxProcessor(OutboxRepositoryPort repository, OutboxPublisherPort publisher) {
    this.repository = Objects.requireNonNull(repository);
    this.publisher = Objects.requireNonNull(publisher);
  }

  @Override
  public void processOnce(int batchSize) {
    int limit = Math.max(1, batchSize);
    List<OutboxMessage> batch = repository.fetchBatch(OutboxStatus.PENDING, limit);

    for (OutboxMessage msg : batch) {
      try {
        publisher.publish(msg);
        repository.markPublished(msg.id(), Instant.now());
      } catch (RuntimeException ex) {
        int attempts = msg.attempts() + 1;
        repository.markFailed(msg.id(), ex.getClass().getSimpleName(), attempts);
      }
    }
  }
}
