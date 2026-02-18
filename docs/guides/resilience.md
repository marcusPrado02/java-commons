# Guia: Resiliência com commons-app-resilience

## Visão Geral

O módulo `commons-app-resilience` fornece padrões de resiliência framework-agnostic para operações de I/O: Circuit Breaker, Retry, Timeout, Bulkhead, Rate Limiter e Cache.

## 📦 Instalação

```xml
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-app-resilience</artifactId>
    <version>${commons.version}</version>
</dependency>

<!-- Implementação (opcional) -->
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-adapters-resilience4j</artifactId>
</dependency>
```

---

## 🏗️ Arquitetura

### ResiliencePolicySet

Container imutável de políticas a serem aplicadas:

```java
public record ResiliencePolicySet(
    CachePolicy cachePolicy,
    TimeoutPolicy timeoutPolicy,
    CircuitBreakerPolicy circuitBreakerPolicy,
    BulkheadPolicy bulkheadPolicy,
    RateLimiterPolicy rateLimiterPolicy,
    RetryPolicy retryPolicy
) {}
```

### ResilienceExecutor

Executa operações aplicando as políticas:

```java
public interface ResilienceExecutor {
    <T> T supply(String operationName, 
                 ResiliencePolicySet policies, 
                 Supplier<T> supplier);
    
    <T> T supply(String operationName, 
                 ResiliencePolicySet policies, 
                 Supplier<T> supplier,
                 FallbackStrategy<T> fallback);
}
```

---

## 🔄 Circuit Breaker

Protege contra falhas em cascata abrindo o circuito quando muitas falhas ocorrem.

### Estados

```
CLOSED → OPEN → HALF_OPEN → CLOSED
   ↓        ↓        ↓
 Normal  Rejeitando  Testando
```

### Configuração

```java
CircuitBreakerPolicy policy = new CircuitBreakerPolicy(
    50.0f,   // failureRateThreshold (50%)
    20       // minimumNumberOfCalls
);

// Abre o circuito se:
// - Pelo menos 20 chamadas foram feitas
// - Taxa de falha >= 50%
```

### Exemplo Básico

```java
public class PaymentService {
    
    private final ResilienceExecutor resilience;
    private final PaymentGatewayClient gatewayClient;
    
    public PaymentResult processPayment(Payment payment) {
        CircuitBreakerPolicy cbPolicy = new CircuitBreakerPolicy(50.0f, 10);
        
        ResiliencePolicySet policies = new ResiliencePolicySet(
            null, null, cbPolicy, null, null, null
        );
        
        return resilience.supply(
            "payment.process",
            policies,
            () -> gatewayClient.charge(payment)
        );
    }
}
```

### Com Fallback

```java
public PaymentResult processPayment(Payment payment) {
    return resilience.supply(
        "payment.process",
        policies,
        () -> gatewayClient.charge(payment),
        FallbackStrategy.value(PaymentResult.pending())  // Fallback
    );
}
```

### Monitoramento

```java
// Registra métricas do circuit breaker
resilience.supply("payment.process", policies, () -> {
    // ... operação
}).onFailure(ex -> {
    if (ex instanceof CircuitBreakerOpenException) {
        metrics.incrementCounter("circuit_breaker.open", 
            Map.of("operation", "payment.process"));
    }
});
```

---

## ⏱️ Timeout

Limita tempo máximo de execução de uma operação.

### Configuração

```java
TimeoutPolicy timeout = new TimeoutPolicy(
    Duration.ofSeconds(5)  // Máximo 5 segundos
);

ResiliencePolicySet policies = new ResiliencePolicySet(
    null, timeout, null, null, null, null
);
```

### Exemplo

