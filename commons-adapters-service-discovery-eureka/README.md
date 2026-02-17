# Commons Adapters - Service Discovery Eureka

Netflix Eureka service discovery adapter implementation for the Commons Platform.

## Overview

This module provides a production-ready implementation of the `ServiceRegistry` port interface using **Netflix Eureka** server. Eureka is a REST-based service that is primarily used in the AWS cloud for locating services for the purpose of load balancing and failover.

**Key Features:**
- ✅ Service registration with Eureka server
- ✅ Automatic heartbeat and renewal (self-preservation)
- ✅ Service discovery with health-based filtering
- ✅ Instance metadata support
- ✅ Configurable lease and fetch intervals
- ✅ Zone-aware routing support
- ✅ Result-based error handling
- ✅ Integration-ready with Spring Cloud Netflix

## Installation

Add the dependency to your `pom.xml`:

```xml
<dependency>
  <groupId>com.marcusprado02.commons</groupId>
  <artifactId>commons-adapters-service-discovery-eureka</artifactId>
  <version>${commons.version}</version>
</dependency>
```

## Quick Start

### 1. Start Eureka Server

Using Docker Compose:

```yaml
# docker-compose.yml
version: '3.8'
services:
  eureka:
    image: springcloud/eureka:latest
    ports:
      - "8761:8761"
    environment:
      - EUREKA_INSTANCE_HOSTNAME=eureka
      - EUREKA_CLIENT_REGISTER_WITH_EUREKA=false
      - EUREKA_CLIENT_FETCH_REGISTRY=false
```

```bash
docker-compose up -d
```

Access Eureka Dashboard at http://localhost:8761

### 2. Register a Service

```java
import com.marcusprado02.commons.adapters.servicediscovery.eureka.EurekaServiceRegistry;
import com.marcusprado02.commons.ports.servicediscovery.ServiceInstance;
import com.marcusprado02.commons.kernel.result.Result;

// Create Eureka registry client
EurekaServiceRegistry registry = EurekaServiceRegistry.builder()
    .eurekaServerUrl("http://localhost:8761/eureka")
    .appName("payment-service")
    .hostName("localhost")
    .port(8080)
    .renewalInterval(Duration.ofSeconds(30))     // Heartbeat interval
    .expirationDuration(Duration.ofSeconds(90))  // Lease expiration
    .metadata("version", "1.0.0")
    .metadata("region", "us-east-1")
    .build();

// Create service instance
ServiceInstance instance = ServiceInstance.builder()
    .serviceId("payment-service")
    .instanceId("payment-01")
    .host("localhost")
    .port(8080)
    .addMetadata("version", "1.0.0")
    .addMetadata("environment", "production")
    .build();

// Register
Result<Void> result = registry.register(instance);
if (result.isOk()) {
    System.out.println("Service registered successfully!");
}

// Shutdown gracefully
Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    registry.deregister("payment-01");
    registry.shutdown();
}));
```

### 3. Discover Services

```java
// Discover healthy instances
Result<List<ServiceInstance>> instances = registry.discover("payment-service");

if (instances.isOk()) {
    instances.getOrNull().forEach(instance -> {
        String url = instance.getUri();
        System.out.println("Instance: " + url);
        System.out.println("Metadata: " + instance.metadata());
    });
}

// Get all instances (including unhealthy)
Result<List<ServiceInstance>> allInstances = registry.getInstances("payment-service");

// List all registered services
Result<List<String>> services = registry.listServices();
services.getOrNull().forEach(System.out::println);
```

## Health Checks and Heartbeats

Eureka uses a **heartbeat mechanism** (lease renewal) for health checking:

### Automatic Heartbeats

Eureka client automatically sends heartbeats to the server at the configured renewal interval:

```java
EurekaServiceRegistry registry = EurekaServiceRegistry.builder()
    .eurekaServerUrl("http://localhost:8761/eureka")
    .appName("my-service")
    .renewalInterval(Duration.ofSeconds(30))     // Send heartbeat every 30s
    .expirationDuration(Duration.ofSeconds(90))  // Instance expires after 90s
    .build();
```

**How it works:**
1. Client sends renewal (heartbeat) every 30 seconds
2. If server doesn't receive renewal within 90 seconds, instance is marked as DOWN
3. DOWN instances are eventually evicted from the registry

### Manual Heartbeat (Optional)

```java
// Trigger manual heartbeat
Result<Void> result = registry.heartbeat("payment-01");
if (result.isOk()) {
    System.out.println("Heartbeat sent successfully");
}
```

### Self-Preservation Mode

