package com.marcusprado02.commons.kernel.ddd.event;

import com.marcusprado02.commons.kernel.ddd.tenant.TenantId;
import java.util.Map;

/**
 * Carries cross-cutting metadata (correlation, causation, tenant, attributes) for domain events.
 */
public record EventMetadata(
    String correlationId, String causationId, TenantId tenantId, Map<String, Object> attributes) {
  public EventMetadata {
    attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
  }

  /** Returns an empty EventMetadata with no correlation, causation, tenant, or attributes. */
  public static EventMetadata empty() {
    return new EventMetadata(null, null, null, Map.of());
  }

  /** Returns an EventMetadata with only the given tenant set. */
  public static EventMetadata withTenant(TenantId tenantId) {
    return new EventMetadata(null, null, tenantId, Map.of());
  }
}
