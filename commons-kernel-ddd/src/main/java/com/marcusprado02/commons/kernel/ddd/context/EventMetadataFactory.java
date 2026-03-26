package com.marcusprado02.commons.kernel.ddd.context;

import com.marcusprado02.commons.kernel.ddd.event.EventMetadata;
import java.util.Map;
import java.util.Objects;

/** Produces EventMetadata consistently across the platform. */
public final class EventMetadataFactory {

  private final TenantProvider tenant;
  private final CorrelationProvider correlation;

  /**
   * Creates a factory backed by the given tenant and correlation providers.
   *
   * @param tenant the tenant provider
   * @param correlation the correlation provider
   */
  public EventMetadataFactory(TenantProvider tenant, CorrelationProvider correlation) {
    this.tenant = Objects.requireNonNull(tenant, "tenant");
    this.correlation = Objects.requireNonNull(correlation, "correlation");
  }

  /**
   * Returns base EventMetadata with correlation, causation, and tenant from the providers.
   *
   * @return base event metadata
   */
  public EventMetadata base() {
    return new EventMetadata(
        correlation.correlationId(), correlation.causationId(), tenant.currentTenant(), Map.of());
  }

  /**
   * Returns EventMetadata with the given extra attributes alongside correlation and tenant.
   *
   * @param attributes additional metadata attributes
   * @return event metadata with attributes
   */
  public EventMetadata withAttributes(Map<String, Object> attributes) {
    Objects.requireNonNull(attributes, "attributes");
    return new EventMetadata(
        correlation.correlationId(), correlation.causationId(), tenant.currentTenant(), attributes);
  }
}
