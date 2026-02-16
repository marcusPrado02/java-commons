package com.marcusprado02.commons.app.apiversion;

import java.util.Optional;

/**
 * Strategy interface for resolving the API version from a request.
 *
 * <p>Different implementations can extract the version from different sources such as URL path,
 * headers, query parameters, or content negotiation.
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * // URL-based versioning
 * VersionResolver urlResolver = new UrlPathVersionResolver();
 * Optional<ApiVersion> version = urlResolver.resolve(request);
 *
 * // Header-based versioning
 * VersionResolver headerResolver = new HeaderVersionResolver("Api-Version");
 * Optional<ApiVersion> version = headerResolver.resolve(request);
 * }</pre>
 *
 * @param <T> the request type (e.g., HttpServletRequest, ServerHttpRequest)
 */
@FunctionalInterface
public interface VersionResolver<T> {

  /**
   * Resolves the API version from the request.
   *
   * @param request the request object
   * @return optional containing the resolved version, or empty if not found
   */
  Optional<ApiVersion> resolve(T request);

  /**
   * Creates a composite resolver that tries multiple resolvers in sequence.
   *
   * @param resolvers the resolvers to try
   * @param <T> the request type
   * @return a composite resolver
   */
  @SafeVarargs
  static <T> VersionResolver<T> composite(VersionResolver<T>... resolvers) {
    return request -> {
      for (VersionResolver<T> resolver : resolvers) {
        Optional<ApiVersion> version = resolver.resolve(request);
        if (version.isPresent()) {
          return version;
        }
      }
      return Optional.empty();
    };
  }
}