```java
public class UserService {
    
    public User fetchUserFromExternalApi(String userId) {
        TimeoutPolicy timeout = new TimeoutPolicy(Duration.ofSeconds(3));
        
        ResiliencePolicySet policies = new ResiliencePolicySet(
            null, timeout, null, null, null, null
        );
        
        try {
            return resilience.supply(
                "user.fetch",
                policies,
                () -> externalApiClient.getUser(userId)
            );
        } catch (TimeoutException ex) {
            logger.warn("User fetch timed out after 3s: {}", userId);
            throw new UserFetchException("Timeout fetching user", ex);
        }
    }
}
```

### Timeout + Fallback

```java
User user = resilience.supply(
    "user.fetch",
    policies,
    () -> externalApiClient.getUser(userId),
    FallbackStrategy.value(User.anonymous())  // Usuário anônimo se timeout
);
```

---

## 🔁 Retry

Tenta novamente operações que falharam.

### Configuração

```java
RetryPolicy retry = new RetryPolicy(
    3,                           // maxAttempts: até 3 tentativas
    Duration.ofMillis(500),      // initialBackoff: espera inicial 500ms
    2.0f                         // backoffMultiplier: dobra a cada tentativa
);

// Tentativas:
// 1ª falha → espera 500ms → 2ª tentativa
// 2ª falha → espera 1000ms (500 * 2) → 3ª tentativa
// 3ª falha → desiste
```

### Exemplo

```java
public class OrderService {
    
    public Order fetchOrder(OrderId orderId) {
        RetryPolicy retry = new RetryPolicy(3, Duration.ofMillis(200), 2.0f);
        
        ResiliencePolicySet policies = new ResiliencePolicySet(
            null, null, null, null, null, retry
        );
        
        return resilience.supply(
            "order.fetch",
            policies,
            () -> orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId))
        );
    }
}
```

### Retry Condicional

```java
public class EmailService {
    
    public void sendEmail(Email email) {
        RetryPolicy retry = RetryPolicy.builder()
            .maxAttempts(5)
            .initialBackoff(Duration.ofSeconds(1))
            .backoffMultiplier(2.0f)
            .retryableExceptions(List.of(
                NetworkException.class,
                TemporaryServerException.class
            ))
            .build();
        
        resilience.supply("email.send", policies, () -> {
            emailClient.send(email);
            return null;
        });
    }
}
```

---

## 🚧 Bulkhead

Limita concorrência para evitar esgotamento de recursos.

### Configuração

```java
BulkheadPolicy bulkhead = BulkheadPolicy.of(
    10,                        // maxConcurrentCalls: máximo 10 simultâneas
    Duration.ofMillis(500)     // maxWaitDuration: espera até 500ms por slot
);

// Se 11ª requisição chegar:
// - Espera até 500ms por um slot livre
// - Se não liberar, rejeita com BulkheadFullException
```

### Exemplo

```java
public class ReportService {
    
    // Relatórios consomem muita CPU/memória
    // Limita a 5 relatórios simultâneos
    private static final BulkheadPolicy BULKHEAD = 
        BulkheadPolicy.of(5, Duration.ofSeconds(2));
    
    public Report generateReport(ReportRequest request) {
        ResiliencePolicySet policies = new ResiliencePolicySet(
            null, null, null, BULKHEAD, null, null
        );
        
        return resilience.supply(
            "report.generate",
            policies,
            () -> reportGenerator.generate(request)
        );
    }
}
```

### Bulkhead por Recurso

```java
public class MultiTenantService {
    
    // Diferentes bulkheads por tenant
    private final Map<TenantId, BulkheadPolicy> bulkheadsByTenant = new ConcurrentHashMap<>();
    
    public Data processData(TenantId tenantId, Data data) {
        BulkheadPolicy bulkhead = bulkheadsByTenant.computeIfAbsent(
            tenantId,
            tid -> BulkheadPolicy.of(10, Duration.ofMillis(100))
        );
        
        ResiliencePolicySet policies = new ResiliencePolicySet(
            null, null, null, bulkhead, null, null
        );
        
        return resilience.supply(
            "data.process." + tenantId.value(),
            policies,
            () -> dataProcessor.process(data)
        );
    }
}
```

