package com.marcusprado02.commons.adapters.web.spring.envelope;

import com.marcusprado02.commons.adapters.web.envelope.ApiMeta;
import com.marcusprado02.commons.adapters.web.spring.context.SpringRequestContextHolder;
import java.time.Instant;

/**
 * Factory that creates {@link com.marcusprado02.commons.adapters.web.envelope.ApiMeta} from the
 * current Spring request context.
 */
public final class SpringApiMetaFactory {

  private SpringApiMetaFactory() {}

  /**
   * Returns the {@link com.marcusprado02.commons.adapters.web.envelope.ApiMeta} for the current
   * request.
   */
  public static ApiMeta current() {
    var ctx = SpringRequestContextHolder.get().orElse(null);
    if (ctx == null) {
      return ApiMeta.empty();
    }

    String tenant = ctx.tenantId() == null ? null : ctx.tenantId().value();
    String actor = ctx.actorId() == null ? null : ctx.actorId().value();

    return new ApiMeta(
        ctx.correlationId(), ctx.causationId(), tenant, actor, Instant.now(), java.util.Map.of());
  }
}
