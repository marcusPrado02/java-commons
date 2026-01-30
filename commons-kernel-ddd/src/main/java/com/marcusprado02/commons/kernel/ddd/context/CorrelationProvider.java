package com.marcusprado02.commons.kernel.ddd.context;

/**
 * Port to resolve correlation/causation IDs for tracing.
 *
 * <p>CorrelationId: ties a whole request/flow. CausationId: ties a child action/event to a specific
 * parent (optional).
 */
public interface CorrelationProvider {

  String correlationId();

  default String causationId() {
    return null;
  }
}
