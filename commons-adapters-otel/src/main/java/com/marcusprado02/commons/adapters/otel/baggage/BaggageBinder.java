package com.marcusprado02.commons.adapters.otel.baggage;

import com.marcusprado02.commons.platform.context.RequestContextSnapshot;
import com.marcusprado02.commons.platform.http.StandardHeaders;
import io.opentelemetry.api.baggage.Baggage;

public final class BaggageBinder {

  private BaggageBinder() {}

  public static Baggage bind(RequestContextSnapshot ctx) {
    if (ctx == null) {
      return Baggage.current();
    }

    var builder = Baggage.current().toBuilder();

    putIfNotBlank(builder, StandardHeaders.CORRELATION_ID, ctx.correlationId());
    putIfNotBlank(builder, StandardHeaders.CAUSATION_ID, ctx.causationId());

    if (ctx.tenantId() != null) {
      putIfNotBlank(builder, StandardHeaders.TENANT_ID, ctx.tenantId().value());
    }

    if (ctx.actorId() != null) {
      putIfNotBlank(builder, StandardHeaders.ACTOR_ID, ctx.actorId().value());
    }

    return builder.build();
  }

  private static void putIfNotBlank(Object builder, String key, String value) {
    if (value == null || value.isBlank()) {
      return;
    }

    // Safe cast: the object is always the builder returned by Baggage.current().toBuilder()
    ((io.opentelemetry.api.baggage.BaggageBuilder) builder).put(key, value);
  }
}
