package com.marcusprado02.commons.kernel.ddd.context;

import com.marcusprado02.commons.kernel.ddd.tenant.TenantId;
import java.util.Objects;

/** TenantProvider that always returns a fixed tenant. */
public final class FixedTenantProvider implements TenantProvider {

  private final TenantId tenant;

  public FixedTenantProvider(TenantId tenant) {
    this.tenant = Objects.requireNonNull(tenant, "tenant");
  }

  @Override
  public TenantId currentTenant() {
    return tenant;
  }
}
