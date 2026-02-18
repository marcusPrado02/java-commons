# API Reference: API Gateway

## Visão Geral

`commons-app-api-gateway` fornece padrões e componentes para construir API Gateways com roteamento, autenticação, rate limiting, agregação e transformação de requisições.

**Quando usar:**
- Single entry point para microservices
- Agregar múltiplas chamadas de serviços
- Autenticação e autorização centralizadas
- Rate limiting e throttling
- Request/Response transformation
- Service discovery integration

---

## 📦 Instalação

```xml
<dependency>
    <groupId>com.marcusprado02.commons</groupId>
    <artifactId>commons-app-api-gateway</artifactId>
    <version>${commons.version}</version>
</dependency>
```

---

## 🔑 Core Components

### Route

Define rota para upstream service.

```java
public class Route {
    
    private final RouteId id;
    private final String path;              // /api/orders/**
    private final String upstream;          // http://order-service:8080
    private final List<String> methods;     // GET, POST
    private final List<Filter> filters;
    private final RouteConfiguration config;
    
    public boolean matches(String requestPath, String method) {
        return pathMatcher.match(path, requestPath)
            && (methods.isEmpty() || methods.contains(method));
    }
}
```

### GatewayFilter

Processa requisição antes/depois de upstream.

```java
public interface GatewayFilter {
    
    /**
     * Ordem de execução (menor = primeiro).
     */
    int getOrder();
    
    /**
     * Processa requisição.
     */
    Result<GatewayResponse> filter(
        GatewayRequest request,
        FilterChain chain
    );
}
```

### RouteResolver

Resolve rota para requisição.

```java
public interface RouteResolver {
    
    /**
     * Encontra rota para requisição.
     */
    Optional<Route> resolve(GatewayRequest request);
    
    /**
     * Lista todas as rotas.
     */
    List<Route> getAllRoutes();
}
```

---

## 💡 Basic Gateway

### Simple Gateway

```java
@RestController
public class ApiGatewayController {
    
    private final RouteResolver routeResolver;
    private final GatewayExecutor gatewayExecutor;
    
    @RequestMapping("/**")
    public ResponseEntity<?> gateway(HttpServletRequest request) {
        // Criar gateway request
        GatewayRequest gatewayRequest = GatewayRequest.from(request);
        
        // Resolver rota
        Optional<Route> route = routeResolver.resolve(gatewayRequest);
        
        if (route.isEmpty()) {
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Problem.of("ROUTE_NOT_FOUND", "No route found"));
        }
        
        // Executar com filters
        Result<GatewayResponse> result = gatewayExecutor.execute(
            gatewayRequest,
            route.get()
        );
        
        return result
            .map(this::toResponseEntity)
            .recover(problem -> ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(problem));
    }
    
    private ResponseEntity<?> toResponseEntity(GatewayResponse response) {
        return ResponseEntity
            .status(response.statusCode())
            .headers(response.headers())
            .body(response.body());
    }
}
```

### Route Configuration

```yaml
gateway:
  routes:
    - id: orders-service
      path: /api/orders/**
      upstream: http://order-service:8080
      methods: [GET, POST, PUT, DELETE]
      filters:
        - authentication
        - rate-limiting
        - logging
      config:
        timeout: 30s
        retry:
          max-attempts: 3
          backoff: 1s
    
    - id: users-service
      path: /api/users/**
      upstream: http://user-service:8080
      methods: [GET, POST]
      filters:
        - authentication
        - audit-log
      config:
        timeout: 10s
    
    - id: payments-service
      path: /api/payments/**
      upstream: http://payment-service:8080
      methods: [POST]
      filters:
        - authentication
        - idempotency
        - encryption
      config:
        timeout: 60s
        retry:
          max-attempts: 1
```

---

## 🛡️ Gateway Filters

### Authentication Filter