---

## ⏳ Rate Limiter

Limita taxa de requisições por janela de tempo.

### Configuração

```java
RateLimiterPolicy rateLimiter = new RateLimiterPolicy(
    100,                          // limitForPeriod: 100 requisições
    Duration.ofMinutes(1),        // limitRefreshPeriod: por minuto
    Duration.ofMillis(500)        // timeoutDuration: espera até 500ms
);

// Permite 100 requisições/minuto
// Se exceder, espera até 500ms ou rejeita
```

### Exemplo - API Externa

```java
public class GeocodingService {
    
    // API Gratuita: máximo 60 req/min
    private static final RateLimiterPolicy RATE_LIMITER = 
        new RateLimiterPolicy(60, Duration.ofMinutes(1), Duration.ZERO);
    
    public Coordinates geocode(Address address) {
        ResiliencePolicySet policies = new ResiliencePolicySet(
            null, null, null, null, RATE_LIMITER, null
        );
        
        try {
            return resilience.supply(
                "geocoding.geocode",
                policies,
                () -> geocodingClient.geocode(address)
            );
        } catch (RateLimitExceededException ex) {
            logger.warn("Rate limit exceeded for geocoding");
            return Coordinates.unknown();
        }
    }
}
```

### Rate Limiter por Usuário

```java
public class ApiGateway {
    
    private final Map<UserId, RateLimiterPolicy> limitsByUser = new ConcurrentHashMap<>();
    
    public Response handleRequest(UserId userId, Request request) {
        RateLimiterPolicy rateLimiter = limitsByUser.computeIfAbsent(
            userId,
            uid -> new RateLimiterPolicy(
                100,                        // 100 req/min por usuário
                Duration.ofMinutes(1),
                Duration.ZERO
            )
        );
        
        ResiliencePolicySet policies = new ResiliencePolicySet(
            null, null, null, null, rateLimiter, null
        );
        
        return resilience.supply(
            "api.request." + userId.value(),
            policies,
            () -> processRequest(request)
        );
    }
}
```

---

## 💾 Cache

Cache de resultados de operações.

### Configuração

```java
CachePolicy cache = new CachePolicy(
    Duration.ofMinutes(5),  // ttl: cache válido por 5 minutos
    1000                    // maxSize: máximo 1000 entradas
);
```

### Exemplo

```java
public class ProductService {
    
    private static final CachePolicy CACHE = 
        new CachePolicy(Duration.ofMinutes(10), 1000);
    
    public Product getProduct(ProductId productId) {
        ResiliencePolicySet policies = new ResiliencePolicySet(
            CACHE, null, null, null, null, null
        );
        
        return resilience.supplyCached(
            "product.get." + productId.value(),  // Cache key
            policies,
            () -> productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId))
        );
    }
}
```

### Cache Refresh

```java
public void refreshProductCache(ProductId productId) {
    // Invalida cache
    resilience.evictFromCache("product.get." + productId.value());
    
    // Próxima chamada vai buscar do repositório
    Product product = getProduct(productId);
}
```

---

## 🎯 Combinando Políticas

### Stack Completo

```java
public class OrderService {
    
    private final ResilienceExecutor resilience;
    private final OrderApiClient orderClient;
    
    public Order fetchOrder(OrderId orderId) {
        // Define todas as políticas
        ResiliencePolicySet policies = new ResiliencePolicySet(
            new CachePolicy(Duration.ofMinutes(5), 1000),        // Cache 5min
            new TimeoutPolicy(Duration.ofSeconds(10)),           // Timeout 10s
            new CircuitBreakerPolicy(50.0f, 20),                // CB 50% / 20 calls
            BulkheadPolicy.of(50, Duration.ofMillis(100)),      // Max 50 concurrent
            new RateLimiterPolicy(1000, Duration.ofMinutes(1), Duration.ZERO),  // 1000/min
            new RetryPolicy(3, Duration.ofMillis(500), 2.0f)    // 3 retries, backoff
        );
        
        return resilience.supply(
            "order.fetch." + orderId.value(),
            policies,
            () -> orderClient.getOrder(orderId),
            FallbackStrategy.value(Order.notFound(orderId))
        );
    }
}
```

