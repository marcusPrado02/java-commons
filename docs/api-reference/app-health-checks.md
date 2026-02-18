# API Reference: commons-app-health-checks

## Visão Geral

O módulo `commons-app-health-checks` fornece infraestrutura para health checks de aplicações, seguindo as práticas de Kubernetes liveness e readiness probes.

**Dependência Maven:**
```xml
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-app-health-checks</artifactId>
</dependency>
```

---

## Core Concepts

### HealthCheck

Interface base para health checks.

```java
public interface HealthCheck {
    
    /**
     * Nome único do health check
     */
    String name();
    
    /**
     * Executa o health check
     */
    HealthCheckResult check();
}
```

### HealthCheckResult

Resultado de um health check.

```java
public class HealthCheckResult {
    
    private String name;
    private HealthStatus status;       // UP, DOWN, DEGRADED, UNKNOWN
    private String message;
    private Map<String, Object> details;
    private Duration responseTime;
    private Instant timestamp;
    
    public static HealthCheckResult up(String name) {
        return builder()
            .name(name)
            .status(HealthStatus.UP)
            .build();
    }
    
    public static HealthCheckResult down(String name, String message) {
        return builder()
            .name(name)
            .status(HealthStatus.DOWN)
            .message(message)
            .build();
    }
}

public enum HealthStatus {
    UP,        // Saudável
    DOWN,      // Falha crítica
    DEGRADED,  // Funcionando mas com problemas
    UNKNOWN    // Estado desconhecido
}
```

---

## Built-in Health Checks

### 1. Database Health Check

```java
@Component
public class DatabaseHealthCheck implements HealthCheck {
    
    private final DataSource dataSource;
    
    @Override
    public String name() {
        return "database";
    }
    
    @Override
    public HealthCheckResult check() {
        Instant start = Instant.now();
        
        try (Connection conn = dataSource.getConnection()) {
            // Executa query simples
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT 1");
                rs.next();
            }
            
            Duration responseTime = Duration.between(start, Instant.now());
            
            return HealthCheckResult.builder()
                .name(name())
                .status(HealthStatus.UP)
                .detail("responseTime", responseTime.toMillis() + "ms")
                .detail("url", getMaskedUrl(conn))
                .responseTime(responseTime)
                .build();
                
        } catch (Exception e) {
            return HealthCheckResult.builder()
                .name(name())
                .status(HealthStatus.DOWN)
                .message("Database connection failed: " + e.getMessage())
                .build();
        }
    }
}
```

### 2. Messaging Health Check

```java
@Component
public class KafkaHealthCheck implements HealthCheck {
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    
    @Override
    public String name() {
        return "kafka";
    }
    
    @Override
    public HealthCheckResult check() {
        try {
            // Testa conectividade
            Map<String, Object> configs = kafkaTemplate.getProducerFactory()
                .getConfigurationProperties();
            
            String bootstrapServers = (String) configs.get("bootstrap.servers");
            
            // Tenta listar tópicos
            AdminClient adminClient = AdminClient.create(configs);
            ListTopicsResult topics = adminClient.listTopics();
            topics.names().get(5, TimeUnit.SECONDS);  // Timeout 5s
            
            return HealthCheckResult.builder()
                .name(name())
                .status(HealthStatus.UP)
                .detail("bootstrapServers", bootstrapServers)
                .build();
                
        } catch (TimeoutException e) {
            return HealthCheckResult.builder()
                .name(name())
                .status(HealthStatus.DEGRADED)
                .message("Kafka is slow to respond")
                .build();
                
        } catch (Exception e) {
            return HealthCheckResult.builder()
                .name(name())
                .status(HealthStatus.DOWN)
                .message("Kafka connection failed: " + e.getMessage())
                .build();
        }
    }
}
```

### 3. Redis Health Check

```java
@Component
public class RedisHealthCheck implements HealthCheck {
    
    private final RedisTemplate<String, String> redisTemplate;
    
    @Override
    public String name() {
        return "redis";
    }
    
    @Override
    public HealthCheckResult check() {
        try {
            String response = redisTemplate.getConnectionFactory()
                .getConnection()
                .ping();
            
            if ("PONG".equals(response)) {
                return HealthCheckResult.up(name())
                    .withDetail("status", "Connected");
            }
            
            return HealthCheckResult.down(name(), "Unexpected ping response: " + response);
            
        } catch (Exception e) {
            return HealthCheckResult.down(name(), e.getMessage());
        }
    }
}
```

### 4. HTTP Dependency Health Check

```java
@Component
public class PaymentGatewayHealthCheck implements HealthCheck {
    
    private final RestTemplate restTemplate;
    private final String healthEndpoint = "https://api.payment-gateway.com/health";
    
    @Override
    public String name() {
        return "payment-gateway";
    }
    
    @Override
    public HealthCheckResult check() {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                healthEndpoint,
                String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                return HealthCheckResult.up(name())
                    .withDetail("endpoint", healthEndpoint);
            }
            
            return HealthCheckResult.down(name(), 
                "Unexpected status: " + response.getStatusCode());
                
        } catch (Exception e) {
            return HealthCheckResult.down(name(), e.getMessage());
        }
    }
}
```

