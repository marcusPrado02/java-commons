package com.marcusprado02.commons.app.multitenancy;

import java.util.Optional;

/**
 * Tenant resolver interface.
 *
 * <p>Implementations resolve tenant context from different sources (HTTP headers, subdomains, paths,
 * etc.).
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Header-based resolver
 * TenantResolver resolver = new HeaderTenantResolver("X-Tenant-ID");
 * Optional<TenantContext> context = resolver.resolve(request);
 *
 * // Subdomain-based resolver
 * TenantResolver resolver = new SubdomainTenantResolver();
 * Optional<TenantContext> context = resolver.resolve(request);
 * }</pre>
 *
 * @param <T> request type (HttpServletRequest, ServerRequest, etc.)
 */
public interface TenantResolver<T> {

  /**
   * Resolves tenant context from the request.
   *
   * @param request HTTP request
   * @return tenant context if resolved, empty otherwise
   */
  Optional<TenantContext> resolve(T request);

  /**
   * Gets the priority of this resolver (lower numbers = higher priority).
   *
   * @return priority value
   */
  default int getPriority() {
    return 100;
  }
}
