package com.marcusprado02.commons.kernel.ddd.context;

import com.marcusprado02.commons.kernel.ddd.audit.ActorId;
import java.util.Objects;

public final class FixedActorProvider implements ActorProvider {

  private final ActorId actor;

  public FixedActorProvider(ActorId actor) {
    this.actor = Objects.requireNonNull(actor, "actor");
  }

  @Override
  public ActorId currentActor() {
    return actor;
  }
}
