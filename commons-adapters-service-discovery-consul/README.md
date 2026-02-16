# Commons Adapters - Service Discovery Consul

Consul-based implementation of service discovery and registration for microservices architecture.

## Overview

This module provides a production-ready Consul adapter for the `ServiceRegistry` port interface, enabling:

- **Service Registration**: Register microservice instances with Consul agent
- **Health Checking**: HTTP, TCP, and TTL-based health checks
- **Service Discovery**: Find healthy instances by service name
- **Metadata Tags**: Support for version tags, regions, and custom metadata
- **Load Balancing**: Client-side load balancing via multiple instances
- **Automatic Deregistration**: Clean removal of dead services

## Installation

### Maven

```xml
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-adapters-service-discovery-consul</artifactId>
    <version>${commons.version}</version>
</dependency>
```

### Gradle

```kotlin
implementation("com.marcusprado02.commons:commons-adapters-service-discovery-consul:${commonsVersion}")
```

## Quick Start

### 1. Start Consul

```bash
# Development mode (single node)
docker run -d -p 8500:8500 --name=consul consul:1.15 agent -dev -client=0.0.0.0

# Access Consul UI
open http://localhost:8500/ui
```

### 2. Register a Service

```java
import com.ecwid.consul.v1.ConsulClient;
import com.marcusprado02.commons.adapters.servicediscovery.consul.ConsulServiceRegistry;
import com.marcusprado02.commons.ports.servicediscovery.*;
import java.time.Duration;

// Create registry
ConsulClient consulClient = new ConsulClient("localhost", 8500);
ServiceRegistry registry = new ConsulServiceRegistry(consulClient);

// Define service instance
ServiceInstance instance = ServiceInstance.builder()
    .serviceId("payment-service")
    .instanceId("payment-01")
    .host("192.168.1.100")
    .port(8080)
    .secure(true)
    .addMetadata("version", "1.2.0")
    .addMetadata("region", "us-east")
    .build();

// Define health check
HealthCheck healthCheck = HealthCheck.http("https://192.168.1.100:8080/health")
    .interval(Duration.ofSeconds(10))
    .timeout(Duration.ofSeconds(5))
    .deregisterAfter(Duration.ofMinutes(1))
    .build();

// Register
Result<Void> result = registry.register(instance, healthCheck);
result.ifSuccess(() -> System.out.println("Service registered!"));
```

### 3. Discover Services

```java
// Find all healthy instances
Result<List<ServiceInstance>> instances = registry.discover("payment-service");

instances.ifSuccess(list -> {
    for (ServiceInstance svc : list) {
        System.out.println("Found: " + svc.getUri());
        System.out.println("Version: " + svc.getMetadata("version"));
    }
});
```

### 4. Deregister on Shutdown

```java
// Shutdown hook
Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    registry.deregister("payment-01");
}));
```

## Health Check Types

### HTTP Health Check

Consul polls the HTTP endpoint at regular intervals:

```java
HealthCheck httpCheck = HealthCheck.http("http://localhost:8080/health")
    .interval(Duration.ofSeconds(10))  // Check every 10 seconds
    .timeout(Duration.ofSeconds(5))    // Timeout after 5 seconds
    .deregisterAfter(Duration.ofMinutes(1)) // Deregister if unhealthy for 1 minute
    .build();
```

**Requirements:**
- Endpoint must return HTTP 2xx for healthy
- Any other status code marks service as unhealthy

### TCP Health Check

Consul attempts to establish TCP connection:

```java
HealthCheck tcpCheck = HealthCheck.tcp("localhost:8080")
    .interval(Duration.ofSeconds(30))
    .timeout(Duration.ofSeconds(10))
    .build();
```

**Use Cases:**
- Non-HTTP services (gRPC, raw TCP)
- Quick connectivity checks
- Database connections

### TTL Health Check

Service must send periodic heartbeats:

```java
HealthCheck ttlCheck = HealthCheck.ttl(Duration.ofSeconds(30))
    .deregisterAfter(Duration.ofMinutes(1))
    .build();

registry.register(instance, ttlCheck);

// Send heartbeat from service
ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
scheduler.scheduleAtFixedRate(
    () -> registry.heartbeat("payment-01"),
    0, 15, TimeUnit.SECONDS  // Send every 15s (TTL is 30s)
);
```

**Use Cases:**
- Services that can't expose HTTP endpoints
- Push-based health monitoring
- Custom health logic

## Advanced Usage

### Service Metadata and Tags

```java
ServiceInstance instance = ServiceInstance.builder()
    .serviceId("api-gateway")
    .instanceId("gateway-01")
    .host("10.0.1.50")
    .port(443)
    .secure(true)
    .addMetadata("version", "2.1.0")
    .addMetadata("region", "us-west")
    .addMetadata("az", "us-west-2a")
    .addMetadata("environment", "production")
    .build();
```

