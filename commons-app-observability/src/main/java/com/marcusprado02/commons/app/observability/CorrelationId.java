package com.marcusprado02.commons.app.observability;

import java.util.UUID;

public final class CorrelationId {

  private CorrelationId() {}

  public static String newId() {
    return UUID.randomUUID().toString();
  }
}
