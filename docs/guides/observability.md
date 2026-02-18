# Guia: Observabilidade com commons-app-observability

## Visão Geral

O módulo `commons-app-observability` fornece APIs framework-agnostic para os três pilares da observabilidade:
- **Logs** estruturados com contexto automático
- **Métricas** (SLI/SLO) 
- **Tracing** distribuído
- **Health Checks** (liveness/readiness)

## 📦 Instalação

```xml
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-app-observability</artifactId>
    <version>${commons.version}</version>
</dependency>

<!-- Adapters (escolha um) -->
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-adapters-otel</artifactId>
</dependency>
```

---

## 📊 Logs Estruturados

### RequestContext

Thread-local context para correlation-id, tenant-id, actor-id, etc.

#### Uso Básico

```java
import com.marcusprado02.commons.app.observability.RequestContext;
import com.marcusprado02.commons.app.observability.ContextKeys;

public class OrderController {
    
    @PostMapping("/orders")
    public ResponseEntity<?> createOrder(@RequestBody OrderRequest request) {
        // 1. Popula contexto no início do request
        RequestContext.put(ContextKeys.CORRELATION_ID, UUID.randomUUID().toString());
        RequestContext.put(ContextKeys.TENANT_ID, getTenantId());
        RequestContext.put(ContextKeys.ACTOR_ID, getCurrentUserId());
        
        try {
            // 2. Contexto é automaticamente incluído em todos os logs
            Order order = orderService.createOrder(request);
            return ResponseEntity.ok(order);
            
        } finally {
            // 3. SEMPRE limpe o contexto
            RequestContext.clear();
        }
    }
}
```

#### Context Keys Padrão

```java
public final class ContextKeys {
    public static final String CORRELATION_ID = "correlationId";
    public static final String TENANT_ID = "tenantId";
    public static final String ACTOR_ID = "actorId";
    public static final String REQUEST_ID = "requestId";
    public static final String SESSION_ID = "sessionId";
    public static final String SOURCE_IP = "sourceIp";
    public static final String USER_AGENT = "userAgent";
}
```

#### Snapshot e Propagação

```java
// Captura snapshot do contexto atual
Map<String, String> snapshot = RequestContext.snapshot();

// Propaga para outra thread (CompletableFuture, @Async, etc.)
CompletableFuture.supplyAsync(() -> {
    RequestContext.restoreFrom(snapshot);
    try {
        // O contexto está disponível nesta thread
        return orderService.processOrder();
    } finally {
        RequestContext.clear();
    }
});
```

### StructuredLog

Builder para criar logs estruturados em formato JSON.

#### Uso Básico

```java
import com.marcusprado02.commons.app.observability.StructuredLog;

public class OrderService {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);
    
    public Order createOrder(CreateOrderCommand cmd) {
        Map<String, Object> log = StructuredLog.builder()
            .level("INFO")
            .message("Creating order")
            .field("userId", cmd.userId())
            .field("itemCount", cmd.items().size())
            .build();
        
        logger.info("{}", log);
        // Saída JSON:
        // {
        //   "timestamp": "2026-02-17T10:30:00Z",
        //   "level": "INFO",
        //   "message": "Creating order",
        //   "correlationId": "abc-123",  // Do RequestContext
        //   "tenantId": "tenant-1",      // Do RequestContext
        //   "userId": "user-456",
        //   "itemCount": 3
        // }
        
        // ... lógica
    }
}
```

#### Atalhos Convenientes

```java
// INFO
Map<String, Object> log = StructuredLog.info("User logged in");

// WARN
Map<String, Object> log = StructuredLog.warn("High memory usage");

// ERROR com exception
Map<String, Object> log = StructuredLog.error("Order failed", exception);
```

#### Com Campos Customizados

```java
Map<String, Object> log = StructuredLog.builder()
    .level("INFO")
    .message("Payment processed")
    .field("orderId", order.id().value())
    .field("amount", order.totalAmount().toString())
    .field("paymentMethod", paymentMethod)
    .field("duration", Duration.between(start, end).toMillis())
    .build();

logger.info("{}", log);
```

#### Sanitização Automática

Chaves sensíveis são automaticamente redadas:

```java
Map<String, Object> log = StructuredLog.builder()
    .message("User authenticated")
    .field("username", "john")
    .field("password", "secret123")      // ⚠️ Sensível
    .field("authorization", "Bearer xyz") // ⚠️ Sensível
    .build();

// Saída:
// {
//   "username": "john",
//   "password": "***REDACTED***",  // ✅ Sanitizado
//   "authorization": "***REDACTED***"  // ✅ Sanitizado
// }
```