Use metadata for:
- **Version-based routing**: Route to specific versions
- **Region filtering**: Keep traffic within regions
- **Canary deployments**: Route percentage to new version
- **Blue-green deployments**: Switch between versions

### Multiple Instances (Load Balancing)

```java
// Register multiple instances of the same service
for (int i = 1; i <= 3; i++) {
    ServiceInstance instance = ServiceInstance.builder()
        .serviceId("order-service")
        .instanceId("order-" + String.format("%02d", i))
        .host("192.168.1." + (100 + i))
        .port(8080)
        .build();

    registry.register(instance);
}

// Discover returns all healthy instances
Result<List<ServiceInstance>> instances = registry.discover("order-service");

// Implement client-side load balancing
ServiceInstance chosen = instances.value().get(
    ThreadLocalRandom.current().nextInt(instances.value().size())
);
```

### Service Discovery with Filtering

```java
Result<List<ServiceInstance>> instances = registry.discover("api-service");

instances.ifSuccess(list -> {
    // Filter by version
    List<ServiceInstance> v2Instances = list.stream()
        .filter(svc -> "2.0.0".equals(svc.getMetadata("version")))
        .toList();

    // Filter by region
    List<ServiceInstance> usEastInstances = list.stream()
        .filter(svc -> "us-east".equals(svc.getMetadata("region")))
        .toList();

    // Filter by secure connection
    List<ServiceInstance> secureInstances = list.stream()
        .filter(ServiceInstance::secure)
        .toList();
});
```

### List All Services

```java
Result<List<String>> services = registry.listServices();

services.ifSuccess(serviceIds -> {
    System.out.println("Registered services:");
    serviceIds.forEach(id -> System.out.println("  - " + id));
});
```

### Get All Instances (Including Unhealthy)

```java
// Includes unhealthy instances (useful for monitoring dashboards)
Result<List<ServiceInstance>> allInstances = registry.getInstances("payment-service");

allInstances.ifSuccess(list -> {
    System.out.println("Total instances: " + list.size());
});
```

## Spring Boot Integration

### Configuration

```java
@Configuration
public class ConsulConfig {

    @Value("${consul.host:localhost}")
    private String consulHost;

    @Value("${consul.port:8500}")
    private int consulPort;

    @Bean
    public ConsulClient consulClient() {
        return new ConsulClient(consulHost, consulPort);
    }

    @Bean
    public ServiceRegistry serviceRegistry(ConsulClient consulClient) {
        return new ConsulServiceRegistry(consulClient);
    }
}
```

### Automatic Registration

```java
@Component
public class ServiceRegistrar implements ApplicationListener<ApplicationReadyEvent> {

    private final ServiceRegistry registry;
    private final ServerProperties serverProperties;

    @Value("${spring.application.name}")
    private String serviceName;

    @Value("${server.port:8080}")
    private int port;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        String instanceId = serviceName + "-" + UUID.randomUUID();

        ServiceInstance instance = ServiceInstance.builder()
            .serviceId(serviceName)
            .instanceId(instanceId)
            .host InetAddress.getLocalHost().getHostAddress())
            .port(port)
            .addMetadata("spring-boot-version", SpringBootVersion.getVersion())
            .build();

        HealthCheck healthCheck = HealthCheck.http(
            "http://localhost:" + port + "/actuator/health"
        ).interval(Duration.ofSeconds(10)).build();

        registry.register(instance, healthCheck);
    }
}
```

### Graceful Shutdown

```java
@Component
public class ServiceDeregistrar {

    private final ServiceRegistry registry;

    @Value("${service.instance.id}")
    private String instanceId;

    @PreDestroy
    public void deregister() {
        registry.deregister(instanceId);
    }
}
```

## Kubernetes Integration

### Deployment with Consul Agent

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: payment-service
spec:
  replicas: 3
  template:
    spec:
      containers:
        - name: payment-service
          image: payment-service:1.0.0
          ports:
            - containerPort: 8080
          env:
            - name: CONSUL_HOST
              value: "consul.default.svc.cluster.local"
            - name: CONSUL_PORT
              value: "8500"

        # Consul agent sidecar
        - name: consul-agent
          image: consul:1.15
          args:
            - "agent"
            - "-retry-join=consul-server"
            - "-bind=0.0.0.0"
          ports:
            - containerPort: 8500
