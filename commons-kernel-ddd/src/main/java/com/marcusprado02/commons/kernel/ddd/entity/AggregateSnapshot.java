package com.marcusprado02.commons.kernel.ddd.entity;

import com.marcusprado02.commons.kernel.ddd.tenant.TenantId;

/** Immutable snapshot of an aggregate root's identity and version at a point in time. */
public record AggregateSnapshot<I>(I id, String type, TenantId tenantId, long version) {}
