package com.marcusprado02.commons.app.multitenancy.spring;

import com.marcusprado02.commons.app.multitenancy.TenantContext;
import com.marcusprado02.commons.app.multitenancy.TenantContextHolder;
import com.marcusprado02.commons.app.multitenancy.TenantResolver;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Optional;

/**
 * Servlet filter that resolves and sets tenant context for each request.
 *
 * <p>The filter uses configured {@link TenantResolver} to identify the tenant from the HTTP request
 * and sets the tenant context in {@link TenantContextHolder} for the duration of the request.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * @Configuration
 * public class FilterConfig {
 *
 *     @Bean
 *     public FilterRegistrationBean<TenantFilter> tenantFilter(
 *         TenantResolver<HttpServletRequest> resolver) {
 *
 *         FilterRegistrationBean<TenantFilter> registration =
 *             new FilterRegistrationBean<>();
 *         registration.setFilter(new TenantFilter(resolver));
 *         registration.setOrder(1);
 *         return registration;
 *     }
 * }
 * }</pre>
 */
public class TenantFilter implements Filter {

  private final TenantResolver<HttpServletRequest> tenantResolver;

  public TenantFilter(TenantResolver<HttpServletRequest> tenantResolver) {
    this.tenantResolver = tenantResolver;
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    if (!(request instanceof HttpServletRequest httpRequest)) {
      chain.doFilter(request, response);
      return;
    }

    try {
      Optional<TenantContext> tenantContext = tenantResolver.resolve(httpRequest);

      if (tenantContext.isPresent()) {
        TenantContextHolder.setContext(tenantContext.get());
      }

      chain.doFilter(request, response);

    } finally {
      TenantContextHolder.clear();
    }
  }
}
