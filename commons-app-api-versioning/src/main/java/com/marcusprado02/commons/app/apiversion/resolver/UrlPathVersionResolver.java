package com.marcusprado02.commons.app.apiversion.resolver;

import com.marcusprado02.commons.app.apiversion.ApiVersion;
import com.marcusprado02.commons.app.apiversion.VersionResolver;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves API version from URL path segments.
 *
 * <p>Supports patterns like:
 *
 * <ul>
 *   <li>/v1/users
 *   <li>/api/v2/products
 *   <li>/v1.2/orders
 * </ul>
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * VersionResolver<HttpServletRequest> resolver = new UrlPathVersionResolver();
 * Optional<ApiVersion> version = resolver.resolve(request);
 * }</pre>
 *
 * @param <T> the request type
 */
public class UrlPathVersionResolver<T> implements VersionResolver<T> {

  private static final Pattern VERSION_PATTERN = Pattern.compile("/v(\\d+)(?:\\.(\\d+))?");

  private final PathExtractor<T> pathExtractor;

  public UrlPathVersionResolver(PathExtractor<T> pathExtractor) {
    this.pathExtractor = Objects.requireNonNull(pathExtractor, "pathExtractor cannot be null");
  }

  @Override
  public Optional<ApiVersion> resolve(T request) {
    String path = pathExtractor.extractPath(request);
    if (path == null || path.isEmpty()) {
      return Optional.empty();
    }

    Matcher matcher = VERSION_PATTERN.matcher(path);
    if (matcher.find()) {
      try {
        int major = Integer.parseInt(matcher.group(1));
        int minor = matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : 0;
        return Optional.of(ApiVersion.of(major, minor));
      } catch (NumberFormatException e) {
        return Optional.empty();
      }
    }

    return Optional.empty();
  }

  @FunctionalInterface
  public interface PathExtractor<T> {
    String extractPath(T request);
  }
}
