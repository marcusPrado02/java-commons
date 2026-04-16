package com.marcusprado02.commons.app.health.indicators;

import com.marcusprado02.commons.app.health.Health;
import com.marcusprado02.commons.app.health.HealthIndicator;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.util.Objects;

/**
 * Health indicator for URL connectivity.
 *
 * <p>Performs HTTP GET request to check if a URL is reachable.
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * HealthIndicator apiHealth = new UrlHealthIndicator(
 *     "external-api",
 *     "https://api.example.com/health",
 *     Duration.ofSeconds(5)
 * );
 * }</pre>
 */
public final class UrlHealthIndicator implements HealthIndicator {

  private final String name;
  private final String url;
  private final Duration timeout;
  private final boolean critical;

  /**
   * Creates a URL health indicator.
   *
   * @param name the indicator name
   * @param url the URL to check
   * @param timeout the connection timeout
   * @param critical whether this check is critical
   */
  public UrlHealthIndicator(String name, String url, Duration timeout, boolean critical) {
    this.name = Objects.requireNonNull(name, "name cannot be null");
    this.url = Objects.requireNonNull(url, "url cannot be null");
    this.timeout = timeout != null ? timeout : Duration.ofSeconds(3);
    this.critical = critical;
  }

  /**
   * Creates a non-critical URL health indicator with 3 second timeout.
   *
   * @param name the indicator name
   * @param url the URL to check
   */
  public UrlHealthIndicator(String name, String url) {
    this(name, url, Duration.ofSeconds(3), false);
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public Health check() {
    long startTime = System.currentTimeMillis();

    try {
      URL urlObj = new URL(url);
      HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection();
      connection.setRequestMethod("GET");
      connection.setConnectTimeout((int) timeout.toMillis());
      connection.setReadTimeout((int) timeout.toMillis());

      int responseCode = connection.getResponseCode();
      long responseTime = System.currentTimeMillis() - startTime;

      if (responseCode >= 200 && responseCode < 300) {
        return Health.up()
            .withDetail("url", url)
            .withDetail("responseCode", responseCode)
            .withDetail("responseTime", responseTime + "ms")
            .build();
      } else if (responseCode >= 500) {
        return Health.down()
            .withDetail("url", url)
            .withDetail("responseCode", responseCode)
            .withDetail("responseTime", responseTime + "ms")
            .build();
      } else {
        return Health.degraded()
            .withDetail("url", url)
            .withDetail("responseCode", responseCode)
            .withDetail("responseTime", responseTime + "ms")
            .build();
      }

    } catch (Exception e) {
      long responseTime = System.currentTimeMillis() - startTime;
      return Health.down()
          .withDetail("url", url)
          .withDetail("responseTime", responseTime + "ms")
          .withException(e)
          .build();
    }
  }

  @Override
  public boolean isCritical() {
    return critical;
  }
}
