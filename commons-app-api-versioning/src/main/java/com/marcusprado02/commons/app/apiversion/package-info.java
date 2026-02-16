/**
 * API Versioning abstraction for managing multiple API versions.
 *
 * <p>This module provides interfaces and implementations for handling API versioning using
 * different strategies:
 *
 * <ul>
 *   <li><b>URL Path Versioning:</b> Version in the URL path (e.g., /v1/users, /v2/products)
 *   <li><b>Header Versioning:</b> Version in custom headers (e.g., Api-Version: 1)
 *   <li><b>Content Negotiation:</b> Version in Accept header (e.g.,
 *       application/vnd.company.v1+json)
 *   <li><b>Query Parameter:</b> Version in query string (e.g., /users?version=1)
 * </ul>
 *
 * <h2>Core Concepts</h2>
 *
 * <ul>
 *   <li>{@link com.marcusprado02.commons.app.apiversion.ApiVersion}: Represents an API version
 *       (major.minor)
 *   <li>{@link com.marcusprado02.commons.app.apiversion.VersionResolver}: Strategy for extracting
 *       version from requests
 *   <li>{@link com.marcusprado02.commons.app.apiversion.VersionRegistry}: Registry of supported and
 *       deprecated versions
 *   <li>{@link com.marcusprado02.commons.app.apiversion.DeprecationInfo}: Metadata about deprecated
 *       versions
 * </ul>
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * // Define versions
 * ApiVersion v1 = ApiVersion.of(1, 0);
 * ApiVersion v2 = ApiVersion.of(2, 0);
 *
 * // Register versions
 * VersionRegistry registry = VersionRegistry.builder()
 *     .defaultVersion(v1)
 *     .latestVersion(v2)
 *     .deprecate(
 *         DeprecationInfo.builder(v1)
 *             .sunsetDate(LocalDate.of(2025, 12, 31))
 *             .replacementVersion(v2)
 *             .build()
 *     )
 *     .build();
 *
 * // Resolve version from request
 * VersionResolver<HttpServletRequest> resolver = VersionResolver.composite(
 *     new UrlPathVersionResolver<>(HttpServletRequest::getRequestURI),
 *     new HeaderVersionResolver<>("Api-Version", HttpServletRequest::getHeader)
 * );
 *
 * Optional<ApiVersion> version = resolver.resolve(request);
 * ApiVersion actualVersion = version.orElse(registry.getDefaultVersion());
 *
 * // Check deprecation
 * if (registry.isDeprecated(actualVersion)) {
 *     DeprecationInfo info = registry.getDeprecationInfo(actualVersion).get();
 *     // Add deprecation warnings to response
 * }
 * }</pre>
 *
 * <h2>Best Practices</h2>
 *
 * <ul>
 *   <li>Use semantic versioning (major.minor) for API versions
 *   <li>Increment major version for breaking changes
 *   <li>Increment minor version for backward-compatible changes
 *   <li>Provide clear deprecation notices with sunset dates
 *   <li>Maintain at least 2 major versions simultaneously
 *   <li>Give clients sufficient migration time (6-12 months)
 * </ul>
 */
package com.marcusprado02.commons.app.apiversion;
