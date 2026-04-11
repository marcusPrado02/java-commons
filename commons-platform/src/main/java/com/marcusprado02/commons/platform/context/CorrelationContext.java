package com.marcusprado02.commons.platform.context;

import java.util.Optional;

/** Provides correlation and causation IDs for distributed tracing across service boundaries. */
public interface CorrelationContext {

  Optional<String> correlationId();

  Optional<String> causationId();
}
