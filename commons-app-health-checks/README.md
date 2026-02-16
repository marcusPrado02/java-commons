# Commons App Health Checks

Advanced health check system with support for custom indicators, dependency monitoring, degraded state, and health aggregation.

## Features

- **Multiple health states** - UP, DOWN, DEGRADED, UNKNOWN
- **Custom health indicators** - Easy interface for implementing checks
- **Health aggregation** - Combine multiple checks into overall status
- **Built-in indicators** - Ping, disk space, memory, URL connectivity
- **Critical vs non-critical** - Mark checks as critical or informational
- **Degraded state support** - Report partial functionality
- **Detailed health information** - Include metadata and error details
- **Flexible aggregation strategies** - Configure how statuses combine

## Installation

```xml
<dependency>
    <groupId>com.marcusprado02</groupId>
    <artifactId>commons-app-health-checks</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## Quick Start

### Creating Health Checks

```java
import com.marcusprado02.commons.app.health.*;

// Simple UP status
Health health = Health.up()
    .withDetail("version", "1.0.0")
    .withDetail("uptime", "5 days")
    .build();

// DOWN status with error
Health health = Health.down()
    .withException(new IOException("Connection failed"))
    .build();

// DEGRADED status
Health health = Health.degraded()
    .withDetail("message", "Running with reduced capacity")
    .withDetail("availableNodes", 2)
    .withDetail("totalNodes", 5)
    .build();

// UNKNOWN status
Health health = Health.unknown()
    .withDetail("reason", "Cannot determine status")
    .build();
```

### Custom Health Indicators

```java
public class DatabaseHealthIndicator implements HealthIndicator {

    private final DataSource dataSource;

    public DatabaseHealthIndicator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public String name() {
        return "database";
    }

    @Override
    public Health check() {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();

            return Health.up()
                .withDetail("database", metaData.getDatabaseProductName())
                .withDetail("version", metaData.getDatabaseProductVersion())
                .withDetail("url", metaData.getURL())
                .build();

        } catch (SQLException e) {
            return Health.down()
                .withException(e)
                .build();
        }
    }

    @Override
    public boolean isCritical() {
        return true; // Database is critical for the application
    }
}
```

### Aggregating Health Checks

```java
// Create indicators
List<HealthIndicator> indicators = List.of(
    new PingHealthIndicator(),
    new DatabaseHealthIndicator(dataSource),
    new CacheHealthIndicator(cacheManager),
    new DiskSpaceHealthIndicator(new File("/")),
    new MemoryHealthIndicator()
);

// Create aggregator
HealthAggregator aggregator = new HealthAggregator(indicators);

// Get overall health
CompositeHealth health = aggregator.aggregate();

System.out.println("Overall status: " + health.getStatus());

// Inspect individual components
health.getComponents().forEach((name, componentHealth) -> {
    System.out.println(name + ": " + componentHealth.getStatus());
    componentHealth.getDetails().forEach((key, value) -> {
        System.out.println("  " + key + ": " + value);
    });
});
```

## Built-in Health Indicators

### PingHealthIndicator

Simple liveness check that always returns UP.

```java
HealthIndicator ping = new PingHealthIndicator();
Health health = ping.check(); // Always UP
```

### DiskSpaceHealthIndicator

Monitors disk space and reports DEGRADED or DOWN based on thresholds.

```java
// Default thresholds (1GB warning, 100MB critical)
HealthIndicator disk = new DiskSpaceHealthIndicator(new File("/"));

// Custom thresholds
HealthIndicator disk = new DiskSpaceHealthIndicator(
    new File("/data"),
    5L * 1024 * 1024 * 1024,     // 5GB warning threshold
    1L * 1024 * 1024 * 1024       // 1GB critical threshold
);

Health health = disk.check();
// Returns: UP, DEGRADED, or DOWN based on available space
```

### MemoryHealthIndicator

Monitors JVM memory usage.

```java
// Default thresholds (80% warning, 95% critical)
HealthIndicator memory = new MemoryHealthIndicator();

