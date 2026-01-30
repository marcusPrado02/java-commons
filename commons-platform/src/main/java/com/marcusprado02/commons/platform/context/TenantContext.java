package com.marcusprado02.commons.platform.context;

import com.marcusprado02.commons.kernel.ddd.tenant.TenantId;
import java.util.Optional;

public interface TenantContext {

  Optional<TenantId> tenantId();
}
