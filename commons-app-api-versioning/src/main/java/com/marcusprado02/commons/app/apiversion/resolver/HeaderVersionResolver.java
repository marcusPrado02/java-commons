package com.marcusprado02.commons.app.apiversion.resolver;

import com.marcusprado02.commons.app.apiversion.ApiVersion;
import com.marcusprado02.commons.app.apiversion.VersionResolver;
import java.util.Objects;
import java.util.Optional;

/**
 * Resolves API version from HTTP headers.
 *
 * <p>Supports custom header names like:
 *
 * <ul>
 *   <li>Api-Version: 1
 *   <li>X-API-Version: v2
 *   <li>Version: 1.2
 * </ul>
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * VersionResolver<HttpServletRequest> resolver =
 *     new HeaderVersionResolver<>(req -> req.getHeader("Api-Version"));
 * Optional<ApiVersion> version = resolver.resolve(request);
 * }</pre>
 *
 * @param <T> the request type
 */
public class HeaderVersionResolver<T> implements VersionResolver<T> {

  private final String headerName;
  private final HeaderExtractor<T> headerExtractor;

  /**
   * Creates a resolver that reads the API version from a request header.
   *
   * @param headerName the HTTP header name to inspect
   * @param headerExtractor strategy to read the header value from the request
   */
  public HeaderVersionResolver(String headerName, HeaderExtractor<T> headerExtractor) {
    this.headerName = Objects.requireNonNull(headerName, "headerName cannot be null");
    this.headerExtractor =
        Objects.requireNonNull(headerExtractor, "headerExtractor cannot be null");
  }

  @Override
  public Optional<ApiVersion> resolve(T request) {
    String headerValue = headerExtractor.extractHeader(request, headerName);
    if (headerValue == null || headerValue.isEmpty()) {
      return Optional.empty();
    }

    try {
      return Optional.of(ApiVersion.parse(headerValue));
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }

  /** Extracts a named header value from a request of type {@code T}. */
  @FunctionalInterface
  public interface HeaderExtractor<T> {
    String extractHeader(T request, String headerName);
  }
}
