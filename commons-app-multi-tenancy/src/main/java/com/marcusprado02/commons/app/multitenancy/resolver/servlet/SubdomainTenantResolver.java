package com.marcusprado02.commons.app.multitenancy.resolver.servlet;

import com.marcusprado02.commons.app.multitenancy.TenantContext;
import com.marcusprado02.commons.app.multitenancy.TenantResolver;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;

/**
 * Tenant resolver that extracts tenant ID from subdomain.
 *
 * <p>Supports patterns like:
 *
 * <ul>
 *   <li>{@code tenant1.example.com} → tenant1
 *   <li>{@code api-tenant2.example.com} → tenant2 (with prefix stripping)
 *   <li>{@code tenant3.api.example.com} → tenant3 (first subdomain)
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Simple subdomain extraction
 * TenantResolver<HttpServletRequest> resolver =
 *     new SubdomainTenantResolver();
 *
 * // With prefix stripping
 * TenantResolver<HttpServletRequest> resolver =
 *     new SubdomainTenantResolver("api-", "-staging");
 * }</pre>
 */
public class SubdomainTenantResolver implements TenantResolver<HttpServletRequest> {

  private final String[] prefixesToStrip;
  private final String[] suffixesToStrip;

  public SubdomainTenantResolver() {
    this.prefixesToStrip = new String[0];
    this.suffixesToStrip = new String[0];
  }

  public SubdomainTenantResolver(String... prefixesToStrip) {
    this.prefixesToStrip = prefixesToStrip != null ? prefixesToStrip : new String[0];
    this.suffixesToStrip = new String[0];
  }

  public SubdomainTenantResolver(String[] prefixesToStrip, String[] suffixesToStrip) {
    this.prefixesToStrip = prefixesToStrip != null ? prefixesToStrip : new String[0];
    this.suffixesToStrip = suffixesToStrip != null ? suffixesToStrip : new String[0];
  }

  @Override
  public Optional<TenantContext> resolve(HttpServletRequest request) {
    String host = getHost(request);
    if (host == null) {
      return Optional.empty();
    }

    String subdomain = extractSubdomain(host);
    if (subdomain == null || subdomain.isEmpty()) {
      return Optional.empty();
    }

    String tenantId = cleanTenantId(subdomain);
    if (tenantId.isEmpty()) {
      return Optional.empty();
    }

    return Optional.of(TenantContext.builder()
        .tenantId(tenantId)
        .domain(host)
        .build());
  }

  @Override
  public int getPriority() {
    return 20; // Medium-high priority
  }

  private String getHost(HttpServletRequest request) {
    String host = request.getHeader("Host");
    if (host != null) {
      // Remove port if present
      int colonIndex = host.indexOf(':');
      if (colonIndex > 0) {
        host = host.substring(0, colonIndex);
      }
    }
    return host;
  }

  private String extractSubdomain(String host) {
    if (host == null) {
      return null;
    }

    String[] parts = host.split("\\.");
    if (parts.length < 3) {
      // Need at least subdomain.domain.tld
      return null;
    }

    // Return first part as subdomain
    return parts[0];
  }

  private String cleanTenantId(String subdomain) {
    String tenantId = subdomain;

    // Strip prefixes
    for (String prefix : prefixesToStrip) {
      if (tenantId.startsWith(prefix)) {
        tenantId = tenantId.substring(prefix.length());
      }
    }

    // Strip suffixes
    for (String suffix : suffixesToStrip) {
      if (tenantId.endsWith(suffix)) {
        tenantId = tenantId.substring(0, tenantId.length() - suffix.length());
      }
    }

    return tenantId;
  }
}
