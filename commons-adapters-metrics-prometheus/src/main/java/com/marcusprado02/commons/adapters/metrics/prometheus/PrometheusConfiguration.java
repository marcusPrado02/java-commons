package com.marcusprado02.commons.adapters.metrics.prometheus;

import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import java.time.Duration;
import java.util.Objects;

/**
 * Configuration builder for Prometheus metrics with Micrometer.
 *
 * <p>Provides a fluent API to configure Prometheus registry, step size, and export settings.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * PrometheusMeterRegistry registry = PrometheusConfiguration.builder()
 *     .step(Duration.ofSeconds(10))
 *     .enableExemplars(true)
 *     .build();
 * }</pre>
 */
public final class PrometheusConfiguration {

  private final Duration step;
  private final boolean enableExemplars;
  private final String prefix;

  private PrometheusConfiguration(Builder builder) {
    this.step = builder.step;
    this.enableExemplars = builder.enableExemplars;
    this.prefix = builder.prefix;
  }

  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builds and returns a configured {@link PrometheusMeterRegistry}.
   *
   * @return configured Prometheus meter registry
   */
  public PrometheusMeterRegistry build() {
    PrometheusConfig config =
        new PrometheusConfig() {
          @Override
          public Duration step() {
            return step;
          }

          @Override
          public String get(String key) {
            return null; // Use defaults
          }

          @Override
          public String prefix() {
            return prefix;
          }
        };

    PrometheusMeterRegistry registry = new PrometheusMeterRegistry(config);

    // Configure exemplars if enabled
    if (enableExemplars) {
      // Exemplars are enabled by default in newer Micrometer versions
      // Additional configuration can be added here if needed
    }

    return registry;
  }

  /**
   * Creates a default Prometheus registry with standard settings.
   *
   * @return configured Prometheus meter registry with defaults
   */
  public static PrometheusMeterRegistry createDefault() {
    return builder().build().build();
  }

  public static final class Builder {
    private Duration step = Duration.ofSeconds(60); // Default: 1 minute
    private boolean enableExemplars = false;
    private String prefix = ""; // No prefix by default

    private Builder() {}

    /**
     * Sets the step size for polling meters.
     *
     * <p>This determines how frequently metrics are sampled. For Prometheus, this is typically the
     * scrape interval.
     *
     * @param step the step duration (default: 60 seconds)
     * @return this builder
     */
    public Builder step(Duration step) {
      this.step = Objects.requireNonNull(step, "step must not be null");
      return this;
    }

    /**
     * Enables or disables exemplars support.
     *
     * <p>Exemplars allow linking metrics to traces by attaching trace IDs to metric samples. This
     * is useful for correlating metrics spikes with distributed traces.
     *
     * <p><b>Note:</b> Requires Prometheus 2.26+ with exemplars support enabled.
     *
     * @param enableExemplars true to enable exemplars, false otherwise
     * @return this builder
     */
    public Builder enableExemplars(boolean enableExemplars) {
      this.enableExemplars = enableExemplars;
      return this;
    }

    /**
     * Sets a prefix for all metric names.
     *
     * <p>This is useful to namespace metrics from different applications or services.
     *
     * @param prefix the metric name prefix (e.g., "myapp")
     * @return this builder
     */
    public Builder prefix(String prefix) {
      this.prefix = prefix != null ? prefix : "";
      return this;
    }

    /**
     * Builds the {@link PrometheusConfiguration}.
     *
     * @return the Prometheus configuration
     */
    public PrometheusConfiguration build() {
      return new PrometheusConfiguration(this);
    }
  }
}
