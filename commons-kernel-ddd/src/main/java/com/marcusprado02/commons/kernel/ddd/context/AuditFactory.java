package com.marcusprado02.commons.kernel.ddd.context;

import com.marcusprado02.commons.kernel.ddd.audit.AuditStamp;
import com.marcusprado02.commons.kernel.ddd.audit.DeletionStamp;
import com.marcusprado02.commons.kernel.ddd.time.ClockProvider;
import java.util.Objects;

/** Produces audit stamps with consistent time + actor. */
public final class AuditFactory {

  private final ClockProvider clock;
  private final ActorProvider actor;

  public AuditFactory(ClockProvider clock, ActorProvider actor) {
    this.clock = Objects.requireNonNull(clock, "clock");
    this.actor = Objects.requireNonNull(actor, "actor");
  }

  public AuditStamp created() {
    return AuditStamp.of(clock.now(), actor.currentActor());
  }

  public AuditStamp updated() {
    return AuditStamp.of(clock.now(), actor.currentActor());
  }

  public DeletionStamp deleted() {
    return DeletionStamp.of(clock.now(), actor.currentActor());
  }
}
