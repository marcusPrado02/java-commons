package com.marcusprado02.commons.platform.http;

/** Canonical HTTP headers used across services. */
public final class StandardHeaders {

  private StandardHeaders() {}

  public static final String CORRELATION_ID = "X-Correlation-Id";
  public static final String CAUSATION_ID = "X-Causation-Id";
  public static final String TENANT_ID = "X-Tenant-Id";
  public static final String ACTOR_ID = "X-Actor-Id";
  public static final String REQUEST_ID = "X-Request-Id";
}
