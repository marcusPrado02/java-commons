package com.marcusprado02.commons.adapters.web.spring.cors;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for CORS (Cross-Origin Resource Sharing).
 *
 * <p><strong>Example configuration (application.yml):</strong>
 *
 * <pre>
 * commons:
 *   web:
 *     cors:
 *       enabled: true
 *       allowed-origins:
 *         - http://localhost:3000
 *         - https://app.example.com
 *       allowed-methods:
 *         - GET
 *         - POST
 *         - PUT
 *         - DELETE
 *       allowed-headers:
 *         - "*"
 *       exposed-headers:
 *         - X-Correlation-Id
 *         - X-Total-Count
 *       allow-credentials: true
 *       max-age: 3600
 * </pre>
 */
@ConfigurationProperties(prefix = "commons.web.cors")
public class CorsProperties {

  /** Whether CORS is enabled. Default: false */
  private boolean enabled = false;

  /** Allowed origins. Use "*" to allow all origins. Default: empty (no origins allowed) */
  private List<String> allowedOrigins = List.of();

  /**
   * Allowed origin patterns. Supports wildcards (e.g., "https://*.example.com"). Default: empty
   */
  private List<String> allowedOriginPatterns = List.of();

  /** Allowed HTTP methods. Default: GET, POST, PUT, DELETE, PATCH */
  private List<String> allowedMethods = List.of("GET", "POST", "PUT", "DELETE", "PATCH");

  /** Allowed headers. Use "*" to allow all headers. Default: * */
  private List<String> allowedHeaders = List.of("*");

  /** Exposed headers visible to the client. Default: empty */
  private List<String> exposedHeaders = List.of();

  /** Whether credentials (cookies, authorization headers) are allowed. Default: false */
  private boolean allowCredentials = false;

  /** How long (in seconds) the browser should cache preflight responses. Default: 1 hour */
  private Duration maxAge = Duration.ofHours(1);

  /** URL patterns to apply CORS configuration to. Default: /** */
  private List<String> pathPatterns = List.of("/**");

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public List<String> getAllowedOrigins() {
    return allowedOrigins;
  }

  public void setAllowedOrigins(List<String> allowedOrigins) {
    this.allowedOrigins = allowedOrigins;
  }

  public List<String> getAllowedOriginPatterns() {
    return allowedOriginPatterns;
  }

  public void setAllowedOriginPatterns(List<String> allowedOriginPatterns) {
    this.allowedOriginPatterns = allowedOriginPatterns;
  }

  public List<String> getAllowedMethods() {
    return allowedMethods;
  }

  public void setAllowedMethods(List<String> allowedMethods) {
    this.allowedMethods = allowedMethods;
  }

  public List<String> getAllowedHeaders() {
    return allowedHeaders;
  }

  public void setAllowedHeaders(List<String> allowedHeaders) {
    this.allowedHeaders = allowedHeaders;
  }

  public List<String> getExposedHeaders() {
    return exposedHeaders;
  }

  public void setExposedHeaders(List<String> exposedHeaders) {
    this.exposedHeaders = exposedHeaders;
  }

  public boolean isAllowCredentials() {
    return allowCredentials;
  }

  public void setAllowCredentials(boolean allowCredentials) {
    this.allowCredentials = allowCredentials;
  }

  public Duration getMaxAge() {
    return maxAge;
  }

  public void setMaxAge(Duration maxAge) {
    this.maxAge = maxAge;
  }

  public List<String> getPathPatterns() {
    return pathPatterns;
  }

  public void setPathPatterns(List<String> pathPatterns) {
    this.pathPatterns = pathPatterns;
  }
}
