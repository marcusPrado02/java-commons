/**
 * Prometheus metrics collection integration with Micrometer.
 *
 * <p>This module provides utilities and configuration for collecting and exposing Prometheus
 * metrics using Micrometer. It offers high-level abstractions for:
 *
 * <ul>
 *   <li>Configuring Prometheus registry
 *   <li>Creating counters, gauges, timers, and distribution summaries
 *   <li>Managing metric labels in a type-safe manner
 *   <li>Recording measurements with minimal boilerplate
 * </ul>
 *
 * <h2>Quick Start</h2>
 *
 * <p>1. Configure Prometheus registry:
 *
 * <pre>{@code
 * PrometheusMeterRegistry registry = PrometheusConfiguration.builder()
 *     .step(Duration.ofSeconds(10))
 *     .prefix("myapp")
 *     .build()
 *     .build();
 *
 * PrometheusMetrics metrics = new PrometheusMetrics(registry);
 * }</pre>
 *
 * <p>2. Create and use metrics:
 *
 * <pre>{@code
 * // Counter
 * Counter requests = metrics.counter("http.requests", "Total HTTP requests");
 * requests.increment();
 *
 * // Counter with labels
 * Counter statusCounter = metrics.counter("http.requests",
 *     "HTTP requests by status",
 *     MetricLabels.http("GET", 200));
 * statusCounter.increment();
 *
 * // Gauge
 * Runtime runtime = Runtime.getRuntime();
 * metrics.gauge("jvm.memory.used", "JVM memory used",
 *     runtime, Runtime::totalMemory);
 *
 * // Timer
 * Timer timer = metrics.timer("http.request.duration",
 *     "HTTP request duration");
 * timer.record(() -> handleRequest());
 * }</pre>
 *
 * <p>3. Use metric labels:
 *
 * <pre>{@code
 * // Predefined HTTP labels
 * MetricLabels httpLabels = MetricLabels.http("POST", 201, "/api/users");
 *
 * // Custom labels
 * MetricLabels customLabels = MetricLabels.builder()
 *     .label("service", "user-service")
 *     .label("region", "us-east-1")
 *     .label("env", "production")
 *     .build();
 *
 * Counter counter = metrics.counter("api.calls",
 *     "API calls", customLabels);
 * }</pre>
 *
 * <p>4. Expose metrics endpoint:
 *
 * <pre>{@code
 * // Spring Boot - automatic exposure at /actuator/prometheus
 *
 * // Manual exposure
 * String scrapeData = registry.scrape();
 * // Return scrapeData in HTTP response with content-type: text/plain
 * }</pre>
 *
 * <h2>Metric Types</h2>
 *
 * <ul>
 *   <li><b>Counter</b> - Monotonically increasing value (e.g., total requests, errors)
 *   <li><b>Gauge</b> - Current value that can go up or down (e.g., active connections, memory
 *       usage)
 *   <li><b>Timer</b> - Measures duration and provides histogram buckets (e.g., request duration)
 *   <li><b>DistributionSummary</b> - Measures distribution of values (e.g., request sizes)
 * </ul>
 *
 * <h2>Best Practices</h2>
 *
 * <ol>
 *   <li>Use descriptive metric names following Prometheus naming conventions (e.g., <code>
 *       http_requests_total</code>)
 *   <li>Add labels for multi-dimensional metrics but avoid high cardinality
 *   <li>Use timers for measuring durations (automatic histogram buckets)
 *   <li>Use distribution summaries for non-duration values (e.g., sizes)
 *   <li>Reuse metric instances instead of creating new ones for each measurement
 *   <li>Keep label values controlled (avoid dynamic user input as label values)
 * </ol>
 *
 * <h2>Integration with Spring Boot</h2>
 *
 * <p>For Spring Boot applications, Micrometer is auto-configured. Simply add the dependency and
 * metrics will be exposed at <code>/actuator/prometheus</code>:
 *
 * <pre>{@code
 * // application.yml
 * management:
 *   endpoints:
 *     web:
 *       exposure:
 *         include: prometheus
 *   metrics:
 *     export:
 *       prometheus:
 *         enabled: true
 * }</pre>
 *
 * @see com.marcusprado02.commons.adapters.metrics.prometheus.PrometheusConfiguration
 * @see com.marcusprado02.commons.adapters.metrics.prometheus.PrometheusMetrics
 * @see com.marcusprado02.commons.adapters.metrics.prometheus.MetricLabels
 */
package com.marcusprado02.commons.adapters.metrics.prometheus;