#### Customizar Sanitização

```java
LogSanitizer customSanitizer = new LogSanitizer() {
    @Override
    public String sanitize(String key, String value) {
        if (key.toLowerCase().contains("ssn")) {
            return "***SSN****";
        }
        return value;  // Não sanitiza
    }
};

Map<String, Object> log = StructuredLog.builder()
    .message("User registered")
    .field("ssn", "123-45-6789")
    .sanitizer(customSanitizer)
    .build();
```

---

## 📈 Métricas

### MetricsFacade

Interface abstrata para registrar métricas SLI/SLO.

#### Configuração

```java
import com.marcusprado02.commons.app.observability.MetricsFacade;
import com.marcusprado02.commons.adapters.otel.OtelMetricsFacade;

@Configuration
public class ObservabilityConfig {
    
    @Bean
    public MetricsFacade metricsFacade() {
        // Produção: OpenTelemetry, Prometheus, Micrometer, etc.
        return OtelMetricsFacade.create();
        
        // Testes/Dev: No-op
        // return MetricsFacade.noop();
    }
}
```

#### Counters

```java
public class OrderService {
    
    private final MetricsFacade metrics;
    
    public void createOrder(Order order) {
        // Incrementa contador
        metrics.incrementCounter(
            "order.created",
            Map.of(
                "status", order.status().name(),
                "channel", "web"
            )
        );
    }
    
    public void cancelOrder(Order order) {
        metrics.incrementCounter(
            "order.cancelled",
            Map.of("reason", order.cancellationReason())
        );
    }
}
```

#### Histograms

Para distribuições (latências, tamanhos, etc.):

```java
public class PaymentService {
    
    private final MetricsFacade metrics;
    
    public void processPayment(Payment payment) {
        Instant start = Instant.now();
        
        try {
            // ... processa pagamento
            
            Duration duration = Duration.between(start, Instant.now());
            
            metrics.recordHistogram(
                "payment.processing.duration",
                duration.toMillis(),
                Map.of(
                    "method", payment.method().name(),
                    "status", "success"
                )
            );
            
        } catch (Exception ex) {
            Duration duration = Duration.between(start, Instant.now());
            
            metrics.recordHistogram(
                "payment.processing.duration",
                duration.toMillis(),
                Map.of(
                    "method", payment.method().name(),
                    "status", "failure"
                )
            );
            
            throw ex;
        }
    }
}
```

#### Gauges

Para valores instantâneos (pool size, queue depth, etc.):

```java
public class CacheMonitor {
    
    private final MetricsFacade metrics;
    private final Cache cache;
    
    @Scheduled(fixedRate = 5000)  // A cada 5 segundos
    public void reportMetrics() {
        metrics.recordGauge(
            "cache.size",
            cache.size(),
            Map.of("cache", cache.name())
        );
        
        metrics.recordGauge(
            "cache.hit.ratio",
            cache.hitRatio(),
            Map.of("cache", cache.name())
        );
    }
}
```

### Metrics Helper

Funções utilitárias de alto nível:

```java
import com.marcusprado02.commons.app.observability.Metrics;

public class UserController {
    
    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestBody UserRequest request) {
        Instant start = Instant.now();
        
        try {
            User user = userService.createUser(request);
            
            // Helper: registra request bem-sucedido com duração
            Metrics.recordRequest(
                metrics,
                "CreateUser",
                Duration.between(start, Instant.now()),
                true  // success
            );
            
            return ResponseEntity.ok(user);
            
        } catch (Exception ex) {
            Metrics.recordRequest(
                metrics,
                "CreateUser",
                Duration.between(start, Instant.now()),
                false  // failure
            );
            
            throw ex;
        }
    }
}
```

---

## 🔍 Tracing Distribuído

### TracerFacade

