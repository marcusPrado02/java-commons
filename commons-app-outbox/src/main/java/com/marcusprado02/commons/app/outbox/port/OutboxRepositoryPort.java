package com.marcusprado02.commons.app.outbox.port;

import com.marcusprado02.commons.app.outbox.model.OutboxMessage;
import com.marcusprado02.commons.app.outbox.model.OutboxMessageId;
import com.marcusprado02.commons.app.outbox.model.OutboxStatus;
import java.time.Instant;
import java.util.List;

public interface OutboxRepositoryPort {

  void append(OutboxMessage message);

  List<OutboxMessage> fetchBatch(OutboxStatus status, int limit);

  void markPublished(OutboxMessageId id, Instant publishedAt);

  void markFailed(OutboxMessageId id, String reason, int attempts);

  void markDead(OutboxMessageId id, String reason, int attempts);
}
