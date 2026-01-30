package com.marcusprado02.commons.kernel.ddd.event;

import com.marcusprado02.commons.kernel.ddd.tenant.TenantId;
import java.util.Map;

public record EventMetadata(
    String correlationId, String causationId, TenantId tenantId, Map<String, Object> attributes) {
  public EventMetadata {
    attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
  }

  public static EventMetadata empty() {
    return new EventMetadata(null, null, null, Map.of());
  }

  public static EventMetadata withTenant(TenantId tenantId) {
    return new EventMetadata(null, null, tenantId, Map.of());
  }
}
