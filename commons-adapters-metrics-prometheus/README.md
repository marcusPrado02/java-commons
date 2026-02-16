# Commons Adapters - Metrics - Prometheus

Prometheus metrics collection integration with Micrometer.

This module provides utilities and configuration for collecting and exposing [Prometheus](https://prometheus.io/) metrics using [Micrometer](https://micrometer.io/). It offers high-level abstractions for creating counters, gauges, timers, and distribution summaries with type-safe label management.

## Features

- ✅ **Counter Metrics** - Monotonically increasing counters for totals
- ✅ **Gauge Metrics** - Current values that can go up or down
- ✅ **Timer Metrics** - Duration measurements with histogram buckets
- ✅ **Distribution Summary** - Value distributions (sizes, amounts)
- ✅ **Type-Safe Labels** - Builder pattern for metric labels/tags
- ✅ **Exemplars Support** - Link metrics to traces (Prometheus 2.26+)
- ✅ **Common Label Factories** - Predefined labels for HTTP, DB, messaging, cache
- ✅ **Production Ready** - Sensible defaults with full customization

## Installation

```xml
<dependency>
  <groupId>com.marcusprado02.commons</groupId>
  <artifactId>commons-adapters-metrics-prometheus</artifactId>
  <version>${commons.version}</version>
</dependency>
```

## Quick Start

### 1. Configure Prometheus Registry

```java
import com.marcusprado02.commons.adapters.metrics.prometheus.*;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;

// Simple configuration
PrometheusMeterRegistry registry = PrometheusConfiguration.createDefault();

// Custom configuration
PrometheusMeterRegistry registry = PrometheusConfiguration.builder()
    .step(Duration.ofSeconds(10))    // Scrape interval
    .prefix("myapp")                 // Metric name prefix
    .enableExemplars(true)           // Enable trace linking
    .build()
    .build();

// Create metrics wrapper
PrometheusMetrics metrics = new PrometheusMetrics(registry);
```

### 2. Counter Metrics

```java
// Simple counter
Counter requests = metrics.counter("http.requests.total", "Total HTTP requests");
requests.increment();

// Counter with labels
Counter statusCounter = metrics.counter(
    "http.requests.total",
    "HTTP requests by status",
    MetricLabels.http("GET", 200)
);
statusCounter.increment();

// Increment by amount
requests.increment(5.0);

// Helper method
metrics.incrementCounter("order.created", "Orders created");
```

### 3. Gauge Metrics

```java
// Gauge from state object
Runtime runtime = Runtime.getRuntime();
Gauge memoryGauge = metrics.gauge(
    "jvm.memory.used",
    "JVM memory used in bytes",
    runtime,
    Runtime::totalMemory
);

// Gauge from supplier
Gauge activeUsers = metrics.gauge(
    "users.active",
    "Active users",
    () -> userService.getActiveUserCount()
);

// Gauge with labels
Gauge dbConnections = metrics.gauge(
    "db.connections.active",
    "Active database connections",
    connectionPool,
    ConnectionPool::getActiveCount,
    MetricLabels.database("SELECT", "users")
);
```

### 4. Timer Metrics (Durations)

```java
// Simple timer
Timer requestTimer = metrics.timer("http.request.duration", "HTTP request duration");

// Record using Runnable
requestTimer.record(() -> {
    handleRequest();
});

// Record using Supplier
String result = requestTimer.record(() -> {
    return processRequest();
});

// Record duration directly
requestTimer.record(150, TimeUnit.MILLISECONDS);

// Helper method
metrics.recordTimer("order.processing", "Order processing time", () -> {
    processOrder();
});

// Timer with histogram buckets (SLA)
Timer slaTimer = metrics.timer(
    "http.request.duration",
    "HTTP request duration",
    Duration.ofMillis(10),    // 10ms
    Duration.ofMillis(50),    // 50ms
    Duration.ofMillis(100),   // 100ms
    Duration.ofMillis(500),   // 500ms
    Duration.ofSeconds(1)     // 1s
);

// Timer with percentiles
Timer percentileTimer = metrics.timerWithPercentiles(
    "http.request.duration",
    "HTTP request duration",
    0.5,   // p50 (median)
    0.95,  // p95
    0.99   // p99
);
```

### 5. Distribution Summary (Non-Duration Values)

```java
// Simple summary
DistributionSummary requestSize = metrics.summary(
    "http.request.size",
    "HTTP request size in bytes"
);
requestSize.record(1024);

// Summary with labels
DistributionSummary responseSize = metrics.summary(
    "http.response.size",
    "HTTP response size in bytes",
    MetricLabels.http("GET", 200, "/api/users")
);
responseSize.record(2048);

// Summary with buckets
DistributionSummary sizeSummary = metrics.summary(
    "http.request.size",
    "HTTP request size in bytes",
    100.0,    // 100 bytes
    1000.0,   // 1 KB
    10000.0,  // 10 KB
    100000.0  // 100 KB
);

// Helper method
metrics.recordSummary("order.total", "Order total amount", 99.99);
```

## Metric Labels

### Creating Labels

```java
// Simple key-value pairs
MetricLabels labels1 = MetricLabels.of("method", "GET", "status", "200");

// Single pair
MetricLabels labels2 = MetricLabels.of("service", "user-service");

// From map
Map<String, String> labelMap = Map.of(
    "method", "POST",
    "endpoint", "/api/users"
);
MetricLabels labels3 = MetricLabels.of(labelMap);

// Builder pattern
MetricLabels labels4 = MetricLabels.builder()
    .label("service", "user-service")
    .label("region", "us-east-1")
    .label("env", "production")
    .labelIfPresent("tenant", tenantId)  // Add if not null
    .build();

// Empty labels
MetricLabels empty = MetricLabels.empty();
```

### Combining Labels

```java
// Base labels
MetricLabels baseLabels = MetricLabels.of("service", "user-service");

// Add more labels
MetricLabels combined = baseLabels
    .with("method", "GET")
    .with("status", "200");

// Combine with other labels
MetricLabels otherLabels = MetricLabels.of("region", "us-east-1");
MetricLabels all = baseLabels.with(otherLabels);
```

### Predefined Label Factories

```java
// HTTP labels
MetricLabels httpLabels = MetricLabels.http("GET", 200);
MetricLabels httpWithEndpoint = MetricLabels.http("POST", 201, "/api/users");

// Database labels
MetricLabels dbLabels = MetricLabels.database("SELECT", "users");

// Messaging labels
MetricLabels msgLabels = MetricLabels.messaging("order-events", "send");

// Cache labels
MetricLabels cacheLabels = MetricLabels.cache("user-cache", "hit");

// Service call labels
MetricLabels serviceLabels = MetricLabels.service("payment-service", "charge");
```

## Complete Examples

### Example 1: HTTP Metrics

```java
public class HttpMetricsCollector {

    private final PrometheusMetrics metrics;
    private final Counter requestCounter;
    private final Timer requestTimer;
    private final DistributionSummary requestSize;
    private final DistributionSummary responseSize;

    public HttpMetricsCollector(PrometheusMeterRegistry registry) {
        this.metrics = new PrometheusMetrics(registry);

        // Initialize metrics
        this.requestCounter = metrics.counter(
            "http_requests_total",
            "Total HTTP requests"
        );

        this.requestTimer = metrics.timer(
            "http_request_duration_seconds",
            "HTTP request duration",
            Duration.ofMillis(10),
            Duration.ofMillis(50),
            Duration.ofMillis(100),
            Duration.ofMillis(500),
            Duration.ofSeconds(1)
        );

        this.requestSize = metrics.summary(
            "http_request_size_bytes",
            "HTTP request size in bytes"
        );

        this.responseSize = metrics.summary(
            "http_response_size_bytes",
            "HTTP response size in bytes"
        );
    }

    public void recordRequest(String method, String endpoint, int status,
                               long durationMs, int reqSize, int respSize) {
        // Record counter
        MetricLabels labels = MetricLabels.http(method, status, endpoint);
        metrics.counter("http_requests_total", "Total HTTP requests", labels)
            .increment();

        // Record duration
        metrics.timer("http_request_duration_seconds", "Request duration", labels)
            .record(durationMs, TimeUnit.MILLISECONDS);

        // Record sizes
        metrics.summary("http_request_size_bytes", "Request size", labels)
            .record(reqSize);
        metrics.summary("http_response_size_bytes", "Response size", labels)
            .record(respSize);
    }
}
```

### Example 2: Database Metrics

```java
public class DatabaseMetricsCollector {

    private final PrometheusMetrics metrics;

    public DatabaseMetricsCollector(PrometheusMeterRegistry registry) {
        this.metrics = new PrometheusMetrics(registry);

        // Gauge for active connections
        metrics.gauge(
            "db_connections_active",
            "Active database connections",
            connectionPool,
            ConnectionPool::getActiveCount
        );

        // Gauge for idle connections
        metrics.gauge(
            "db_connections_idle",
            "Idle database connections",
            connectionPool,
            ConnectionPool::getIdleCount
        );
    }

    public <T> T executeQuery(String operation, String table,
                               Supplier<T> query) {
        MetricLabels labels = MetricLabels.database(operation, table);

        // Increment query counter
        metrics.counter("db_queries_total", "Total database queries", labels)
            .increment();

        // Time the query
        Timer timer = metrics.timer("db_query_duration_seconds",
            "Database query duration", labels);

        try {
            return timer.record(query);
        } catch (Exception e) {
            // Record error
            MetricLabels errorLabels = labels.with("error", "true");
            metrics.counter("db_queries_total", "Total queries", errorLabels)
                .increment();
            throw e;
        }
    }
}
```

### Example 3: Business Metrics

```java
public class OrderMetrics {

    private final PrometheusMetrics metrics;

    public OrderMetrics(PrometheusMeterRegistry registry) {
        this.metrics = new PrometheusMetrics(registry);
    }

    public void recordOrder(Order order) {
        // Order created counter
        MetricLabels labels = MetricLabels.of(
            "status", order.getStatus(),
            "payment_method", order.getPaymentMethod()
        );

        metrics.counter("orders_created_total", "Total orders created", labels)
            .increment();

        // Order value distribution
        metrics.summary("order_value_usd", "Order value in USD", labels)
            .record(order.getTotalAmount());

        // Items per order distribution
        metrics.summary("order_items_count", "Items per order", labels)
            .record(order.getItems().size());
    }

    public void recordOrderProcessingTime(long durationMs, String status) {
        MetricLabels labels = MetricLabels.of("status", status);

        metrics.timer("order_processing_duration_seconds",
            "Order processing duration", labels)
            .record(durationMs, TimeUnit.MILLISECONDS);
    }

    public void recordInventoryLevel(String product, int quantity) {
        MetricLabels labels = MetricLabels.of("product", product);

        metrics.gauge("inventory_level", "Current inventory level",
            quantity, q -> q.doubleValue(), labels);
    }
}
```

## Exposing Metrics

### Spring Boot (Automatic)

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

Metrics will be available at: `http://localhost:8080/actuator/prometheus`

### Manual Exposure

```java
@RestController
public class MetricsController {

    private final PrometheusMeterRegistry registry;

    @GetMapping(value = "/metrics", produces = "text/plain")
    public String metrics() {
        return registry.scrape();
    }
}
```

## Prometheus Configuration

### prometheus.yml

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'my-application'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8080']
```

### Running Prometheus with Docker

```bash
docker run -d --name prometheus \
  -p 9090:9090 \
  -v $(pwd)/prometheus.yml:/etc/prometheus/prometheus.yml \
  prom/prometheus
```

Access Prometheus UI at: `http://localhost:9090`

## Exemplars (Linking Metrics to Traces)

Exemplars allow you to link metric spikes to distributed traces.

### Enable Exemplars

```java
PrometheusMeterRegistry registry = PrometheusConfiguration.builder()
    .enableExemplars(true)
    .build()
    .build();
```

### Prometheus Configuration for Exemplars

```yaml
# prometheus.yml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'my-app'
    scrape_interval: 10s
    # Enable exemplars
    metric_relabel_configs:
      - source_labels: [__name__]
        regex: '.*'
        action: keep
```

**Requirements**: Prometheus 2.26+ with `--enable-feature=exemplar-storage`

## Best Practices

### 1. Metric Naming

Follow Prometheus naming conventions:

```java
// ✅ Good - descriptive, snake_case, with unit suffix
"http_request_duration_seconds"
"order_value_usd"
"db_connections_active"

// ❌ Bad - camelCase, no units, unclear
"httpReqDur"
"orderVal"
"connections"
```

### 2. Label Management

```java
// ✅ Good - low cardinality labels
MetricLabels.of("method", "GET", "status", "200", "endpoint", "/api/users")

// ❌ Bad - high cardinality (user IDs)
MetricLabels.of("user_id", "12345")  // Avoid!

// ✅ Good - bounded label values
MetricLabels.of("status_group", "2xx")  // Instead of individual status codes

// Limit label cardinality
// - method: ~10 values (GET, POST, PUT, DELETE, ...)
// - status: ~50 values (200, 201, 400, 404, ...)
// - endpoint: ~100 values (group similar endpoints)
```

### 3. Metric Reuse

```java
// ✅ Good - reuse metric instances
private final Counter requestCounter = metrics.counter(...);

public void handle() {
    requestCounter.increment();
}

// ❌ Bad - creating new metrics for each call
public void handle() {
    metrics.counter(...).increment();  // Creates new metric each time!
}
```

### 4. Timer vs. Distribution Summary

```java
// ✅ Use Timer for durations
Timer requestTimer = metrics.timer("http_request_duration_seconds", ...);

// ✅ Use DistributionSummary for sizes/amounts
DistributionSummary requestSize = metrics.summary("http_request_size_bytes", ...);

// ❌ Don't use DistributionSummary for durations
DistributionSummary duration = metrics.summary("request_duration", ...);  // Bad!
```

### 5. Histogram Buckets

```java
// ✅ Good - meaningful SLA boundaries
Timer timer = metrics.timer(
    "http_request_duration_seconds",
    "Request duration",
    Duration.ofMillis(10),   // Fast
    Duration.ofMillis(100),  // Normal
    Duration.ofMillis(500),  // Slow
    Duration.ofSeconds(1),   // Very slow
    Duration.ofSeconds(5)    // Timeout threshold
);

// ✅ Good - percentiles for detailed analysis
Timer timer = metrics.timerWithPercentiles(
    "http_request_duration_seconds",
    "Request duration",
    0.5, 0.95, 0.99, 0.999
);
```

## Common Queries

### Request Rate

```promql
# Requests per second
rate(http_requests_total[5m])

# By status
rate(http_requests_total{status="200"}[5m])
```

### Error Rate

```promql
# Error rate (4xx and 5xx)
rate(http_requests_total{status=~"[45].."}[5m])

# Error ratio
rate(http_requests_total{status=~"[45].."}[5m])
  /
rate(http_requests_total[5m])
```

### Latency Percentiles

```promql
# p95 latency
histogram_quantile(0.95,
  rate(http_request_duration_seconds_bucket[5m])
)

# p99 latency by endpoint
histogram_quantile(0.99,
  rate(http_request_duration_seconds_bucket{endpoint="/api/users"}[5m])
)
```

### Throughput

```promql
# Total requests in last hour
increase(http_requests_total[1h])
```

## Performance Considerations

### Memory

- **Limit label cardinality**: Each unique label combination creates a new time series
- **Maximum ~10 million time series** per Prometheus instance
- **Rule of thumb**: cardinality(label1) × cardinality(label2) × ... < 1000

### CPU

- **Batch operations**: Record metrics in batches when possible
- **Avoid creating metrics in hot paths**: Initialize metrics once
- **Use samplers**: For very high-traffic paths, consider sampling

### Network

- **Scrape interval**: Balance between freshness and overhead (10-60 seconds typical)
- **Compression**: Prometheus automatically compresses responses
- **Point-in-time scraping**: Prometheus pulls metrics (no constant pushing)

## Troubleshooting

### Metrics not appearing

1. **Check registry**: Ensure metrics are registered
2. **Check scrape**: Verify Prometheus is scraping the endpoint
3. **Check naming**: Ensure metric names follow conventions
4. **Check labels**: Verify labels are correctly set

### High memory usage

1. **Check cardinality**: `count(metric_name)` in Prometheus
2. **Reduce labels**: Combine or remove high-cardinality labels
3. **Increase scrape interval**: Reduce frequency of scraping

### Slow scraping

1. **Reduce metric count**: Remove unused metrics
2. **Optimize queries**: Pre-calculate aggregations
3. **Increase timeout**: Allow more time for large metric sets

## Links

- [Prometheus Documentation](https://prometheus.io/docs/)
- [Micrometer Documentation](https://micrometer.io/docs)
- [Prometheus Best Practices](https://prometheus.io/docs/practices/)
- [Prometheus Naming Conventions](https://prometheus.io/docs/practices/naming/)

## License

This module is part of the Commons library and follows the same license.
