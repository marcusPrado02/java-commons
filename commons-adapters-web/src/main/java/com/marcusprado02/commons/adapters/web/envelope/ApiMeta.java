package com.marcusprado02.commons.adapters.web.envelope;

import java.time.Instant;
import java.util.Map;

public record ApiMeta(
    String correlationId,
    String causationId,
    String tenantId,
    String actorId,
    Instant timestamp,
    Map<String, Object> attributes) {

  public ApiMeta {
    attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
  }

  public static ApiMeta empty() {
    return new ApiMeta(null, null, null, null, Instant.now(), Map.of());
  }

  public ApiMeta withAttributes(Map<String, Object> attrs) {
    return new ApiMeta(correlationId, causationId, tenantId, actorId, timestamp, attrs);
  }
}