### 5. Disk Space Health Check

```java
@Component
public class DiskSpaceHealthCheck implements HealthCheck {
    
    private final long thresholdBytes = 1024 * 1024 * 1024; // 1GB
    
    @Override
    public String name() {
        return "disk-space";
    }
    
    @Override
    public HealthCheckResult check() {
        File root = new File("/");
        long usableBytes = root.getUsableSpace();
        long totalBytes = root.getTotalSpace();
        long usedBytes = totalBytes - usableBytes;
        
        double usedPercentage = (double) usedBytes / totalBytes * 100;
        
        HealthStatus status = usableBytes > thresholdBytes 
            ? HealthStatus.UP 
            : HealthStatus.DEGRADED;
        
        return HealthCheckResult.builder()
            .name(name())
            .status(status)
            .detail("total", formatBytes(totalBytes))
            .detail("used", formatBytes(usedBytes))
            .detail("usable", formatBytes(usableBytes))
            .detail("usedPercentage", String.format("%.2f%%", usedPercentage))
            .build();
    }
}
```

---

## Liveness vs Readiness

### Liveness Probe

Verifica se a aplicação está **viva** (não travada).

```java
public interface LivenessCheck extends HealthCheck {
    // Marker interface
}

@Component
public class ApplicationLivenessCheck implements LivenessCheck {
    
    private final ApplicationContext applicationContext;
    
    @Override
    public String name() {
        return "application";
    }
    
    @Override
    public HealthCheckResult check() {
        // Verifica se contexto está rodando
        if (applicationContext.isActive()) {
            return HealthCheckResult.up(name())
                .withDetail("state", "running");
        }
        
        return HealthCheckResult.down(name(), "Application context not active");
    }
}
```

### Readiness Probe

Verifica se a aplicação está **pronta** para receber tráfego.

```java
public interface ReadinessCheck extends HealthCheck {
    // Marker interface
}

@Component
public class DatabaseReadinessCheck implements ReadinessCheck {
    
    private final DataSource dataSource;
    private final int maxPoolSize;
    
    @Override
    public String name() {
        return "database-ready";
    }
    
    @Override
    public HealthCheckResult check() {
        try {
            HikariPoolMXBean pool = ((HikariDataSource) dataSource).getHikariPoolMXBean();
            
            int activeConnections = pool.getActiveConnections();
            int idleConnections = pool.getIdleConnections();
            int totalConnections = pool.getTotalConnections();
            
            // Degradado se pool quase esgotado
            HealthStatus status = activeConnections > (maxPoolSize * 0.9)
                ? HealthStatus.DEGRADED
                : HealthStatus.UP;
            
            return HealthCheckResult.builder()
                .name(name())
                .status(status)
                .detail("activeConnections", activeConnections)
                .detail("idleConnections", idleConnections)
                .detail("totalConnections", totalConnections)
                .detail("maxPoolSize", maxPoolSize)
                .build();
                
        } catch (Exception e) {
            return HealthCheckResult.down(name(), e.getMessage());
        }
    }
}
```

---

## Health Aggregator

Agrega múltiplos health checks.

```java
@Service
public class HealthAggregator {
    
    private final List<HealthCheck> healthChecks;
    
    public AggregatedHealth checkAll() {
        List<HealthCheckResult> results = healthChecks.parallelStream()
            .map(HealthCheck::check)
            .toList();
        
        HealthStatus overallStatus = determineOverallStatus(results);
        
        return AggregatedHealth.builder()
            .status(overallStatus)
            .checks(results)
            .timestamp(Instant.now())
            .build();
    }
    
    public AggregatedHealth checkLiveness() {
        List<HealthCheckResult> results = healthChecks.stream()
            .filter(check -> check instanceof LivenessCheck)
            .map(HealthCheck::check)
            .toList();
        
        HealthStatus overallStatus = determineOverallStatus(results);
        
        return AggregatedHealth.builder()
            .status(overallStatus)
            .checks(results)
            .build();
    }
    
    public AggregatedHealth checkReadiness() {
        List<HealthCheckResult> results = healthChecks.stream()
            .filter(check -> check instanceof ReadinessCheck)
            .map(HealthCheck::check)
            .toList();
        
        HealthStatus overallStatus = determineOverallStatus(results);
        
        return AggregatedHealth.builder()
            .status(overallStatus)
            .checks(results)
            .build();
    }
    
    private HealthStatus determineOverallStatus(List<HealthCheckResult> results) {
        if (results.stream().anyMatch(r -> r.status() == HealthStatus.DOWN)) {
            return HealthStatus.DOWN;
        }
        
        if (results.stream().anyMatch(r -> r.status() == HealthStatus.DEGRADED)) {
            return HealthStatus.DEGRADED;
        }
        
        if (results.stream().allMatch(r -> r.status() == HealthStatus.UP)) {
            return HealthStatus.UP;
        }
        
        return HealthStatus.UNKNOWN;
    }
}
```

---

## REST Endpoints

### Health Controller

