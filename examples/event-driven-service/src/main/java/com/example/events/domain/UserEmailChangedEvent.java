package com.example.events.domain;

import com.marcusprado02.commons.kernel.ddd.event.DomainEvent;
import com.marcusprado02.commons.kernel.ddd.event.EventId;
import com.marcusprado02.commons.kernel.ddd.event.EventMetadata;
import java.time.Instant;

/** Fired when a user changes their e-mail address. */
public record UserEmailChangedEvent(
    String userId,
    String oldEmail,
    String newEmail,
    Instant changedAt
) implements DomainEvent {

  @Override public EventId eventId()        { return EventId.newId(); }
  @Override public Instant occurredAt()     { return changedAt; }
  @Override public String aggregateType()   { return "User"; }
  @Override public String aggregateId()     { return userId; }
  @Override public long aggregateVersion()  { return 1L; }
  @Override public EventMetadata metadata() { return EventMetadata.empty(); }
}
