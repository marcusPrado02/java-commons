package com.marcusprado02.commons.kernel.ddd.entity;

import com.marcusprado02.commons.kernel.ddd.tenant.TenantId;

public record AggregateSnapshot<ID>(ID id, String type, TenantId tenantId, long version) {}
