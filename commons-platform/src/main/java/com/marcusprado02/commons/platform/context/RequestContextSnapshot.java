package com.marcusprado02.commons.platform.context;

import com.marcusprado02.commons.kernel.ddd.audit.ActorId;
import com.marcusprado02.commons.kernel.ddd.tenant.TenantId;
import java.util.Optional;

public record RequestContextSnapshot(
    String correlationId, String causationId, TenantId tenantId, ActorId actorId) {

  public Optional<String> correlationIdOpt() {
    return Optional.ofNullable(correlationId);
  }

  public Optional<String> causationIdOpt() {
    return Optional.ofNullable(causationId);
  }

  public Optional<TenantId> tenantIdOpt() {
    return Optional.ofNullable(tenantId);
  }

  public Optional<ActorId> actorIdOpt() {
    return Optional.ofNullable(actorId);
  }
}