### Ordem de Aplicação

As políticas são aplicadas na seguinte ordem (de fora para dentro):

```
1. Cache (se hit, retorna imediatamente)
2. Rate Limiter
3. Bulkhead
4. Circuit Breaker
5. Timeout
6. Retry
   └─> Operação Real
```

### Exemplo Prático

```java
// 1. Verifica cache → HIT? retorna
// 2. Rate limiter → Excedeu? rejeita
// 3. Bulkhead → Cheio? espera ou rejeita
// 4. Circuit breaker → Aberto? rejeita
// 5. Timeout → Inicia cronômetro
// 6. Retry → Executa com retries
//    └─> Chamada HTTP para API externa
```

---

## 📊 Métricas e Monitoramento

### Integração com Observabilidade

```java
public class ResilientUserService {
    
    private final ResilienceExecutor resilience;
    private final MetricsFacade metrics;
    private final UserApiClient userClient;
    
    public User getUser(UserId userId) {
        Instant start = Instant.now();
        String operation = "user.get";
        
        try {
            User user = resilience.supply(
                operation,
                createPolicies(),
                () -> userClient.getUser(userId),
                FallbackStrategy.throwing()
            );
            
            // Sucesso
            recordMetrics(operation, start, "success", null);
            return user;
            
        } catch (CircuitBreakerOpenException ex) {
            recordMetrics(operation, start, "circuit_breaker_open", ex);
            throw ex;
            
        } catch (RateLimitExceededException ex) {
            recordMetrics(operation, start, "rate_limit_exceeded", ex);
            throw ex;
            
        } catch (BulkheadFullException ex) {
            recordMetrics(operation, start, "bulkhead_full", ex);
            throw ex;
            
        } catch (TimeoutException ex) {
            recordMetrics(operation, start, "timeout", ex);
            throw ex;
            
        } catch (Exception ex) {
            recordMetrics(operation, start, "failure", ex);
            throw ex;
        }
    }
    
    private void recordMetrics(String operation, Instant start, 
                              String outcome, Exception error) {
        Duration duration = Duration.between(start, Instant.now());
        
        metrics.incrementCounter(
            operation + ".calls",
            Map.of("outcome", outcome)
        );
        
        metrics.recordHistogram(
            operation + ".duration",
            duration.toMillis(),
            Map.of("outcome", outcome)
        );
        
        if (error != null) {
            metrics.incrementCounter(
                operation + ".errors",
                Map.of("type", error.getClass().getSimpleName())
            );
        }
    }
}
```

---

## 🏭 Padrões Avançados

### Fallback Hierarchy

```java
public class WeatherService {
    
    public Weather getWeather(Location location) {
        return resilience.supply(
            "weather.get",
            policies,
            () -> primaryWeatherApi.getWeather(location),
            // Fallback 1: Tenta API secundária
            FallbackStrategy.fallbackTo(() -> 
                backupWeatherApi.getWeather(location)
            )
            // Fallback 2: Retorna cache antigo
            .orElse(() -> weatherCache.getStale(location))
            // Fallback 3: Retorna valor padrão
            .orElse(Weather.unknown())
        );
    }
}
```

### Conditional Retry

```java
public class SmartRetryService {
    
    public Data fetchData() {
        RetryPolicy retry = RetryPolicy.builder()
            .maxAttempts(5)
            .initialBackoff(Duration.ofSeconds(1))
            .backoffMultiplier(2.0f)
            // Só retry em erros temporários
            .retryPredicate(ex -> {
                if (ex instanceof HttpException httpEx) {
                    int status = httpEx.getStatusCode();
                    // Retry: 408, 429, 500, 502, 503, 504
                    return status == 408 || status == 429 || 
                           (status >= 500 && status <= 504);
                }
                return ex instanceof IOException;  // Problemas de rede
            })
            .build();
        
        // ... usa retry policy
    }
}
```

