package com.marcusprado02.commons.adapters.web.spring.http;

import com.marcusprado02.commons.platform.context.RequestContextSnapshot;
import com.marcusprado02.commons.platform.http.ContextHeaderWriter;
import com.marcusprado02.commons.platform.http.StandardHeaders;

public final class SpringContextHeaderWriter implements ContextHeaderWriter {

  @Override
  public void write(RequestContextSnapshot ctx, HeaderSink sink) {
    if (ctx == null) return;

    setIfNotBlank(sink, StandardHeaders.CORRELATION_ID, ctx.correlationId());
    setIfNotBlank(sink, StandardHeaders.CAUSATION_ID, ctx.causationId());
    if (ctx.tenantId() != null) {
      setIfNotBlank(sink, StandardHeaders.TENANT_ID, ctx.tenantId().value());
    }
    if (ctx.actorId() != null) {
      setIfNotBlank(sink, StandardHeaders.ACTOR_ID, ctx.actorId().value());
    }
  }

  private static void setIfNotBlank(HeaderSink sink, String name, String value) {
    if (value != null && !value.isBlank()) {
      sink.set(name, value);
    }
  }
}
