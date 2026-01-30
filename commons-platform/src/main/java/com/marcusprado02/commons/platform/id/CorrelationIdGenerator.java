package com.marcusprado02.commons.platform.id;

import java.util.UUID;

public final class CorrelationIdGenerator {

  private CorrelationIdGenerator() {}

  public static String newCorrelationId() {
    return UUID.randomUUID().toString();
  }
}
