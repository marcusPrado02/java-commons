package com.marcusprado02.commons.app.multitenancy.resolver;

import com.marcusprado02.commons.app.multitenancy.TenantContext;
import com.marcusprado02.commons.app.multitenancy.TenantResolver;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Composite tenant resolver that tries multiple resolvers in priority order.
 *
 * <p>Iterates through resolvers by priority and returns the first successful resolution.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * CompositeTenantResolver<HttpServletRequest> resolver =
 *     CompositeTenantResolver.<HttpServletRequest>builder()
 *         .resolver(new HeaderTenantResolver("X-Tenant-ID"))
 *         .resolver(new SubdomainTenantResolver())
 *         .resolver(new PathTenantResolver())
 *         .build();
 *
 * Optional<TenantContext> context = resolver.resolve(request);
 * }</pre>
 *
 * @param <T> request type
 */
public class CompositeTenantResolver<T> implements TenantResolver<T> {

  private final List<TenantResolver<T>> resolvers;

  public CompositeTenantResolver(List<TenantResolver<T>> resolvers) {
    this.resolvers = resolvers.stream()
        .sorted(Comparator.comparingInt(TenantResolver::getPriority))
        .toList();
  }

  @Override
  public Optional<TenantContext> resolve(T request) {
    for (TenantResolver<T> resolver : resolvers) {
      Optional<TenantContext> result = resolver.resolve(request);
      if (result.isPresent()) {
        return result;
      }
    }
    return Optional.empty();
  }

  @Override
  public int getPriority() {
    return resolvers.isEmpty() ? 100 : resolvers.get(0).getPriority();
  }

  public static <T> Builder<T> builder() {
    return new Builder<>();
  }

  public static class Builder<T> {
    private final java.util.List<TenantResolver<T>> resolvers = new java.util.ArrayList<>();

    public Builder<T> resolver(TenantResolver<T> resolver) {
      this.resolvers.add(resolver);
      return this;
    }

    public Builder<T> resolvers(List<TenantResolver<T>> resolvers) {
      this.resolvers.addAll(resolvers);
      return this;
    }

    public CompositeTenantResolver<T> build() {
      return new CompositeTenantResolver<>(List.copyOf(resolvers));
    }
  }
}
