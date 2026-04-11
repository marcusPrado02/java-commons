package com.marcusprado02.commons.adapters.web.spring.client;

import com.marcusprado02.commons.adapters.web.spring.context.SpringRequestContextHolder;
import com.marcusprado02.commons.platform.http.StandardHeaders;
import java.util.LinkedHashMap;
import java.util.Map;

/** Collects outbound context headers from the current request context. */
public final class OutboundContextHeaders {

  private OutboundContextHeaders() {}

  /** Returns outbound context headers collected from the current request context. */
  public static Map<String, String> currentHeaders() {
    Map<String, String> headers = new LinkedHashMap<>();

    SpringRequestContextHolder.get()
        .ifPresent(
            ctx -> {
              putIfNotBlank(headers, StandardHeaders.CORRELATION_ID, ctx.correlationId());
              putIfNotBlank(headers, StandardHeaders.CAUSATION_ID, ctx.causationId());
              if (ctx.tenantId() != null) {
                putIfNotBlank(headers, StandardHeaders.TENANT_ID, ctx.tenantId().value());
              }
              if (ctx.actorId() != null) {
                putIfNotBlank(headers, StandardHeaders.ACTOR_ID, ctx.actorId().value());
              }
            });

    return Map.copyOf(headers);
  }

  private static void putIfNotBlank(Map<String, String> map, String key, String value) {
    if (value != null && !value.isBlank()) {
      map.put(key, value);
    }
  }
}