```

### Service Registration in K8s

```java
ServiceInstance instance = ServiceInstance.builder()
    .serviceId("payment-service")
    .instanceId(System.getenv("HOSTNAME")) // Pod name
    .host(System.getenv("POD_IP"))
    .port(8080)
    .addMetadata("k8s-namespace", System.getenv("POD_NAMESPACE"))
    .addMetadata("k8s-pod", System.getenv("HOSTNAME"))
    .build();
```

## Error Handling

All operations return `Result<T>`, enabling type-safe error handling:

```java
Result<Void> result = registry.register(instance);

result
    .ifSuccess(() -> logger.info("Registration successful"))
    .ifFailure(problem -> {
        logger.error("Registration failed: {}", problem.title());
        logger.error("Error code: {}", problem.code());
        logger.error("Details: {}", problem.context());
    });

// Pattern matching (Java 21+)
switch (result) {
    case Success<Void> s -> System.out.println("OK");
    case Failure<Void> f -> System.err.println("Error: " + f.problem().title());
}
```

## Best Practices

### 1. Use Appropriate Health Check Intervals

```java
// Production: Balance between quick detection and network overhead
HealthCheck.http("...").interval(Duration.ofSeconds(10)).build();

// Development: Faster detection
HealthCheck.http("...").interval(Duration.ofSeconds(5)).build();

// Database connections: Longer intervals
HealthCheck.tcp("db:5432").interval(Duration.ofSeconds(30)).build();
```

### 2. Set Realistic Timeouts

```java
// Local services: Short timeout
HealthCheck.http("http://localhost:8080/health")
    .timeout(Duration.ofSeconds(2))
    .build();

// Remote services: Longer timeout
HealthCheck.http("https://api.remote-service.com/health")
    .timeout(Duration.ofSeconds(10))
    .build();
```

### 3. Configure Deregistration Delay

```java
// Quick cleanup (development)
HealthCheck.http("...")
    .deregisterAfter(Duration.ofSeconds(30))
    .build();

// Conservative (production)
HealthCheck.http("...")
    .deregisterAfter(Duration.ofMinutes(5))
    .build();
```

### 4. Use Meaningful Instance IDs

```java
// Bad: random ID with no context
String instanceId = UUID.randomUUID().toString();

// Good: includes hostname and service
String instanceId = String.format("%s-%s-%s",
    serviceName,
    InetAddress.getLocalHost().getHostName(),
    port
);
```

### 5. Handle Registration Failures

```java
Result<Void> result = registry.register(instance);

if (result.isFailure()) {
    // Retry with exponential backoff
    for (int i = 0; i < 3; i++) {
        Thread.sleep(Duration.ofSeconds((long) Math.pow(2, i)));
        result = registry.register(instance);
        if (result.isSuccess()) break;
    }

    if (result.isFailure()) {
        // Alert ops team, continue running in degraded mode
        alerting.send("Service registration failed after retries");
    }
}
```

## Monitoring & Observability

### Consul UI

Access at `http://localhost:8500/ui` to view:
- All registered services
- Instance health status
- Service metadata/tags
- Health check history

### HTTP API

```bash
# List all services
curl http://localhost:8500/v1/catalog/services

# Get healthy instances
curl http://localhost:8500/v1/health/service/payment-service?passing

# Get service details
curl http://localhost:8500/v1/agent/services
```

### Metrics

Instrument your service discovery operations:

```java
Counter registrations = Counter.builder("consul.registrations.total")
    .tag("service", serviceName)
    .register(meterRegistry);

Timer discoveryTime = Timer.builder("consul.discovery.duration")
    .register(meterRegistry);

registrations.increment();
discoveryTime.record(() -> registry.discover("service-name"));
```

## Troubleshooting

### Connection Refused

```
Problem: Unable to connect to Consul agent
Solution: Verify Consul is running and accessible
```

```bash
# Check Consul is running
curl http://localhost:8500/v1/status/leader

# Check Docker container
docker ps | grep consul

# Check logs
docker logs consul
```

### Service Not Discovered

```
Problem: Service registered but not returned by discover()
Reason: Health check is failing
```

```bash
# Check health in Consul UI
open http://localhost:8500/ui/dc1/services/your-service

# Use getInstances() instead of discover() to see unhealthy instances
registry.getInstances("your-service");
```

### Health Check Failures

```
Problem: Health check endpoint returns 503
Solution: Ensure /health endpoint is accessible and returns 200
```

```java
// Verify endpoint manually
curl -v http://localhost:8080/health

// Check service logs for errors
// Ensure dependencies (DB, Redis) are healthy
```

## Dependencies

- **consul-api**: `com.ecwid.consul:consul-api:1.4.5`
- **commons-ports-service-discovery**: Port interfaces
- **commons-kernel-result**: Result pattern
- **commons-kernel-errors**: Error handling

## License

This module is part of the Commons Platform and is licensed under the MIT License.
