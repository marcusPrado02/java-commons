package com.marcusprado02.commons.app.outbox;

import com.marcusprado02.commons.kernel.ddd.context.ActorProvider;
import com.marcusprado02.commons.kernel.ddd.context.CorrelationProvider;
import com.marcusprado02.commons.kernel.ddd.context.TenantProvider;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class OutboxMetadataEnricher {

  private final TenantProvider tenant;
  private final CorrelationProvider correlation;
  private final ActorProvider actor;

  public OutboxMetadataEnricher(
      TenantProvider tenant, CorrelationProvider correlation, ActorProvider actor) {
    this.tenant = Objects.requireNonNull(tenant, "tenant");
    this.correlation = Objects.requireNonNull(correlation, "correlation");
    this.actor = Objects.requireNonNull(actor, "actor");
  }

  public Map<String, Object> enrich() {
    Map<String, Object> metadata = new LinkedHashMap<>();
    metadata.put("tenantId", tenant.currentTenant().value());
    metadata.put("correlationId", correlation.correlationId());
    metadata.put("causationId", correlation.causationId());
    metadata.put("actorId", actor.currentActor().value());
    return Map.copyOf(metadata);
  }
}