```java
@Component
@Order(1)  // Primeiro filtro
public class AuthenticationFilter implements GatewayFilter {
    
    private final JwtValidator jwtValidator;
    
    @Override
    public int getOrder() {
        return 1;
    }
    
    @Override
    public Result<GatewayResponse> filter(
        GatewayRequest request,
        FilterChain chain
    ) {
        // Extrair token
        Optional<String> token = extractToken(request);
        
        if (token.isEmpty()) {
            return Result.fail(Problem.of(
                "AUTH.TOKEN_MISSING",
                "Authorization token is required"
            ));
        }
        
        // Validar token
        Result<Claims> validation = jwtValidator.validate(token.get());
        
        if (validation.isFail()) {
            return Result.fail(Problem.of(
                "AUTH.INVALID_TOKEN",
                "Invalid or expired token"
            ));
        }
        
        // Adicionar claims ao request
        Claims claims = validation.getOrThrow();
        GatewayRequest enriched = request
            .withHeader("X-User-Id", claims.getSubject())
            .withHeader("X-User-Roles", String.join(",", claims.getRoles()));
        
        // Continuar chain
        return chain.filter(enriched);
    }
    
    private Optional<String> extractToken(GatewayRequest request) {
        return request.getHeader("Authorization")
            .filter(h -> h.startsWith("Bearer "))
            .map(h -> h.substring(7));
    }
}
```

### Rate Limiting Filter

```java
@Component
@Order(2)
public class RateLimitingFilter implements GatewayFilter {
    
    private final RateLimiter rateLimiter;
    
    @Override
    public int getOrder() {
        return 2;
    }
    
    @Override
    public Result<GatewayResponse> filter(
        GatewayRequest request,
        FilterChain chain
    ) {
        // Identificar cliente (por IP ou user ID)
        String clientId = request.getHeader("X-User-Id")
            .orElse(request.getRemoteAddr());
        
        // Verificar rate limit
        boolean allowed = rateLimiter.tryAcquire(
            clientId,
            1,  // 1 token
            Duration.ofSeconds(1)
        );
        
        if (!allowed) {
            return Result.fail(Problem.of(
                "RATE_LIMIT.EXCEEDED",
                "Rate limit exceeded. Try again later."
            ));
        }
        
        // Continuar
        return chain.filter(request);
    }
}
```

### Request Logging Filter

```java
@Component
@Order(100)  // Último filtro (antes de upstream)
public class RequestLoggingFilter implements GatewayFilter {
    
    private final StructuredLog log;
    private final MetricsFacade metrics;
    
    @Override
    public int getOrder() {
        return 100;
    }
    
    @Override
    public Result<GatewayResponse> filter(
        GatewayRequest request,
        FilterChain chain
    ) {
        Instant start = Instant.now();
        String correlationId = UUID.randomUUID().toString();
        
        log.info("Gateway request")
            .field("correlationId", correlationId)
            .field("method", request.method())
            .field("path", request.path())
            .field("remoteAddr", request.getRemoteAddr())
            .log();
        
        // Adicionar correlation ID
        GatewayRequest enriched = request
            .withHeader("X-Correlation-Id", correlationId);
        
        // Executar
        Result<GatewayResponse> result = chain.filter(enriched);
        
        // Log resultado
        Duration duration = Duration.between(start, Instant.now());
        
        if (result.isOk()) {
            GatewayResponse response = result.getOrThrow();
            
            log.info("Gateway response")
                .field("correlationId", correlationId)
                .field("statusCode", response.statusCode())
                .field("duration", duration.toMillis() + "ms")
                .log();
            
            metrics.recordTimer(
                "gateway.request.duration",
                duration,
                "method", request.method(),
                "path", request.path(),
                "status", String.valueOf(response.statusCode())
            );
        } else {
            log.warn("Gateway request failed")
                .field("correlationId", correlationId)
                .field("error", result.problemOrNull().detail())
                .field("duration", duration.toMillis() + "ms")
                .log();
            
            metrics.incrementCounter(
                "gateway.request.error",
                "method", request.method(),
                "path", request.path()
            );
        }
        
        return result;
    }
}
```

---

## 🔄 Request Aggregation

### Aggregator Pattern

