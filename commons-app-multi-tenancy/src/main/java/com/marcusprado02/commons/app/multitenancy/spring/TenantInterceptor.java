package com.marcusprado02.commons.app.multitenancy.spring;

import com.marcusprado02.commons.app.multitenancy.TenantContext;
import com.marcusprado02.commons.app.multitenancy.TenantContextHolder;
import com.marcusprado02.commons.app.multitenancy.TenantResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;
import java.util.Optional;

/**
 * Spring MVC interceptor that resolves and sets tenant context for each request.
 *
 * <p>Alternative to {@link TenantFilter} for Spring MVC applications. The interceptor sets tenant
 * context before handler execution and ensures cleanup after completion.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * @Configuration
 * public class WebConfig implements WebMvcConfigurer {
 *
 *     @Autowired
 *     private TenantResolver<HttpServletRequest> tenantResolver;
 *
 *     @Override
 *     public void addInterceptors(InterceptorRegistry registry) {
 *         registry.addInterceptor(new TenantInterceptor(tenantResolver))
 *             .addPathPatterns("/**")
 *             .excludePathPatterns("/health", "/metrics");
 *     }
 * }
 * }</pre>
 */
public class TenantInterceptor implements HandlerInterceptor {

  private final TenantResolver<HttpServletRequest> tenantResolver;

  public TenantInterceptor(TenantResolver<HttpServletRequest> tenantResolver) {
    this.tenantResolver = tenantResolver;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    Optional<TenantContext> tenantContext = tenantResolver.resolve(request);

    if (tenantContext.isPresent()) {
      TenantContextHolder.setContext(tenantContext.get());
    }

    return true;
  }

  @Override
  public void afterCompletion(
      HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
    TenantContextHolder.clear();
  }
}
