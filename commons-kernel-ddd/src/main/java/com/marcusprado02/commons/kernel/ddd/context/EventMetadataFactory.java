package com.marcusprado02.commons.kernel.ddd.context;

import com.marcusprado02.commons.kernel.ddd.event.EventMetadata;
import java.util.Map;
import java.util.Objects;

/** Produces EventMetadata consistently across the platform. */
public final class EventMetadataFactory {

  private final TenantProvider tenant;
  private final CorrelationProvider correlation;

  public EventMetadataFactory(TenantProvider tenant, CorrelationProvider correlation) {
    this.tenant = Objects.requireNonNull(tenant, "tenant");
    this.correlation = Objects.requireNonNull(correlation, "correlation");
  }

  public EventMetadata base() {
    return new EventMetadata(
        correlation.correlationId(), correlation.causationId(), tenant.currentTenant(), Map.of());
  }

  public EventMetadata withAttributes(Map<String, Object> attributes) {
    Objects.requireNonNull(attributes, "attributes");
    return new EventMetadata(
        correlation.correlationId(), correlation.causationId(), tenant.currentTenant(), attributes);
  }
}