```java
import com.marcusprado02.commons.app.observability.TracerFacade;

public class OrderService {
    
    private final TracerFacade tracer;
    
    public Order createOrder(CreateOrderCommand cmd) {
        // Inicia span
        var span = tracer.startSpan("OrderService.createOrder");
        
        try {
            span.setAttribute("userId", cmd.userId());
            span.setAttribute("itemCount", cmd.items().size());
            
            // ... lógica
            
            span.setStatus("OK");
            return order;
            
        } catch (Exception ex) {
            span.setStatus("ERROR");
            span.recordException(ex);
            throw ex;
            
        } finally {
            span.end();
        }
    }
    
    public void processOrder(Order order) {
        // Nested span
        var parentSpan = tracer.getCurrentSpan();
        var childSpan = tracer.startSpan("processOrder", parentSpan);
        
        try {
            // ... lógica
            childSpan.setStatus("OK");
        } finally {
            childSpan.end();
        }
    }
}
```

---

## 🏥 Health Checks

### HealthCheck Interface

```java
public interface HealthCheck {
    String name();
    HealthCheckType type();  // LIVENESS ou READINESS
    HealthCheckResult check();
}
```

### Implementações Customizadas

#### Database Health Check

```java
public class DatabaseHealthCheck implements HealthCheck {
    
    private final DataSource dataSource;
    
    @Override
    public String name() {
        return "database";
    }
    
    @Override
    public HealthCheckType type() {
        return HealthCheckType.READINESS;  // Afeta readiness probe
    }
    
    @Override
    public HealthCheckResult check() {
        try {
            Connection conn = dataSource.getConnection();
            boolean isValid = conn.isValid(3);  // 3 segundos timeout
            conn.close();
            
            if (isValid) {
                return HealthCheckResult.up(name(), type())
                    .withDetail("vendor", getDatabaseVendor())
                    .build();
            } else {
                return HealthCheckResult.down(name(), type())
                    .withDetail("error", "Connection validation failed")
                    .build();
            }
            
        } catch (SQLException ex) {
            return HealthCheckResult.down(name(), type())
                .withDetail("error", ex.getMessage())
                .build();
        }
    }
}
```

#### Redis Health Check

```java
public class RedisHealthCheck implements HealthCheck {
    
    private final RedisClient redis;
    
    @Override
    public String name() {
        return "redis";
    }
    
    @Override
    public HealthCheckType type() {
        return HealthCheckType.READINESS;
    }
    
    @Override
    public HealthCheckResult check() {
        try {
            String pong = redis.ping();
            
            if ("PONG".equals(pong)) {
                return HealthCheckResult.up(name(), type())
                    .withDetail("latency", measureLatency() + "ms")
                    .build();
            } else {
                return HealthCheckResult.down(name(), type()).build();
            }
            
        } catch (Exception ex) {
            return HealthCheckResult.down(name(), type())
                .withDetail("error", ex.getMessage())
                .build();
        }
    }
}
```

#### Disk Space Health Check

```java
public class DiskSpaceHealthCheck implements HealthCheck {
    
    private static final long THRESHOLD_BYTES = 100 * 1024 * 1024; // 100 MB
    
    @Override
    public String name() {
        return "diskSpace";
    }
    
    @Override
    public HealthCheckType type() {
        return HealthCheckType.LIVENESS;  // Afeta liveness probe
    }
    
    @Override
    public HealthCheckResult check() {
        File root = new File("/");
        long usableSpace = root.getUsableSpace();
        long totalSpace = root.getTotalSpace();
        
        double usagePercent = 100.0 * (totalSpace - usableSpace) / totalSpace;
        
        HealthStatus status = usableSpace > THRESHOLD_BYTES 
            ? HealthStatus.UP 
            : HealthStatus.DOWN;
        
        return HealthCheckResult.builder()
            .name(name())
            .type(type())
            .status(status)
            .detail("total", formatBytes(totalSpace))
            .detail("free", formatBytes(usableSpace))
            .detail("usagePercent", String.format("%.2f%%", usagePercent))
            .build();
    }
}
```

### HealthChecks Registry

```java
import com.marcusprado02.commons.app.observability.HealthChecks;
import com.marcusprado02.commons.app.observability.HealthReport;

@Configuration
public class HealthCheckConfig {
    
    @Bean
    public HealthChecks healthChecks(
            DatabaseHealthCheck databaseCheck,
            RedisHealthCheck redisCheck,
            DiskSpaceHealthCheck diskCheck) {
        
        return new HealthChecks(List.of(
            databaseCheck,
            redisCheck,
            diskCheck
        ));
    }
}

@RestController
public class HealthController {
    
    private final HealthChecks healthChecks;
    
    @GetMapping("/health/liveness")
    public ResponseEntity<?> liveness() {
        HealthReport report = healthChecks.liveness();
        
        HttpStatus status = report.status() == HealthStatus.UP
            ? HttpStatus.OK
            : HttpStatus.SERVICE_UNAVAILABLE;
        
        return ResponseEntity.status(status).body(report);
    }
    
    @GetMapping("/health/readiness")
    public ResponseEntity<?> readiness() {
        HealthReport report = healthChecks.readiness();
        
        HttpStatus status = report.status() == HealthStatus.UP
            ? HttpStatus.OK
            : HttpStatus.SERVICE_UNAVAILABLE;
        
        return ResponseEntity.status(status).body(report);
    }
}
```