Eureka server enters **self-preservation mode** when it detects network issues:
- Prevents mass eviction of instances
- Server stops evicting instances for a period
- Dashboard shows warning: "EMERGENCY! EUREKA MAY BE INCORRECTLY CLAIMING INSTANCES ARE UP"

**Disable for development:**
```yaml
# Eureka Server application.yml
eureka:
  server:
    enable-self-preservation: false
    eviction-interval-timer-in-ms: 5000
```

## Advanced Usage

### Multiple Eureka Servers (High Availability)

```java
EurekaServiceRegistry registry = EurekaServiceRegistry.builder()
    .eurekaServerUrl("http://eureka1:8761/eureka,http://eureka2:8761/eureka,http://eureka3:8761/eureka")
    .appName("payment-service")
    .registryFetchInterval(Duration.ofSeconds(30))
    .build();
```

### Custom Metadata

```java
ServiceInstance instance = ServiceInstance.builder()
    .serviceId("payment-service")
    .instanceId("payment-01")
    .host("10.0.1.50")
    .port(8080)
    .addMetadata("version", "2.1.0")
    .addMetadata("datacenter", "us-west-2a")
    .addMetadata("weight", "100")          // Load balancing weight
    .addMetadata("canary", "true")         // Canary deployment
    .addMetadata("protocols", "http,grpc") // Supported protocols
    .build();

registry.register(instance);
```

### Filtering by Metadata

```java
Result<List<ServiceInstance>> instances = registry.discover("payment-service");

if (instances.isOk()) {
    // Filter canary instances
    List<ServiceInstance> canaryInstances = instances.getOrNull().stream()
        .filter(inst -> "true".equals(inst.metadata().get("canary")))
        .collect(Collectors.toList());

    // Filter by version
    List<ServiceInstance> v2Instances = instances.getOrNull().stream()
        .filter(inst -> inst.metadata().get("version").startsWith("2."))
        .collect(Collectors.toList());
}
```

### Secure Instances (HTTPS)

```java
ServiceInstance secureInstance = ServiceInstance.builder()
    .serviceId("payment-service")
    .instanceId("payment-secure-01")
    .host("api.example.com")
    .port(443)
    .secure(true)  // Use HTTPS
    .build();

EurekaServiceRegistry registry = EurekaServiceRegistry.builder()
    .eurekaServerUrl("https://eureka.example.com/eureka")
    .appName("payment-service")
    .securePort(true)
    .port(443)
    .build();

registry.register(secureInstance);
```

### Zone-Aware Routing

```java
// Register instance in specific availability zone
EurekaServiceRegistry registry = EurekaServiceRegistry.builder()
    .eurekaServerUrl("http://localhost:8761/eureka")
    .appName("payment-service")
    .metadata("zone", "us-east-1a")
    .metadata("region", "us-east-1")
    .build();

// Discover instances in same zone
Result<List<ServiceInstance>> instances = registry.discover("user-service");
if (instances.isOk()) {
    // Prefer instances in same zone
    List<ServiceInstance> sameZone = instances.getOrNull().stream()
        .filter(inst -> "us-east-1a".equals(inst.metadata().get("zone")))
        .collect(Collectors.toList());
}
```

## Spring Boot Integration

### Configuration Class

```java
import com.marcusprado02.commons.adapters.servicediscovery.eureka.EurekaServiceRegistry;
import com.marcusprado02.commons.ports.servicediscovery.ServiceRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Duration;

@Configuration
public class ServiceDiscoveryConfig {

    @Bean
    public ServiceRegistry serviceRegistry(
            @Value("${eureka.client.serviceUrl.defaultZone}") String eurekaUrl,
            @Value("${spring.application.name}") String appName,
            @Value("${server.port}") int port) {

        return EurekaServiceRegistry.builder()
            .eurekaServerUrl(eurekaUrl)
            .appName(appName)
            .port(port)
            .renewalInterval(Duration.ofSeconds(30))
            .expirationDuration(Duration.ofSeconds(90))
            .registryFetchInterval(Duration.ofSeconds(30))
            .build();
    }

    @Bean
    public ServiceRegistrar serviceRegistrar(
            ServiceRegistry registry,
            @Value("${spring.application.name}") String appName,
            @Value("${server.port}") int port) {
        return new ServiceRegistrar(registry, appName, port);
    }
}
```

### Application Properties

