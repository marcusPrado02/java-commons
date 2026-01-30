package com.marcusprado02.commons.platform.context;

/** Standard keys used across logs, tracing, headers and envelopes. */
public final class ContextKeys {

  private ContextKeys() {}

  public static final String CORRELATION_ID = "correlationId";
  public static final String CAUSATION_ID = "causationId";
  public static final String TENANT_ID = "tenantId";
  public static final String ACTOR_ID = "actorId";
}