---

## 🎯 Padrões de Uso Completos

### Observabilidade End-to-End

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);
    
    private final OrderApplicationService orderService;
    private final MetricsFacade metrics;
    private final TracerFacade tracer;
    
    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody CreateOrderRequest request,
                                        @RequestHeader("X-Correlation-ID") String correlationId) {
        // 1. Setup context
        RequestContext.put(ContextKeys.CORRELATION_ID, correlationId);
        RequestContext.put(ContextKeys.TENANT_ID, getTenantId());
        RequestContext.put(ContextKeys.ACTOR_ID, getCurrentUserId());
        
        // 2. Start span
        var span = tracer.startSpan("POST /api/orders");
        Instant start = Instant.now();
        
        try {
            // 3. Log início
            logger.info("{}", StructuredLog.builder()
                .message("Creating order")
                .field("itemCount", request.items().size())
                .build());
            
            // 4. Executa operação
            Result<OrderId> result = orderService.createOrder(request);
            
            Duration duration = Duration.between(start, Instant.now());
            
            return result
                .map(orderId -> {
                    // 5. Sucesso: logs, métricas, span
                    logger.info("{}", StructuredLog.builder()
                        .message("Order created successfully")
                        .field("orderId", orderId.value())
                        .field("duration", duration.toMillis())
                        .build());
                    
                    metrics.incrementCounter("order.created", Map.of("status", "success"));
                    Metrics.recordRequest(metrics, "CreateOrder", duration, true);
                    
                    span.setStatus("OK");
                    span.setAttribute("orderId", orderId.value());
                    
                    return ResponseEntity
                        .status(HttpStatus.CREATED)
                        .body(Map.of("orderId", orderId.value()));
                })
                .orElseGet(() -> {
                    // 6. Falha: logs, métricas, span
                    Problem problem = result.problemOrNull();
                    
                    logger.warn("{}", StructuredLog.builder()
                        .level("WARN")
                        .message("Order creation failed")
                        .field("errorCode", problem.code())
                        .field("duration", duration.toMillis())
                        .build());
                    
                    metrics.incrementCounter("order.created", Map.of("status", "failure"));
                    Metrics.recordRequest(metrics, "CreateOrder", duration, false);
                    
                    span.setStatus("ERROR");
                    span.setAttribute("errorCode", problem.code());
                    
                    return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(problem);
                });
                
        } catch (Exception ex) {
            // 7. Exception: logs, métricas, span
            logger.error("{}", StructuredLog.error("Order creation exception", ex));
            
            metrics.incrementCounter("order.error", Map.of("type", ex.getClass().getSimpleName()));
            
            span.setStatus("ERROR");
            span.recordException(ex);
            
            throw ex;
            
        } finally {
            // 8. Cleanup
            span.end();
            RequestContext.clear();
        }
    }
}
```

---

## Kubernetes Integration

### Probes Configuration

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: myapp
spec:
  containers:
  - name: myapp
    image: myapp:1.0
    ports:
    - containerPort: 8080
    
    # Liveness Probe - App está viva?
    livenessProbe:
      httpGet:
        path: /health/liveness
        port: 8080
      initialDelaySeconds: 30
      periodSeconds: 10
      timeoutSeconds: 5
      failureThreshold: 3
    
    # Readiness Probe - App está pronta para receber tráfego?
    readinessProbe:
      httpGet:
        path: /health/readiness
        port: 8080
      initialDelaySeconds: 10
      periodSeconds: 5
      timeoutSeconds: 3
      failureThreshold: 2
```

---

## Ver Também

- [commons-adapters-otel](../adapters/otel.md) - OpenTelemetry integration
- [commons-adapters-metrics-prometheus](../adapters/prometheus.md) - Prometheus metrics
- [commons-adapters-tracing-jaeger](../adapters/jaeger.md) - Jaeger tracing