### Circuit Breaker por Tenant

```java
public class MultiTenantOrderService {
    
    private final Map<TenantId, CircuitBreakerPolicy> cbByTenant = 
        new ConcurrentHashMap<>();
    
    public Order createOrder(TenantId tenantId, CreateOrderCommand cmd) {
        CircuitBreakerPolicy cb = cbByTenant.computeIfAbsent(
            tenantId,
            tid -> new CircuitBreakerPolicy(50.0f, 10)
        );
        
        ResiliencePolicySet policies = new ResiliencePolicySet(
            null, null, cb, null, null, null
        );
        
        return resilience.supply(
            "order.create." + tenantId.value(),
            policies,
            () -> orderService.create(cmd)
        );
    }
}
```

---

## Testing

### Simulando Falhas

```java
@Test
void shouldOpenCircuitBreakerAfterConsecutiveFailures() {
    // Given
    CircuitBreakerPolicy cb = new CircuitBreakerPolicy(50.0f, 5);
    ResiliencePolicySet policies = new ResiliencePolicySet(
        null, null, cb, null, null, null
    );
    
    // When - Gera falhas
    for (int i = 0; i < 10; i++) {
        try {
            resilience.supply("test.operation", policies, () -> {
                throw new RuntimeException("Simulated failure");
            });
        } catch (Exception ignored) {}
    }
    
    // Then - Circuit breaker deve estar aberto
    assertThatThrownBy(() -> {
        resilience.supply("test.operation", policies, () -> "success");
    }).isInstanceOf(CircuitBreakerOpenException.class);
}

@Test
void shouldRetryThreeTimes() {
    // Given
    AtomicInteger attempts = new AtomicInteger(0);
    RetryPolicy retry = new RetryPolicy(3, Duration.ofMillis(10), 1.0f);
    
    ResiliencePolicySet policies = new ResiliencePolicySet(
        null, null, null, null, null, retry
    );
    
    // When
    try {
        resilience.supply("test.retry", policies, () -> {
            attempts.incrementAndGet();
            throw new RuntimeException("Fail");
        });
    } catch (Exception ignored) {}
    
    // Then
    assertThat(attempts.get()).isEqualTo(3);  // 3 tentativas
}
```

---

## Best Practices

### ✅ DO

```java
// ✅ Use nomes de operação significativos
resilience.supply("order.create", policies, supplier);

// ✅ Configure políticas adequadas ao tipo de operação
// I/O externa: timeout, retry, circuit breaker
// CPU-bound: bulkhead
// APIs de terceiros: rate limiter

// ✅ Monitore métricas
metrics.incrementCounter("resilience.circuit_breaker.open");

// ✅ Forneça fallbacks razoáveis
FallbackStrategy.value(Order.notFound())
```

### ❌ DON'T

```java
// ❌ NÃO use timeout muito longo
new TimeoutPolicy(Duration.ofMinutes(5));  // Muito tempo!

// ❌ NÃO retry em erros de validação (4xx)
retry.retryOn(ValidationException.class);  // Não faz sentido

// ❌ NÃO abuse de circuit breaker em operações locais
// CB é para falhas externas (APIs, DB), não lógica local

// ❌ NÃO ignore exceptions de resiliência
try {
    result = resilience.supply(...);
} catch (Exception ignored) {}  // ❌ Não ignore!
```

---

## Ver Também

- [commons-adapters-resilience4j](../adapters/resilience4j.md) - Implementação Resilience4j
- [commons-app-observability](observability.md) - Métricas e logs
- [Padrão Circuit Breaker](https://martinfowler.com/bliki/CircuitBreaker.html)
