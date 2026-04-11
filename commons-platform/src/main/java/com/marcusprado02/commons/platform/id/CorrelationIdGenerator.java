package com.marcusprado02.commons.platform.id;

import java.util.UUID;

/** Generates unique correlation IDs for distributed tracing. */
public final class CorrelationIdGenerator {

  private CorrelationIdGenerator() {}

  public static String newCorrelationId() {
    return UUID.randomUUID().toString();
  }
}
