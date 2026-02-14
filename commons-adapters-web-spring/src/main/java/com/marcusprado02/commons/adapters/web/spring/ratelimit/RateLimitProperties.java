package com.marcusprado02.commons.adapters.web.spring.ratelimit;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for rate limiting.
 *
 * <p><strong>Example configuration (application.yml):</strong>
 *
 * <pre>
 * commons:
 *   web:
 *     rate-limit:
 *       enabled: true
 *       limit: 100
 *       window: 1m
 *       key-type: IP_ADDRESS
 * </pre>
 */
@ConfigurationProperties(prefix = "commons.web.rate-limit")
public class RateLimitProperties {

  /** Whether rate limiting is enabled. Default: false */
  private boolean enabled = false;

  /** Maximum number of requests allowed per window. Default: 60 */
  private int limit = 60;

  /** Time window for rate limiting. Default: 1 minute */
  private Duration window = Duration.ofMinutes(1);

  /** Key type for rate limiting. Default: IP_ADDRESS */
  private KeyType keyType = KeyType.IP_ADDRESS;

  /** Custom header name for API key-based rate limiting. Only used when keyType = API_KEY */
  private String apiKeyHeader = "X-API-Key";

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public int getLimit() {
    return limit;
  }

  public void setLimit(int limit) {
    this.limit = limit;
  }

  public Duration getWindow() {
    return window;
  }

  public void setWindow(Duration window) {
    this.window = window;
  }

  public KeyType getKeyType() {
    return keyType;
  }

  public void setKeyType(KeyType keyType) {
    this.keyType = keyType;
  }

  public String getApiKeyHeader() {
    return apiKeyHeader;
  }

  public void setApiKeyHeader(String apiKeyHeader) {
    this.apiKeyHeader = apiKeyHeader;
  }

  public enum KeyType {
    /** Rate limit by client IP address */
    IP_ADDRESS,
    /** Rate limit by authenticated user (requires security context) */
    USER,
    /** Rate limit by API key from custom header */
    API_KEY
  }
}
