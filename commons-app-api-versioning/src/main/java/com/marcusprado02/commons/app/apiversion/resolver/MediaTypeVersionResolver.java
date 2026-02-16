package com.marcusprado02.commons.app.apiversion.resolver;

import com.marcusprado02.commons.app.apiversion.ApiVersion;
import com.marcusprado02.commons.app.apiversion.VersionResolver;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves API version from Accept header media type.
 *
 * <p>Supports vendor-specific media types like:
 *
 * <ul>
 *   <li>application/vnd.mycompany.v1+json
 *   <li>application/vnd.mycompany.v2+xml
 *   <li>application/vnd.api.v1.2+json
 * </ul>
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * VersionResolver<HttpServletRequest> resolver =
 *     new MediaTypeVersionResolver<>(
 *         "mycompany",
 *         req -> req.getHeader("Accept"));
 * Optional<ApiVersion> version = resolver.resolve(request);
 * }</pre>
 *
 * @param <T> the request type
 */
public class MediaTypeVersionResolver<T> implements VersionResolver<T> {

  private final Pattern versionPattern;
  private final AcceptHeaderExtractor<T> acceptHeaderExtractor;

  /**
   * Creates a media type version resolver with a vendor prefix.
   *
   * @param vendorPrefix the vendor prefix (e.g., "mycompany")
   * @param acceptHeaderExtractor function to extract Accept header
   */
  public MediaTypeVersionResolver(
      String vendorPrefix, AcceptHeaderExtractor<T> acceptHeaderExtractor) {
    Objects.requireNonNull(vendorPrefix, "vendorPrefix cannot be null");
    this.acceptHeaderExtractor =
        Objects.requireNonNull(acceptHeaderExtractor, "acceptHeaderExtractor cannot be null");
    this.versionPattern =
        Pattern.compile(
            "application/vnd\\." + Pattern.quote(vendorPrefix) + "\\.v(\\d+)(?:\\.(\\d+))?");
  }

  @Override
  public Optional<ApiVersion> resolve(T request) {
    String acceptHeader = acceptHeaderExtractor.extractAcceptHeader(request);
    if (acceptHeader == null || acceptHeader.isEmpty()) {
      return Optional.empty();
    }

    Matcher matcher = versionPattern.matcher(acceptHeader);
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
  public interface AcceptHeaderExtractor<T> {
    String extractAcceptHeader(T request);
  }
}
