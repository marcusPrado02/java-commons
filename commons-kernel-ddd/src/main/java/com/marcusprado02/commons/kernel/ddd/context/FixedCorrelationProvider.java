package com.marcusprado02.commons.kernel.ddd.context;

import java.util.Objects;

public final class FixedCorrelationProvider implements CorrelationProvider {

  private final String correlationId;
  private final String causationId;

  public FixedCorrelationProvider(String correlationId) {
    this(correlationId, null);
  }

  public FixedCorrelationProvider(String correlationId, String causationId) {
    this.correlationId = requireNonBlank(correlationId, "correlationId");
    this.causationId = causationId;
  }

  @Override
  public String correlationId() {
    return correlationId;
  }

  @Override
  public String causationId() {
    return causationId;
  }

  private static String requireNonBlank(String value, String name) {
    Objects.requireNonNull(value, name);
    if (value.isBlank()) throw new IllegalArgumentException(name + " cannot be blank");
    return value;
  }
}