```yaml
# application.yml
spring:
  application:
    name: payment-service

server:
  port: 8080

eureka:
  client:
    serviceUrl:
      defaultZone: http://localhost:8761/eureka
    registry-fetch-interval-seconds: 30
    instance-info-replication-interval-seconds: 30
  instance:
    lease-renewal-interval-in-seconds: 30
    lease-expiration-duration-in-seconds: 90
    hostname: ${HOSTNAME:localhost}
    prefer-ip-address: true
    metadata-map:
      version: @project.version@
      region: ${AWS_REGION:us-east-1}
```

### Auto-Registration on Startup

```java
import com.marcusprado02.commons.ports.servicediscovery.ServiceInstance;
import com.marcusprado02.commons.ports.servicediscovery.ServiceRegistry;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import jakarta.annotation.PreDestroy;

@Component
public class ServiceRegistrar {

    private final ServiceRegistry registry;
    private final String appName;
    private final int port;
    private String instanceId;

    public ServiceRegistrar(ServiceRegistry registry, String appName, int port) {
        this.registry = registry;
        this.appName = appName;
        this.port = port;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void registerOnStartup() {
        instanceId = appName + "-" + System.currentTimeMillis();

        ServiceInstance instance = ServiceInstance.builder()
            .serviceId(appName)
            .instanceId(instanceId)
            .host(getHostName())
            .port(port)
            .addMetadata("startTime", String.valueOf(System.currentTimeMillis()))
            .build();

        registry.register(instance).onFailure(problem ->
            System.err.println("Failed to register: " + problem.message())
        );
    }

    @PreDestroy
    public void deregisterOnShutdown() {
        if (instanceId != null) {
            registry.deregister(instanceId);
        }
    }

    private String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "localhost";
        }
    }
}
```

### Service Discovery with Load Balancing

```java
import com.marcusprado02.commons.ports.servicediscovery.ServiceInstance;
import com.marcusprado02.commons.ports.servicediscovery.ServiceRegistry;
import java.util.List;
import java.util.Random;
import org.springframework.stereotype.Service;

@Service
public class UserServiceClient {

    private final ServiceRegistry registry;
    private final RestTemplate restTemplate;
    private final Random random = new Random();

    public UserServiceClient(ServiceRegistry registry, RestTemplate restTemplate) {
        this.registry = registry;
        this.restTemplate = restTemplate;
    }

    public User getUser(String userId) {
        // Discover user-service instances
        List<ServiceInstance> instances = registry.discover("user-service")
            .getOrElse(List.of());

        if (instances.isEmpty()) {
            throw new ServiceUnavailableException("user-service not available");
        }

        // Simple random load balancing
        ServiceInstance instance = instances.get(random.nextInt(instances.size()));
        String url = instance.getUri() + "/users/" + userId;

        return restTemplate.getForObject(url, User.class);
    }
}
```

## Kubernetes Deployment

### Eureka Server Deployment

```yaml
# eureka-server.yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: eureka-server
spec:
  serviceName: eureka-server
  replicas: 3
  selector:
    matchLabels:
      app: eureka-server
  template:
    metadata:
      labels:
        app: eureka-server
    spec:
      containers:
      - name: eureka
        image: springcloud/eureka:latest
        ports:
        - containerPort: 8761
        env:
        - name: EUREKA_INSTANCE_HOSTNAME
          valueFrom:
            fieldRef:
              fieldPath: metadata.name
        - name: EUREKA_CLIENT_SERVICEURL_DEFAULTZONE
          value: "http://eureka-server-0.eureka-server:8761/eureka/,http://eureka-server-1.eureka-server:8761/eureka/,http://eureka-server-2.eureka-server:8761/eureka/"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8761
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8761
          initialDelaySeconds: 30
          periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: eureka-server
spec:
  clusterIP: None
  selector:
    app: eureka-server
  ports:
  - port: 8761
    targetPort: 8761
```

### Client Application Deployment

```yaml
# payment-service.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: payment-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: payment-service
  template:
    metadata:
      labels:
        app: payment-service
    spec:
      containers:
      - name: payment-service
        image: payment-service:latest
        ports:
        - containerPort: 8080
        env:
        - name: EUREKA_CLIENT_SERVICEURL_DEFAULTZONE
          value: "http://eureka-server-0.eureka-server:8761/eureka/,http://eureka-server-1.eureka-server:8761/eureka/,http://eureka-server-2.eureka-server:8761/eureka/"
        - name: EUREKA_INSTANCE_HOSTNAME
          valueFrom:
            fieldRef:
              fieldPath: status.podIP
        - name: EUREKA_INSTANCE_PREFER_IP_ADDRESS
          value: "true"
---
apiVersion: v1
kind: Service
metadata:
  name: payment-service
spec:
  selector:
    app: payment-service
  ports:
  - port: 8080
    targetPort: 8080
```