// Custom thresholds
HealthIndicator memory = new MemoryHealthIndicator(
    0.75,  // 75% warning
    0.90   // 90% critical
);

Health health = memory.check();
// Details include: used, max, usage percentage
```

### UrlHealthIndicator

Checks HTTP endpoint connectivity.

```java
// Default timeout (3 seconds), non-critical
HealthIndicator api = new UrlHealthIndicator(
    "external-api",
    "https://api.example.com/health"
);

// Custom timeout and criticality
HealthIndicator api = new UrlHealthIndicator(
    "payment-service",
    "http://payments:8080/health",
    Duration.ofSeconds(5),
    true  // Mark as critical
);

Health health = api.check();
// Details include: url, responseCode, responseTime
```

## Custom Health Indicators Examples

### Cache Health Check

```java
public class CacheHealthIndicator implements HealthIndicator {

    private final CacheManager cacheManager;

    @Override
    public String name() {
        return "cache";
    }

    @Override
    public Health check() {
        try {
            // Perform cache operation
            cacheManager.getCache("test").put("health-check", "ok");
            String value = cacheManager.getCache("test").get("health-check", String.class);

            if ("ok".equals(value)) {
                return Health.up()
                    .withDetail("caches", cacheManager.getCacheNames())
                    .build();
            } else {
                return Health.down()
                    .withDetail("error", "Cache read/write mismatch")
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withException(e)
                .build();
        }
    }

    @Override
    public boolean isCritical() {
        return false; // Cache is not critical, app can work without it
    }
}
```

### Message Queue Health Check

```java
public class RabbitMQHealthIndicator implements HealthIndicator {

    private final ConnectionFactory connectionFactory;

    @Override
    public String name() {
        return "rabbitmq";
    }

    @Override
    public Health check() {
        try (Connection connection = connectionFactory.newConnection()) {
            int channelMax = connection.getChannelMax();

            return Health.up()
                .withDetail("host", connectionFactory.getHost())
                .withDetail("port", connectionFactory.getPort())
                .withDetail("channelMax", channelMax)
                .build();

        } catch (Exception e) {
            return Health.down()
                .withException(e)
                .build();
        }
    }

    @Override
    public boolean isCritical() {
        return true;
    }
}
```

### Elasticsearch Health Check

```java
public class ElasticsearchHealthIndicator implements HealthIndicator {

    private final RestHighLevelClient client;

    @Override
    public String name() {
        return "elasticsearch";
    }

    @Override
    public Health check() {
        try {
            ClusterHealthResponse response = client.cluster()
                .health(new ClusterHealthRequest(), RequestOptions.DEFAULT);

            ClusterHealthStatus status = response.getStatus();

            Health.Builder builder;
            switch (status) {
                case GREEN:
                    builder = Health.up();
                    break;
                case YELLOW:
                    builder = Health.degraded();
                    break;
                case RED:
                default:
                    builder = Health.down();
                    break;
            }

            return builder
                .withDetail("clusterName", response.getClusterName())
                .withDetail("numberOfNodes", response.getNumberOfNodes())
                .withDetail("numberOfDataNodes", response.getNumberOfDataNodes())
                .withDetail("status", status.name())
                .build();

        } catch (Exception e) {
            return Health.down()
                .withException(e)
                .build();
        }
    }
}
```

## Health Status Interpretation

### UP
Component is fully operational:
```java
Health.up()
    .withDetail("connections", 10)
    .withDetail("latency", "5ms")
    .build();
```

### DEGRADED
Component is working but with limitations:
```java
Health.degraded()
    .withDetail("availableNodes", 2)
    .withDetail("requiredNodes", 3)
    .withDetail("message", "Running with reduced capacity")
    .build();
```

### DOWN
Component is not functioning:
```java
Health.down()
    .withException(new SQLException("Connection refused"))
    .build();
```

### UNKNOWN
Status cannot be determined:
```java
Health.unknown()
    .withDetail("reason", "Timeout occurred")
    .build();
```

## Aggregation Strategies

### WORST_STATUS (Default)

Overall status is the worst of all checks, with critical checks taking priority:

```java
HealthAggregator aggregator = new HealthAggregator(
    indicators,
    HealthAggregator.AggregationStrategy.WORST_STATUS
);
```

Rules:
- If any **critical** check is DOWN or UNKNOWN → overall is DOWN
- Otherwise, worst status wins: DOWN > UNKNOWN > DEGRADED > UP
- Non-critical checks can be DOWN without affecting overall if all critical checks are UP

### ALL_UP

Overall status is UP only if all checks are UP:

```java
HealthAggregator aggregator = new HealthAggregator(
    indicators,
    HealthAggregator.AggregationStrategy.ALL_UP
);
```

Rules:
- All checks must be UP for overall UP
- Any check not UP → overall is DOWN

## Spring Integration

```java
@Configuration
public class HealthConfig {

