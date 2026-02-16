/**
 * Strategy implementations for resolving API versions from different sources.
 *
 * <p>This package contains concrete implementations of {@link
 * com.marcusprado02.commons.app.apiversion.VersionResolver} that can extract version information
 * from:
 *
 * <ul>
 *   <li>URL path segments ({@link UrlPathVersionResolver})
 *   <li>HTTP headers ({@link HeaderVersionResolver})
 *   <li>Accept header media types ({@link MediaTypeVersionResolver})
 *   <li>Query parameters ({@link QueryParameterVersionResolver})
 * </ul>
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * // Combine multiple resolvers
 * VersionResolver<HttpServletRequest> resolver = VersionResolver.composite(
 *     new UrlPathVersionResolver<>(HttpServletRequest::getRequestURI),
 *     new HeaderVersionResolver<>("Api-Version", HttpServletRequest::getHeader)
 * );
 * }</pre>
 */
package com.marcusprado02.commons.app.apiversion.resolver;
