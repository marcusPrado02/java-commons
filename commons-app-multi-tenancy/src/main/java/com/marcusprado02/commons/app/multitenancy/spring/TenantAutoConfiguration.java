package com.marcusprado02.commons.app.multitenancy.spring;

import com.marcusprado02.commons.app.multitenancy.TenantResolver;
import com.marcusprado02.commons.app.multitenancy.resolver.CompositeTenantResolver;
import com.marcusprado02.commons.app.multitenancy.resolver.servlet.HeaderTenantResolver;
import com.marcusprado02.commons.app.multitenancy.resolver.servlet.PathTenantResolver;
import com.marcusprado02.commons.app.multitenancy.resolver.servlet.SubdomainTenantResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration for multi-tenancy support.
 *
 * <p>Provides default tenant resolver and Spring integration components.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * @Configuration
 * @Import(TenantAutoConfiguration.class)
 * public class AppConfig {
 *
 *     // Override default resolvers if needed
 *     @Bean
 *     @Primary
 *     public TenantResolver<HttpServletRequest> customTenantResolver() {
 *         return new HeaderTenantResolver("X-Custom-Tenant");
 *     }
 * }
 * }</pre>
 *
 * <p>For filter registration in Spring Boot applications, add:
 *
 * <pre>{@code
 * @Bean
 * public FilterRegistrationBean<TenantFilter> tenantFilterRegistration(
 *         TenantResolver<HttpServletRequest> tenantResolver) {
 *     FilterRegistrationBean<TenantFilter> registration = new FilterRegistrationBean<>();
 *     registration.setFilter(new TenantFilter(tenantResolver));
 *     registration.setUrlPatterns(List.of("/*"));
 *     registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
 *     return registration;
 * }
 * }</pre>
 */
@Configuration
public class TenantAutoConfiguration {

  /**
   * Default composite tenant resolver with header, subdomain, and path resolution.
   *
   * <p>Tries resolvers in priority order:
   *
   * <ol>
   *   <li>Header-based (X-Tenant-ID) - Priority 10
   *   <li>Header-based (Tenant-ID) - Priority 10
   *   <li>Subdomain-based - Priority 20
   *   <li>Path-based - Priority 30
   * </ol>
   *
   * @return composite tenant resolver
   */
  @Bean
  public TenantResolver<HttpServletRequest> tenantResolver() {
    return CompositeTenantResolver.<HttpServletRequest>builder()
        .resolver(new HeaderTenantResolver("X-Tenant-ID"))
        .resolver(new HeaderTenantResolver("Tenant-ID"))
        .resolver(new SubdomainTenantResolver())
        .resolver(new PathTenantResolver())
        .build();
  }

  /**
   * Tenant filter for automatic tenant context management.
   *
   * <p>This filter needs to be manually registered in web applications. For Spring Boot
   * applications, use FilterRegistrationBean.
   *
   * @param tenantResolver the tenant resolver to use
   * @return tenant filter
   */
  @Bean
  public TenantFilter tenantFilter(TenantResolver<HttpServletRequest> tenantResolver) {
    return new TenantFilter(tenantResolver);
  }
}
