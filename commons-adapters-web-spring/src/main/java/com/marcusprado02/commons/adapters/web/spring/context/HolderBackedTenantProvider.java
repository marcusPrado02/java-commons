package com.marcusprado02.commons.adapters.web.spring.context;

import com.marcusprado02.commons.kernel.ddd.context.TenantProvider;
import com.marcusprado02.commons.kernel.ddd.tenant.TenantId;

public final class HolderBackedTenantProvider implements TenantProvider {

  @Override
  public TenantId currentTenant() {
    return SpringRequestContextHolder.get()
        .map(ctx -> ctx.tenantId())
        .orElseThrow(() -> new IllegalStateException("No tenant in request context"));
  }
}
