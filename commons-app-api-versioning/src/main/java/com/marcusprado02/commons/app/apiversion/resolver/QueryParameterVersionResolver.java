package com.marcusprado02.commons.app.apiversion.resolver;

import com.marcusprado02.commons.app.apiversion.ApiVersion;
import com.marcusprado02.commons.app.apiversion.VersionResolver;
import java.util.Objects;
import java.util.Optional;

/**
 * Resolves API version from query parameters.
 *
 * <p>Supports query strings like:
 *
 * <ul>
 *   <li>/users?version=1
 *   <li>/products?api-version=v2
 *   <li>/orders?v=1.2
 * </ul>
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * VersionResolver<HttpServletRequest> resolver =
 *     new QueryParameterVersionResolver<>(
 *         "version",
 *         (req, param) -> req.getParameter(param));
 * Optional<ApiVersion> version = resolver.resolve(request);
 * }</pre>
 *
 * @param <T> the request type
 */
public class QueryParameterVersionResolver<T> implements VersionResolver<T> {

  private final String parameterName;
  private final ParameterExtractor<T> parameterExtractor;

  public QueryParameterVersionResolver(
      String parameterName, ParameterExtractor<T> parameterExtractor) {
    this.parameterName = Objects.requireNonNull(parameterName, "parameterName cannot be null");
    this.parameterExtractor =
        Objects.requireNonNull(parameterExtractor, "parameterExtractor cannot be null");
  }

  @Override
  public Optional<ApiVersion> resolve(T request) {
    String paramValue = parameterExtractor.extractParameter(request, parameterName);
    if (paramValue == null || paramValue.isEmpty()) {
      return Optional.empty();
    }

    try {
      return Optional.of(ApiVersion.parse(paramValue));
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }

  @FunctionalInterface
  public interface ParameterExtractor<T> {
    String extractParameter(T request, String parameterName);
  }
}