```java
@Service
public class OrderAggregatorService {
    
    private final OrderServiceClient orderClient;
    private final UserServiceClient userClient;
    private final InventoryServiceClient inventoryClient;
    
    /**
     * Agrega dados de múltiplos serviços.
     */
    public CompletableFuture<Result<OrderAggregateResponse>> aggregateOrder(
        String orderId
    ) {
        // Buscar ordem
        CompletableFuture<Result<Order>> orderFuture = 
            orderClient.getOrder(orderId);
        
        // Paralelo: buscar customer e itens
        CompletableFuture<Result<OrderAggregateResponse>> aggregated = orderFuture
            .thenCompose(orderResult -> {
                if (orderResult.isFail()) {
                    return CompletableFuture.completedFuture(
                        Result.fail(orderResult.problemOrNull())
                    );
                }
                
                Order order = orderResult.getOrThrow();
                
                // Buscar customer
                CompletableFuture<Result<User>> userFuture = 
                    userClient.getUser(order.customerId());
                
                // Buscar inventory para cada item
                List<CompletableFuture<Result<InventoryInfo>>> inventoryFutures = 
                    order.items().stream()
                        .map(item -> inventoryClient.getInventory(item.productId()))
                        .toList();
                
                // Aguardar todos
                return CompletableFuture.allOf(
                    Stream.concat(
                        Stream.of(userFuture),
                        inventoryFutures.stream()
                    ).toArray(CompletableFuture[]::new)
                ).thenApply(v -> {
                    // Combinar resultados
                    User user = userFuture.join().getOrThrow();
                    List<InventoryInfo> inventory = inventoryFutures.stream()
                        .map(f -> f.join().getOrThrow())
                        .toList();
                    
                    return Result.ok(new OrderAggregateResponse(
                        order,
                        user,
                        inventory
                    ));
                });
            });
        
        return aggregated;
    }
}
```

### GraphQL-like Aggregation

```java
@RestController
@RequestMapping("/api/gateway/aggregate")
public class AggregationController {
    
    @PostMapping("/orders/{orderId}")
    public ResponseEntity<?> aggregateOrder(
        @PathVariable String orderId,
        @RequestBody AggregationRequest request
    ) {
        // Request especifica quais dados incluir
        CompletableFuture<Map<String, Object>> result = 
            CompletableFuture.completedFuture(new HashMap<>());
        
        // Order (sempre incluído)
        result = result.thenCombine(
            orderClient.getOrder(orderId),
            (map, order) -> {
                map.put("order", order);
                return map;
            }
        );
        
        // Customer (se solicitado)
        if (request.includes("customer")) {
            result = result.thenCombine(
                orderClient.getOrder(orderId)
                    .thenCompose(order -> 
                        userClient.getUser(order.customerId())),
                (map, customer) -> {
                    map.put("customer", customer);
                    return map;
                }
            );
        }
        
        // Inventory (se solicitado)
        if (request.includes("inventory")) {
            result = result.thenCombine(
                aggregateInventory(orderId),
                (map, inventory) -> {
                    map.put("inventory", inventory);
                    return map;
                }
            );
        }
        
        return ResponseEntity.ok(
            result.join()
        );
    }
}
```

---

## 🔌 Service Discovery

### Consul Integration

```java
@Component
public class ConsulServiceDiscovery implements ServiceDiscovery {
    
    private final ConsulClient consulClient;
    
    @Override
    public Optional<ServiceInstance> discover(String serviceName) {
        try {
            // Buscar instâncias saudáveis
            List<HealthService> healthServices = consulClient
                .getHealthServices(serviceName, true)
                .getValue();
            
            if (healthServices.isEmpty()) {
                return Optional.empty();
            }
            
            // Load balance (round-robin simples)
            HealthService selected = selectInstance(healthServices);
            
            ServiceInstance instance = new ServiceInstance(
                selected.getService().getId(),
                serviceName,
                selected.getService().getAddress(),
                selected.getService().getPort()
            );
            
            return Optional.of(instance);
            
        } catch (Exception e) {
            log.error("Service discovery failed", e).log();
            return Optional.empty();
        }
    }
    
    private HealthService selectInstance(List<HealthService> services) {
        // Round-robin simples
        int index = counter.getAndIncrement() % services.size();
        return services.get(index);
    }
}
```

