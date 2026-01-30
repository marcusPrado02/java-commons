package com.marcusprado02.commons.platform.context;

import java.util.Optional;

public interface CorrelationContext {

  Optional<String> correlationId();

  Optional<String> causationId();
}
