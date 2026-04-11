package com.marcusprado02.commons.platform.context;

import com.marcusprado02.commons.kernel.ddd.audit.ActorId;
import java.util.Optional;

/** Provides access to the authenticated actor within the current security context. */
public interface SecurityContext {

  Optional<ActorId> actor();
}
