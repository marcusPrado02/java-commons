package com.marcusprado02.commons.platform.context;

import com.marcusprado02.commons.kernel.ddd.audit.ActorId;
import java.util.Optional;

public interface SecurityContext {

  Optional<ActorId> actor();
}