    @Bean
    public HealthAggregator healthAggregator(
            DataSource dataSource,
            CacheManager cacheManager) {

        List<HealthIndicator> indicators = List.of(
            new PingHealthIndicator(),
            new DatabaseHealthIndicator(dataSource),
            new CacheHealthIndicator(cacheManager),
            new DiskSpaceHealthIndicator(new File("/")),
            new MemoryHealthIndicator(),
            new UrlHealthIndicator("auth-service",
                "http://auth:8080/health",
                Duration.ofSeconds(3),
                true)
        );

        return new HealthAggregator(indicators);
    }
}

@RestController
@RequestMapping("/health")
public class HealthController {

    private final HealthAggregator healthAggregator;

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        CompositeHealth health = healthAggregator.aggregate();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", health.getStatus());
        response.put("timestamp", health.getTimestamp());

        Map<String, Object> components = new LinkedHashMap<>();
        health.getComponents().forEach((name, componentHealth) -> {
            Map<String, Object> component = new LinkedHashMap<>();
            component.put("status", componentHealth.getStatus());
            component.put("details", componentHealth.getDetails());
            components.put(name, component);
        });
        response.put("components", components);

        int statusCode = health.getStatus().isHealthy() ? 200 : 503;
        return ResponseEntity
            .status(statusCode)
            .body(response);
    }
}
```

## Kubernetes Liveness and Readiness

```java
@RestController
public class KubernetesHealthController {

    private final HealthAggregator healthAggregator;

    // Liveness probe - is the app alive?
    @GetMapping("/health/liveness")
    public ResponseEntity<Void> liveness() {
        // Simple ping check
        Health ping = new PingHealthIndicator().check();
        return ping.getStatus() == HealthStatus.UP
            ? ResponseEntity.ok().build()
            : ResponseEntity.status(503).build();
    }

    // Readiness probe - is the app ready to serve traffic?
    @GetMapping("/health/readiness")
    public ResponseEntity<Map<String, Object>> readiness() {
        CompositeHealth health = healthAggregator.aggregate();

        Map<String, Object> response = Map.of(
            "status", health.getStatus(),
            "ready", health.getStatus().isHealthy()
        );

        int statusCode = health.getStatus().isHealthy() ? 200 : 503;
        return ResponseEntity
            .status(statusCode)
            .body(response);
    }
}
```

## Best Practices

1. **Mark Critical Dependencies** - Set `isCritical()` to `true` for dependencies required for core functionality
2. **Use Degraded State** - Report DEGRADED instead of DOWN when partially functional
3. **Include Useful Details** - Add relevant information in health check details
4. **Set Appropriate Timeouts** - Don't let health checks hang
5. **Avoid Heavy Operations** - Health checks should be lightweight
6. **Cache Results** - For expensive checks, consider caching results briefly
7. **Monitor Health Endpoints** - Use monitoring tools to track health over time

## Thread Safety

All classes in this module are thread-safe and can be safely shared across threads.

## License

This module is part of the commons library and follows the same license.
