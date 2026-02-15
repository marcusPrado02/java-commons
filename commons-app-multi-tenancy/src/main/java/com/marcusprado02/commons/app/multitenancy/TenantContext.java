package com.marcusprado02.commons.app.multitenancy;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Tenant context information.
 *
 * <p>Contains tenant identification, configuration, and metadata for multi-tenant operations.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * TenantContext context = TenantContext.builder()
 *     .tenantId("tenant123")
 *     .name("Acme Corp")
 *     .domain("acme.example.com")
 *     .attribute("plan", "premium")
 *     .attribute("region", "us-east-1")
 *     .build();
 * }</pre>
 */
public record TenantContext(
    String tenantId, String name, String domain, Map<String, Object> attributes) {

  public TenantContext {
    Objects.requireNonNull(tenantId, "tenantId");
    attributes = attributes != null ? Map.copyOf(attributes) : Map.of();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static TenantContext of(String tenantId) {
    return new TenantContext(tenantId, null, null, Map.of());
  }

  public static TenantContext of(String tenantId, String name) {
    return new TenantContext(tenantId, name, null, Map.of());
  }

  public Optional<String> getName() {
    return Optional.ofNullable(name);
  }

  public Optional<String> getDomain() {
    return Optional.ofNullable(domain);
  }

  public Optional<Object> getAttribute(String key) {
    return Optional.ofNullable(attributes.get(key));
  }

  @SuppressWarnings("unchecked")
  public <T> Optional<T> getAttribute(String key, Class<T> type) {
    Object value = attributes.get(key);
    if (value != null && type.isInstance(value)) {
      return Optional.of((T) value);
    }
    return Optional.empty();
  }

  public static class Builder {
    private String tenantId;
    private String name;
    private String domain;
    private Map<String, Object> attributes = Map.of();

    public Builder tenantId(String tenantId) {
      this.tenantId = tenantId;
      return this;
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder domain(String domain) {
      this.domain = domain;
      return this;
    }

    public Builder attributes(Map<String, Object> attributes) {
      this.attributes = attributes;
      return this;
    }

    public Builder attribute(String key, Object value) {
      this.attributes = new java.util.HashMap<>(attributes);
      this.attributes.put(key, value);
      return this;
    }

    public TenantContext build() {
      return new TenantContext(tenantId, name, domain, attributes);
    }
  }
}
