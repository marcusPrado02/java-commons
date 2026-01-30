package com.marcusprado02.commons.kernel.ddd.context;

import com.marcusprado02.commons.kernel.ddd.tenant.TenantId;

/** Port to resolve the current tenant. */
public interface TenantProvider {

  TenantId currentTenant();
}