## Error Handling

All operations return `Result<T>` for explicit error handling:

```java
Result<Void> result = registry.register(instance);

// Pattern matching style
result
    .onSuccess(v -> System.out.println("Registered!"))
    .onFailure(problem -> {
        System.err.println("Error: " + problem.message());
        System.err.println("Code: " + problem.code().value());
        System.err.println("Category: " + problem.category());
    });

// Traditional style
if (result.isOk()) {
    System.out.println("Success!");
} else {
    Problem problem = result.problemOrNull();
    switch (problem.code().value()) {
        case "REGISTRATION_FAILED":
            // Retry logic
            break;
        case "DISCOVERY_FAILED":
            // Fallback to cached instances
            break;
    }
}
```

**Common Error Codes:**
- `REGISTRATION_FAILED` - Failed to register service
- `DEREGISTRATION_FAILED` - Failed to deregister service
- `DISCOVERY_FAILED` - Failed to discover services
- `LIST_SERVICES_FAILED` - Failed to list services
- `GET_INSTANCES_FAILED` - Failed to get instances
- `HEARTBEAT_FAILED` - Failed to send heartbeat
- `OPERATION_NOT_SUPPORTED` - Operation not supported by Eureka

## Best Practices

### 1. Graceful Shutdown

```java
Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    try {
        registry.deregister(instanceId);
        registry.shutdown();
        System.out.println("Service deregistered successfully");
    } catch (Exception e) {
        System.err.println("Error during shutdown: " + e.getMessage());
    }
}));
```

### 2. Heartbeat Configuration

```java
// Production settings
EurekaServiceRegistry registry = EurekaServiceRegistry.builder()
    .renewalInterval(Duration.ofSeconds(30))     // Default: 30s
    .expirationDuration(Duration.ofSeconds(90))  // 3x renewal interval
    .registryFetchInterval(Duration.ofSeconds(30))
    .build();
```

**Recommendations:**
- Renewal interval: 30 seconds (default)
- Expiration: 90 seconds (3x renewal)
- Fetch interval: 30 seconds for clients

### 3. Instance Naming

Use consistent naming patterns:
```java
String instanceId = String.format("%s-%s-%d",
    appName,
    hostname,
    System.currentTimeMillis()
);
```

### 4. Caching Discovered Instances

```java
public class CachedServiceDiscovery {
    private final ServiceRegistry registry;
    private final Map<String, CachedInstances> cache = new ConcurrentHashMap<>();

    public List<ServiceInstance> discover(String serviceId) {
        CachedInstances cached = cache.get(serviceId);

        if (cached != null && !cached.isExpired()) {
            return cached.instances;
        }

        Result<List<ServiceInstance>> result = registry.discover(serviceId);
        if (result.isOk()) {
            cache.put(serviceId, new CachedInstances(result.getOrNull()));
            return result.getOrNull();
        }

        // Return stale cache on error
        return cached != null ? cached.instances : List.of();
    }

    private static class CachedInstances {
        final List<ServiceInstance> instances;
        final long timestamp;

        CachedInstances(List<ServiceInstance> instances) {
            this.instances = instances;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > 30_000; // 30s TTL
        }
    }
}
```

### 5. Zone-Aware Client

```java
public class ZoneAwareDiscovery {
    private final ServiceRegistry registry;
    private final String currentZone;

    public List<ServiceInstance> discover(String serviceId) {
        List<ServiceInstance> allInstances = registry.discover(serviceId)
            .getOrElse(List.of());

        // Prefer instances in same zone
        List<ServiceInstance> sameZone = allInstances.stream()
            .filter(inst -> currentZone.equals(inst.metadata().get("zone")))
            .collect(Collectors.toList());

        // Fallback to all instances if none in same zone
        return sameZone.isEmpty() ? allInstances : sameZone;
    }
}
```

## Monitoring

### Key Metrics to Monitor

1. **Registration Status**
   - Track successful/failed registrations
   - Monitor registration latency

2. **Heartbeat Success Rate**
   - Monitor renewal success/failure
   - Alert on consecutive failures

3. **Service Discovery**
   - Track discovery latency
   - Monitor instance count per service
   - Alert on empty discovery results

4. **Eureka Server Health**
   - Monitor server availability
   - Track self-preservation mode activation
   - Monitor registry size

### Micrometer Integration

