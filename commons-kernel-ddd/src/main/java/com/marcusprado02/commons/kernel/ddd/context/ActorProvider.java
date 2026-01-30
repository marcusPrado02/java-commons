package com.marcusprado02.commons.kernel.ddd.context;

import com.marcusprado02.commons.kernel.ddd.audit.ActorId;

/** Port to resolve the current actor (user/service/job). */
public interface ActorProvider {

  ActorId currentActor();
}
