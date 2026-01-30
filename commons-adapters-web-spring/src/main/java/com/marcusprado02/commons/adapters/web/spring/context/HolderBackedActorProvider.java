package com.marcusprado02.commons.adapters.web.spring.context;

import com.marcusprado02.commons.kernel.ddd.audit.ActorId;
import com.marcusprado02.commons.kernel.ddd.context.ActorProvider;

public final class HolderBackedActorProvider implements ActorProvider {

  @Override
  public ActorId currentActor() {
    return SpringRequestContextHolder.get()
        .map(ctx -> ctx.actorId() == null ? ActorId.system() : ctx.actorId())
        .orElse(ActorId.system());
  }
}
