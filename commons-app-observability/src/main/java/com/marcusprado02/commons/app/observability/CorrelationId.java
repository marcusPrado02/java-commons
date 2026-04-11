package com.marcusprado02.commons.app.observability;

import java.util.UUID;

/** Utility class for generating and managing correlation IDs for distributed tracing. */
public final class CorrelationId {

  private CorrelationId() {}

  public static String newId() {
    return UUID.randomUUID().toString();
  }
}