### Dynamic Routing

```java
@Component
public class DynamicRouteResolver implements RouteResolver {
    
    private final ServiceDiscovery serviceDiscovery;
    private final RouteRepository routeRepository;
    
    @Override
    public Optional<Route> resolve(GatewayRequest request) {
        // Buscar rota configurada
        Optional<Route> staticRoute = routeRepository.findByPath(
            request.path()
        );
        
        if (staticRoute.isEmpty()) {
            return Optional.empty();
        }
        
        Route route = staticRoute.get();
        
        // Resolver upstream via service discovery
        Optional<ServiceInstance> instance = 
            serviceDiscovery.discover(route.serviceName());
        
        if (instance.isEmpty()) {
            return Optional.empty();
        }
        
        // Atualizar upstream dinamicamente
        ServiceInstance service = instance.get();
        String upstream = String.format(
            "http://%s:%d",
            service.getHost(),
            service.getPort()
        );
        
        return Optional.of(route.withUpstream(upstream));
    }
}
```

---

## 🧪 Testing

### Gateway Integration Tests

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class ApiGatewayIntegrationTest {
    
    @LocalServerPort
    private int port;
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    private MockWebServer orderService;
    
    @BeforeEach
    void setUp() throws IOException {
        orderService = new MockWebServer();
        orderService.start();
    }
    
    @AfterEach
    void tearDown() throws IOException {
        orderService.shutdown();
    }
    
    @Test
    void shouldProxyRequestToUpstream() {
        // Given: Mock upstream response
        orderService.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody("{\"orderId\": \"order-123\"}")
            .setHeader("Content-Type", "application/json"));
        
        // Configure route to mock server
        configureRoute("/api/orders/**", orderService.url("/").toString());
        
        // When: Make request through gateway
        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/api/orders/123",
            String.class
        );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("order-123");
    }
    
    @Test
    void shouldApplyFilters() {
        // Given
        orderService.enqueue(new MockResponse().setResponseCode(200));
        
        // When: Request without auth token
        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/api/orders/123",
            String.class
        );
        
        // Then: Should be rejected by authentication filter
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
```

---

## Best Practices

### ✅ DO

```java
// ✅ Use circuit breaker para upstream services
@CircuitBreaker(name = "order-service")
public Result<Order> getOrder(String id) { ... }

// ✅ Configure timeouts apropriados
config.timeout(Duration.ofSeconds(30));

// ✅ Implemente retry com backoff
config.retry(3, Duration.ofSeconds(1), 2.0);

// ✅ Log todas as requisições com correlation ID
request.withHeader("X-Correlation-Id", UUID.randomUUID());

// ✅ Cache respostas quando apropriado
@Cacheable("orders")
public Result<Order> getOrder(String id) { ... }

// ✅ Use service discovery para routing dinâmico
Optional<ServiceInstance> instance = discovery.discover(serviceName);
```

### ❌ DON'T

```java
// ❌ NÃO faça agregação síncrona sequencial
Order order = orderClient.getOrder(id).block();  // ❌ Bloqueante
User user = userClient.getUser(order.customerId()).block();  // ❌

// ❌ NÃO exponha erros internos de upstream
catch (Exception e) {
    return ResponseEntity.internalServerError()
        .body(e.getMessage());  // ❌ Expõe detalhes internos
}

// ❌ NÃO ignore rate limiting
// Implementar sempre para proteger upstream services

// ❌ NÃO hardcode upstream URLs
String upstream = "http://localhost:8080";  // ❌ Use service discovery

// ❌ NÃO esqueça circuit breaker
// Sem circuit breaker, falhas em cascade podem derrubar tudo
```

---

## Ver Também

- [Resilience Guide](../guides/resilience.md) - Circuit breaker, retry
- [API Versioning](app-api-versioning.md) - API lifecycle
- [Rate Limiting](app-api-rate-limiting.md) - Throttling
- [Service Discovery](adapters/service-discovery.md) - Consul, Eureka
