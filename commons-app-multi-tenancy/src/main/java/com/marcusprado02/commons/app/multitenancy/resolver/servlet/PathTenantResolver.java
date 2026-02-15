package com.marcusprado02.commons.app.multitenancy.resolver.servlet;

import com.marcusprado02.commons.app.multitenancy.TenantContext;
import com.marcusprado02.commons.app.multitenancy.TenantResolver;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tenant resolver that extracts tenant ID from URL path.
 *
 * <p>Supports patterns like:
 *
 * <ul>
 *   <li>{@code /tenant1/api/users} → tenant1
 *   <li>{@code /api/v1/tenant2/users} → tenant2
 *   <li>{@code /t/tenant3/data} → tenant3 (custom prefix)
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Extract from first path segment
 * TenantResolver<HttpServletRequest> resolver =
 *     new PathTenantResolver("/([^/]+)/.*");
 *
 * // Extract from specific position
 * TenantResolver<HttpServletRequest> resolver =
 *     new PathTenantResolver("/api/v1/([^/]+)/.*");
 *
 * // Custom prefix
 * TenantResolver<HttpServletRequest> resolver =
 *     new PathTenantResolver("/t/([^/]+)/.*");
 * }</pre>
 */
public class PathTenantResolver implements TenantResolver<HttpServletRequest> {

  private final Pattern pattern;

  /**
   * Creates path resolver with default pattern (first path segment).
   */
  public PathTenantResolver() {
    this("/([^/]+)/.*");
  }

  /**
   * Creates path resolver with custom regex pattern.
   *
   * @param pathPattern regex pattern with single capture group for tenant ID
   */
  public PathTenantResolver(String pathPattern) {
    this.pattern = Pattern.compile(pathPattern);
  }

  @Override
  public Optional<TenantContext> resolve(HttpServletRequest request) {
    String path = request.getRequestURI();
    if (path == null) {
      return Optional.empty();
    }

    Matcher matcher = pattern.matcher(path);
    if (!matcher.matches()) {
      return Optional.empty();
    }

    String tenantId = matcher.group(1);
    if (tenantId == null || tenantId.trim().isEmpty()) {
      return Optional.empty();
    }

    return Optional.of(TenantContext.of(tenantId.trim()));
  }

  @Override
  public int getPriority() {
    return 30; // Medium priority
  }
}
