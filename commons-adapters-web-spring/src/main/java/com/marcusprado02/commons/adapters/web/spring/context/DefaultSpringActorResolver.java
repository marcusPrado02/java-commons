package com.marcusprado02.commons.adapters.web.spring.context;

import com.marcusprado02.commons.kernel.ddd.audit.ActorId;
import jakarta.servlet.http.HttpServletRequest;

/** Default actor resolver when no security framework is present. */
public final class DefaultSpringActorResolver implements SpringActorResolver {

  @Override
  public ActorId resolve(HttpServletRequest request) {
    return ActorId.system();
  }
}