```java
@RestController
@RequestMapping("/actuator")
public class HealthController {
    
    private final HealthAggregator healthAggregator;
    
    @GetMapping("/health")
    public ResponseEntity<AggregatedHealth> health() {
        AggregatedHealth health = healthAggregator.checkAll();
        
        HttpStatus httpStatus = switch (health.status()) {
            case UP -> HttpStatus.OK;
            case DEGRADED -> HttpStatus.OK;  // 200 mas com warning
            case DOWN -> HttpStatus.SERVICE_UNAVAILABLE;
            case UNKNOWN -> HttpStatus.SERVICE_UNAVAILABLE;
        };
        
        return ResponseEntity
            .status(httpStatus)
            .body(health);
    }
    
    @GetMapping("/health/liveness")
    public ResponseEntity<AggregatedHealth> liveness() {
        AggregatedHealth health = healthAggregator.checkLiveness();
        
        return health.status() == HealthStatus.UP
            ? ResponseEntity.ok(health)
            : ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(health);
    }
    
    @GetMapping("/health/readiness")
    public ResponseEntity<AggregatedHealth> readiness() {
        AggregatedHealth health = healthAggregator.checkReadiness();
        
        return health.status() == HealthStatus.UP
            ? ResponseEntity.ok(health)
            : ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(health);
    }
}
```

### Response Format

```json
{
  "status": "UP",
  "timestamp": "2026-02-17T10:30:00Z",
  "checks": [
    {
      "name": "database",
      "status": "UP",
      "responseTime": "15ms",
      "details": {
        "url": "jdbc:postgresql://localhost:5432/mydb"
      }
    },
    {
      "name": "kafka",
      "status": "UP",
      "details": {
        "bootstrapServers": "localhost:9092"
      }
    },
    {
      "name": "redis",
      "status": "DEGRADED",
      "message": "High latency detected",
      "details": {
        "latency": "250ms"
      }
    }
  ]
}
```

---

## Kubernetes Integration

### Deployment YAML

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: myapp
spec:
  template:
    spec:
      containers:
      - name: myapp
        image: myapp:latest
        ports:
        - containerPort: 8080
        
        # Liveness: Reinicia se falhar
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
        
        # Readiness: Remove do load balancer se falhar
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 10
          periodSeconds: 5
          timeoutSeconds: 3
          failureThreshold: 2
```

---

## Caching

```java
@Component
public class CachedHealthCheck implements HealthCheck {
    
    private final HealthCheck delegate;
    private final Duration cacheTtl = Duration.ofSeconds(30);
    private volatile HealthCheckResult cachedResult;
    private volatile Instant lastCheck;
    
    @Override
    public String name() {
        return delegate.name();
    }
    
    @Override
    public HealthCheckResult check() {
        Instant now = Instant.now();
        
        if (cachedResult != null && 
            lastCheck != null &&
            Duration.between(lastCheck, now).compareTo(cacheTtl) < 0) {
            return cachedResult;  // Cache hit
        }
        
        // Cache miss - executa
        HealthCheckResult result = delegate.check();
        cachedResult = result;
        lastCheck = now;
        
        return result;
    }
}
```

---

## Monitoring

```java
@Component
public class HealthCheckMetrics {
    
    private final MetricsFacade metrics;
    
    @EventListener
    public void onHealthCheckExecuted(HealthCheckExecutedEvent event) {
        HealthCheckResult result = event.result();
        
        // Incrementa contador
        metrics.incrementCounter(
            "health.check.executed",
            "name", result.name(),
            "status", result.status().name()
        );
        
        // Registra tempo de resposta
        metrics.recordTimer(
            "health.check.duration",
            result.responseTime(),
            "name", result.name()
        );
        
        // Gauge de status (UP=1, DOWN=0)
        metrics.recordGauge(
            "health.check.status",
            result.status() == HealthStatus.UP ? 1 : 0,
            "name", result.name()
        );
    }
}
```

---

## Best Practices

### ✅ DO

```java
// ✅ Timeouts em health checks externos
RestTemplate restTemplate = new RestTemplateBuilder()
    .setConnectTimeout(Duration.ofSeconds(2))
    .setReadTimeout(Duration.ofSeconds(3))
    .build();

// ✅ Cache health checks pesados
@Cacheable(value = "health", ttl = 30)
public HealthCheckResult check() { ... }

// ✅ Diferencie liveness e readiness
// - Liveness: App está viva? (simples, rápido)
// - Readiness: App pronta para tráfego? (verifica deps)

// ✅ Use timeouts agressivos
@Override
public HealthCheckResult check() {
    try {
        return CompletableFuture
            .supplyAsync(this::doCheck)
            .get(5, TimeUnit.SECONDS);
    } catch (TimeoutException e) {
        return HealthCheckResult.down(name(), "Timeout");
    }
}
```

### ❌ DON'T

```java
// ❌ NÃO faça health checks pesados em liveness
// ❌ NÃO bloqueie health checks por muito tempo
// ❌ NÃO exponha dados sensíveis em health checks
// ❌ NÃO use health checks para business logic
```

---

## Ver Também

- [Observability](../guides/observability.md)
- [Kubernetes Deployment](../guides/kubernetes.md)
- [Monitoring Best Practices](../guides/monitoring.md)
