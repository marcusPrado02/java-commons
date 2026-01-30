package com.marcusprado02.commons.adapters.web.spring.context;

import com.marcusprado02.commons.platform.context.ContextKeys;
import com.marcusprado02.commons.platform.context.RequestContextSnapshot;
import org.slf4j.MDC;

public final class MdcContextBinder {

  private MdcContextBinder() {}

  public static void bind(RequestContextSnapshot ctx) {
    if (ctx == null) return;

    putIfNotNull(ContextKeys.CORRELATION_ID, ctx.correlationId());
    putIfNotNull(ContextKeys.CAUSATION_ID, ctx.causationId());
    putIfNotNull(ContextKeys.TENANT_ID, ctx.tenantId() == null ? null : ctx.tenantId().value());
    putIfNotNull(ContextKeys.ACTOR_ID, ctx.actorId() == null ? null : ctx.actorId().value());
  }

  public static void clear() {
    MDC.remove(ContextKeys.CORRELATION_ID);
    MDC.remove(ContextKeys.CAUSATION_ID);
    MDC.remove(ContextKeys.TENANT_ID);
    MDC.remove(ContextKeys.ACTOR_ID);
  }

  private static void putIfNotNull(String key, String value) {
    if (value != null && !value.isBlank()) {
      MDC.put(key, value);
    }
  }
}
