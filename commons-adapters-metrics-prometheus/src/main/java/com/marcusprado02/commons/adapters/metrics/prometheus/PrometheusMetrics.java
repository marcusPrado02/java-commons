package com.marcusprado02.commons.adapters.metrics.prometheus;

import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;

/**
 * Wrapper for Prometheus metrics using Micrometer.
 *
 * <p>Provides a simplified API for creating and managing Counter, Gauge, Histogram/Timer, and
 * Summary metrics with optional labels.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * PrometheusMetrics metrics = new PrometheusMetrics(registry);
 *
 * // Counter
 * Counter requests = metrics.counter("http.requests", "Total HTTP requests");
 * requests.increment();
 *
 * // Counter with labels
 * Counter statusCounter = metrics.counter("http.requests",
 *     "HTTP requests by status",
 *     MetricLabels.of("status", "200"));
 * statusCounter.increment();
 *
 * // Gauge
 * metrics.gauge("jvm.memory.used", "JVM memory used",
 *     runtime, Runtime::totalMemory);
 *
 * // Timer (histogram)
 * Timer timer = metrics.timer("http.request.duration", "HTTP request duration");
 * timer.record(() -> handleRequest());
 * }</pre>
 */
public final class PrometheusMetrics {

  private final MeterRegistry registry;

  /**
   * Creates a new PrometheusMetrics instance.
   *
   * @param registry the meter registry to use
   */
  public PrometheusMetrics(MeterRegistry registry) {
    this.registry = Objects.requireNonNull(registry, "registry must not be null");
  }

  // ===== Counter =====

  /**
   * Creates or retrieves a counter metric.
   *
   * @param name metric name
   * @param description metric description
   * @return the counter
   */
  public Counter counter(String name, String description) {
    return Counter.builder(name).description(description).register(registry);
  }

  /**
   * Creates or retrieves a counter metric with labels.
   *
   * @param name metric name
   * @param description metric description
   * @param labels metric labels
   * @return the counter
   */
  public Counter counter(String name, String description, MetricLabels labels) {
    return Counter.builder(name).description(description).tags(labels.toTags()).register(registry);
  }

  /**
   * Creates or retrieves a counter metric with tags.
   *
   * @param name metric name
   * @param description metric description
   * @param tags metric tags (key1, value1, key2, value2, ...)
   * @return the counter
   */
  public Counter counter(String name, String description, String... tags) {
    return Counter.builder(name).description(description).tags(tags).register(registry);
  }

  // ===== Gauge =====

  /**
   * Creates or retrieves a gauge metric.
   *
   * @param name metric name
   * @param description metric description
   * @param stateObject the object to measure
   * @param valueFunction function to extract the value from the state object
   * @param <T> type of the state object
   * @return the gauge
   */
  public <T> Gauge gauge(
      String name, String description, T stateObject, ToDoubleFunction<T> valueFunction) {
    return Gauge.builder(name, stateObject, valueFunction)
        .description(description)
        .register(registry);
  }

  /**
   * Creates or retrieves a gauge metric with labels.
   *
   * @param name metric name
   * @param description metric description
   * @param stateObject the object to measure
   * @param valueFunction function to extract the value from the state object
   * @param labels metric labels
   * @param <T> type of the state object
   * @return the gauge
   */
  public <T> Gauge gauge(
      String name,
      String description,
      T stateObject,
      ToDoubleFunction<T> valueFunction,
      MetricLabels labels) {
    return Gauge.builder(name, stateObject, valueFunction)
        .description(description)
        .tags(labels.toTags())
        .register(registry);
  }

  /**
   * Creates or retrieves a gauge metric from a supplier.
   *
   * @param name metric name
   * @param description metric description
   * @param valueSupplier supplier that provides the current value
   * @return the gauge
   */
  public Gauge gauge(String name, String description, Supplier<Number> valueSupplier) {
    return Gauge.builder(name, valueSupplier, s -> s.get().doubleValue())
        .description(description)
        .register(registry);
  }

  // ===== Timer (Histogram for durations) =====

  /**
   * Creates or retrieves a timer metric.
   *
   * <p>Timers are used to measure durations and automatically provide summary statistics (count,
   * total time, max) and histogram buckets.
   *
   * @param name metric name
   * @param description metric description
   * @return the timer
   */
  public Timer timer(String name, String description) {
    return Timer.builder(name).description(description).register(registry);
  }

