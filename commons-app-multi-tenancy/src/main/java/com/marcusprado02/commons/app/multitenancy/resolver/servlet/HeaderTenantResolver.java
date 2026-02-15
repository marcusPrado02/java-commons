package com.marcusprado02.commons.app.multitenancy.resolver.servlet;

import com.marcusprado02.commons.app.multitenancy.TenantContext;
import com.marcusprado02.commons.app.multitenancy.TenantResolver;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;

/**
 * Tenant resolver that extracts tenant ID from HTTP header.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * TenantResolver<HttpServletRequest> resolver =
 *     new HeaderTenantResolver("X-Tenant-ID");
 *
 * Optional<TenantContext> context = resolver.resolve(request);
 * }</pre>
 */
public class HeaderTenantResolver implements TenantResolver<HttpServletRequest> {

  private final String headerName;

  public HeaderTenantResolver(String headerName) {
    this.headerName = headerName;
  }

  @Override
  public Optional<TenantContext> resolve(HttpServletRequest request) {
    String tenantId = request.getHeader(headerName);

    if (tenantId == null || tenantId.trim().isEmpty()) {
      return Optional.empty();
    }

    return Optional.of(TenantContext.of(tenantId.trim()));
  }

  @Override
  public int getPriority() {
    return 10; // High priority
  }
}
