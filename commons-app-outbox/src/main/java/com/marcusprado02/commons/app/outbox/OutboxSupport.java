package com.marcusprado02.commons.app.outbox;

import com.marcusprado02.commons.kernel.ddd.entity.AggregateRoot;
import java.util.Objects;

public final class OutboxSupport {

  private final OutboxPublisher publisher;

  public OutboxSupport(OutboxPublisher publisher) {
    this.publisher = Objects.requireNonNull(publisher, "publisher");
  }

  public <A extends AggregateRoot<?>> A persistAndPublish(A aggregate, Runnable persistAction) {
    Objects.requireNonNull(aggregate, "aggregate");
    Objects.requireNonNull(persistAction, "persistAction");

    persistAction.run();
    publisher.publishFrom(aggregate);
    return aggregate;
  }
}