  /**
   * Creates or retrieves a timer metric with labels.
   *
   * @param name metric name
   * @param description metric description
   * @param labels metric labels
   * @return the timer
   */
  public Timer timer(String name, String description, MetricLabels labels) {
    return Timer.builder(name).description(description).tags(labels.toTags()).register(registry);
  }

  /**
   * Creates or retrieves a timer metric with SLA boundaries (histogram buckets).
   *
   * @param name metric name
   * @param description metric description
   * @param slaBoundaries SLA boundaries in milliseconds (e.g., [10, 50, 100, 500, 1000])
   * @return the timer
   */
  public Timer timer(String name, String description, Duration... slaBoundaries) {
    return Timer.builder(name)
        .description(description)
        .serviceLevelObjectives(slaBoundaries)
        .register(registry);
  }

  /**
   * Creates or retrieves a timer metric with percentile buckets.
   *
   * @param name metric name
   * @param description metric description
   * @param percentiles percentiles to calculate (e.g., 0.5, 0.95, 0.99)
   * @return the timer
   */
  public Timer timerWithPercentiles(String name, String description, double... percentiles) {
    return Timer.builder(name)
        .description(description)
        .publishPercentiles(percentiles)
        .register(registry);
  }

  // ===== Distribution Summary (Histogram for non-duration values) =====

  /**
   * Creates or retrieves a distribution summary metric.
   *
   * <p>Distribution summaries are used to measure the distribution of non-duration values (e.g.,
   * request sizes, response sizes).
   *
   * @param name metric name
   * @param description metric description
   * @return the distribution summary
   */
  public DistributionSummary summary(String name, String description) {
    return DistributionSummary.builder(name).description(description).register(registry);
  }

  /**
   * Creates or retrieves a distribution summary metric with labels.
   *
   * @param name metric name
   * @param description metric description
   * @param labels metric labels
   * @return the distribution summary
   */
  public DistributionSummary summary(String name, String description, MetricLabels labels) {
    return DistributionSummary.builder(name)
        .description(description)
        .tags(labels.toTags())
        .register(registry);
  }

  /**
   * Creates or retrieves a distribution summary with SLA boundaries.
   *
   * @param name metric name
   * @param description metric description
   * @param slaBoundaries SLA boundaries (e.g., [100, 500, 1000, 5000])
   * @return the distribution summary
   */
  public DistributionSummary summary(String name, String description, double... slaBoundaries) {
    return DistributionSummary.builder(name)
        .description(description)
        .serviceLevelObjectives(slaBoundaries)
        .register(registry);
  }

  // ===== Helper methods =====

  /**
   * Records a value for a timer metric using a runnable.
   *
   * @param name metric name
   * @param description metric description
   * @param runnable the code to time
   */
  public void recordTimer(String name, String description, Runnable runnable) {
    Timer timer = timer(name, description);
    timer.record(runnable);
  }

  /**
   * Records a value for a timer metric using a supplier.
   *
   * @param name metric name
   * @param description metric description
   * @param supplier the code to time
   * @param <T> return type
   * @return the result from the supplier
   */
  public <T> T recordTimer(String name, String description, Supplier<T> supplier) {
    Timer timer = timer(name, description);
    return timer.record(supplier);
  }

  /**
   * Records a duration value for a timer.
   *
   * @param name metric name
   * @param description metric description
   * @param duration the duration to record
   * @param unit the time unit
   */
  public void recordDuration(String name, String description, long duration, TimeUnit unit) {
    Timer timer = timer(name, description);
    timer.record(duration, unit);
  }

  /**
   * Records a value for a distribution summary.
   *
   * @param name metric name
   * @param description metric description
   * @param value the value to record
   */
  public void recordSummary(String name, String description, double value) {
    DistributionSummary summary = summary(name, description);
    summary.record(value);
  }

  /**
   * Increments a counter by 1.
   *
   * @param name metric name
   * @param description metric description
   */
  public void incrementCounter(String name, String description) {
    Counter counter = counter(name, description);
    counter.increment();
  }

  /**
   * Increments a counter by a specific amount.
   *
   * @param name metric name
   * @param description metric description
   * @param amount the amount to increment by
   */
  public void incrementCounter(String name, String description, double amount) {
    Counter counter = counter(name, description);
    counter.increment(amount);
  }

  /**
   * Returns the underlying meter registry.
   *
   * @return the meter registry
   */
  public MeterRegistry getRegistry() {
    return registry;
  }
}