```java
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

public class MonitoredEurekaRegistry implements ServiceRegistry {

    private final EurekaServiceRegistry delegate;
    private final MeterRegistry meterRegistry;

    @Override
    public Result<Void> register(ServiceInstance instance) {
        return Timer.builder("eureka.register")
            .tag("service", instance.serviceId())
            .register(meterRegistry)
            .record(() -> {
                Result<Void> result = delegate.register(instance);
                meterRegistry.counter("eureka.register",
                    "status", result.isOk() ? "success" : "failure"
                ).increment();
                return result;
            });
    }

    @Override
    public Result<List<ServiceInstance>> discover(String serviceId) {
        return Timer.builder("eureka.discover")
            .tag("service", serviceId)
            .register(meterRegistry)
            .record(() -> {
                Result<List<ServiceInstance>> result = delegate.discover(serviceId);
                if (result.isOk()) {
                    meterRegistry.gauge("eureka.instances",
                        List.of(Tag.of("service", serviceId)),
                        result.getOrNull(),
                        List::size);
                }
                return result;
            });
    }
}
```

## Troubleshooting

### Problem: Services not appearing in Eureka

**Symptoms:**
- Service registers but doesn't appear in discovery results
- Empty list when calling `discover()`

**Solutions:**
1. Check Eureka server is running: `curl http://localhost:8761`
2. Verify registration interval configuration
3. Wait for cache refresh (up to 30 seconds)
4. Check application name matches (case-insensitive)
5. Verify network connectivity to Eureka server

```java
// Enable debug logging
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

Logger root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
root.setLevel(Level.DEBUG);
```

### Problem: Instances showing as DOWN

**Symptoms:**
- Instance registered but marked as DOWN
- Discover returns empty list

**Solutions:**
1. Check heartbeat is being sent (renewal interval)
2. Verify expiration duration is sufficient (3x renewal)
3. Check network latency between client and server
4. Disable self-preservation in development

```java
// Adjust timing
EurekaServiceRegistry registry = EurekaServiceRegistry.builder()
    .renewalInterval(Duration.ofSeconds(10))     // More frequent
    .expirationDuration(Duration.ofSeconds(30))  // Longer grace period
    .build();
```

### Problem: Self-Preservation Mode Warning

**Symptoms:**
- Eureka dashboard shows "EMERGENCY!" warning
- Instances not being evicted properly

**Solutions:**
1. For development, disable self-preservation on server:
```yaml
eureka:
  server:
    enable-self-preservation: false
```

2. For production, ensure sufficient heartbeats:
   - Check network stability
   - Increase renewal frequency if needed
   - Monitor renewal success rate

### Problem: Slow discovery updates

**Symptoms:**
- New instances take long to appear
- Deregistered instances still appearing

**Solutions:**
1. Reduce fetch interval:
```java
EurekaServiceRegistry registry = EurekaServiceRegistry.builder()
    .registryFetchInterval(Duration.ofSeconds(10))  // Fetch more frequently
    .build();
```

2. Use caching with shorter TTL
3. Consider client-side load balancing with health checks

### Problem: Connection timeouts

**Symptoms:**
- `REGISTRATION_FAILED` errors
- Timeout exceptions in logs

**Solutions:**
1. Check Eureka server is accessible
2. Verify firewall rules allow connections
3. Increase connection timeout:
```java
System.setProperty("eureka.client.transport.readTimeout", "10000");
System.setProperty("eureka.client.transport.connectTimeout", "5000");
```

## Comparison: Eureka vs Consul

| Feature | Eureka | Consul |
|---------|--------|--------|
| Health Checks | Heartbeat-based (client push) | HTTP/TCP/TTL (server pull) |
| Service Discovery | REST API | DNS + HTTP API |
| Consistency Model | AP (Available, Partition-tolerant) | CP (Consistent, Partition-tolerant) |
| Configuration | No | Yes (KV store) |
| Multi-datacenter | Limited | Native support |
| Performance | High (in-memory) | Medium (persistent) |
| Spring Cloud Support | Native | Via adapter |
| Metadata | Yes | Yes (tags) |

**When to use Eureka:**
- AWS cloud deployments
- Spring Cloud ecosystem
- Focus on availability over consistency
- Simple service discovery needs

**When to use Consul:**
- Multi-datacenter requirements
- Need configuration management
- Require consistent service registry
- Advanced health check requirements

## References

- [Netflix Eureka Wiki](https://github.com/Netflix/eureka/wiki)
- [Spring Cloud Netflix](https://spring.io/projects/spring-cloud-netflix)
- [Service Discovery Pattern](https://microservices.io/patterns/service-registry.html)
- [Commons Platform Documentation](../README.md)

## License

Part of the Commons Platform - see [LICENSE](../LICENSE) for details.
