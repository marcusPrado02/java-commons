package com.marcusprado02.commons.platform.context;

import com.marcusprado02.commons.kernel.ddd.tenant.TenantId;
import java.util.Optional;

/** Provides access to the current tenant identifier within a request context. */
public interface TenantContext {

  Optional<TenantId> tenantId();
}
