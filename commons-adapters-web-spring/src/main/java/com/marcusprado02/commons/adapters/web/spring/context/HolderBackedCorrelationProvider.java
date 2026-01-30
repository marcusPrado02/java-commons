package com.marcusprado02.commons.adapters.web.spring.context;

import com.marcusprado02.commons.kernel.ddd.context.CorrelationProvider;

public final class HolderBackedCorrelationProvider implements CorrelationProvider {

  @Override
  public String correlationId() {
    return SpringRequestContextHolder.get().map(ctx -> ctx.correlationId()).orElse(null);
  }

  @Override
  public String causationId() {
    return SpringRequestContextHolder.get().map(ctx -> ctx.causationId()).orElse(null);
  }
}
